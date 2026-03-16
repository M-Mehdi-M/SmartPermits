import os
import io
from flask import Flask, request, jsonify, send_from_directory, send_file
from flask_cors import CORS
from flask_bcrypt import Bcrypt
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from werkzeug.utils import secure_filename
from models import db, User, Permit, Document, Comment, Appointment
from datetime import datetime, timedelta

app = Flask(__name__)
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///smartpermits.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['JWT_SECRET_KEY'] = 'smart-permits-secret-key-2026'
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(days=30)
app.config['UPLOAD_FOLDER'] = os.path.join(os.path.dirname(__file__), 'uploads')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024

CORS(app)
bcrypt = Bcrypt(app)
jwt = JWTManager(app)
db.init_app(app)

os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

firebase_app = None
try:
    import firebase_admin
    from firebase_admin import credentials, messaging
    cred_path = os.path.join(os.path.dirname(__file__), 'firebase-service-account.json')
    if os.path.exists(cred_path):
        cred = credentials.Certificate(cred_path)
        firebase_app = firebase_admin.initialize_app(cred)
except ImportError:
    pass


def send_push_notification(user_id, title, body, data=None):
    if firebase_app is None:
        return
    try:
        from firebase_admin import messaging
        user = User.query.get(user_id)
        if user and user.fcm_token:
            message = messaging.Message(
                notification=messaging.Notification(title=title, body=body),
                data=data or {},
                token=user.fcm_token,
            )
            messaging.send(message)
    except Exception:
        pass


FEE_TABLE = {
    'Business License': 150.0,
    'Construction Permit': 500.0,
    'Food Service Permit': 200.0,
    'Signage Permit': 75.0,
    'Event Permit': 100.0,
    'Renovation Permit': 350.0,
    'Demolition Permit': 450.0,
    'Occupancy Certificate': 120.0,
}


def seed_data():
    if User.query.filter_by(username='inspector1').first() is None:
        inspector = User(
            username='inspector1',
            email='inspector@city.gov',
            password_hash=bcrypt.generate_password_hash('1q2w3e4r').decode('utf-8'),
            role='inspector',
            full_name='John Inspector'
        )
        db.session.add(inspector)
    if User.query.filter_by(username='citizen1').first() is None:
        citizen = User(
            username='citizen1',
            email='citizen@email.com',
            password_hash=bcrypt.generate_password_hash('1q2w3e4r').decode('utf-8'),
            role='citizen',
            full_name='Maria Popescu'
        )
        db.session.add(citizen)
    db.session.commit()


def run_migrations():
    from sqlalchemy import text, inspect as sa_inspect
    insp = sa_inspect(db.engine)
    with db.engine.connect() as conn:
        user_cols = [c['name'] for c in insp.get_columns('users')]
        if 'avatar_url' not in user_cols:
            conn.execute(text("ALTER TABLE users ADD COLUMN avatar_url VARCHAR(300) DEFAULT ''"))
        if 'fcm_token' not in user_cols:
            conn.execute(text("ALTER TABLE users ADD COLUMN fcm_token VARCHAR(500) DEFAULT ''"))
        permit_cols = [c['name'] for c in insp.get_columns('permits')]
        if 'renewed_from' not in permit_cols:
            conn.execute(text("ALTER TABLE permits ADD COLUMN renewed_from INTEGER"))
        if 'deleted_at' not in permit_cols:
            conn.execute(text("ALTER TABLE permits ADD COLUMN deleted_at DATETIME"))
        if 'latitude' not in permit_cols:
            conn.execute(text("ALTER TABLE permits ADD COLUMN latitude FLOAT"))
        if 'longitude' not in permit_cols:
            conn.execute(text("ALTER TABLE permits ADD COLUMN longitude FLOAT"))
        doc_cols = [c['name'] for c in insp.get_columns('documents')]
        if 'document_label' not in doc_cols:
            conn.execute(text("ALTER TABLE documents ADD COLUMN document_label VARCHAR(200) DEFAULT ''"))
        conn.commit()


with app.app_context():
    db.create_all()
    run_migrations()
    seed_data()


@app.route('/api/auth/register', methods=['POST'])
def register():
    data = request.get_json()
    username = data.get('username', '').strip()
    email = data.get('email', '').strip()
    password = data.get('password', '')
    role = data.get('role', 'citizen')
    full_name = data.get('full_name', '').strip()
    if not username or not email or not password:
        return jsonify({'error': 'All fields are required'}), 400
    if User.query.filter((User.username == username) | (User.email == email)).first():
        return jsonify({'error': 'Username or email already exists'}), 409
    hashed = bcrypt.generate_password_hash(password).decode('utf-8')
    user = User(username=username, email=email, password_hash=hashed, role=role, full_name=full_name)
    db.session.add(user)
    db.session.commit()
    token = create_access_token(identity=str(user.id))
    return jsonify({'token': token, 'user': user.to_dict()}), 201


@app.route('/api/auth/login', methods=['POST'])
def login():
    data = request.get_json()
    username = data.get('username', '')
    password = data.get('password', '')
    user = User.query.filter_by(username=username).first()
    if user is None or not bcrypt.check_password_hash(user.password_hash, password):
        return jsonify({'error': 'Invalid credentials'}), 401
    token = create_access_token(identity=str(user.id))
    return jsonify({'token': token, 'user': user.to_dict()}), 200


@app.route('/api/auth/change-password', methods=['POST'])
@jwt_required()
def change_password():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    data = request.get_json()
    current = data.get('current_password', '')
    new_pass = data.get('new_password', '')
    if not bcrypt.check_password_hash(user.password_hash, current):
        return jsonify({'error': 'Current password is incorrect'}), 400
    if len(new_pass) < 6:
        return jsonify({'error': 'New password must be at least 6 characters'}), 400
    user.password_hash = bcrypt.generate_password_hash(new_pass).decode('utf-8')
    db.session.commit()
    return jsonify({'message': 'Password changed successfully'}), 200


@app.route('/api/auth/fcm-token', methods=['POST'])
@jwt_required()
def save_fcm_token():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    data = request.get_json()
    user.fcm_token = data.get('fcm_token', '')
    db.session.commit()
    return jsonify({'message': 'Token saved'}), 200


@app.route('/api/permits', methods=['GET'])
@jwt_required()
def get_my_permits():
    user_id = int(get_jwt_identity())
    search = request.args.get('search', '')
    status_filter = request.args.get('status', '')
    type_filter = request.args.get('type', '')
    query = Permit.query.filter_by(user_id=user_id).filter(Permit.deleted_at.is_(None))
    if search:
        query = query.filter(Permit.permit_type.ilike(f'%{search}%') | Permit.description.ilike(f'%{search}%'))
    if status_filter:
        query = query.filter_by(status=status_filter)
    if type_filter:
        query = query.filter_by(permit_type=type_filter)
    permits = query.order_by(Permit.created_at.desc()).all()
    return jsonify([p.to_dict() for p in permits]), 200


@app.route('/api/permits', methods=['POST'])
@jwt_required()
def create_permit():
    user_id = int(get_jwt_identity())
    data = request.get_json()
    permit_type = data.get('permit_type', '')
    description = data.get('description', '')
    latitude = data.get('latitude')
    longitude = data.get('longitude')
    if not permit_type:
        return jsonify({'error': 'Permit type is required'}), 400
    fee = FEE_TABLE.get(permit_type, 100.0)
    permit = Permit(
        user_id=user_id,
        permit_type=permit_type,
        description=description,
        status='submitted',
        fee_amount=fee,
        latitude=latitude,
        longitude=longitude
    )
    db.session.add(permit)
    db.session.commit()
    db.session.refresh(permit)
    return jsonify(permit.to_dict()), 201


@app.route('/api/permits/<int:permit_id>', methods=['GET'])
@jwt_required()
def get_permit(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    return jsonify(permit.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/upload', methods=['POST'])
@jwt_required()
def upload_document(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'Empty filename'}), 400
    filename = secure_filename(f"{permit_id}_{file.filename}")
    filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(filepath)
    document_label = request.form.get('document_label', '')
    doc = Document(permit_id=permit_id, file_path=filepath, file_name=filename, document_label=document_label)
    db.session.add(doc)
    db.session.commit()
    return jsonify(doc.to_dict()), 201


@app.route('/api/permits/<int:permit_id>/pay', methods=['POST'])
@jwt_required()
def pay_permit(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    if permit.is_paid:
        return jsonify({'error': 'Already paid'}), 400
    permit.is_paid = True
    if permit.status == 'approved':
        permit.status = 'completed'
    db.session.commit()
    return jsonify(permit.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/renew', methods=['POST'])
@jwt_required()
def renew_permit(permit_id):
    user_id = int(get_jwt_identity())
    old_permit = Permit.query.get_or_404(permit_id)
    if old_permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    if old_permit.status not in ('completed', 'rejected'):
        return jsonify({'error': 'Can only renew completed or reapply rejected permits'}), 400
    fee = FEE_TABLE.get(old_permit.permit_type, 100.0)
    new_permit = Permit(
        user_id=user_id,
        permit_type=old_permit.permit_type,
        description=old_permit.description or '',
        status='submitted',
        fee_amount=fee,
        renewed_from=old_permit.id
    )
    db.session.add(new_permit)
    db.session.commit()
    return jsonify(new_permit.to_dict()), 201


@app.route('/api/permits/pending', methods=['GET'])
@jwt_required()
def get_pending_permits():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    if user.role != 'inspector':
        return jsonify({'error': 'Unauthorized'}), 403
    search = request.args.get('search', '')
    type_filter = request.args.get('type', '')
    query = Permit.query.filter_by(status='submitted').filter(Permit.deleted_at.is_(None))
    if search:
        search_term = f'%{search}%'
        query = query.join(User, Permit.user_id == User.id).filter(
            User.full_name.ilike(search_term) | Permit.permit_type.ilike(search_term) | Permit.description.ilike(search_term)
        )
    if type_filter:
        query = query.filter_by(permit_type=type_filter)
    permits = query.order_by(Permit.created_at.desc()).all()
    return jsonify([p.to_dict() for p in permits]), 200


@app.route('/api/permits/reviewed', methods=['GET'])
@jwt_required()
def get_reviewed_permits():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    if user.role != 'inspector':
        return jsonify({'error': 'Unauthorized'}), 403
    permits = Permit.query.filter(Permit.reviewed_by == user_id, Permit.status.in_(['approved', 'rejected'])).order_by(Permit.updated_at.desc()).all()
    return jsonify([p.to_dict() for p in permits]), 200


@app.route('/api/permits/<int:permit_id>/review', methods=['POST'])
@jwt_required()
def review_permit(permit_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    if user.role != 'inspector':
        return jsonify({'error': 'Unauthorized'}), 403
    data = request.get_json()
    action = data.get('action', '')
    notes = data.get('notes', '')
    permit = Permit.query.get_or_404(permit_id)
    if action not in ('approved', 'rejected'):
        return jsonify({'error': 'Action must be approved or rejected'}), 400
    permit.status = action
    permit.reviewer_notes = notes
    permit.reviewed_by = user_id
    db.session.commit()
    status_text = 'Approved' if action == 'approved' else 'Rejected'
    send_push_notification(
        permit.user_id,
        f'Permit {status_text}',
        f'Your {permit.permit_type} has been {status_text.lower()}.',
        {'permit_id': str(permit.id)}
    )
    return jsonify(permit.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/comments', methods=['GET'])
@jwt_required()
def get_comments(permit_id):
    Permit.query.get_or_404(permit_id)
    comments = Comment.query.filter_by(permit_id=permit_id).order_by(Comment.created_at.asc()).all()
    return jsonify([c.to_dict() for c in comments]), 200


@app.route('/api/permits/<int:permit_id>/comments', methods=['POST'])
@jwt_required()
def add_comment(permit_id):
    user_id = int(get_jwt_identity())
    Permit.query.get_or_404(permit_id)
    data = request.get_json()
    message = data.get('message', '').strip()
    if not message:
        return jsonify({'error': 'Message is required'}), 400
    comment = Comment(permit_id=permit_id, user_id=user_id, message=message)
    db.session.add(comment)
    db.session.commit()
    permit = Permit.query.get(permit_id)
    if permit:
        commenter = User.query.get(user_id)
        commenter_name = commenter.full_name if commenter else 'Someone'
        notify_user_id = permit.reviewed_by if user_id == permit.user_id else permit.user_id
        if notify_user_id:
            send_push_notification(
                notify_user_id,
                'New Comment',
                f'{commenter_name} commented on {permit.permit_type}',
                {'permit_id': str(permit_id)}
            )
    return jsonify(comment.to_dict()), 201


@app.route('/api/permits/<int:permit_id>/appointment', methods=['POST'])
@jwt_required()
def schedule_appointment(permit_id):
    user_id = int(get_jwt_identity())
    permit = Permit.query.get_or_404(permit_id)
    if permit.status != 'approved':
        return jsonify({'error': 'Can only schedule for approved permits'}), 400
    data = request.get_json()
    date = data.get('date', '')
    time_slot = data.get('time_slot', '')
    notes = data.get('notes', '')
    if not date or not time_slot:
        return jsonify({'error': 'Date and time slot are required'}), 400
    existing = Appointment.query.filter_by(permit_id=permit_id, status='scheduled').first()
    if existing:
        return jsonify({'error': 'Appointment already scheduled'}), 400
    appt = Appointment(permit_id=permit_id, user_id=user_id, date=date, time_slot=time_slot, notes=notes)
    db.session.add(appt)
    db.session.commit()
    inspectors = User.query.filter_by(role='inspector').all()
    for insp in inspectors:
        send_push_notification(
            insp.id,
            'New Appointment',
            f'Inspection scheduled for {permit.permit_type} on {date}',
            {'permit_id': str(permit_id)}
        )
    return jsonify(appt.to_dict()), 201


@app.route('/api/appointments', methods=['GET'])
@jwt_required()
def get_appointments():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    if user.role == 'inspector':
        appts = Appointment.query.filter_by(status='scheduled').order_by(Appointment.date.asc()).all()
    else:
        appts = Appointment.query.filter_by(user_id=user_id).order_by(Appointment.date.desc()).all()
    result = []
    for a in appts:
        d = a.to_dict()
        d['permit_type'] = a.permit.permit_type if a.permit else ''
        d['applicant_name'] = a.permit.applicant.full_name if a.permit and a.permit.applicant else ''
        result.append(d)
    return jsonify(result), 200


@app.route('/api/appointments/<int:appt_id>', methods=['PUT'])
@jwt_required()
def update_appointment(appt_id):
    appt = Appointment.query.get_or_404(appt_id)
    data = request.get_json()
    new_status = data.get('status', '')
    if new_status in ('confirmed', 'cancelled', 'completed'):
        appt.status = new_status
    if 'notes' in data:
        appt.notes = data['notes']
    db.session.commit()
    return jsonify(appt.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/certificate', methods=['GET'])
@jwt_required()
def get_certificate(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    if permit.status != 'completed':
        return jsonify({'error': 'Certificate only available for completed permits'}), 400
    try:
        from reportlab.lib.pagesizes import A4
        from reportlab.lib import colors
        from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image as RLImage
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.lib.units import cm
        import qrcode

        qr = qrcode.make(f"SmartPermits-Verify-{permit.id}-{permit.permit_type}")
        qr_buffer = io.BytesIO()
        qr.save(qr_buffer, format='PNG')
        qr_buffer.seek(0)
        qr_path = os.path.join(app.config['UPLOAD_FOLDER'], f'qr_{permit.id}.png')
        with open(qr_path, 'wb') as f:
            f.write(qr_buffer.read())

        pdf_buffer = io.BytesIO()
        doc = SimpleDocTemplate(pdf_buffer, pagesize=A4)
        styles = getSampleStyleSheet()
        title_style = ParagraphStyle('Title2', parent=styles['Title'], fontSize=24, textColor=colors.HexColor('#1E3A5F'))
        subtitle = ParagraphStyle('Sub', parent=styles['Normal'], fontSize=14, textColor=colors.grey)

        elements = []
        elements.append(Paragraph("SmartPermits", title_style))
        elements.append(Paragraph("Official Permit Certificate", subtitle))
        elements.append(Spacer(1, 1 * cm))

        data = [
            ['Field', 'Details'],
            ['Certificate ID', f'SP-{permit.id:05d}'],
            ['Permit Type', permit.permit_type],
            ['Applicant', permit.applicant.full_name if permit.applicant else ''],
            ['Description', permit.description or 'N/A'],
            ['Fee Amount', f'${permit.fee_amount:.2f}'],
            ['Status', 'COMPLETED'],
            ['Issued Date', permit.updated_at.strftime('%Y-%m-%d')],
            ['Application Date', permit.created_at.strftime('%Y-%m-%d')],
        ]

        table = Table(data, colWidths=[5 * cm, 10 * cm])
        table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#1E3A5F')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.white),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, -1), 11),
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#F4F6F9')]),
            ('PADDING', (0, 0), (-1, -1), 8),
        ]))
        elements.append(table)
        elements.append(Spacer(1, 1 * cm))
        elements.append(Paragraph("Verification QR Code:", styles['Normal']))
        elements.append(Spacer(1, 0.3 * cm))
        elements.append(RLImage(qr_path, width=4 * cm, height=4 * cm))
        elements.append(Spacer(1, 1 * cm))
        elements.append(Paragraph("This document is digitally generated by SmartPermits Platform.", ParagraphStyle('Footer', parent=styles['Normal'], fontSize=9, textColor=colors.grey)))

        doc.build(elements)
        pdf_buffer.seek(0)
        os.remove(qr_path)
        return send_file(pdf_buffer, mimetype='application/pdf', as_attachment=True, download_name=f'permit_certificate_{permit.id}.pdf')
    except ImportError:
        return jsonify({'error': 'PDF generation libraries not installed'}), 500


@app.route('/api/permits/<int:permit_id>/trash', methods=['POST'])
@jwt_required()
def trash_permit(permit_id):
    user_id = int(get_jwt_identity())
    permit = Permit.query.get_or_404(permit_id)
    if permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    permit.deleted_at = datetime.utcnow()
    db.session.commit()
    return jsonify({'message': 'Moved to trash'}), 200


@app.route('/api/permits/<int:permit_id>/restore', methods=['POST'])
@jwt_required()
def restore_permit(permit_id):
    user_id = int(get_jwt_identity())
    permit = Permit.query.get_or_404(permit_id)
    if permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    permit.deleted_at = None
    db.session.commit()
    return jsonify(permit.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/permanent-delete', methods=['DELETE'])
@jwt_required()
def permanent_delete_permit(permit_id):
    user_id = int(get_jwt_identity())
    permit = Permit.query.get_or_404(permit_id)
    if permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    if permit.deleted_at is None:
        return jsonify({'error': 'Permit must be in trash first'}), 400
    Comment.query.filter_by(permit_id=permit_id).delete()
    Appointment.query.filter_by(permit_id=permit_id).delete()
    for doc in permit.documents:
        try:
            os.remove(doc.file_path)
        except OSError:
            pass
    Document.query.filter_by(permit_id=permit_id).delete()
    db.session.delete(permit)
    db.session.commit()
    return jsonify({'message': 'Permanently deleted'}), 200


@app.route('/api/permits/trash', methods=['GET'])
@jwt_required()
def get_trash():
    user_id = int(get_jwt_identity())
    permits = Permit.query.filter(
        Permit.user_id == user_id,
        Permit.deleted_at.isnot(None)
    ).order_by(Permit.deleted_at.desc()).all()
    return jsonify([p.to_dict() for p in permits]), 200


@app.route('/api/permits/trash/empty', methods=['DELETE'])
@jwt_required()
def empty_trash():
    user_id = int(get_jwt_identity())
    trashed = Permit.query.filter(
        Permit.user_id == user_id,
        Permit.deleted_at.isnot(None)
    ).all()
    for permit in trashed:
        Comment.query.filter_by(permit_id=permit.id).delete()
        Appointment.query.filter_by(permit_id=permit.id).delete()
        for doc in permit.documents:
            try:
                os.remove(doc.file_path)
            except OSError:
                pass
        Document.query.filter_by(permit_id=permit.id).delete()
        db.session.delete(permit)
    db.session.commit()
    return jsonify({'message': 'Trash emptied'}), 200


def cleanup_old_trash():
    from datetime import timedelta
    cutoff = datetime.utcnow() - timedelta(days=30)
    old_permits = Permit.query.filter(
        Permit.deleted_at.isnot(None),
        Permit.deleted_at < cutoff
    ).all()
    for permit in old_permits:
        Comment.query.filter_by(permit_id=permit.id).delete()
        Appointment.query.filter_by(permit_id=permit.id).delete()
        for doc in permit.documents:
            try:
                os.remove(doc.file_path)
            except OSError:
                pass
        Document.query.filter_by(permit_id=permit.id).delete()
        db.session.delete(permit)
    if old_permits:
        db.session.commit()


@app.before_request
def run_trash_cleanup():
    if not hasattr(app, '_trash_cleaned'):
        app._trash_cleaned = True
        cleanup_old_trash()


@app.route('/api/permits/stats/analytics', methods=['GET'])
@jwt_required()
def get_analytics():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    if user.role != 'inspector':
        return jsonify({'error': 'Unauthorized'}), 403

    total_reviewed = Permit.query.filter(Permit.reviewed_by == user_id).count()
    total_approved = Permit.query.filter(Permit.reviewed_by == user_id, Permit.status.in_(['approved', 'completed'])).count()
    total_rejected = Permit.query.filter(Permit.reviewed_by == user_id, Permit.status == 'rejected').count()
    total_pending = Permit.query.filter_by(status='submitted').count()

    type_counts = {}
    all_permits = Permit.query.filter(Permit.reviewed_by == user_id).all()
    for p in all_permits:
        type_counts[p.permit_type] = type_counts.get(p.permit_type, 0) + 1

    total_hours = 0
    count = 0
    reviewed_permits = Permit.query.filter(Permit.reviewed_by == user_id, Permit.status.in_(['approved', 'rejected', 'completed'])).all()
    for p in reviewed_permits:
        if p.updated_at and p.created_at:
            diff = (p.updated_at - p.created_at).total_seconds() / 3600
            if diff > 0:
                total_hours += diff
                count += 1
    avg_processing = round(total_hours / count, 1) if count > 0 else 0

    return jsonify({
        'total_reviewed': total_reviewed,
        'total_approved': total_approved,
        'total_rejected': total_rejected,
        'total_pending': total_pending,
        'permit_type_counts': type_counts,
        'avg_processing_hours': avg_processing,
    }), 200


@app.route('/api/uploads/<filename>', methods=['GET'])
def get_upload(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)


@app.route('/api/auth/profile', methods=['GET'])
@jwt_required()
def get_profile():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    return jsonify(user.to_dict()), 200


@app.route('/api/auth/profile', methods=['PUT'])
@jwt_required()
def update_profile():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    data = request.get_json()
    if 'full_name' in data and data['full_name'].strip():
        user.full_name = data['full_name'].strip()
    if 'email' in data and data['email'].strip():
        existing = User.query.filter(User.email == data['email'].strip(), User.id != user_id).first()
        if existing:
            return jsonify({'error': 'Email already in use'}), 409
        user.email = data['email'].strip()
    db.session.commit()
    return jsonify(user.to_dict()), 200


@app.route('/api/auth/profile/avatar', methods=['POST'])
@jwt_required()
def upload_avatar():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'Empty filename'}), 400
    filename = secure_filename(f"avatar_{user_id}_{file.filename}")
    filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(filepath)
    user.avatar_url = filename
    db.session.commit()
    return jsonify(user.to_dict()), 200


@app.route('/api/auth/delete-account', methods=['DELETE'])
@jwt_required()
def delete_account():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    permits = Permit.query.filter_by(user_id=user_id).all()
    for permit in permits:
        Comment.query.filter_by(permit_id=permit.id).delete()
        Appointment.query.filter_by(permit_id=permit.id).delete()
        for doc in permit.documents:
            try:
                os.remove(doc.file_path)
            except OSError:
                pass
        Document.query.filter_by(permit_id=permit.id).delete()
        db.session.delete(permit)
    Comment.query.filter_by(user_id=user_id).delete()
    Appointment.query.filter_by(user_id=user_id).delete()
    if user.avatar_url:
        try:
            os.remove(os.path.join(app.config['UPLOAD_FOLDER'], user.avatar_url))
        except OSError:
            pass
    db.session.delete(user)
    db.session.commit()
    return jsonify({'message': 'Account deleted successfully'}), 200


@app.route('/api/permit-types', methods=['GET'])
def get_permit_types():
    types = [{'name': k, 'fee': v} for k, v in FEE_TABLE.items()]
    return jsonify(types), 200


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
