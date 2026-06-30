import app as appmod


def test_certificate_for_completed_permit_returns_pdf(client, citizen_headers, inspector_headers, helpers):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    resp = client.get(f"/api/permits/{completed['id']}/certificate", headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.mimetype == 'application/pdf'
    assert resp.data[:4] == b'%PDF'


def test_certificate_has_attachment_disposition(client, citizen_headers, inspector_headers, helpers):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    resp = client.get(f"/api/permits/{completed['id']}/certificate", headers=citizen_headers)
    assert 'attachment' in resp.headers.get('Content-Disposition', '')


def test_certificate_only_for_completed_permits(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.get(f"/api/permits/{p['id']}/certificate", headers=citizen_headers)
    assert resp.status_code == 400


def test_certificate_unauthorized_returns_403(client, citizen_headers, inspector_headers, helpers):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    client.post('/api/auth/register', json={
        'username': 'pdfother', 'email': 'pdfother@user.com', 'password': 'secret1'
    })
    token = helpers.login(client, 'pdfother', 'secret1')
    resp = client.get(f"/api/permits/{completed['id']}/certificate", headers=helpers.auth(token))
    assert resp.status_code == 403


def test_certificate_localized_language(client, citizen_headers, inspector_headers, helpers):
    completed = helpers.complete_permit(client, citizen_headers, inspector_headers)
    resp = client.get(f"/api/permits/{completed['id']}/certificate?lang=ro", headers=citizen_headers)
    assert resp.status_code == 200
    assert resp.mimetype == 'application/pdf'


def test_font_registration_returns_three_names():
    regular, bold, mono = appmod._register_unicode_fonts()
    assert regular and bold and mono
