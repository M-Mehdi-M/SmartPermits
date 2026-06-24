from flask_sqlalchemy import SQLAlchemy
from datetime import datetime, timedelta

db = SQLAlchemy()


class User(db.Model):
    __tablename__ = 'users'
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    email = db.Column(db.String(120), unique=True, nullable=False)
    password_hash = db.Column(db.String(200), nullable=False)
    role = db.Column(db.String(20), nullable=False, default='citizen')
    full_name = db.Column(db.String(150), nullable=False, default='')
    avatar_url = db.Column(db.String(300), nullable=True, default='')
    fcm_token = db.Column(db.String(500), nullable=True, default='')
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    permits = db.relationship('Permit', backref='applicant', lazy=True, foreign_keys='Permit.user_id')

    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username,
            'email': self.email,
            'role': self.role,
            'full_name': self.full_name,
            'avatar_url': self.avatar_url or '',
            'created_at': self.created_at.isoformat()
        }


PERMIT_VALIDITY_DAYS = {
    'Construction Permit': 365,
    'Renovation Permit': 180,
    'Business License': 365,
    'Food Service Permit': 365,
    'Event Permit': 30,
    'Signage Permit': 730,
    'Demolition Permit': 180,
    'Occupancy Certificate': 0,
}


class Permit(db.Model):
    __tablename__ = 'permits'
    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    permit_type = db.Column(db.String(100), nullable=False)
    description = db.Column(db.Text, nullable=True)
    status = db.Column(db.String(30), nullable=False, default='submitted')
    fee_amount = db.Column(db.Float, default=0.0)
    is_paid = db.Column(db.Boolean, default=False)
    reviewer_notes = db.Column(db.Text, nullable=True)
    reviewed_by = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=True)
    renewed_from = db.Column(db.Integer, nullable=True)
    latitude = db.Column(db.Float, nullable=True)
    longitude = db.Column(db.Float, nullable=True)
    deleted_at = db.Column(db.DateTime, nullable=True, default=None)
    ai_analysis = db.Column(db.Text, nullable=True, default=None)
    ai_analysis_lang = db.Column(db.String(10), nullable=True, default=None)
    blockchain_hash = db.Column(db.String(66), nullable=True, default=None)
    blockchain_tx_hash = db.Column(db.String(70), nullable=True, default=None)
    blockchain_error = db.Column(db.String(500), nullable=True, default=None)
    expires_at = db.Column(db.DateTime, nullable=True, default=None)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    documents = db.relationship('Document', backref='permit', lazy=True)
    comments = db.relationship('Comment', backref='permit', lazy=True, order_by='Comment.created_at')
    appointments = db.relationship('Appointment', backref='permit', lazy=True)
    events = db.relationship('PermitEvent', backref='permit', lazy=True, order_by='PermitEvent.created_at')

    def compute_predicted_wait(self):
        if self.status != 'submitted':
            return None, None, None
        try:
            from sqlalchemy import func

            base_result = db.session.query(
                func.avg(
                    (func.julianday(Permit.updated_at) - func.julianday(Permit.created_at)) * 24
                ),
                func.count(Permit.id)
            ).filter(
                Permit.permit_type == self.permit_type,
                Permit.status.in_(['approved', 'rejected', 'completed']),
                Permit.updated_at.isnot(None),
                Permit.created_at.isnot(None)
            ).first()

            if not base_result or not base_result[0] or base_result[0] <= 0:
                return None, None, None

            avg_hours = float(base_result[0])
            sample_count = int(base_result[1])

            pending_count = Permit.query.filter_by(status='submitted').count()
            queue_factor = 1.0 + (pending_count * 0.05)

            doc_count = len(self.documents) if self.documents else 0
            doc_factor = 1.0 + (max(0, doc_count - 3) * 0.03)

            now = datetime.utcnow()
            day_of_week = now.weekday()
            day_factor = 1.15 if day_of_week >= 4 else 1.0

            predicted = avg_hours * queue_factor * doc_factor * day_factor

            variance = 0.3 if sample_count < 5 else 0.2 if sample_count < 15 else 0.12
            low = predicted * (1.0 - variance)
            high = predicted * (1.0 + variance)

            confidence = min(95, 50 + sample_count * 3)

            def format_time(h):
                if h < 1:
                    return "< 1 hour"
                if h < 24:
                    return f"{int(h)} hours"
                days = h / 24
                if days < 1.5:
                    return "1 day"
                return f"{round(days, 1)} days"

            est_text = f"{format_time(low)} - {format_time(high)}"
            return est_text, confidence, format_time(predicted)
        except Exception:
            return None, None, None

    def to_dict(self):
        est_text, confidence, point_est = self.compute_predicted_wait()

        days_to_expiry = None
        is_expired = False
        if self.expires_at:
            delta = (self.expires_at - datetime.utcnow()).days
            days_to_expiry = max(0, delta)
            is_expired = delta < 0

        reviewer_name = None
        if self.reviewed_by:
            r = User.query.get(self.reviewed_by)
            if r:
                reviewer_name = r.full_name

        timeline = []
        if self.events:
            for ev in self.events:
                timeline.append(ev.to_dict())

        # Expose the newest comment so clients can detect new messages via polling
        # (a reliable fallback for the real-time 'comment_notification' socket event).
        last_comment_id = 0
        last_comment_user_id = None
        if self.comments:
            last_comment = self.comments[-1]
            last_comment_id = last_comment.id
            last_comment_user_id = last_comment.user_id

        return {
            'id': self.id,
            'user_id': self.user_id,
            'applicant_name': self.applicant.full_name if self.applicant else '',
            'permit_type': self.permit_type,
            'description': self.description,
            'status': self.status,
            'fee_amount': self.fee_amount,
            'is_paid': self.is_paid,
            'reviewer_notes': self.reviewer_notes,
            'reviewed_by': self.reviewed_by,
            'reviewer_name': reviewer_name,
            'renewed_from': self.renewed_from,
            'latitude': self.latitude,
            'longitude': self.longitude,
            'estimated_processing_time': est_text,
            'prediction_confidence': confidence,
            'point_estimate': point_est,
            'ai_analysis': self.ai_analysis,
            'ai_analysis_lang': self.ai_analysis_lang,
            'blockchain_hash': self.blockchain_hash,
            'blockchain_tx_hash': self.blockchain_tx_hash,
            'blockchain_error': self.blockchain_error,
            'expires_at': self.expires_at.isoformat() if self.expires_at else None,
            'days_to_expiry': days_to_expiry,
            'is_expired': is_expired,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None,
            'deleted_at': self.deleted_at.isoformat() if self.deleted_at else None,
            'days_until_permanent_delete': max(0, 30 - (datetime.utcnow() - self.deleted_at).days) if self.deleted_at else None,
            'documents': [d.to_dict() for d in self.documents],
            'timeline': timeline,
            'last_comment_id': last_comment_id,
            'last_comment_user_id': last_comment_user_id
        }


class PermitEvent(db.Model):
    __tablename__ = 'permit_events'
    id = db.Column(db.Integer, primary_key=True)
    permit_id = db.Column(db.Integer, db.ForeignKey('permits.id'), nullable=False)
    event_type = db.Column(db.String(50), nullable=False)
    actor_name = db.Column(db.String(150), nullable=True, default='')
    actor_role = db.Column(db.String(20), nullable=True, default='')
    notes = db.Column(db.Text, nullable=True, default='')
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'permit_id': self.permit_id,
            'event_type': self.event_type,
            'actor_name': self.actor_name or '',
            'actor_role': self.actor_role or '',
            'notes': self.notes or '',
            'created_at': self.created_at.isoformat()
        }


class Document(db.Model):
    __tablename__ = 'documents'
    id = db.Column(db.Integer, primary_key=True)
    permit_id = db.Column(db.Integer, db.ForeignKey('permits.id'), nullable=False)
    file_path = db.Column(db.String(300), nullable=False)
    file_name = db.Column(db.String(200), nullable=False)
    document_label = db.Column(db.String(200), nullable=True, default='')
    uploaded_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'permit_id': self.permit_id,
            'file_name': self.file_name,
            'document_label': self.document_label or '',
            'uploaded_at': self.uploaded_at.isoformat()
        }


class Comment(db.Model):
    __tablename__ = 'comments'
    id = db.Column(db.Integer, primary_key=True)
    permit_id = db.Column(db.Integer, db.ForeignKey('permits.id'), nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    message = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    author = db.relationship('User', lazy=True)

    def to_dict(self):
        return {
            'id': self.id,
            'permit_id': self.permit_id,
            'user_id': self.user_id,
            'author_name': self.author.full_name if self.author else '',
            'author_role': self.author.role if self.author else '',
            'message': self.message,
            'created_at': self.created_at.isoformat()
        }


class Appointment(db.Model):
    __tablename__ = 'appointments'
    id = db.Column(db.Integer, primary_key=True)
    permit_id = db.Column(db.Integer, db.ForeignKey('permits.id'), nullable=False)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    date = db.Column(db.String(20), nullable=False)
    time_slot = db.Column(db.String(20), nullable=False)
    status = db.Column(db.String(20), nullable=False, default='scheduled')
    notes = db.Column(db.Text, nullable=True)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'permit_id': self.permit_id,
            'user_id': self.user_id,
            'date': self.date,
            'time_slot': self.time_slot,
            'status': self.status,
            'notes': self.notes,
            'created_at': self.created_at.isoformat()
        }
