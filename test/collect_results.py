import os
import re
import csv
import sys
import json
import subprocess

_THIS_DIR = os.path.dirname(os.path.abspath(__file__))
_RESULTS_DIR = os.path.join(_THIS_DIR, 'results')
os.makedirs(_RESULTS_DIR, exist_ok=True)

_MODULE_ORDER = ['auth', 'permits', 'ai', 'copilot', 'blockchain', 'pdf',
                 'trash', 'prediction', 'timeline', 'misc']
_MODULE_LABELS = {
    'auth': 'Auth', 'permits': 'Permits', 'ai': 'AI', 'copilot': 'Copilot',
    'blockchain': 'Blockchain', 'pdf': 'PDF', 'trash': 'Trash',
    'prediction': 'Prediction', 'timeline': 'Timeline', 'misc': 'Misc',
}


def collect_tests():
    env = dict(os.environ)
    env['PYTHONWARNINGS'] = 'ignore'
    proc = subprocess.run(
        [sys.executable, '-W', 'ignore', '-m', 'pytest', '-v', '--no-header'],
        cwd=_THIS_DIR, env=env, capture_output=True, text=True
    )
    output = proc.stdout

    with open(os.path.join(_THIS_DIR, 'green_run.txt'), 'w', encoding='utf-8') as f:
        f.write(output)

    modules = {}
    line_re = re.compile(r'^test_(\w+)\.py::(\S+)\s+(PASSED|FAILED|ERROR|SKIPPED)')
    for line in output.splitlines():
        m = line_re.match(line.strip())
        if not m:
            continue
        mod, _name, status = m.group(1), m.group(2), m.group(3)
        bucket = modules.setdefault(mod, {'passed': 0, 'failed': 0})
        if status == 'PASSED':
            bucket['passed'] += 1
        else:
            bucket['failed'] += 1

    summary_line = ''
    for line in output.splitlines():
        if re.search(r'\d+ passed', line) or re.search(r'\d+ failed', line):
            summary_line = line.strip().strip('=').strip()

    total_passed = sum(b['passed'] for b in modules.values())
    total_failed = sum(b['failed'] for b in modules.values())

    ordered = [m for m in _MODULE_ORDER if m in modules]
    ordered += [m for m in sorted(modules) if m not in _MODULE_ORDER]

    payload = {
        'total_tests': total_passed + total_failed,
        'total_passed': total_passed,
        'total_failed': total_failed,
        'summary_line': summary_line,
        'modules': [
            {
                'key': mod,
                'label': _MODULE_LABELS.get(mod, mod.capitalize()),
                'passed': modules[mod]['passed'],
                'failed': modules[mod]['failed'],
                'total': modules[mod]['passed'] + modules[mod]['failed'],
            }
            for mod in ordered
        ],
    }
    out = os.path.join(_RESULTS_DIR, 'test_summary.json')
    with open(out, 'w', encoding='utf-8') as f:
        json.dump(payload, f, indent=2)
    print(f'Test summary written to: {out}  ({total_passed} passed, {total_failed} failed)')
    return payload


def _normalize_status(raw):
    low = raw.strip().lower()
    if low.startswith('pass'):
        return 'pass'
    if low.startswith('partial'):
        return 'partial'
    return 'not_covered'


def collect_coverage():
    path = os.path.join(_THIS_DIR, 'coverage_map.md')
    cf_rows = []
    cnf_rows = []
    row_re = re.compile(r'^\|\s*(C[FN]+\d+)\s*\|(.+)\|\s*$')
    with open(path, encoding='utf-8') as f:
        for line in f:
            m = row_re.match(line.rstrip())
            if not m:
                continue
            ident = m.group(1)
            cells = [c.strip() for c in m.group(2).split('|')]
            if len(cells) < 3:
                continue
            description, test, status = cells[0], cells[1], cells[-1]
            entry = {
                'id': ident,
                'description': description,
                'test': test.replace('`', ''),
                'status': _normalize_status(status),
                'status_label': status,
            }
            if ident.startswith('CNF'):
                cnf_rows.append(entry)
            else:
                cf_rows.append(entry)

    def _counts(rows):
        return {
            'total': len(rows),
            'pass': sum(1 for r in rows if r['status'] == 'pass'),
            'partial': sum(1 for r in rows if r['status'] == 'partial'),
            'not_covered': sum(1 for r in rows if r['status'] == 'not_covered'),
        }

    payload = {
        'cf': {'counts': _counts(cf_rows), 'items': cf_rows},
        'cnf': {'counts': _counts(cnf_rows), 'items': cnf_rows},
    }
    out = os.path.join(_RESULTS_DIR, 'coverage.json')
    with open(out, 'w', encoding='utf-8') as f:
        json.dump(payload, f, indent=2)
    c = payload['cf']['counts']
    print(f"Coverage map written to: {out}  (CF: {c['pass']} pass / {c['partial']} partial / {c['not_covered']} not covered of {c['total']})")
    return payload


_STATUS_DISPLAY = {'pass': 'Pass', 'partial': 'Partial', 'not_covered': 'Not covered'}


def write_coverage_tables(coverage):
    cf = coverage['cf']
    cnf = coverage['cnf']

    csv_path = os.path.join(_THIS_DIR, 'coverage_table.csv')
    with open(csv_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f)
        writer.writerow(['category', 'id', 'description', 'test', 'status'])
        for group, items in (('CF', cf['items']), ('CNF', cnf['items'])):
            for r in items:
                writer.writerow([group, r['id'], r['description'], r['test'],
                                 _STATUS_DISPLAY.get(r['status'], r['status_label'])])

    md_path = os.path.join(_THIS_DIR, 'coverage_table.md')
    lines = ['# SmartPermits — Tabel sintetic al acoperirii funcționalităților', '']
    cc, nc = cf['counts'], cnf['counts']
    lines.append(f"**Cerințe funcționale (CF):** {cc['pass']} acoperite complet / "
                 f"{cc['partial']} parțial / {cc['not_covered']} neacoperite — din {cc['total']}.")
    lines.append('')
    lines.append(f"**Cerințe nefuncționale (CNF):** {nc['pass']} acoperite complet / "
                 f"{nc['partial']} parțial / {nc['not_covered']} neacoperite — din {nc['total']}.")
    lines.append('')

    def _table(title, items):
        out = [f'## {title}', '', '| ID | Descriere | Test(e) | Status |', '|----|-----------|---------|--------|']
        for r in items:
            test = r['test'] if r['test'] else '—'
            out.append(f"| {r['id']} | {r['description']} | {test} | {_STATUS_DISPLAY.get(r['status'], r['status_label'])} |")
        out.append('')
        return out

    lines += _table('Cerințe funcționale (CF1–CF43)', cf['items'])
    lines += _table('Cerințe nefuncționale (CNF1–CNF13)', cnf['items'])

    with open(md_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print(f'Coverage table written to: {md_path}')
    print(f'Coverage CSV written to: {csv_path}')


if __name__ == '__main__':
    collect_tests()
    cov = collect_coverage()
    write_coverage_tables(cov)
