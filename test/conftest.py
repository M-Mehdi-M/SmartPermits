import os
import sys
import tempfile
import shutil
import types as _pytypes
from unittest.mock import MagicMock

import pytest

_THIS_DIR = os.path.dirname(os.path.abspath(__file__))
_BACKEND_DIR = os.path.join(os.path.dirname(_THIS_DIR), 'smart_permits_api')
sys.path.insert(0, _BACKEND_DIR)

_TMP_ROOT = tempfile.mkdtemp(prefix='smartpermits_test_')
_DB_PATH = os.path.join(_TMP_ROOT, 'test.db').replace('\\', '/')
_UPLOAD_DIR = os.path.join(_TMP_ROOT, 'uploads')
os.makedirs(_UPLOAD_DIR, exist_ok=True)

os.environ['DATABASE_URL'] = 'sqlite:///' + _DB_PATH
os.environ.setdefault('GEMINI_API_KEY', '')
os.environ.setdefault('ETH_PRIVATE_KEY', '')
os.environ.setdefault('ETH_RPC_URL', '')
os.environ.setdefault('ETH_WALLET_ADDRESS', '')

import app as appmod

appmod.app.config['UPLOAD_FOLDER'] = _UPLOAD_DIR
appmod.app.config['TESTING'] = True
appmod.app.config['BCRYPT_LOG_ROUNDS'] = 4


def pytest_sessionfinish(session, exitstatus):
    try:
        shutil.rmtree(_TMP_ROOT, ignore_errors=True)
    except Exception:
        pass


@pytest.fixture(autouse=True)
def _clean_db():
    with appmod.app.app_context():
        appmod.db.drop_all()
        appmod.db.create_all()
        appmod.seed_data()
        appmod._trash_cleaned = False
        yield
        appmod.db.session.remove()


@pytest.fixture(autouse=True)
def _isolate_external(monkeypatch):
    monkeypatch.setenv('GEMINI_API_KEY', '')
    monkeypatch.setenv('ETH_PRIVATE_KEY', '')
    monkeypatch.setenv('ETH_RPC_URL', '')
    monkeypatch.setenv('ETH_WALLET_ADDRESS', '')


@pytest.fixture(autouse=True)
def socket_mock(monkeypatch):
    m = MagicMock()
    monkeypatch.setattr(appmod.socketio, 'emit', m)
    return m


@pytest.fixture
def client():
    return appmod.app.test_client()


@pytest.fixture
def app_context():
    with appmod.app.app_context():
        yield


def _login(client, username, password):
    resp = client.post('/api/auth/login', json={'username': username, 'password': password})
    assert resp.status_code == 200, resp.get_data(as_text=True)
    return resp.get_json()['token']


def _auth(token):
    return {'Authorization': f'Bearer {token}'}


@pytest.fixture
def citizen_token(client):
    return _login(client, 'citizen1', '1q2w3e4r')


@pytest.fixture
def inspector_token(client):
    return _login(client, 'inspector1', '1q2w3e4r')


@pytest.fixture
def citizen_headers(citizen_token):
    return _auth(citizen_token)


@pytest.fixture
def inspector_headers(inspector_token):
    return _auth(inspector_token)


@pytest.fixture
def helpers():
    return _Helpers()


class _Helpers:
    auth = staticmethod(_auth)
    login = staticmethod(_login)

    @staticmethod
    def citizen_id():
        with appmod.app.app_context():
            return appmod.User.query.filter_by(username='citizen1').first().id

    @staticmethod
    def inspector_id():
        with appmod.app.app_context():
            return appmod.User.query.filter_by(username='inspector1').first().id

    @staticmethod
    def create_permit(client, headers, permit_type='Construction Permit', description='Test project'):
        resp = client.post('/api/permits', headers=headers,
                           json={'permit_type': permit_type, 'description': description})
        assert resp.status_code == 201, resp.get_data(as_text=True)
        return resp.get_json()

    @staticmethod
    def add_document(permit_id, file_name='plan.png', label='Site Plan', file_path=None):
        with appmod.app.app_context():
            doc = appmod.Document(
                permit_id=permit_id,
                file_path=file_path or os.path.join(_UPLOAD_DIR, file_name),
                file_name=file_name,
                document_label=label,
            )
            appmod.db.session.add(doc)
            appmod.db.session.commit()
            return doc.id

    @staticmethod
    def approve_permit(client, inspector_headers, permit_id, notes='Looks good'):
        resp = client.post(f'/api/permits/{permit_id}/review', headers=inspector_headers,
                           json={'action': 'approved', 'notes': notes})
        assert resp.status_code == 200, resp.get_data(as_text=True)
        return resp.get_json()

    @staticmethod
    def pay_permit(client, citizen_headers, permit_id):
        resp = client.post(f'/api/permits/{permit_id}/pay', headers=citizen_headers)
        assert resp.status_code == 200, resp.get_data(as_text=True)
        return resp.get_json()

    @staticmethod
    def complete_permit(client, citizen_headers, inspector_headers, permit_type='Construction Permit'):
        permit = _Helpers.create_permit(client, citizen_headers, permit_type=permit_type)
        _Helpers.approve_permit(client, inspector_headers, permit['id'])
        return _Helpers.pay_permit(client, citizen_headers, permit['id'])


class _FakeFunctionCall:
    def __init__(self, args):
        self.args = args


class _FakePart:
    def __init__(self, text=None, function_call=None):
        if text is not None:
            self.text = text
        if function_call is not None:
            self.function_call = function_call


class _FakeContent:
    def __init__(self, parts):
        self.parts = parts


class _FakeCandidate:
    def __init__(self, parts):
        self.content = _FakeContent(parts)


class _FakeResponse:
    def __init__(self, text=None, parts=None):
        self.text = text or ''
        if parts is not None:
            self.candidates = [_FakeCandidate(parts)]
        else:
            self.candidates = [_FakeCandidate([_FakePart(text=text or '')])]


@pytest.fixture
def fake_genai():
    return _GenaiFactory


class _GenaiFactory:
    Part = _FakePart
    FunctionCall = _FakeFunctionCall
    Response = _FakeResponse

    @staticmethod
    def text_response(text):
        return _FakeResponse(text=text)

    @staticmethod
    def function_call_response(permit_type, suggested_description, summary):
        fc = _FakeFunctionCall({
            'permit_type': permit_type,
            'suggested_description': suggested_description,
            'summary': summary,
        })
        return _FakeResponse(parts=[_FakePart(function_call=fc)])

    @staticmethod
    def patch_client(monkeypatch, response=None, raises=None):
        from google import genai

        gen = MagicMock()
        if raises is not None:
            gen.side_effect = raises
        else:
            gen.return_value = response

        client_obj = MagicMock()
        client_obj.models.generate_content = gen

        client_cls = MagicMock(return_value=client_obj)
        monkeypatch.setattr(genai, 'Client', client_cls)
        return gen
