from flask_sqlalchemy import SQLAlchemy
from datetime import datetime

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
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    documents = db.relationship('Document', backref='permit', lazy=True)
    comments = db.relationship('Comment', backref='permit', lazy=True, order_by='Comment.created_at')
    appointments = db.relationship('Appointment', backref='permit', lazy=True)

    def to_dict(self):
        avg_time = None
        if self.status == 'submitted':
            try:
                from sqlalchemy import func
                result = db.session.query(
                    func.avg(
                        (func.julianday(Permit.updated_at) - func.julianday(Permit.created_at)) * 24
                    )
                ).filter(
                    Permit.permit_type == self.permit_type,
                    Permit.status.in_(['approved', 'rejected', 'completed']),
                    Permit.updated_at.isnot(None),
                    Permit.created_at.isnot(None)
                ).scalar()
                if result and result > 0:
                    avg_hours = float(result)
                    if avg_hours < 24:
                        avg_time = f"{int(avg_hours)} hours"
                    else:
                        avg_time = f"{round(avg_hours / 24, 1)} days"
            except Exception:
                avg_time = None

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
            'renewed_from': self.renewed_from,
            'latitude': self.latitude,
            'longitude': self.longitude,
            'estimated_processing_time': avg_time,
            'ai_analysis': self.ai_analysis,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None,
            'deleted_at': self.deleted_at.isoformat() if self.deleted_at else None,
            'days_until_permanent_delete': max(0, 30 - (datetime.utcnow() - self.deleted_at).days) if self.deleted_at else None,
            'documents': [d.to_dict() for d in self.documents]
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
