import os

import blockchain


def test_compute_hash_is_deterministic():
    args = (1, 'Construction Permit', 'desc', 'approved', [{'file_name': 'a.png', 'file_hash': 'x'}], '2026-01-01T00:00:00')
    h1 = blockchain.compute_permit_hash(*args)
    h2 = blockchain.compute_permit_hash(*args)
    assert h1 == h2
    assert len(h1) == 64


def test_compute_hash_canonical_regardless_of_doc_order():
    docs_a = [{'b': 2, 'a': 1}]
    docs_b = [{'a': 1, 'b': 2}]
    h1 = blockchain.compute_permit_hash(1, 'T', 'd', 's', docs_a, '2026-01-01')
    h2 = blockchain.compute_permit_hash(1, 'T', 'd', 's', docs_b, '2026-01-01')
    assert h1 == h2


def test_compute_hash_changes_on_single_byte_difference():
    base = blockchain.compute_permit_hash(1, 'T', 'desc', 'approved', [], '2026-01-01')
    changed = blockchain.compute_permit_hash(1, 'T', 'descX', 'approved', [], '2026-01-01')
    assert base != changed


def test_compute_hash_handles_none_description():
    h = blockchain.compute_permit_hash(1, 'T', None, 'approved', [], '2026-01-01')
    assert len(h) == 64


def test_write_hash_missing_env_returns_error(monkeypatch):
    monkeypatch.delenv('ETH_PRIVATE_KEY', raising=False)
    monkeypatch.delenv('ETH_RPC_URL', raising=False)
    monkeypatch.delenv('ETH_WALLET_ADDRESS', raising=False)
    tx_hash, error = blockchain.write_hash_to_blockchain('a' * 64)
    assert tx_hash is None
    assert 'not configured' in error.lower()


def test_notarize_returns_hash_even_when_blockchain_fails(monkeypatch, tmp_path):
    monkeypatch.delenv('ETH_PRIVATE_KEY', raising=False)
    monkeypatch.delenv('ETH_RPC_URL', raising=False)
    monkeypatch.delenv('ETH_WALLET_ADDRESS', raising=False)

    class FakePermit:
        id = 7
        permit_type = 'Construction Permit'
        description = 'x'
        status = 'approved'
        created_at = None

    permit_hash, tx_hash, error = blockchain.notarize_permit(FakePermit(), [], str(tmp_path))
    assert len(permit_hash) == 64
    assert tx_hash is None
    assert error is not None


def test_notarize_hashes_document_files(monkeypatch, tmp_path):
    monkeypatch.delenv('ETH_PRIVATE_KEY', raising=False)
    f = tmp_path / 'doc.txt'
    f.write_text('hello world')

    class FakeDoc:
        file_path = str(f)
        file_name = 'doc.txt'
        document_label = 'Label'

    class FakePermit:
        id = 1
        permit_type = 'T'
        description = 'd'
        status = 's'
        created_at = None

    h1, _, _ = blockchain.notarize_permit(FakePermit(), [FakeDoc()], str(tmp_path))
    f.write_text('tampered content')
    h2, _, _ = blockchain.notarize_permit(FakePermit(), [FakeDoc()], str(tmp_path))
    assert h1 != h2


def test_verify_endpoint_shape_after_approval(client, citizen_headers, inspector_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    resp = client.get(f"/api/permits/{p['id']}/verify-blockchain")
    assert resp.status_code == 200
    body = resp.get_json()
    assert body['blockchain_hash'] is not None
    assert body['verified'] is False
    assert body['etherscan_url'] is None


def test_verify_endpoint_no_record(client, citizen_headers, helpers):
    p = helpers.create_permit(client, citizen_headers)
    resp = client.get(f"/api/permits/{p['id']}/verify-blockchain")
    assert resp.status_code == 200
    assert resp.get_json()['verified'] is False


def test_verify_etherscan_url_built_from_tx_hash(client, citizen_headers, inspector_headers, helpers, app_context):
    import app as appmod
    p = helpers.create_permit(client, citizen_headers)
    helpers.approve_permit(client, inspector_headers, p['id'])
    permit = appmod.Permit.query.get(p['id'])
    permit.blockchain_tx_hash = '0x' + 'd' * 64
    appmod.db.session.commit()
    body = client.get(f"/api/permits/{p['id']}/verify-blockchain").get_json()
    assert body['verified'] is True
    assert body['etherscan_url'].startswith('https://sepolia.etherscan.io/tx/0x')
