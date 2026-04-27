import hashlib
import os
import json
from datetime import datetime


def compute_permit_hash(permit_id, permit_type, description, status, documents_data, created_at_iso=None):
    payload = {
        'permit_id': permit_id,
        'permit_type': permit_type,
        'description': description or '',
        'status': status,
        'timestamp': created_at_iso or '',
        'documents': documents_data
    }
    raw = json.dumps(payload, sort_keys=True).encode('utf-8')
    return hashlib.sha256(raw).hexdigest()


def write_hash_to_blockchain(permit_hash):
    private_key = os.environ.get('ETH_PRIVATE_KEY', '')
    rpc_url = os.environ.get('ETH_RPC_URL', '')
    wallet_address = os.environ.get('ETH_WALLET_ADDRESS', '')

    if not private_key or not rpc_url or not wallet_address:
        return None, 'Blockchain not configured: missing ETH_PRIVATE_KEY, ETH_RPC_URL, or ETH_WALLET_ADDRESS'

    if not private_key.startswith('0x'):
        private_key = '0x' + private_key

    try:
        from web3 import Web3

        w3 = Web3(Web3.HTTPProvider(rpc_url))

        if not w3.is_connected():
            return None, 'Cannot connect to Ethereum network'

        chain_id = w3.eth.chain_id
        nonce = w3.eth.get_transaction_count(wallet_address)
        gas_price = w3.eth.gas_price

        tx = {
            'nonce': nonce,
            'to': wallet_address,
            'value': 0,
            'gas': 50000,
            'gasPrice': gas_price,
            'data': w3.to_bytes(hexstr='0x' + permit_hash),
            'chainId': chain_id
        }

        signed = w3.eth.account.sign_transaction(tx, private_key)
        tx_hash = w3.eth.send_raw_transaction(signed.raw_transaction)
        tx_hex = w3.to_hex(tx_hash)

        return tx_hex, None

    except ImportError:
        return None, 'web3 package not installed'
    except Exception as e:
        return None, f'Blockchain error: {str(e)}'


def notarize_permit(permit, documents, upload_folder):
    docs_data = []
    for doc in documents:
        file_path = doc.file_path
        file_hash = ''
        try:
            with open(file_path, 'rb') as f:
                file_hash = hashlib.sha256(f.read()).hexdigest()
        except Exception:
            file_hash = 'file_not_readable'
        docs_data.append({
            'file_name': doc.file_name,
            'document_label': doc.document_label or '',
            'file_hash': file_hash
        })

    permit_hash = compute_permit_hash(
        permit.id,
        permit.permit_type,
        permit.description,
        permit.status,
        docs_data,
        permit.created_at.isoformat() if permit.created_at else datetime.utcnow().isoformat()
    )

    tx_hash, error = write_hash_to_blockchain(permit_hash)

    return permit_hash, tx_hash, error

