import os
import io
_env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
try:
    from dotenv import load_dotenv
    load_dotenv(_env_path, override=True)
except ImportError:
    if os.path.exists(_env_path):
        with open(_env_path) as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    k, v = line.split('=', 1)
                    os.environ[k.strip()] = v.strip()
from flask import Flask, request, jsonify, send_from_directory, send_file
from flask_cors import CORS
from flask_bcrypt import Bcrypt
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from werkzeug.utils import secure_filename
from models import db, User, Permit, Document, Comment, Appointment, PermitEvent, PERMIT_VALIDITY_DAYS
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


def log_permit_event(permit_id, event_type, actor_name='', actor_role='', notes=''):
    event = PermitEvent(
        permit_id=permit_id,
        event_type=event_type,
        actor_name=actor_name,
        actor_role=actor_role,
        notes=notes
    )
    db.session.add(event)


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
            full_name='Test Inspector'
        )
        db.session.add(inspector)
    if User.query.filter_by(username='citizen1').first() is None:
        citizen = User(
            username='citizen1',
            email='citizen@email.com',
            password_hash=bcrypt.generate_password_hash('1q2w3e4r').decode('utf-8'),
            role='citizen',
            full_name='Test User'
        )
        db.session.add(citizen)
    db.session.commit()


def run_migrations():
    from sqlalchemy import text, inspect as sa_inspect
    insp = sa_inspect(db.engine)
    tables = insp.get_table_names()
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
        permit_cols2 = [c['name'] for c in insp.get_columns('permits')]
        if 'ai_analysis' not in permit_cols2:
            conn.execute(text("ALTER TABLE permits ADD COLUMN ai_analysis TEXT"))
        if 'ai_analysis_lang' not in permit_cols2:
            conn.execute(text("ALTER TABLE permits ADD COLUMN ai_analysis_lang VARCHAR(10)"))
        if 'blockchain_hash' not in permit_cols2:
            conn.execute(text("ALTER TABLE permits ADD COLUMN blockchain_hash VARCHAR(66)"))
        if 'blockchain_tx_hash' not in permit_cols2:
            conn.execute(text("ALTER TABLE permits ADD COLUMN blockchain_tx_hash VARCHAR(70)"))
        if 'blockchain_error' not in permit_cols2:
            conn.execute(text("ALTER TABLE permits ADD COLUMN blockchain_error VARCHAR(500)"))
        if 'expires_at' not in permit_cols2:
            conn.execute(text("ALTER TABLE permits ADD COLUMN expires_at DATETIME"))
        conn.commit()


with app.app_context():
    db.create_all()
    run_migrations()
    seed_data()


import re as _re

def _valid_email(email):
    return bool(_re.match(r'^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$', email))

def _get_current_user():
    uid = int(get_jwt_identity())
    return User.query.get(uid)

@app.route('/api/auth/register', methods=['POST'])
def register():
    data = request.get_json(silent=True) or {}
    username = data.get('username', '').strip()
    email = data.get('email', '').strip()
    password = data.get('password', '')
    role = data.get('role', 'citizen')
    if role not in ('citizen', 'inspector'):
        role = 'citizen'
    full_name = data.get('full_name', '').strip()
    if not username or not email or not password:
        return jsonify({'error': 'All fields are required'}), 400
    if not _valid_email(email):
        return jsonify({'error': 'Invalid email format'}), 400
    if len(password) < 6:
        return jsonify({'error': 'Password must be at least 6 characters'}), 400
    if len(username) < 3:
        return jsonify({'error': 'Username must be at least 3 characters'}), 400
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
    data = request.get_json(silent=True) or {}
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
    data = request.get_json(silent=True) or {}
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
    data = request.get_json(silent=True) or {}
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
    user = User.query.get(user_id)
    data = request.get_json(silent=True) or {}
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
    db.session.flush()
    log_permit_event(permit.id, 'Submitted', actor_name=user.full_name if user else '', actor_role='citizen', notes=f'{permit_type} application submitted')
    db.session.commit()
    db.session.refresh(permit)
    return jsonify(permit.to_dict()), 201


@app.route('/api/permits/<int:permit_id>', methods=['GET'])
@jwt_required()
def get_permit(permit_id):
    user_id = int(get_jwt_identity())
    permit = Permit.query.get_or_404(permit_id)
    user = User.query.get(user_id)
    if permit.user_id != user_id and (not user or user.role != 'inspector'):
        return jsonify({'error': 'Unauthorized'}), 403
    return jsonify(permit.to_dict()), 200


ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'pdf', 'doc', 'docx'}

@app.route('/api/permits/<int:permit_id>/upload', methods=['POST'])
@jwt_required()
def upload_document(permit_id):
    user_id = int(get_jwt_identity())
    permit = Permit.query.get_or_404(permit_id)
    if permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    if 'file' not in request.files:
        return jsonify({'error': 'No file provided'}), 400
    file = request.files['file']
    if file.filename == '':
        return jsonify({'error': 'Empty filename'}), 400
    ext = file.filename.rsplit('.', 1)[-1].lower() if '.' in file.filename else ''
    if ext not in ALLOWED_EXTENSIONS:
        return jsonify({'error': 'File type not allowed'}), 400
    filename = secure_filename(f"{permit_id}_{file.filename}")
    filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(filepath)
    document_label = request.form.get('document_label', '')
    doc = Document(permit_id=permit_id, file_path=filepath, file_name=filename, document_label=document_label)
    db.session.add(doc)
    db.session.commit()
    return jsonify(doc.to_dict()), 201


REQUIRED_DOCUMENTS = {
    'Construction Permit': [
        'Urban Planning Certificate',
        'Land Registry Extract',
        'Topographic Survey Plan',
        'Authorized Technical Project',
        'Utility Approvals (Water, Gas, Electricity)',
        'Geotechnical Study',
        'Fee Payment Proof',
    ],
    'Renovation Permit': [
        'Urban Planning Certificate',
        'Existing Condition Survey',
        'Renovation Technical Project',
        'Homeowners Association Approval (if applicable)',
        'Affected Utility Approvals',
        'Fee Payment Proof',
    ],
    'Business License': [
        'Business Registration Certificate',
        'Articles of Incorporation',
        'Office Space Lease Agreement',
        'Fire Safety Approval',
        'Tax Clearance Certificate',
        'Business Registry Certificate',
    ],
    'Food Service Permit': [
        'Veterinary Sanitary Authorization',
        'HACCP Plan',
        'Pest Control Service Contract',
        'Environmental Approval',
        'Business Registration Certificate',
        'Water Quality Analysis Report',
    ],
    'Event Permit': [
        'Event Organization Request',
        'Security Plan',
        'Police Approval',
        'Fire Department Approval',
        'Sanitation Service Contract',
        'Liability Insurance Policy',
    ],
    'Signage Permit': [
        'Signage Placement Request',
        'Site Sketch',
        'Urban Planning / Architecture Approval',
        'Property Owner Agreement',
        'Photo Simulation / Mockup',
    ],
    'Demolition Permit': [
        'Urban Planning Certificate',
        'Land Registry Extract',
        'Demolition Technical Project',
        'Demolition Plan',
        'Environmental Approval',
        'Waste Management Study',
        'Fee Payment Proof',
    ],
    'Occupancy Certificate': [
        'Work Completion Inspection Report',
        'Energy Performance Certificate',
        'Cadastral Documentation',
        'Project Verifier Reports',
        'Installation Compliance Declaration',
        'Fee Payment Proof',
    ],
}


LANG_NAMES = {
    'en': 'English', 'ro': 'Romanian', 'es': 'Spanish', 'fr': 'French',
    'it': 'Italian', 'de': 'German', 'pt': 'Portuguese', 'pl': 'Polish',
    'tr': 'Turkish', 'uk': 'Ukrainian',
}


@app.route('/api/permits/<int:permit_id>/ai-analyze', methods=['POST'])
@jwt_required()
def ai_analyze(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    force = request.args.get('force', '').lower() in ('1', 'true', 'yes')
    requested_lang = request.args.get('lang', 'en').strip().lower() or 'en'

    is_error = permit.ai_analysis and (
        permit.ai_analysis.startswith('AI analysis unavailable')
        or permit.ai_analysis.startswith('AI analysis failed')
        or permit.ai_analysis.startswith('No documents')
    )
    lang_changed = permit.ai_analysis_lang and permit.ai_analysis_lang != requested_lang

    if not force and not is_error and not lang_changed and permit.ai_analysis:
        return jsonify({'ai_analysis': permit.ai_analysis, 'ai_analysis_lang': permit.ai_analysis_lang}), 200

    api_key = os.environ.get('GEMINI_API_KEY', '')
    if not api_key:
        permit.ai_analysis = 'AI analysis unavailable: GEMINI_API_KEY not configured.'
        permit.ai_analysis_lang = requested_lang
        db.session.commit()
        return jsonify({'ai_analysis': permit.ai_analysis, 'ai_analysis_lang': requested_lang}), 200

    docs = Document.query.filter_by(permit_id=permit_id).all()
    if not docs:
        permit.ai_analysis = 'No documents uploaded for analysis.'
        permit.ai_analysis_lang = requested_lang
        db.session.commit()
        return jsonify({'ai_analysis': permit.ai_analysis, 'ai_analysis_lang': requested_lang}), 200

    try:
        import time as _time
        from google import genai
        from PIL import Image

        client = genai.Client(api_key=api_key)

        required = REQUIRED_DOCUMENTS.get(permit.permit_type, [])
        required_list = '\n'.join(f'  - {d}' for d in required) if required else '  (No specific list available)'

        doc_labels = []
        for i, d in enumerate(docs):
            label = d.document_label if d.document_label else 'Unlabeled'
            doc_labels.append(f'  Document {i+1}: "{label}" (file: {d.file_name})')
        doc_list = '\n'.join(doc_labels)

        lang_name = LANG_NAMES.get(requested_lang, 'English')
        lang_instruction = f"\n\nIMPORTANT: Write your ENTIRE response in {lang_name}." if requested_lang != 'en' else ""

        prompt = (
            f"You are an expert municipal permit document reviewer.\n"
            f"This is a \"{permit.permit_type}\" application.\n"
            f"The applicant uploaded {len(docs)} document(s):\n{doc_list}\n\n"
            f"Required documents for this permit type:\n{required_list}\n\n"
            f"Analyze ALL the uploaded document images together. For each document:\n"
            f"1. Identify what type of document it appears to be\n"
            f"2. Extract key visible information (dates, names, addresses, stamps, signatures)\n"
            f"3. Note any issues (blurry, incomplete, expired dates, missing stamps)\n\n"
            f"Then provide:\n"
            f"- Overall completeness assessment (which required documents appear present/missing)\n"
            f"- Any warnings or concerns\n"
            f"- Brief recommendation for the inspector\n\n"
            f"Respond in a clear, structured format. Be concise but thorough.{lang_instruction}"
        )

        contents = [prompt]
        for d in docs:
            try:
                img = Image.open(d.file_path)
                contents.append(img)
            except Exception:
                contents.append(f"[Could not load image: {d.file_name}]")

        models_to_try = ['gemini-2.5-flash', 'gemini-2.0-flash', 'gemini-2.5-flash-lite']
        analysis_text = None
        last_error = None
        for model_name in models_to_try:
            for attempt in range(3):
                try:
                    response = client.models.generate_content(
                        model=model_name,
                        contents=contents
                    )
                    analysis_text = response.text
                    break
                except Exception as retry_err:
                    last_error = retry_err
                    err_str = str(retry_err)
                    if '503' in err_str or 'UNAVAILABLE' in err_str:
                        _time.sleep(2 * (attempt + 1))
                        continue
                    elif '429' in err_str or 'RESOURCE_EXHAUSTED' in err_str:
                        _time.sleep(3 * (attempt + 1))
                        continue
                    else:
                        break
            if analysis_text:
                break

        if not analysis_text:
            raise last_error or Exception('All models failed')

        permit.ai_analysis = analysis_text
        permit.ai_analysis_lang = requested_lang
        log_permit_event(permit.id, 'Documents Analyzed by AI', actor_name='Gemini AI', actor_role='system', notes='Automated document analysis completed')
        db.session.commit()
        return jsonify({'ai_analysis': permit.ai_analysis, 'ai_analysis_lang': requested_lang}), 200
    except Exception as e:
        error_msg = f'AI analysis failed: {str(e)}'
        permit.ai_analysis = error_msg
        permit.ai_analysis_lang = requested_lang
        db.session.commit()
        return jsonify({'ai_analysis': error_msg, 'ai_analysis_lang': requested_lang}), 200


@app.route('/api/permits/<int:permit_id>/pay', methods=['POST'])
@jwt_required()
def pay_permit(permit_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    permit = Permit.query.get_or_404(permit_id)
    if permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    if permit.status != 'approved':
        return jsonify({'error': 'Only approved permits can be paid'}), 400
    if permit.is_paid:
        return jsonify({'error': 'Already paid'}), 400
    permit.is_paid = True
    permit.status = 'completed'
    validity = PERMIT_VALIDITY_DAYS.get(permit.permit_type, 365)
    if validity > 0:
        permit.expires_at = datetime.utcnow() + timedelta(days=validity)
    log_permit_event(permit.id, 'Payment Received', actor_name=user.full_name if user else '', actor_role='citizen', notes=f'${permit.fee_amount:.2f} paid')
    log_permit_event(permit.id, 'Certificate Issued', actor_name='System', actor_role='system', notes='Permit completed and certificate available for download')
    db.session.commit()
    return jsonify(permit.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/renew', methods=['POST'])
@jwt_required()
def renew_permit(permit_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
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
    db.session.flush()
    log_permit_event(new_permit.id, 'Submitted', actor_name=user.full_name if user else '', actor_role='citizen', notes=f'Renewed from permit #{old_permit.id}')
    db.session.commit()
    return jsonify(new_permit.to_dict()), 201


@app.route('/api/permits/pending', methods=['GET'])
@jwt_required()
def get_pending_permits():
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
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
    user = User.query.get_or_404(user_id)
    if user.role != 'inspector':
        return jsonify({'error': 'Unauthorized'}), 403
    permits = Permit.query.filter(Permit.reviewed_by == user_id, Permit.status.in_(['approved', 'rejected'])).order_by(Permit.updated_at.desc()).all()
    return jsonify([p.to_dict() for p in permits]), 200


@app.route('/api/permits/<int:permit_id>/review', methods=['POST'])
@jwt_required()
def review_permit(permit_id):
    user_id = int(get_jwt_identity())
    user = User.query.get_or_404(user_id)
    if user.role != 'inspector':
        return jsonify({'error': 'Unauthorized'}), 403
    data = request.get_json(silent=True) or {}
    action = data.get('action', '')
    notes = data.get('notes', '')
    permit = Permit.query.get_or_404(permit_id)
    if permit.status != 'submitted':
        return jsonify({'error': 'Permit has already been reviewed'}), 400
    if action not in ('approved', 'rejected'):
        return jsonify({'error': 'Action must be approved or rejected'}), 400
    permit.status = action
    permit.reviewer_notes = notes
    permit.reviewed_by = user_id
    status_label = 'Approved' if action == 'approved' else 'Rejected'
    log_permit_event(permit.id, f'Reviewed by Inspector', actor_name=user.full_name, actor_role='inspector', notes=f'{status_label}' + (f': {notes}' if notes else ''))
    if action == 'approved':
        try:
            from blockchain import notarize_permit
            docs = Document.query.filter_by(permit_id=permit_id).all()
            permit_hash, tx_hash, bc_error = notarize_permit(permit, docs, app.config['UPLOAD_FOLDER'])
            permit.blockchain_hash = permit_hash
            permit.blockchain_tx_hash = tx_hash
            permit.blockchain_error = bc_error
            if tx_hash:
                log_permit_event(permit.id, 'Blockchain Notarized', actor_name='Ethereum Sepolia', actor_role='system', notes=f'TX: {tx_hash[:20]}...')
        except Exception as e:
            permit.blockchain_error = f'Notarization failed: {str(e)}'
    db.session.commit()
    send_push_notification(
        permit.user_id,
        f'Permit {status_label}',
        f'Your {permit.permit_type} has been {status_label.lower()}.',
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
    data = request.get_json(silent=True) or {}
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
    user = User.query.get(user_id)
    permit = Permit.query.get_or_404(permit_id)
    if permit.user_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    if permit.status != 'approved':
        return jsonify({'error': 'Can only schedule for approved permits'}), 400
    data = request.get_json(silent=True) or {}
    date = data.get('date', '')
    time_slot = data.get('time_slot', '')
    notes = data.get('notes', '')
    if not date or not time_slot:
        return jsonify({'error': 'Date and time slot are required'}), 400
    try:
        appt_date = datetime.strptime(date, '%Y-%m-%d').date()
        if appt_date < datetime.utcnow().date():
            return jsonify({'error': 'Cannot schedule in the past'}), 400
    except ValueError:
        return jsonify({'error': 'Invalid date format'}), 400
    existing = Appointment.query.filter_by(permit_id=permit_id, status='scheduled').first()
    if existing:
        return jsonify({'error': 'Appointment already scheduled'}), 400
    appt = Appointment(permit_id=permit_id, user_id=user_id, date=date, time_slot=time_slot, notes=notes)
    db.session.add(appt)
    log_permit_event(permit.id, 'Appointment Scheduled', actor_name=user.full_name if user else '', actor_role='citizen', notes=f'{date} at {time_slot}')
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
    data = request.get_json(silent=True) or {}
    new_status = data.get('status', '')
    if new_status in ('confirmed', 'cancelled', 'completed'):
        old_status = appt.status
        appt.status = new_status
        if new_status == 'completed':
            user = User.query.get(int(get_jwt_identity()))
            log_permit_event(appt.permit_id, 'Inspection Completed', actor_name=user.full_name if user else '', actor_role=user.role if user else '', notes='On-site inspection completed')
    if 'notes' in data:
        appt.notes = data['notes']
    db.session.commit()
    return jsonify(appt.to_dict()), 200


@app.route('/api/permits/<int:permit_id>/verify-blockchain', methods=['GET'])
def verify_blockchain(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    if not permit.blockchain_hash:
        return jsonify({'verified': False, 'error': 'No blockchain record for this permit'}), 200
    result = {
        'permit_id': permit.id,
        'permit_type': permit.permit_type,
        'status': permit.status,
        'blockchain_hash': permit.blockchain_hash,
        'blockchain_tx_hash': permit.blockchain_tx_hash,
        'verified': permit.blockchain_tx_hash is not None,
        'etherscan_url': f'https://sepolia.etherscan.io/tx/{permit.blockchain_tx_hash}' if permit.blockchain_tx_hash else None,
        'error': permit.blockchain_error
    }
    return jsonify(result), 200


@app.route('/api/permits/<int:permit_id>/timeline', methods=['GET'])
@jwt_required()
def get_timeline(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    events = PermitEvent.query.filter_by(permit_id=permit_id).order_by(PermitEvent.created_at.asc()).all()
    return jsonify([e.to_dict() for e in events]), 200


PDF_TRANSLATIONS = {
    'en': {
        'subtitle': 'Official Municipal Permit Certificate',
        'cert_no': 'Certificate No.',
        'permit_info': 'Permit Information',
        'permit_type': 'Permit Type',
        'applicant': 'Applicant',
        'description': 'Description',
        'fee_amount': 'Fee Amount',
        'status': 'Status',
        'completed': 'COMPLETED',
        'application_date': 'Application Date',
        'issued_date': 'Issued Date',
        'valid_until': 'Valid Until',
        'non_expiring': 'Non-expiring',
        'na': 'N/A',
        'blockchain_verification': 'Blockchain Verification',
        'tx_hash': 'Transaction Hash',
        'network': 'Network',
        'etherscan_link': 'Etherscan Link',
        'doc_hash': 'Document Hash (SHA-256)',
        'verification_status': 'Verification Status',
        'verified_blockchain': 'Verified on Blockchain',
        'hash_local': 'Hash Recorded Locally',
        'scan_verify': 'Scan to Verify',
        'view_etherscan': 'View on Etherscan',
        'footer_digital': 'This document is digitally generated and verified by SmartPermits Platform.',
        'footer_generated': 'Generated on',
        'footer_tamper': 'Tampering with this certificate is a criminal offense under municipal regulations.',
    },
    'ro': {
        'subtitle': 'Certificat Oficial de Autorizație Municipală',
        'cert_no': 'Certificat Nr.',
        'permit_info': 'Informații Autorizație',
        'permit_type': 'Tip Autorizație',
        'applicant': 'Solicitant',
        'description': 'Descriere',
        'fee_amount': 'Taxă',
        'status': 'Stare',
        'completed': 'FINALIZAT',
        'application_date': 'Data Depunerii',
        'issued_date': 'Data Emiterii',
        'valid_until': 'Valabil Până La',
        'non_expiring': 'Fără expirare',
        'na': 'N/A',
        'blockchain_verification': 'Verificare Blockchain',
        'tx_hash': 'Hash Tranzacție',
        'network': 'Rețea',
        'etherscan_link': 'Link Etherscan',
        'doc_hash': 'Hash Document (SHA-256)',
        'verification_status': 'Stare Verificare',
        'verified_blockchain': 'Verificat pe Blockchain',
        'hash_local': 'Hash Înregistrat Local',
        'scan_verify': 'Scanează pentru Verificare',
        'view_etherscan': 'Vezi pe Etherscan',
        'footer_digital': 'Acest document este generat digital și verificat de Platforma SmartPermits.',
        'footer_generated': 'Generat la',
        'footer_tamper': 'Falsificarea acestui certificat constituie infracțiune conform reglementărilor municipale.',
    },
    'es': {
        'subtitle': 'Certificado Oficial de Permiso Municipal',
        'cert_no': 'Certificado Nro.',
        'permit_info': 'Información del Permiso',
        'permit_type': 'Tipo de Permiso',
        'applicant': 'Solicitante',
        'description': 'Descripción',
        'fee_amount': 'Tarifa',
        'status': 'Estado',
        'completed': 'COMPLETADO',
        'application_date': 'Fecha de Solicitud',
        'issued_date': 'Fecha de Emisión',
        'valid_until': 'Válido Hasta',
        'non_expiring': 'Sin vencimiento',
        'na': 'N/A',
        'blockchain_verification': 'Verificación Blockchain',
        'tx_hash': 'Hash de Transacción',
        'network': 'Red',
        'etherscan_link': 'Enlace Etherscan',
        'doc_hash': 'Hash del Documento (SHA-256)',
        'verification_status': 'Estado de Verificación',
        'verified_blockchain': 'Verificado en Blockchain',
        'hash_local': 'Hash Registrado Localmente',
        'scan_verify': 'Escanear para Verificar',
        'view_etherscan': 'Ver en Etherscan',
        'footer_digital': 'Este documento es generado digitalmente y verificado por la Plataforma SmartPermits.',
        'footer_generated': 'Generado el',
        'footer_tamper': 'La falsificación de este certificado es un delito penal según las regulaciones municipales.',
    },
    'fr': {
        'subtitle': 'Certificat Officiel de Permis Municipal',
        'cert_no': 'Certificat N°',
        'permit_info': 'Informations du Permis',
        'permit_type': 'Type de Permis',
        'applicant': 'Demandeur',
        'description': 'Description',
        'fee_amount': 'Montant des Frais',
        'status': 'Statut',
        'completed': 'TERMINÉ',
        'application_date': 'Date de Demande',
        'issued_date': "Date d'Émission",
        'valid_until': 'Valide Jusqu\'au',
        'non_expiring': 'Sans expiration',
        'na': 'N/A',
        'blockchain_verification': 'Vérification Blockchain',
        'tx_hash': 'Hash de Transaction',
        'network': 'Réseau',
        'etherscan_link': 'Lien Etherscan',
        'doc_hash': 'Hash du Document (SHA-256)',
        'verification_status': 'Statut de Vérification',
        'verified_blockchain': 'Vérifié sur Blockchain',
        'hash_local': 'Hash Enregistré Localement',
        'scan_verify': 'Scanner pour Vérifier',
        'view_etherscan': 'Voir sur Etherscan',
        'footer_digital': 'Ce document est généré numériquement et vérifié par la Plateforme SmartPermits.',
        'footer_generated': 'Généré le',
        'footer_tamper': 'La falsification de ce certificat est une infraction pénale selon les réglementations municipales.',
    },
    'it': {
        'subtitle': 'Certificato Ufficiale di Permesso Comunale',
        'cert_no': 'Certificato N.',
        'permit_info': 'Informazioni Permesso',
        'permit_type': 'Tipo di Permesso',
        'applicant': 'Richiedente',
        'description': 'Descrizione',
        'fee_amount': 'Importo Tassa',
        'status': 'Stato',
        'completed': 'COMPLETATO',
        'application_date': 'Data di Richiesta',
        'issued_date': 'Data di Emissione',
        'valid_until': 'Valido Fino Al',
        'non_expiring': 'Senza scadenza',
        'na': 'N/A',
        'blockchain_verification': 'Verifica Blockchain',
        'tx_hash': 'Hash Transazione',
        'network': 'Rete',
        'etherscan_link': 'Link Etherscan',
        'doc_hash': 'Hash Documento (SHA-256)',
        'verification_status': 'Stato Verifica',
        'verified_blockchain': 'Verificato su Blockchain',
        'hash_local': 'Hash Registrato Localmente',
        'scan_verify': 'Scansiona per Verificare',
        'view_etherscan': 'Vedi su Etherscan',
        'footer_digital': 'Questo documento è generato digitalmente e verificato dalla Piattaforma SmartPermits.',
        'footer_generated': 'Generato il',
        'footer_tamper': 'La manomissione di questo certificato è un reato penale ai sensi dei regolamenti comunali.',
    },
    'de': {
        'subtitle': 'Offizielle Kommunale Genehmigungsurkunde',
        'cert_no': 'Zertifikat Nr.',
        'permit_info': 'Genehmigungsinformationen',
        'permit_type': 'Genehmigungstyp',
        'applicant': 'Antragsteller',
        'description': 'Beschreibung',
        'fee_amount': 'Gebührenbetrag',
        'status': 'Status',
        'completed': 'ABGESCHLOSSEN',
        'application_date': 'Antragsdatum',
        'issued_date': 'Ausstellungsdatum',
        'valid_until': 'Gültig Bis',
        'non_expiring': 'Unbefristet',
        'na': 'N/A',
        'blockchain_verification': 'Blockchain-Verifizierung',
        'tx_hash': 'Transaktions-Hash',
        'network': 'Netzwerk',
        'etherscan_link': 'Etherscan-Link',
        'doc_hash': 'Dokument-Hash (SHA-256)',
        'verification_status': 'Verifizierungsstatus',
        'verified_blockchain': 'Auf Blockchain verifiziert',
        'hash_local': 'Hash Lokal Gespeichert',
        'scan_verify': 'Zum Verifizieren Scannen',
        'view_etherscan': 'Auf Etherscan Ansehen',
        'footer_digital': 'Dieses Dokument wurde digital erstellt und von der SmartPermits-Plattform verifiziert.',
        'footer_generated': 'Erstellt am',
        'footer_tamper': 'Die Fälschung dieser Urkunde ist eine Straftat gemäß den kommunalen Vorschriften.',
    },
    'pt': {
        'subtitle': 'Certificado Oficial de Licença Municipal',
        'cert_no': 'Certificado Nº',
        'permit_info': 'Informações da Licença',
        'permit_type': 'Tipo de Licença',
        'applicant': 'Requerente',
        'description': 'Descrição',
        'fee_amount': 'Valor da Taxa',
        'status': 'Estado',
        'completed': 'CONCLUÍDO',
        'application_date': 'Data de Pedido',
        'issued_date': 'Data de Emissão',
        'valid_until': 'Válido Até',
        'non_expiring': 'Sem validade',
        'na': 'N/A',
        'blockchain_verification': 'Verificação Blockchain',
        'tx_hash': 'Hash da Transação',
        'network': 'Rede',
        'etherscan_link': 'Link Etherscan',
        'doc_hash': 'Hash do Documento (SHA-256)',
        'verification_status': 'Estado da Verificação',
        'verified_blockchain': 'Verificado na Blockchain',
        'hash_local': 'Hash Registado Localmente',
        'scan_verify': 'Digitalizar para Verificar',
        'view_etherscan': 'Ver no Etherscan',
        'footer_digital': 'Este documento é gerado digitalmente e verificado pela Plataforma SmartPermits.',
        'footer_generated': 'Gerado em',
        'footer_tamper': 'A falsificação deste certificado é crime segundo os regulamentos municipais.',
    },
    'pl': {
        'subtitle': 'Oficjalny Certyfikat Pozwolenia Miejskiego',
        'cert_no': 'Certyfikat Nr',
        'permit_info': 'Informacje o Pozwoleniu',
        'permit_type': 'Typ Pozwolenia',
        'applicant': 'Wnioskodawca',
        'description': 'Opis',
        'fee_amount': 'Kwota Opłaty',
        'status': 'Status',
        'completed': 'ZAKOŃCZONE',
        'application_date': 'Data Złożenia',
        'issued_date': 'Data Wydania',
        'valid_until': 'Ważne Do',
        'non_expiring': 'Bezterminowe',
        'na': 'N/A',
        'blockchain_verification': 'Weryfikacja Blockchain',
        'tx_hash': 'Hash Transakcji',
        'network': 'Sieć',
        'etherscan_link': 'Link Etherscan',
        'doc_hash': 'Hash Dokumentu (SHA-256)',
        'verification_status': 'Status Weryfikacji',
        'verified_blockchain': 'Zweryfikowano na Blockchain',
        'hash_local': 'Hash Zapisany Lokalnie',
        'scan_verify': 'Zeskanuj aby Zweryfikować',
        'view_etherscan': 'Zobacz na Etherscan',
        'footer_digital': 'Ten dokument jest wygenerowany cyfrowo i zweryfikowany przez Platformę SmartPermits.',
        'footer_generated': 'Wygenerowano',
        'footer_tamper': 'Fałszowanie tego certyfikatu jest przestępstwem zgodnie z przepisami miejskimi.',
    },
    'tr': {
        'subtitle': 'Resmi Belediye İzin Belgesi',
        'cert_no': 'Belge No.',
        'permit_info': 'İzin Bilgileri',
        'permit_type': 'İzin Türü',
        'applicant': 'Başvuru Sahibi',
        'description': 'Açıklama',
        'fee_amount': 'Ücret Tutarı',
        'status': 'Durum',
        'completed': 'TAMAMLANDI',
        'application_date': 'Başvuru Tarihi',
        'issued_date': 'Düzenleme Tarihi',
        'valid_until': 'Geçerlilik Tarihi',
        'non_expiring': 'Süresiz',
        'na': 'N/A',
        'blockchain_verification': 'Blockchain Doğrulama',
        'tx_hash': 'İşlem Hash\'i',
        'network': 'Ağ',
        'etherscan_link': 'Etherscan Bağlantısı',
        'doc_hash': 'Belge Hash\'i (SHA-256)',
        'verification_status': 'Doğrulama Durumu',
        'verified_blockchain': 'Blockchain\'de Doğrulandı',
        'hash_local': 'Hash Yerel Olarak Kaydedildi',
        'scan_verify': 'Doğrulamak İçin Tarayın',
        'view_etherscan': 'Etherscan\'de Görüntüle',
        'footer_digital': 'Bu belge SmartPermits Platformu tarafından dijital olarak oluşturulmuş ve doğrulanmıştır.',
        'footer_generated': 'Oluşturulma tarihi',
        'footer_tamper': 'Bu belgenin tahrif edilmesi belediye yönetmeliklerine göre suçtur.',
    },
    'uk': {
        'subtitle': 'Офіційний Муніципальний Дозвільний Сертифікат',
        'cert_no': 'Сертифікат №',
        'permit_info': 'Інформація про Дозвіл',
        'permit_type': 'Тип Дозволу',
        'applicant': 'Заявник',
        'description': 'Опис',
        'fee_amount': 'Сума Збору',
        'status': 'Статус',
        'completed': 'ЗАВЕРШЕНО',
        'application_date': 'Дата Подання',
        'issued_date': 'Дата Видачі',
        'valid_until': 'Дійсний До',
        'non_expiring': 'Безстроковий',
        'na': 'Н/Д',
        'blockchain_verification': 'Верифікація Blockchain',
        'tx_hash': 'Хеш Транзакції',
        'network': 'Мережа',
        'etherscan_link': 'Посилання Etherscan',
        'doc_hash': 'Хеш Документа (SHA-256)',
        'verification_status': 'Статус Верифікації',
        'verified_blockchain': 'Перевірено на Blockchain',
        'hash_local': 'Хеш Збережено Локально',
        'scan_verify': 'Скануйте для Перевірки',
        'view_etherscan': 'Переглянути на Etherscan',
        'footer_digital': 'Цей документ створено цифрово та підтверджено Платформою SmartPermits.',
        'footer_generated': 'Створено',
        'footer_tamper': 'Підробка цього сертифіката є кримінальним правопорушенням згідно з муніципальними правилами.',
    },
}

PERMIT_TYPE_TRANSLATIONS = {
    'en': {
        'Construction Permit': 'Construction Permit', 'Renovation Permit': 'Renovation Permit',
        'Business License': 'Business License', 'Food Service Permit': 'Food Service Permit',
        'Event Permit': 'Event Permit', 'Signage Permit': 'Signage Permit',
        'Demolition Permit': 'Demolition Permit', 'Occupancy Certificate': 'Occupancy Certificate',
    },
    'ro': {
        'Construction Permit': 'Autorizație de Construcție', 'Renovation Permit': 'Autorizație de Renovare',
        'Business License': 'Licență de Afaceri', 'Food Service Permit': 'Autorizație Alimentară',
        'Event Permit': 'Autorizație Eveniment', 'Signage Permit': 'Autorizație Semnalistică',
        'Demolition Permit': 'Autorizație de Demolare', 'Occupancy Certificate': 'Certificat de Ocupare',
    },
    'es': {
        'Construction Permit': 'Permiso de Construcción', 'Renovation Permit': 'Permiso de Renovación',
        'Business License': 'Licencia Comercial', 'Food Service Permit': 'Permiso de Servicio de Alimentos',
        'Event Permit': 'Permiso de Evento', 'Signage Permit': 'Permiso de Señalización',
        'Demolition Permit': 'Permiso de Demolición', 'Occupancy Certificate': 'Certificado de Ocupación',
    },
    'fr': {
        'Construction Permit': 'Permis de Construction', 'Renovation Permit': 'Permis de Rénovation',
        'Business License': 'Licence Commerciale', 'Food Service Permit': 'Permis de Restauration',
        'Event Permit': "Permis d'Événement", 'Signage Permit': 'Permis de Signalisation',
        'Demolition Permit': 'Permis de Démolition', 'Occupancy Certificate': "Certificat d'Occupation",
    },
    'it': {
        'Construction Permit': 'Permesso di Costruzione', 'Renovation Permit': 'Permesso di Ristrutturazione',
        'Business License': 'Licenza Commerciale', 'Food Service Permit': 'Permesso Alimentare',
        'Event Permit': 'Permesso per Eventi', 'Signage Permit': 'Permesso di Segnaletica',
        'Demolition Permit': 'Permesso di Demolizione', 'Occupancy Certificate': 'Certificato di Agibilità',
    },
    'de': {
        'Construction Permit': 'Baugenehmigung', 'Renovation Permit': 'Renovierungsgenehmigung',
        'Business License': 'Gewerbelizenz', 'Food Service Permit': 'Gastronomiegenehmigung',
        'Event Permit': 'Veranstaltungsgenehmigung', 'Signage Permit': 'Beschilderungsgenehmigung',
        'Demolition Permit': 'Abrissgenehmigung', 'Occupancy Certificate': 'Nutzungsbescheinigung',
    },
    'pt': {
        'Construction Permit': 'Licença de Construção', 'Renovation Permit': 'Licença de Renovação',
        'Business License': 'Licença Comercial', 'Food Service Permit': 'Licença de Alimentação',
        'Event Permit': 'Licença de Evento', 'Signage Permit': 'Licença de Sinalização',
        'Demolition Permit': 'Licença de Demolição', 'Occupancy Certificate': 'Certificado de Ocupação',
    },
    'pl': {
        'Construction Permit': 'Pozwolenie na Budowę', 'Renovation Permit': 'Pozwolenie na Remont',
        'Business License': 'Licencja Biznesowa', 'Food Service Permit': 'Pozwolenie Gastronomiczne',
        'Event Permit': 'Pozwolenie na Wydarzenie', 'Signage Permit': 'Pozwolenie na Reklamę',
        'Demolition Permit': 'Pozwolenie na Rozbiórkę', 'Occupancy Certificate': 'Certyfikat Zamieszkania',
    },
    'tr': {
        'Construction Permit': 'İnşaat İzni', 'Renovation Permit': 'Tadilat İzni',
        'Business License': 'İşletme Ruhsatı', 'Food Service Permit': 'Gıda Hizmet İzni',
        'Event Permit': 'Etkinlik İzni', 'Signage Permit': 'Tabela İzni',
        'Demolition Permit': 'Yıkım İzni', 'Occupancy Certificate': 'İskan Belgesi',
    },
    'uk': {
        'Construction Permit': 'Дозвіл на Будівництво', 'Renovation Permit': 'Дозвіл на Ремонт',
        'Business License': 'Ліцензія на Бізнес', 'Food Service Permit': 'Дозвіл на Харчування',
        'Event Permit': 'Дозвіл на Захід', 'Signage Permit': 'Дозвіл на Вивіску',
        'Demolition Permit': 'Дозвіл на Знесення', 'Occupancy Certificate': 'Сертифікат Зайнятості',
    },
}


@app.route('/api/permits/<int:permit_id>/certificate', methods=['GET'])
@jwt_required()
def get_certificate(permit_id):
    permit = Permit.query.get_or_404(permit_id)
    if permit.status != 'completed':
        return jsonify({'error': 'Certificate only available for completed permits'}), 400
    requested_lang = request.args.get('lang', 'en').strip().lower() or 'en'
    t = PDF_TRANSLATIONS.get(requested_lang, PDF_TRANSLATIONS['en'])
    pt_trans = PERMIT_TYPE_TRANSLATIONS.get(requested_lang, PERMIT_TYPE_TRANSLATIONS['en'])
    try:
        from reportlab.lib.pagesizes import A4
        from reportlab.lib import colors as rl_colors
        from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image as RLImage, HRFlowable
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.lib.units import cm, mm
        from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT
        from xml.sax.saxutils import escape as xml_escape
        import qrcode

        qr_data = f"SmartPermits-Verify-{permit.id}-{permit.permit_type}"
        etherscan_url = None
        if permit.blockchain_tx_hash:
            etherscan_url = f"https://sepolia.etherscan.io/tx/{permit.blockchain_tx_hash}"
            qr_data = etherscan_url
        qr = qrcode.make(qr_data)
        qr_buffer = io.BytesIO()
        qr.save(qr_buffer, format='PNG')
        qr_buffer.seek(0)
        qr_path = os.path.join(app.config['UPLOAD_FOLDER'], f'qr_{permit.id}.png')
        with open(qr_path, 'wb') as f:
            f.write(qr_buffer.read())

        pdf_buffer = io.BytesIO()
        doc = SimpleDocTemplate(pdf_buffer, pagesize=A4, topMargin=1.5*cm, bottomMargin=1.5*cm, leftMargin=2*cm, rightMargin=2*cm)
        styles = getSampleStyleSheet()

        primary_color = rl_colors.HexColor('#0D9488')
        dark_color = rl_colors.HexColor('#0F766E')
        light_bg = rl_colors.HexColor('#F0FDFA')
        gold_color = rl_colors.HexColor('#D4A843')

        title_style = ParagraphStyle('CertTitle', parent=styles['Title'], fontSize=28, textColor=primary_color, spaceAfter=4, fontName='Helvetica-Bold', alignment=TA_CENTER)
        subtitle_style = ParagraphStyle('CertSub', parent=styles['Normal'], fontSize=13, textColor=rl_colors.HexColor('#6B7280'), alignment=TA_CENTER, spaceAfter=6)
        cert_id_style = ParagraphStyle('CertId', parent=styles['Normal'], fontSize=11, textColor=dark_color, alignment=TA_CENTER, fontName='Helvetica-Bold', spaceAfter=16)
        section_header = ParagraphStyle('SecHead', parent=styles['Normal'], fontSize=13, textColor=primary_color, fontName='Helvetica-Bold', spaceAfter=8, spaceBefore=12)
        cell_style = ParagraphStyle('Cell', parent=styles['Normal'], fontSize=10, leading=14)
        cell_bold = ParagraphStyle('CellBold', parent=styles['Normal'], fontSize=10, leading=14, fontName='Helvetica-Bold')
        footer_style = ParagraphStyle('Footer', parent=styles['Normal'], fontSize=8, textColor=rl_colors.HexColor('#9CA3AF'), alignment=TA_CENTER, spaceBefore=16)
        link_style = ParagraphStyle('Link', parent=styles['Normal'], fontSize=9, textColor=rl_colors.HexColor('#0D9488'), fontName='Helvetica', alignment=TA_CENTER)

        def _cell(text):
            return Paragraph(xml_escape(str(text or '')), cell_style)

        def _bold(text):
            return Paragraph(xml_escape(str(text or '')), cell_bold)

        elements = []

        elements.append(HRFlowable(width="100%", thickness=3, color=primary_color, spaceAfter=12))
        elements.append(Paragraph("\u2756 SmartPermits", title_style))
        elements.append(Paragraph(t['subtitle'], subtitle_style))
        elements.append(Paragraph(f"{t['cert_no']} SP-{permit.id:05d}", cert_id_style))
        elements.append(HRFlowable(width="100%", thickness=1, color=rl_colors.HexColor('#E5E7EB'), spaceAfter=16))

        applicant_name = permit.applicant.full_name if permit.applicant else ''
        issued = permit.updated_at.strftime('%B %d, %Y') if permit.updated_at else ''
        applied = permit.created_at.strftime('%B %d, %Y') if permit.created_at else ''
        expires = permit.expires_at.strftime('%B %d, %Y') if permit.expires_at else t['non_expiring']

        localized_permit_type = pt_trans.get(permit.permit_type, permit.permit_type)

        elements.append(Paragraph(t['permit_info'], section_header))
        info_data = [
            [_bold(t['permit_type']), _cell(localized_permit_type)],
            [_bold(t['applicant']), _cell(applicant_name)],
            [_bold(t['description']), _cell(permit.description or t['na'])],
            [_bold(t['fee_amount']), _cell(f'${permit.fee_amount:.2f}')],
            [_bold(t['status']), _cell(f'{t["completed"]} \u2714')],
            [_bold(t['application_date']), _cell(applied)],
            [_bold(t['issued_date']), _cell(issued)],
            [_bold(t['valid_until']), _cell(expires)],
        ]

        info_table = Table(info_data, colWidths=[5*cm, 11*cm])
        info_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (0, -1), light_bg),
            ('GRID', (0, 0), (-1, -1), 0.5, rl_colors.HexColor('#D1D5DB')),
            ('ROWBACKGROUNDS', (0, 0), (-1, -1), [rl_colors.white, rl_colors.HexColor('#FAFAFA')]),
            ('PADDING', (0, 0), (-1, -1), 10),
            ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ('TOPPADDING', (0, 0), (-1, -1), 8),
            ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
        ]))
        elements.append(info_table)
        elements.append(Spacer(1, 0.6*cm))

        if permit.blockchain_tx_hash or permit.blockchain_hash:
            elements.append(Paragraph(t['blockchain_verification'], section_header))
            bc_data = []
            if permit.blockchain_tx_hash:
                bc_data.append([_bold(t['tx_hash']), _cell(permit.blockchain_tx_hash)])
                bc_data.append([_bold(t['network']), _cell('Ethereum Sepolia Testnet')])
                bc_data.append([_bold(t['etherscan_link']), Paragraph(f'<a href="{etherscan_url}" color="#0D9488">{etherscan_url}</a>', cell_style)])
            if permit.blockchain_hash:
                bc_data.append([_bold(t['doc_hash']), _cell(permit.blockchain_hash)])
            bc_data.append([_bold(t['verification_status']), _cell(f'{t["verified_blockchain"]} \u2705' if permit.blockchain_tx_hash else t['hash_local'])])

            bc_table = Table(bc_data, colWidths=[5*cm, 11*cm])
            bc_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (0, -1), rl_colors.HexColor('#ECFDF5')),
                ('GRID', (0, 0), (-1, -1), 0.5, rl_colors.HexColor('#D1D5DB')),
                ('PADDING', (0, 0), (-1, -1), 8),
                ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
            ]))
            elements.append(bc_table)
            elements.append(Spacer(1, 0.6*cm))

        elements.append(HRFlowable(width="100%", thickness=0.5, color=rl_colors.HexColor('#E5E7EB'), spaceAfter=12))

        qr_label = ParagraphStyle('QRLabel', parent=styles['Normal'], fontSize=10, textColor=rl_colors.HexColor('#6B7280'), alignment=TA_CENTER, spaceAfter=6)
        elements.append(Paragraph(t['scan_verify'], qr_label))
        elements.append(RLImage(qr_path, width=3.5*cm, height=3.5*cm, hAlign='CENTER'))

        if etherscan_url:
            elements.append(Spacer(1, 0.3*cm))
            elements.append(Paragraph(f'<a href="{etherscan_url}" color="#0D9488">{t["view_etherscan"]} \u2197</a>', link_style))

        elements.append(Spacer(1, 0.8*cm))
        elements.append(HRFlowable(width="100%", thickness=2, color=gold_color, spaceAfter=8))
        elements.append(Paragraph(t['footer_digital'], footer_style))
        elements.append(Paragraph(f"{t['footer_generated']} {datetime.utcnow().strftime('%B %d, %Y at %H:%M UTC')}", footer_style))
        elements.append(Paragraph(t['footer_tamper'], footer_style))

        doc.build(elements)
        pdf_bytes = pdf_buffer.getvalue()
        pdf_buffer.close()
        try:
            os.remove(qr_path)
        except OSError:
            pass
        resp = app.response_class(pdf_bytes, mimetype='application/pdf')
        resp.headers['Content-Disposition'] = f'attachment; filename=permit_certificate_{permit.id}.pdf'
        resp.headers['Content-Length'] = str(len(pdf_bytes))
        return resp
    except Exception as e:
        return jsonify({'error': f'Certificate generation failed: {str(e)}'}), 500


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
    PermitEvent.query.filter_by(permit_id=permit_id).delete()
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
        PermitEvent.query.filter_by(permit_id=permit.id).delete()
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
        PermitEvent.query.filter_by(permit_id=permit.id).delete()
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
    user = User.query.get_or_404(user_id)
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
    data = request.get_json(silent=True) or {}
    if 'full_name' in data and data['full_name'].strip():
        user.full_name = data['full_name'].strip()
    if 'email' in data and data['email'].strip():
        if not _valid_email(data['email'].strip()):
            return jsonify({'error': 'Invalid email format'}), 400
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
        PermitEvent.query.filter_by(permit_id=permit.id).delete()
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
