import os
import json

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Patch

plt.rcParams['font.family'] = 'DejaVu Sans'
plt.rcParams['savefig.dpi'] = 220
plt.rcParams['figure.dpi'] = 220
plt.rcParams['axes.titlesize'] = 13
plt.rcParams['axes.titleweight'] = 'bold'

C_PRIMARY = '#2B6777'
C_ACCENT = '#E0A800'
C_GREEN = '#16A34A'
C_RED = '#DC2626'
C_ORANGE = '#E68A00'
C_GREY = '#CBD5E1'
C_TEXT = '#1F2933'

_THIS_DIR = os.path.dirname(os.path.abspath(__file__))
_RESULTS_DIR = os.path.join(_THIS_DIR, 'results')
_FIGS_DIR = os.path.join(_THIS_DIR, 'figures')
os.makedirs(_FIGS_DIR, exist_ok=True)


def _load(name):
    path = os.path.join(_RESULTS_DIR, name)
    if not os.path.exists(path):
        raise SystemExit(
            f'Missing {path}. Run "python benchmark.py" and "python collect_results.py" first.')
    with open(path, encoding='utf-8') as f:
        return json.load(f)


def _short_route(route):
    return route.replace('/api/', '')


def fig_test_summary():
    data = _load('test_summary.json')
    modules = data['modules']
    labels = [m['label'] for m in modules]
    passed = [m['passed'] for m in modules]
    failed = [m['failed'] for m in modules]
    ypos = range(len(labels))

    fig, ax = plt.subplots(figsize=(9, 5.2))
    ax.barh(ypos, passed, color=C_GREEN, edgecolor='white', label='Trecute')
    ax.barh(ypos, failed, left=passed, color=C_RED, edgecolor='white', label='Eșuate')
    ax.set_yticks(list(ypos))
    ax.set_yticklabels(labels)
    ax.invert_yaxis()
    ax.set_xlabel('Număr de teste', color=C_TEXT)
    for i, (p, f) in enumerate(zip(passed, failed)):
        ax.text(p + f + 0.25, i, str(p + f), va='center', ha='left', fontsize=9, color=C_TEXT)
    ax.set_title(f"Rezultatele testelor pe module ({data['total_passed']}/{data['total_tests']} teste trecute)",
                 color=C_PRIMARY)
    ax.legend(loc='lower right', frameon=False)
    ax.spines[['top', 'right']].set_visible(False)
    ax.set_xlim(0, max(m['total'] for m in modules) + 4)
    fig.tight_layout()
    out = os.path.join(_FIGS_DIR, 'test_results_summary.png')
    fig.savefig(out)
    plt.close(fig)
    print('wrote', out)


def fig_perf_barplot():
    data = _load('benchmark.json')
    routes = sorted(data['routes'], key=lambda r: r['avg_ms'])
    threshold = data['crud_threshold_ms']
    labels = [f"{r['method']} {_short_route(r['route'])}" for r in routes]
    avgs = [r['avg_ms'] for r in routes]
    colors = [C_RED if a > threshold else C_PRIMARY for a in avgs]
    ypos = range(len(labels))

    fig, ax = plt.subplots(figsize=(9.5, 5.4))
    ax.barh(ypos, avgs, color=colors, edgecolor='white')
    ax.set_yticks(list(ypos))
    ax.set_yticklabels(labels, fontsize=9)
    ax.set_xlabel('Timp mediu (ms)', color=C_TEXT)
    for i, a in enumerate(avgs):
        ax.text(a + max(avgs) * 0.01, i, f'{a:.1f}', va='center', ha='left', fontsize=8.5, color=C_TEXT)
    ax.axvline(threshold, color=C_ORANGE, linestyle='--', linewidth=1.6)
    ax.text(threshold, len(labels) - 0.4, f'prag CRUD ({int(threshold)} ms)',
            color=C_ORANGE, ha='right', va='top', fontsize=9, rotation=90)
    crud_avg = data['crud_average_ms']
    ax.set_title(f'Timpul mediu de execuție pe rută HTTP (media CRUD = {crud_avg:.1f} ms)', color=C_PRIMARY)
    ax.spines[['top', 'right']].set_visible(False)
    ax.set_xlim(0, max(max(avgs), threshold) * 1.12)
    legend = [Patch(facecolor=C_PRIMARY, label='sub prag'), Patch(facecolor=C_RED, label='peste prag')]
    ax.legend(handles=legend, loc='lower right', frameon=False)
    fig.tight_layout()
    out = os.path.join(_FIGS_DIR, 'perf_barplot.png')
    fig.savefig(out)
    plt.close(fig)
    print('wrote', out)


def fig_coverage_donut():
    data = _load('coverage.json')
    counts = data['cf']['counts']
    values = [counts['pass'], counts['partial'], counts['not_covered']]
    labels = ['Acoperite complet', 'Parțial', 'Neacoperite']
    colors = [C_GREEN, C_ORANGE, C_RED]
    values_nz, labels_nz, colors_nz = [], [], []
    for v, l, c in zip(values, labels, colors):
        if v > 0:
            values_nz.append(v)
            labels_nz.append(f'{l} ({v})')
            colors_nz.append(c)

    fig, ax = plt.subplots(figsize=(7, 5.4))
    wedges, _ = ax.pie(values_nz, colors=colors_nz, startangle=90,
                       wedgeprops=dict(width=0.42, edgecolor='white', linewidth=2))
    ax.text(0, 0.12, str(counts['total']), ha='center', va='center', fontsize=30,
            fontweight='bold', color=C_PRIMARY)
    ax.text(0, -0.18, 'cerințe CF', ha='center', va='center', fontsize=12, color=C_TEXT)
    ax.set_title('Acoperirea cerințelor funcționale (CF1–CF43)', color=C_PRIMARY)
    ax.legend(wedges, labels_nz, loc='lower center', bbox_to_anchor=(0.5, -0.08),
              ncol=len(labels_nz), frameon=False, fontsize=9.5)
    ax.set_aspect('equal')
    fig.tight_layout()
    out = os.path.join(_FIGS_DIR, 'coverage_donut.png')
    fig.savefig(out)
    plt.close(fig)
    print('wrote', out)


def fig_perf_distribution():
    data = _load('benchmark.json')
    heaviest = sorted(data['routes'], key=lambda r: r['avg_ms'], reverse=True)[:3]
    heaviest = list(reversed(heaviest))
    labels = [f"{r['method']} {_short_route(r['route'])}" for r in heaviest]
    samples = [r['samples_ms'] for r in heaviest]

    fig, ax = plt.subplots(figsize=(9, 4.6))
    bp = ax.boxplot(samples, vert=False, patch_artist=True, widths=0.55,
                    medianprops=dict(color=C_ACCENT, linewidth=2),
                    flierprops=dict(marker='o', markerfacecolor=C_RED, markersize=4, alpha=0.6))
    for patch in bp['boxes']:
        patch.set_facecolor(C_PRIMARY)
        patch.set_alpha(0.75)
        patch.set_edgecolor(C_PRIMARY)
    for i, s in enumerate(samples):
        ax.scatter(s, [i + 1] * len(s), color=C_TEXT, s=10, zorder=3, alpha=0.5)
    ax.set_yticklabels(labels)
    ax.set_xlabel('Timp (ms)', color=C_TEXT)
    ax.set_title(f"Distribuția timpilor pe {data['iterations']} rulări (cele mai costisitoare 3 rute)",
                 color=C_PRIMARY)
    ax.spines[['top', 'right']].set_visible(False)
    fig.tight_layout()
    out = os.path.join(_FIGS_DIR, 'perf_distribution.png')
    fig.savefig(out)
    plt.close(fig)
    print('wrote', out)


if __name__ == '__main__':
    fig_test_summary()
    fig_perf_barplot()
    fig_coverage_donut()
    fig_perf_distribution()
    print('All figures written to', _FIGS_DIR)
