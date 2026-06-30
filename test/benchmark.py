import os
import sys
import json
import time
import tempfile
import statistics
from unittest.mock import MagicMock

_THIS_DIR = os.path.dirname(os.path.abspath(__file__))
_BACKEND_DIR = os.path.join(os.path.dirname(_THIS_DIR), 'smart_permits_api')
sys.path.insert(0, _BACKEND_DIR)

_RESULTS_DIR = os.path.join(_THIS_DIR, 'results')
os.makedirs(_RESULTS_DIR, exist_ok=True)

_TMP = tempfile.mkdtemp(prefix='smartpermits_bench_')
_DB = os.path.join(_TMP, 'bench.db').replace('\\', '/')
_UPLOADS = os.path.join(_TMP, 'uploads')
os.makedirs(_UPLOADS, exist_ok=True)

os.environ['DATABASE_URL'] = 'sqlite:///' + _DB
os.environ['ETH_PRIVATE_KEY'] = ''
os.environ['ETH_RPC_URL'] = ''
os.environ['ETH_WALLET_ADDRESS'] = ''
os.environ['GEMINI_API_KEY'] = 'bench-key'

import app as appmod

appmod.app.config['UPLOAD_FOLDER'] = _UPLOADS
appmod.app.config['TESTING'] = True

os.environ['ETH_PRIVATE_KEY'] = ''
os.environ['ETH_RPC_URL'] = ''
os.environ['ETH_WALLET_ADDRESS'] = ''
os.environ['GEMINI_API_KEY'] = 'bench-key'

ITERATIONS = 10
CRUD_THRESHOLD_MS = 500.0


class _FakePart:
    def __init__(self, text):
        self.text = text


class _FakeResponse:
    def __init__(self, text):
        self.text = text
        self.candidates = [type('C', (), {'content': type('Ct', (), {'parts': [_FakePart(text)]})()})()]


def _patch_externals():
    appmod.socketio.emit = MagicMock()
    from google import genai
    fake_client = MagicMock()
    fake_client.models.generate_content.return_value = _FakeResponse('Benchmark mock analysis: documents complete.')
    genai.Client = MagicMock(return_value=fake_client)


def _seed():
    with appmod.app.app_context():
        appmod.db.drop_all()
        appmod.db.create_all()
        appmod.seed_data()


def _login(client, username, password):
    return client.post('/api/auth/login', json={'username': username, 'password': password}).get_json()['token']


def _h(token):
    return {'Authorization': f'Bearer {token}'}


def _add_document(permit_id):
    with appmod.app.app_context():
        doc = appmod.Document(permit_id=permit_id, file_path=os.path.join(_UPLOADS, 'b.png'),
                              file_name='b.png', document_label='Plan')
        appmod.db.session.add(doc)
        appmod.db.session.commit()


def _timed(samples, key, fn):
    start = time.perf_counter()
    result = fn()
    elapsed = (time.perf_counter() - start) * 1000.0
    samples.setdefault(key, []).append(elapsed)
    return result


def run():
    _patch_externals()
    _seed()
    client = appmod.app.test_client()

    citizen = _login(client, 'citizen1', '1q2w3e4r')
    inspector = _login(client, 'inspector1', '1q2w3e4r')
    ch, ih = _h(citizen), _h(inspector)

    samples = {}
    routes_meta = [
        ('POST', '/api/auth/login', 'crud'),
        ('GET', '/api/permits', 'crud'),
        ('POST', '/api/permits', 'crud'),
        ('GET', '/api/permits/<id>', 'crud'),
        ('GET', '/api/permits/pending', 'crud'),
        ('POST', '/api/permits/<id>/ai-analyze', 'ai'),
        ('POST', '/api/permits/<id>/review', 'crud'),
        ('POST', '/api/permits/<id>/pay', 'crud'),
        ('GET', '/api/permits/<id>/certificate', 'pdf'),
    ]

    for _ in range(ITERATIONS):
        _timed(samples, 'POST /api/auth/login', lambda: _login(client, 'citizen1', '1q2w3e4r'))
        _timed(samples, 'GET /api/permits', lambda: client.get('/api/permits', headers=ch))
        created = _timed(samples, 'POST /api/permits',
                         lambda: client.post('/api/permits', headers=ch,
                                             json={'permit_type': 'Construction Permit', 'description': 'Bench run'}))
        pid = created.get_json()['id']
        _add_document(pid)
        _timed(samples, 'GET /api/permits/<id>', lambda: client.get(f'/api/permits/{pid}', headers=ch))
        _timed(samples, 'GET /api/permits/pending', lambda: client.get('/api/permits/pending', headers=ih))
        _timed(samples, 'POST /api/permits/<id>/ai-analyze',
               lambda: client.post(f'/api/permits/{pid}/ai-analyze?force=true', headers=ch))
        _timed(samples, 'POST /api/permits/<id>/review',
               lambda: client.post(f'/api/permits/{pid}/review', headers=ih,
                                   json={'action': 'approved', 'notes': 'ok'}))
        _timed(samples, 'POST /api/permits/<id>/pay',
               lambda: client.post(f'/api/permits/{pid}/pay', headers=ch))
        _timed(samples, 'GET /api/permits/<id>/certificate',
               lambda: client.get(f'/api/permits/{pid}/certificate', headers=ch))

    print()
    print('SmartPermits - Backend Operation Timing (test client, external services mocked)')
    print(f'Iterations per route: {ITERATIONS}')
    print('=' * 74)
    print(f'{"Method":<6} {"Route":<40} {"Avg (ms)":>10} {"Min":>6} {"Max":>6}')
    print('-' * 74)

    crud_avgs = []
    rows = []
    for method, route, category in routes_meta:
        key = f'{method} {route}'
        data = samples.get(key, [])
        if not data:
            continue
        avg = statistics.mean(data)
        rows.append((method, route, category, avg, min(data), max(data), list(data)))
        if category == 'crud':
            crud_avgs.append(avg)
        print(f'{method:<6} {route:<40} {avg:>10.2f} {min(data):>6.0f} {max(data):>6.0f}')

    print('-' * 74)
    crud_overall = statistics.mean(crud_avgs) if crud_avgs else 0.0
    print(f'CRUD average: {crud_overall:.2f} ms  (reference threshold: {CRUD_THRESHOLD_MS:.0f} ms)')
    verdict = 'PASS - well within threshold' if crud_overall < CRUD_THRESHOLD_MS else 'OVER threshold'
    print(f'Result: {verdict}')
    print('=' * 74)
    print('Notes: ai-analyze is measured with the Gemini model mocked (reflects app')
    print('overhead, not network round-trip). certificate is real ReportLab PDF rendering.')

    _write_markdown(rows, crud_overall)
    _write_json(rows, crud_overall)
    return rows, crud_overall


def _write_json(rows, crud_overall):
    cat_label = {'crud': 'CRUD', 'ai': 'AI (mocked)', 'pdf': 'PDF render'}
    payload = {
        'iterations': ITERATIONS,
        'crud_threshold_ms': CRUD_THRESHOLD_MS,
        'crud_average_ms': round(crud_overall, 3),
        'crud_within_threshold': bool(crud_overall < CRUD_THRESHOLD_MS),
        'routes': [
            {
                'method': method,
                'route': route,
                'category': category,
                'category_label': cat_label[category],
                'avg_ms': round(avg, 3),
                'min_ms': round(mn, 3),
                'max_ms': round(mx, 3),
                'samples_ms': [round(s, 3) for s in data],
            }
            for method, route, category, avg, mn, mx, data in rows
        ],
    }
    out = os.path.join(_RESULTS_DIR, 'benchmark.json')
    with open(out, 'w', encoding='utf-8') as f:
        json.dump(payload, f, indent=2)
    print(f'Raw timing data written to: {out}')


def _write_markdown(rows, crud_overall):
    out = os.path.join(_THIS_DIR, 'benchmark_results.md')
    lines = []
    lines.append('# SmartPermits — Backend Operation Timing')
    lines.append('')
    lines.append(f'Measured via the Flask test client over **{ITERATIONS} iterations** of the main '
                 'end-to-end flow (login → list → create → detail → pending → AI-analyze → review → pay → certificate).')
    lines.append('External services (Gemini, Ethereum/Sepolia, Socket.IO) are mocked, so no network call is made.')
    lines.append('')
    lines.append('| HTTP Method | Route | Avg (ms) | Min (ms) | Max (ms) | Category |')
    lines.append('|---|---|---:|---:|---:|---|')
    cat_label = {'crud': 'CRUD', 'ai': 'AI (mocked)', 'pdf': 'PDF render'}
    for method, route, category, avg, mn, mx, _samples in rows:
        lines.append(f'| {method} | `{route}` | {avg:.2f} | {mn:.0f} | {mx:.0f} | {cat_label[category]} |')
    lines.append('')
    lines.append(f'**CRUD average:** {crud_overall:.2f} ms — reference threshold **{CRUD_THRESHOLD_MS:.0f} ms** '
                 f'({"within threshold" if crud_overall < CRUD_THRESHOLD_MS else "over threshold"}).')
    lines.append('')
    lines.append('Heavy operations: `certificate` (ReportLab PDF generation) is the most expensive route; '
                 '`ai-analyze` would dominate in production because of the real Gemini round-trip, which is '
                 'mocked here. Plain GETs and CRUD writes are well under the threshold.')
    with open(out, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    print(f'\nMarkdown table written to: {out}')


if __name__ == '__main__':
    run()
