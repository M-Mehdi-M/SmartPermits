import io
from datetime import datetime, timedelta

import app as appmod


def test_permit_types_endpoint_lists_fee_table(client):
    resp = client.get('/api/permit-types')
    assert resp.status_code == 200
    data = resp.get_json()
    names = {item['name'] for item in data}
    assert names == set(appmod.FEE_TABLE.keys())


def test_document_upload_endpoint_accepts_image(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    data = {'file': (io.BytesIO(b'fake-image-bytes'), 'plan.png'), 'document_label': 'Site Plan'}
    resp = client.post(f"/api/permits/{p['id']}/upload", headers=citizen_headers,
                       data=data, content_type='multipart/form-data')
    assert resp.status_code == 201
    assert resp.get_json()['document_label'] == 'Site Plan'


def test_document_upload_rejects_disallowed_extension(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    data = {'file': (io.BytesIO(b'x'), 'malware.exe')}
    resp = client.post(f"/api/permits/{p['id']}/upload", headers=citizen_headers,
                       data=data, content_type='multipart/form-data')
    assert resp.status_code == 400


def test_document_upload_unauthorized(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post('/api/auth/register', json={'username': 'upother', 'email': 'up@u.com', 'password': 'secret1'})
    token = helpers.login(client, 'upother', 'secret1')
    data = {'file': (io.BytesIO(b'x'), 'plan.png')}
    resp = client.post(f"/api/permits/{p['id']}/upload", headers=helpers.auth(token),
                       data=data, content_type='multipart/form-data')
    assert resp.status_code == 403


def test_comment_post_and_get(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    post = client.post(f"/api/permits/{p['id']}/comments", headers=citizen_headers,
                      json={'message': 'When will this be reviewed?'})
    assert post.status_code == 201
    got = client.get(f"/api/permits/{p['id']}/comments", headers=inspector_headers)
    assert got.status_code == 200
    assert len(got.get_json()) == 1


def test_comment_empty_message_rejected(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/comments", headers=citizen_headers, json={'message': '   '})
    assert resp.status_code == 400


def test_comment_notifies_inspector_when_citizen_posts(client, citizen_headers, helpers, socket_mock):
    p = helpers.create_permit(client, citizen_headers)
    client.post(f"/api/permits/{p['id']}/comments", headers=citizen_headers, json={'message': 'hello'})
    emitted = [args[0] for args, _ in socket_mock.call_args_list]
    assert 'comment_notification' in emitted


def test_profile_update_changes_full_name(client, citizen_headers):
    resp = client.put('/api/auth/profile', headers=citizen_headers, json={'full_name': 'Updated Name'})
    assert resp.status_code == 200
    assert resp.get_json()['full_name'] == 'Updated Name'


def test_profile_update_duplicate_email_returns_409(client, citizen_headers):
    resp = client.put('/api/auth/profile', headers=citizen_headers, json={'email': 'inspector@city.gov'})
    assert resp.status_code == 409


def test_analytics_inspector_only(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    forbidden = client.get('/api/permits/stats/analytics', headers=citizen_headers)
    assert forbidden.status_code == 403
    ok = client.get('/api/permits/stats/analytics', headers=inspector_headers)
    assert ok.status_code == 200
    body = ok.get_json()
    assert body['total_reviewed'] == 1
    assert body['total_approved'] == 1


def test_appointment_scheduling_for_approved_permit(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    future = (datetime.utcnow() + timedelta(days=5)).strftime('%Y-%m-%d')
    resp = client.post(f"/api/permits/{p['id']}/appointment", headers=citizen_headers,
                      json={'date': future, 'time_slot': '09:00'})
    assert resp.status_code == 201


def test_appointment_rejected_for_non_approved_permit(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    future = (datetime.utcnow() + timedelta(days=5)).strftime('%Y-%m-%d')
    resp = client.post(f"/api/permits/{p['id']}/appointment", headers=citizen_headers,
                      json={'date': future, 'time_slot': '09:00'})
    assert resp.status_code == 400


def test_appointment_past_date_rejected(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    past = (datetime.utcnow() - timedelta(days=2)).strftime('%Y-%m-%d')
    resp = client.post(f"/api/permits/{p['id']}/appointment", headers=citizen_headers,
                      json={'date': past, 'time_slot': '09:00'})
    assert resp.status_code == 400


def test_appointment_double_booking_rejected(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    future = (datetime.utcnow() + timedelta(days=5)).strftime('%Y-%m-%d')
    client.post(f"/api/permits/{p['id']}/appointment", headers=citizen_headers,
               json={'date': future, 'time_slot': '09:00'})
    resp = client.post(f"/api/permits/{p['id']}/appointment", headers=citizen_headers,
                      json={'date': future, 'time_slot': '11:00'})
    assert resp.status_code == 400


def test_fcm_token_save(client, citizen_headers):
    resp = client.post('/api/auth/fcm-token', headers=citizen_headers, json={'fcm_token': 'abc123'})
    assert resp.status_code == 200


def test_delete_account_removes_user_and_permits(client, helpers):
    client.post('/api/auth/register', json={'username': 'tempuser', 'email': 'temp@u.com', 'password': 'secret1'})
    token = helpers.login(client, 'tempuser', 'secret1')
    headers = helpers.auth(token)
    helpers.create_permit(client, headers)
    resp = client.delete('/api/auth/delete-account', headers=headers)
    assert resp.status_code == 200
    relogin = client.post('/api/auth/login', json={'username': 'tempuser', 'password': 'secret1'})
    assert relogin.status_code == 401
