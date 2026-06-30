from datetime import datetime, timedelta

import app as appmod


def test_trash_sets_deleted_at(client, citizen_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/trash", headers=citizen_headers)
    assert resp.status_code == 200
    assert appmod.Permit.query.get(p['id']).deleted_at is not None


def test_trashed_permit_appears_in_trash_listing(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post(f"/api/permits/{p['id']}/trash", headers=citizen_headers)
    data = client.get('/api/permits/trash', headers=citizen_headers).get_json()
    assert len(data) == 1
    assert data[0]['id'] == p['id']


def test_restore_clears_deleted_at(client, citizen_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers)
    client.post(f"/api/permits/{p['id']}/trash", headers=citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/restore", headers=citizen_headers)
    assert resp.status_code == 200
    assert appmod.Permit.query.get(p['id']).deleted_at is None


def test_permanent_delete_requires_trash_first(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.delete(f"/api/permits/{p['id']}/permanent-delete", headers=citizen_headers)
    assert resp.status_code == 400


def test_permanent_delete_cascades(client, citizen_headers, inspector_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    client.post(f"/api/permits/{p['id']}/comments", headers=citizen_headers, json={'message': 'hi'})
    client.post(f"/api/permits/{p['id']}/trash", headers=citizen_headers)
    resp = client.delete(f"/api/permits/{p['id']}/permanent-delete", headers=citizen_headers)
    assert resp.status_code == 200
    assert appmod.Permit.query.get(p['id']) is None
    assert appmod.Comment.query.filter_by(permit_id=p['id']).count() == 0
    assert appmod.PermitEvent.query.filter_by(permit_id=p['id']).count() == 0


def test_empty_trash_removes_all_trashed(client, citizen_headers, helpers, app_context):
    p1 = helpers.create_permit(client, citizen_headers)
    p2 = helpers.create_permit(client, citizen_headers)
    client.post(f"/api/permits/{p1['id']}/trash", headers=citizen_headers)
    client.post(f"/api/permits/{p2['id']}/trash", headers=citizen_headers)
    resp = client.delete('/api/permits/trash/empty', headers=citizen_headers)
    assert resp.status_code == 200
    assert appmod.Permit.query.filter(appmod.Permit.deleted_at.isnot(None)).count() == 0


def test_cleanup_old_trash_respects_30_day_rule(client, citizen_headers, helpers, app_context):
    recent = helpers.create_permit(client, citizen_headers)
    old = helpers.create_permit(client, citizen_headers)
    r = appmod.Permit.query.get(recent['id'])
    o = appmod.Permit.query.get(old['id'])
    r.deleted_at = datetime.utcnow() - timedelta(days=5)
    o.deleted_at = datetime.utcnow() - timedelta(days=31)
    appmod.db.session.commit()
    appmod.cleanup_old_trash()
    assert appmod.Permit.query.get(recent['id']) is not None
    assert appmod.Permit.query.get(old['id']) is None


def test_trash_unauthorized_returns_403(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    client.post('/api/auth/register', json={
        'username': 'trashother', 'email': 'trashother@user.com', 'password': 'secret1'
    })
    token = helpers.login(client, 'trashother', 'secret1')
    resp = client.post(f"/api/permits/{p['id']}/trash", headers=helpers.auth(token))
    assert resp.status_code == 403
