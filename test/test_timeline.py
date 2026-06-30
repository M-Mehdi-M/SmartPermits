import app as appmod


def test_log_permit_event_adds_without_committing(client, citizen_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers)
    before = appmod.PermitEvent.query.filter_by(permit_id=p['id']).count()
    appmod.log_permit_event(p['id'], 'Test Event', actor_name='Tester', actor_role='citizen')
    appmod.db.session.rollback()
    after = appmod.PermitEvent.query.filter_by(permit_id=p['id']).count()
    assert after == before


def test_full_lifecycle_records_expected_event_types(client, citizen_headers, inspector_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    helpers.pay_permit(client, citizen_headers, p['id'])
    types = {e.event_type for e in appmod.PermitEvent.query.filter_by(permit_id=p['id']).all()}
    assert {'Submitted', 'Reviewed by Inspector', 'Payment Received', 'Certificate Issued'}.issubset(types)


def test_timeline_endpoint_returns_events(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.get(f"/api/permits/{p['id']}/timeline", headers=citizen_headers)
    assert resp.status_code == 200
    data = resp.get_json()
    assert any(e['event_type'] == 'Submitted' for e in data)


def test_timeline_unauthorized_returns_403(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post('/api/auth/register', json={
        'username': 'tlother', 'email': 'tlother@user.com', 'password': 'secret1'
    })
    token = helpers.login(client, 'tlother', 'secret1')
    resp = client.get(f"/api/permits/{p['id']}/timeline", headers=helpers.auth(token))
    assert resp.status_code == 403


def test_appointment_completion_logs_event(client, citizen_headers, inspector_headers, helpers, app_context):
    from datetime import datetime, timedelta
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    future = (datetime.utcnow() + timedelta(days=3)).strftime('%Y-%m-%d')
    appt = client.post(f"/api/permits/{p['id']}/appointment", headers=citizen_headers,
                      json={'date': future, 'time_slot': '10:00'}).get_json()
    client.put(f"/api/appointments/{appt['id']}", headers=inspector_headers, json={'status': 'completed'})
    types = {e.event_type for e in appmod.PermitEvent.query.filter_by(permit_id=p['id']).all()}
    assert 'Inspection Completed' in types
