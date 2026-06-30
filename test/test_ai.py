import app as appmod


def test_ai_analyze_without_key_returns_200_unavailable(client, citizen_headers, helpers, monkeypatch):
    monkeypatch.setenv('GEMINI_API_KEY', '')
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.get_json()['ai_analysis'].startswith('AI analysis unavailable')


def test_ai_analyze_no_documents_returns_200_message(client, citizen_headers, helpers, monkeypatch):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    resp = client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.get_json()['ai_analysis'] == 'No documents uploaded for analysis.'


def test_ai_analyze_success_saves_result(client, citizen_headers, helpers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    helpers.add_document(p['id'])
    fake_genai.patch_client(monkeypatch, response=fake_genai.text_response('Documents look complete.'))
    resp = client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.get_json()['ai_analysis'] == 'Documents look complete.'


def test_ai_analyze_success_logs_event(client, citizen_headers, helpers, monkeypatch, fake_genai, app_context):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    helpers.add_document(p['id'])
    fake_genai.patch_client(monkeypatch, response=fake_genai.text_response('OK'))
    client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    events = [e.event_type for e in appmod.PermitEvent.query.filter_by(permit_id=p['id']).all()]
    assert 'Documents Analyzed by AI' in events


def test_ai_analyze_caches_result(client, citizen_headers, helpers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    helpers.add_document(p['id'])
    gen = fake_genai.patch_client(monkeypatch, response=fake_genai.text_response('Cached analysis'))
    client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    assert gen.call_count == 1


def test_ai_analyze_force_recomputes(client, citizen_headers, helpers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    helpers.add_document(p['id'])
    gen = fake_genai.patch_client(monkeypatch, response=fake_genai.text_response('First'))
    client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    client.post(f"/api/permits/{p['id']}/ai-analyze?force=true", headers=citizen_headers)
    assert gen.call_count == 2


def test_ai_analyze_language_change_recomputes(client, citizen_headers, helpers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    helpers.add_document(p['id'])
    gen = fake_genai.patch_client(monkeypatch, response=fake_genai.text_response('analysis'))
    client.post(f"/api/permits/{p['id']}/ai-analyze?lang=en", headers=citizen_headers)
    client.post(f"/api/permits/{p['id']}/ai-analyze?lang=ro", headers=citizen_headers)
    assert gen.call_count == 2


def test_ai_analyze_exception_returns_200_failed_message(client, citizen_headers, helpers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    helpers.add_document(p['id'])
    fake_genai.patch_client(monkeypatch, raises=RuntimeError('boom'))
    resp = client.post(f"/api/permits/{p['id']}/ai-analyze", headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.get_json()['ai_analysis'].startswith('AI analysis failed')


def test_ai_analyze_unauthorized_returns_403(client, citizen_headers, helpers, monkeypatch):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    p = helpers.create_permit(client, citizen_headers)
    client.post('/api/auth/register', json={
        'username': 'aiother', 'email': 'aiother@user.com', 'password': 'secret1'
    })
    token = helpers.login(client, 'aiother', 'secret1')
    resp = client.post(f"/api/permits/{p['id']}/ai-analyze", headers=helpers.auth(token))
    assert resp.status_code == 403
