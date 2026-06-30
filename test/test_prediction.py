from datetime import datetime, timedelta

import app as appmod


def test_estimate_wait_days_base_value(app_context):
    low, high = appmod.estimate_wait_days('Construction Permit')
    assert low == appmod.COPILOT_WAIT_BASE['Construction Permit']
    assert high == low + 2


def test_estimate_wait_days_unknown_type_default(app_context):
    low, high = appmod.estimate_wait_days('Unknown Permit')
    assert low == 3
    assert high == 5


def test_estimate_wait_days_queue_factor(client, citizen_headers, helpers, app_context):
    for _ in range(5):
        helpers.create_permit(client, citizen_headers, permit_type='Event Permit')
    low, high = appmod.estimate_wait_days('Event Permit')
    base = appmod.COPILOT_WAIT_BASE['Event Permit']
    assert low == base + 1


def test_predicted_wait_none_for_non_submitted(client, citizen_headers, inspector_headers, helpers, app_context):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    permit = appmod.Permit.query.get(completed['id'])
    est, conf, point = permit.compute_predicted_wait()
    assert est is None and conf is None and point is None


def test_predicted_wait_none_without_history(client, citizen_headers, helpers, app_context):
    p = helpers.create_permit(client, citizen_headers, permit_type='Demolition Permit')
    permit = appmod.Permit.query.get(p['id'])
    est, conf, point = permit.compute_predicted_wait()
    assert est is None


def test_predicted_wait_uses_history(client, citizen_headers, inspector_headers, helpers, app_context):
    done = helpers.create_permit(client, citizen_headers, permit_type='Signage Permit')
    helpers.approve_permit(client, inspector_headers, done['id'])
    hist = appmod.Permit.query.get(done['id'])
    hist.created_at = datetime.utcnow() - timedelta(hours=10)
    hist.updated_at = datetime.utcnow()
    appmod.db.session.commit()

    new = helpers.create_permit(client, citizen_headers, permit_type='Signage Permit')
    permit = appmod.Permit.query.get(new['id'])
    est, conf, point = permit.compute_predicted_wait()
    assert est is not None
    assert 0 < conf <= 95
    assert point is not None
