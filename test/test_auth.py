import app as appmod


def test_register_success_returns_token_and_user(client):
    resp = client.post('/api/auth/register', json={
        'username': 'newuser', 'email': 'new@user.com', 'password': 'secret1', 'full_name': 'New User'
    })
    assert resp.status_code == 201
    body = resp.get_json()
    assert 'token' in body and body['token']
    assert body['user']['username'] == 'newuser'
    assert body['user']['role'] == 'citizen'


def test_register_hashes_password_with_bcrypt(client, app_context):
    client.post('/api/auth/register', json={
        'username': 'hashuser', 'email': 'hash@user.com', 'password': 'secret1'
    })
    user = appmod.User.query.filter_by(username='hashuser').first()
    assert user.password_hash != 'secret1'
    assert user.password_hash.startswith('$2')
    assert appmod.bcrypt.check_password_hash(user.password_hash, 'secret1')


def test_register_missing_fields_returns_400(client):
    resp = client.post('/api/auth/register', json={'username': 'x', 'password': 'secret1'})
    assert resp.status_code == 400


def test_register_invalid_email_returns_400(client):
    resp = client.post('/api/auth/register', json={
        'username': 'bademail', 'email': 'not-an-email', 'password': 'secret1'
    })
    assert resp.status_code == 400
    assert 'email' in resp.get_json()['error'].lower()


def test_register_short_password_returns_400(client):
    resp = client.post('/api/auth/register', json={
        'username': 'shortpw', 'email': 'sp@user.com', 'password': '123'
    })
    assert resp.status_code == 400


def test_register_short_username_returns_400(client):
    resp = client.post('/api/auth/register', json={
        'username': 'ab', 'email': 'ab@user.com', 'password': 'secret1'
    })
    assert resp.status_code == 400


def test_register_duplicate_username_returns_409(client):
    payload = {'username': 'dupe', 'email': 'dupe1@user.com', 'password': 'secret1'}
    client.post('/api/auth/register', json=payload)
    resp = client.post('/api/auth/register', json={
        'username': 'dupe', 'email': 'dupe2@user.com', 'password': 'secret1'
    })
    assert resp.status_code == 409


def test_register_duplicate_email_returns_409(client):
    client.post('/api/auth/register', json={'username': 'email1', 'email': 'same@user.com', 'password': 'secret1'})
    resp = client.post('/api/auth/register', json={'username': 'email2', 'email': 'same@user.com', 'password': 'secret1'})
    assert resp.status_code == 409


def test_register_invalid_role_forced_to_citizen(client):
    resp = client.post('/api/auth/register', json={
        'username': 'fakeadmin', 'email': 'fa@user.com', 'password': 'secret1', 'role': 'admin'
    })
    assert resp.status_code == 201
    assert resp.get_json()['user']['role'] == 'citizen'


def test_register_inspector_role_allowed(client):
    resp = client.post('/api/auth/register', json={
        'username': 'insp2', 'email': 'insp2@user.com', 'password': 'secret1', 'role': 'inspector'
    })
    assert resp.status_code == 201
    assert resp.get_json()['user']['role'] == 'inspector'


def test_login_correct_password_returns_200_and_token(client):
    resp = client.post('/api/auth/login', json={'username': 'citizen1', 'password': '1q2w3e4r'})
    assert resp.status_code == 200
    assert resp.get_json()['token']


def test_login_incorrect_password_returns_401(client):
    resp = client.post('/api/auth/login', json={'username': 'citizen1', 'password': 'wrong'})
    assert resp.status_code == 401


def test_login_unknown_user_returns_401(client):
    resp = client.post('/api/auth/login', json={'username': 'ghost', 'password': 'whatever'})
    assert resp.status_code == 401


def test_jwt_identifies_current_user_via_profile(client, citizen_headers):
    resp = client.get('/api/auth/profile', headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.get_json()['username'] == 'citizen1'


def test_protected_route_without_token_returns_401(client):
    resp = client.get('/api/auth/profile')
    assert resp.status_code == 401


def test_change_password_success(client, citizen_headers):
    resp = client.post('/api/auth/change-password', headers=citizen_headers,
                       json={'current_password': '1q2w3e4r', 'new_password': 'newsecret'})
    assert resp.status_code == 200
    login = client.post('/api/auth/login', json={'username': 'citizen1', 'password': 'newsecret'})
    assert login.status_code == 200


def test_change_password_wrong_current_returns_400(client, citizen_headers):
    resp = client.post('/api/auth/change-password', headers=citizen_headers,
                       json={'current_password': 'wrong', 'new_password': 'newsecret'})
    assert resp.status_code == 400


def test_change_password_short_new_returns_400(client, citizen_headers):
    resp = client.post('/api/auth/change-password', headers=citizen_headers,
                       json={'current_password': '1q2w3e4r', 'new_password': '123'})
    assert resp.status_code == 400
