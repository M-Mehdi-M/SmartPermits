from datetime import datetime, timedelta

import app as appmod


def test_create_permit_sets_submitted_state_and_fee(client, citizen_headers):
    resp = client.post('/api/permits', headers=citizen_headers,
                       json={'permit_type': 'Construction Permit', 'description': 'New house'})
    assert resp.status_code == 201
    body = resp.get_json()
    assert body['status'] == 'submitted'
    assert body['fee_amount'] == appmod.FEE_TABLE['Construction Permit']
    assert body['is_paid'] is False


def test_create_permit_unknown_type_uses_default_fee(client, citizen_headers):
    resp = client.post('/api/permits', headers=citizen_headers,
                       json={'permit_type': 'Mystery Permit'})
    assert resp.status_code == 201
    assert resp.get_json()['fee_amount'] == 100.0


def test_create_permit_logs_submitted_event(client, citizen_headers, app_context):
    body = client.post('/api/permits', headers=citizen_headers,
                       json={'permit_type': 'Event Permit'}).get_json()
    events = appmod.PermitEvent.query.filter_by(permit_id=body['id']).all()
    assert any(e.event_type == 'Submitted' for e in events)


def test_create_permit_notifies_inspectors_via_socket(client, citizen_headers, socket_mock):
    client.post('/api/permits', headers=citizen_headers, json={'permit_type': 'Event Permit'})
    rooms = [kw.get('room') for _, kw in socket_mock.call_args_list]
    events = [args[0] for args, _ in socket_mock.call_args_list]
    assert 'new_permit_submitted' in events
    assert 'inspectors' in rooms


def test_create_permit_missing_type_returns_400(client, citizen_headers):
    resp = client.post('/api/permits', headers=citizen_headers, json={'description': 'no type'})
    assert resp.status_code == 400


def test_inspector_cannot_create_permit_returns_403(client, inspector_headers):
    resp = client.post('/api/permits', headers=inspector_headers,
                       json={'permit_type': 'Construction Permit'})
    assert resp.status_code == 403


def test_list_my_permits_returns_only_owner_permits(client, citizen_headers, helpers):
    helpers.create_permit(client, citizen_headers, permit_type='Signage Permit')
    resp = client.get('/api/permits', headers=citizen_headers)
    assert resp.status_code == 200
    data = resp.get_json()
    assert len(data) == 1
    assert data[0]['permit_type'] == 'Signage Permit'


def test_list_excludes_trashed_permits(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post(f"/api/permits/{p['id']}/trash", headers=citizen_headers)
    data = client.get('/api/permits', headers=citizen_headers).get_json()
    assert data == []


def test_list_search_filter(client, citizen_headers, helpers):
    helpers.create_permit(client, citizen_headers, permit_type='Construction Permit')
    helpers.create_permit(client, citizen_headers, permit_type='Business License')
    data = client.get('/api/permits?search=Business', headers=citizen_headers).get_json()
    assert len(data) == 1
    assert data[0]['permit_type'] == 'Business License'


def test_list_status_filter(client, citizen_headers, inspector_headers, helpers):
    p1 = helpers.create_permit(client, citizen_headers)
    helpers.create_permit(client, citizen_headers, permit_type='Event Permit')
    helpers.approve_permit(client, inspector_headers, p1['id'])
    data = client.get('/api/permits?status=approved', headers=citizen_headers).get_json()
    assert len(data) == 1
    assert data[0]['id'] == p1['id']


def test_list_type_filter(client, citizen_headers, helpers):
    helpers.create_permit(client, citizen_headers, permit_type='Event Permit')
    helpers.create_permit(client, citizen_headers, permit_type='Signage Permit')
    data = client.get('/api/permits?type=Event Permit', headers=citizen_headers).get_json()
    assert len(data) == 1


def test_get_permit_owner_access(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.get(f"/api/permits/{p['id']}", headers=citizen_headers)
    assert resp.status_code == 200


def test_get_permit_inspector_access(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.get(f"/api/permits/{p['id']}", headers=inspector_headers)
    assert resp.status_code == 200


def test_get_permit_other_citizen_forbidden(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post('/api/auth/register', json={
        'username': 'intruder', 'email': 'intruder@user.com', 'password': 'secret1'
    })
    token = helpers.login(client, 'intruder', 'secret1')
    resp = client.get(f"/api/permits/{p['id']}", headers=helpers.auth(token))
    assert resp.status_code == 403


def test_get_permit_not_found_returns_404(client, citizen_headers):
    resp = client.get('/api/permits/9999', headers=citizen_headers)
    assert resp.status_code == 404


def test_review_approve_sets_state_and_reviewer(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/review", headers=inspector_headers,
                      json={'action': 'approved', 'notes': 'All good'})
    assert resp.status_code == 200
    body = resp.get_json()
    assert body['status'] == 'approved'
    assert body['reviewer_notes'] == 'All good'
    assert body['reviewed_by'] == helpers.inspector_id()


def test_review_reject_sets_state(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    body = client.post(f"/api/permits/{p['id']}/review", headers=inspector_headers,
                      json={'action': 'rejected', 'notes': 'Incomplete'}).get_json()
    assert body['status'] == 'rejected'


def test_review_double_review_guarded(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    resp = client.post(f"/api/permits/{p['id']}/review", headers=inspector_headers,
                      json={'action': 'rejected'})
    assert resp.status_code == 400


def test_review_invalid_action_returns_400(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/review", headers=inspector_headers,
                      json={'action': 'maybe'})
    assert resp.status_code == 400


def test_review_by_citizen_forbidden(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/review", headers=citizen_headers,
                      json={'action': 'approved'})
    assert resp.status_code == 403


def test_review_logs_event_and_notifies_owner(client, citizen_headers, inspector_headers, helpers, socket_mock, app_context):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    events = [e.event_type for e in appmod.PermitEvent.query.filter_by(permit_id=p['id']).all()]
    assert 'Reviewed by Inspector' in events
    emitted = [args[0] for args, _ in socket_mock.call_args_list]
    assert 'permit_status_updated' in emitted


def test_approve_sets_blockchain_hash_locally(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    body = helpers.approve_permit(client, inspector_headers, p['id'])
    assert body['blockchain_hash'] is not None
    assert len(body['blockchain_hash']) == 64
    assert body['blockchain_tx_hash'] is None
    assert body['blockchain_error']


def test_pay_completes_permit_and_sets_expiry(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers, permit_type='Construction Permit')
    helpers.approve_permit(client, inspector_headers, p['id'])
    body = helpers.pay_permit(client, citizen_headers, p['id'])
    assert body['status'] == 'completed'
    assert body['is_paid'] is True
    assert body['expires_at'] is not None


def test_pay_occupancy_certificate_has_no_expiry(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers, permit_type='Occupancy Certificate')
    helpers.approve_permit(client, inspector_headers, p['id'])
    body = helpers.pay_permit(client, citizen_headers, p['id'])
    assert body['status'] == 'completed'
    assert body['expires_at'] is None


def test_pay_non_approved_returns_400(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/pay", headers=citizen_headers)
    assert resp.status_code == 400


def test_pay_already_paid_returns_400(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    helpers.pay_permit(client, citizen_headers, p['id'])
    resp = client.post(f"/api/permits/{p['id']}/pay", headers=citizen_headers)
    assert resp.status_code == 400


def test_pay_logs_payment_and_certificate_events(client, citizen_headers, inspector_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    helpers.pay_permit(client, citizen_headers, p['id'])
    events = [e.event_type for e in appmod.PermitEvent.query.filter_by(permit_id=p['id']).all()]
    assert 'Payment Received' in events
    assert 'Certificate Issued' in events


def test_renew_creates_new_submitted_permit_with_link(client, citizen_headers, inspector_headers, helpers):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    resp = client.post(f"/api/permits/{completed['id']}/renew", headers=citizen_headers)
    assert resp.status_code == 201
    body = resp.get_json()
    assert body['status'] == 'submitted'
    assert body['renewed_from'] == completed['id']
    assert body['permit_type'] == completed['permit_type']


def test_reapply_rejected_permit_allowed(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post(f"/api/permits/{p['id']}/review", headers=inspector_headers, json={'action': 'rejected'})
    resp = client.post(f"/api/permits/{p['id']}/renew", headers=citizen_headers)
    assert resp.status_code == 201


def test_renew_submitted_permit_returns_400(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/renew", headers=citizen_headers)
    assert resp.status_code == 400


def test_expire_sweep_marks_completed_past_expiry(client, citizen_headers, inspector_headers, helpers, app_context):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    permit = appmod.Permit.query.get(completed['id'])
    permit.expires_at = datetime.utcnow() - timedelta(days=1)
    appmod.db.session.commit()
    count = appmod.sweep_expired_permits()
    assert count >= 1
    assert appmod.Permit.query.get(completed['id']).status == 'expired'


def test_expire_check_endpoint(client, citizen_headers):
    resp = client.post('/api/permits/expire-check', headers=citizen_headers)
    assert resp.status_code == 200
    assert 'expired' in resp.get_json()


def test_pending_list_inspector_only(client, citizen_headers, inspector_headers, helpers):
    helpers.create_permit(client, citizen_headers)
    resp_citizen = client.get('/api/permits/pending', headers=citizen_headers)
    assert resp_citizen.status_code == 403
    resp_insp = client.get('/api/permits/pending', headers=inspector_headers)
    assert resp_insp.status_code == 200
    assert len(resp_insp.get_json()) == 1


def test_reviewed_list_returns_inspector_decisions(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    data = client.get('/api/permits/reviewed', headers=inspector_headers).get_json()
    assert len(data) == 1
    assert data[0]['id'] == p['id']
