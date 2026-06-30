import app as appmod


def test_copilot_without_key_returns_unavailable(client, citizen_headers, monkeypatch):
    monkeypatch.setenv('GEMINI_API_KEY', '')
    resp = client.post('/api/copilot/chat', headers=citizen_headers,
                       json={'messages': [{'role': 'user', 'content': 'hi'}]})
    assert resp.status_code == 200
    assert resp.get_json()['recommendation'] is None
    assert 'unavailable' in resp.get_json()['reply'].lower()


def test_copilot_empty_messages_returns_empty(client, citizen_headers, monkeypatch):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    resp = client.post('/api/copilot/chat', headers=citizen_headers, json={'messages': []})
    assert resp.status_code == 200
    assert resp.get_json()['recommendation'] is None


def test_copilot_recommendation_enriched_from_tables(client, citizen_headers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    resp_obj = fake_genai.function_call_response(
        'Construction Permit', 'Build a two-storey house.', 'You need a construction permit.')
    fake_genai.patch_client(monkeypatch, response=resp_obj)
    resp = client.post('/api/copilot/chat', headers=citizen_headers,
                       json={'messages': [{'role': 'user', 'content': 'I want to build a house'}]})
    assert resp.status_code == 200
    rec = resp.get_json()['recommendation']
    assert rec['permit_type'] == 'Construction Permit'
    assert rec['fee'] == appmod.FEE_TABLE['Construction Permit']
    assert rec['validity_days'] == appmod.PERMIT_VALIDITY_DAYS['Construction Permit']
    assert rec['required_documents'] == appmod.REQUIRED_DOCUMENTS['Construction Permit']
    assert rec['estimated_days_min'] <= rec['estimated_days_max']


def test_copilot_invalid_permit_type_falls_back_to_enum(client, citizen_headers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    resp_obj = fake_genai.function_call_response('Spaceport Permit', 'desc', 'summary')
    fake_genai.patch_client(monkeypatch, response=resp_obj)
    resp = client.post('/api/copilot/chat', headers=citizen_headers,
                       json={'messages': [{'role': 'user', 'content': 'x'}]})
    rec = resp.get_json()['recommendation']
    assert rec['permit_type'] in appmod.FEE_TABLE


def test_copilot_ambiguous_returns_text_reply(client, citizen_headers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    fake_genai.patch_client(monkeypatch, response=fake_genai.text_response('Could you tell me more about the project?'))
    resp = client.post('/api/copilot/chat', headers=citizen_headers,
                       json={'messages': [{'role': 'user', 'content': 'something'}]})
    body = resp.get_json()
    assert body['recommendation'] is None
    assert 'project' in body['reply'].lower()


def test_copilot_exception_returns_graceful_message(client, citizen_headers, monkeypatch, fake_genai):
    monkeypatch.setenv('GEMINI_API_KEY', 'test-key')
    fake_genai.patch_client(monkeypatch, raises=RuntimeError('api down'))
    resp = client.post('/api/copilot/chat', headers=citizen_headers,
                       json={'messages': [{'role': 'user', 'content': 'hi'}]})
    assert resp.status_code == 200
    assert 'problem' in resp.get_json()['reply'].lower()
