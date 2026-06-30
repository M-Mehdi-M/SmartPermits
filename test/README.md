# SmartPermits — Backend Test Suite (`test/`)

Automated unit + integration tests, a performance benchmark, and a requirements-coverage
map for the SmartPermits Flask backend. This is the implementation behind the thesis
chapter **"Testare și Validare"**.

Everything testing-related lives in this folder; production code
(`smart_permits_api/app.py`, `models.py`, `blockchain.py`) is untouched except for a
single clearly-marked hook (see [Production-code change](#production-code-change)).

---

## 1. Test strategy

**Framework:** `pytest` (test runner + fixtures) with `unittest.mock` / `pytest-mock`
for mocking and `pytest-cov` for line coverage. This is the de-facto standard for Flask
and integrates cleanly with the existing Flask test client, so no other framework was needed.

**Isolation — external services are never contacted:**
- **Database:** each test runs against a throwaway, file-backed SQLite database created in a
  temp directory (`conftest.py`). Every test gets a fresh schema (`drop_all` → `create_all`)
  plus the two seeded users, so tests never touch the real `smartpermits.db`.
- **Google Gemini:** `google.genai.Client` is patched with a fake that returns canned text or
  function-call responses (`fake_genai` fixture). No AI request leaves the machine.
- **Ethereum / Sepolia (web3):** the `_isolate_external` autouse fixture blanks the
  `ETH_*` environment variables, so notarization always takes the graceful-degradation path
  (hash computed locally, no on-chain transaction). The pure hashing functions are tested directly.
- **Socket.IO:** `socketio.emit` is replaced with a `MagicMock`, which also lets tests assert
  that the right real-time events are emitted to the right rooms.
- **Firebase:** no service-account file is present, so push notifications are already inert.

**Unit vs. integration split:**
- **Unit tests** exercise pure logic in isolation — `blockchain.compute_permit_hash`,
  `estimate_wait_days`, `Permit.compute_predicted_wait`, `log_permit_event`,
  `_register_unicode_fonts`. (`test_blockchain.py`, `test_prediction.py`, parts of `test_timeline.py`.)
- **Integration tests** drive the real HTTP routes through the Flask test client across the
  database, auth, authorization and serialization layers — registration→login→permit
  lifecycle→review→pay→certificate, etc. (`test_auth.py`, `test_permits.py`, `test_ai.py`,
  `test_copilot.py`, `test_pdf.py`, `test_trash.py`, `test_misc.py`.)

---

## 2. Folder contents

| File | Purpose |
|------|---------|
| `conftest.py` | Fixtures: isolated DB, test client, token/header helpers, mocked Gemini/web3/Socket.IO. |
| `test_auth.py` | Registration, login, JWT, change-password. |
| `test_permits.py` | Permit lifecycle: create, list/filter, review, pay, renew, expiry, role gating. |
| `test_ai.py` | AI document-analysis endpoint (graceful failure, caching, language). |
| `test_copilot.py` | Permit Copilot conversational endpoint + recommendation enrichment. |
| `test_blockchain.py` | Hash determinism, graceful degradation, verification endpoint. |
| `test_pdf.py` | Certificate PDF generation, completed-only guard, localization, fonts. |
| `test_trash.py` | Soft-delete trash, restore, permanent delete, 30-day cleanup. |
| `test_prediction.py` | Wait-time estimator and statistical prediction. |
| `test_timeline.py` | Audit/event logging and timeline. |
| `test_misc.py` | Documents upload, comments, appointments, analytics, profile, account deletion. |
| `benchmark.py` | Performance driver — times the main routes over 10 iterations; writes `results/benchmark.json` + `benchmark_results.md`. |
| `collect_results.py` | Runs the suite + parses `coverage_map.md`; writes `results/test_summary.json`, `results/coverage.json`, `green_run.txt`. |
| `make_figures.py` | Generates all PNG figures from the `results/*.json` files (matplotlib only). |
| `coverage_map.md` | CF1–CF43 / CNF1–CNF13 → test mapping (the "tabel sintetic al acoperirii"). |
| `coverage_table.md` / `coverage_table.csv` | Combined CF+CNF table generated from `results/coverage.json` (Markdown + CSV, ready to import). |
| `benchmark_results.md` | Generated timing table (produced by `benchmark.py`). |
| `green_run.txt` | Captured verbose green run (figure for the thesis). |
| `results/` | Raw machine-readable numbers behind every figure (`benchmark.json`, `test_summary.json`, `coverage.json`). |
| `figures/` | Generated thesis figures at 220 DPI (PNG). |
| `requirements-test.txt` | Test-only dependencies. |
| `pytest.ini` | Pytest configuration. |

### `results/` — raw numbers (single source of truth for the figures)

| File | Produced by | Contents |
|------|-------------|----------|
| `results/benchmark.json` | `benchmark.py` | Per-route avg/min/max **and the 10 raw samples**, CRUD average, threshold. |
| `results/test_summary.json` | `collect_results.py` | Per-module passed/failed counts and totals, parsed from the actual pytest run. |
| `results/coverage.json` | `collect_results.py` | CF/CNF counts (pass/partial/not-covered) + per-requirement rows, parsed from `coverage_map.md`. |

### `figures/` — generated images (220 DPI PNG)

| File | Source data | Shows |
|------|-------------|-------|
| `figures/test_results_summary.png` | `test_summary.json` | Horizontal bar of tests per module, pass/fail coloured; total in title. |
| `figures/perf_barplot.png` | `benchmark.json` | Avg ms per route, sorted ascending, dashed 500 ms `prag CRUD` line. |
| `figures/coverage_donut.png` | `coverage.json` | CF requirements covered fully / partial / not covered. |
| `figures/perf_distribution.png` | `benchmark.json` | Boxplot of the 10 runs for the 3 heaviest routes (variance). |

---

## 3. How to run

From the `test/` directory (Python 3.12, backend dependencies already installed):

```bash
# install the test-only dependencies
pip install -r requirements-test.txt

# run the full suite
python -m pytest

# run with coverage of the backend modules
python -m pytest --cov=app --cov=models --cov=blockchain --cov-report=term-missing

# run a single module
python -m pytest test_permits.py -v

# run the performance benchmark (writes results/benchmark.json + benchmark_results.md)
python benchmark.py
```

### Regenerate every artifact (numbers + figures)

The figures are never hardcoded — they are drawn from `results/*.json`. Full reproducible pipeline:

```bash
pip install -r requirements-test.txt   # pytest, pytest-mock, pytest-cov, matplotlib
python benchmark.py                     # -> results/benchmark.json, benchmark_results.md
python collect_results.py               # -> results/test_summary.json, results/coverage.json, green_run.txt
python make_figures.py                  # -> figures/*.png  (reads only results/*.json)
```

`make_figures.py` aborts with a clear message if a `results/*.json` file is missing, so a figure can
never be produced from stale or invented data. `matplotlib` is the only extra dependency for figures
(no seaborn); the teal/gold palette and DejaVu Sans (for Romanian diacritics) are set in the script.

> On Windows, prefix with `python -W ignore` to silence an unrelated urllib3 import warning.

---

## 4. Green run

```
============================= test session starts =============================
collected 118 items
...
======================= 118 passed in 89.41s (0:01:29) ========================
```

**118 tests, 118 passed, 0 failed.** Full per-test output is saved in
[`green_run.txt`](green_run.txt) for use as a figure.

Backend line coverage (`pytest-cov`): **app.py 78%**, **models.py 93%**, **blockchain.py 55%**,
**85% overall**. The uncovered `blockchain.py` lines are exactly the live web3 transaction-signing
code, which is intentionally never run.

---

## 5. Performance measurement

`benchmark.py` runs the main end-to-end flow 10× through the Flask test client and times each
route with `time.perf_counter()` (no production-code hook required). Representative result on the
development machine:

| HTTP Method | Route | Avg (ms) | Category |
|---|---|---:|---|
| POST | `/api/auth/login` | ~205 | CRUD (bcrypt cost factor 12) |
| GET | `/api/permits` | ~11 | CRUD |
| POST | `/api/permits` | ~14 | CRUD |
| GET | `/api/permits/<id>` | ~5 | CRUD |
| GET | `/api/permits/pending` | ~5 | CRUD |
| POST | `/api/permits/<id>/ai-analyze` | ~8 | AI (Gemini mocked) |
| POST | `/api/permits/<id>/review` | ~11 | CRUD |
| POST | `/api/permits/<id>/pay` | ~10 | CRUD |
| GET | `/api/permits/<id>/certificate` | ~48 | PDF render |

**CRUD average ≈ 37 ms**, comfortably under the **500 ms** reference threshold. The heaviest route
is the ReportLab PDF certificate; `login` is dominated by the intentional bcrypt cost factor; and
`ai-analyze` would be the real bottleneck in production because of the Gemini round-trip, which is
mocked here. Exact numbers are regenerated into `benchmark_results.md` on every run.

---

## 6. Coverage map

See [`coverage_map.md`](coverage_map.md) for the full CF1–CF43 / CNF1–CNF13 → test mapping with
Pass / Partial / Not-covered markers.

---

## 7. Production-code change

To let the suite point at an isolated database **without** altering runtime behaviour, exactly one
line in `smart_permits_api/app.py` was changed (clearly marked with a `# --- TEST HOOK ... ---`
comment):

```python
# before
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///smartpermits.db'
# after
app.config['SQLALCHEMY_DATABASE_URI'] = os.environ.get('DATABASE_URL', 'sqlite:///smartpermits.db')
```

It defaults to the original production database, so nothing changes when the app runs normally.
No other production file was modified. The request-timing measurement is done entirely from the
test client in `benchmark.py`, so **no** timing hook was added to `app.py`.

---

## 8. Assumptions about the code structure

If any of these differ in your repo, adjust `conftest.py` / `benchmark.py` accordingly:

- The backend lives in `../smart_permits_api/` relative to this folder, with modules importable as
  `app`, `models`, `blockchain` (added to `sys.path` in `conftest.py`).
- `app.py` exposes module-level `app`, `db`, `socketio`, `bcrypt`, `seed_data`, the model classes,
  and the constant tables (`FEE_TABLE`, `PERMIT_VALIDITY_DAYS`, `REQUIRED_DOCUMENTS`,
  `COPILOT_WAIT_BASE`). It seeds users `inspector1` / `citizen1`, both with password `1q2w3e4r`.
- The AI/Copilot routes read `GEMINI_API_KEY` at request time and import `google.genai` lazily; the
  blockchain code reads `ETH_PRIVATE_KEY` / `ETH_RPC_URL` / `ETH_WALLET_ADDRESS` at call time.
- `UPLOAD_FOLDER` is read from `app.config` at request time (so tests can redirect it to a temp dir).

---

## 9. Known limitations (limitări cunoscute)

- **No on-device / UI tests.** The native Android (Java) client is not exercised; only the backend
  API it depends on. Espresso/instrumented UI tests are out of scope here.
- **AI calls are mocked.** Real Google Gemini latency, token limits, and response quality are not
  measured — only the app's handling of success/failure/caching is tested.
- **Blockchain is never written on-chain.** True Sepolia transaction latency, gas estimation, nonce
  handling and confirmation times are not measured; tests cover the deterministic hashing and the
  graceful-degradation path only. (`blockchain.py` network lines therefore show as uncovered.)
- **Push notifications (FCM/Firebase) are inert** in the test environment (no service account), so
  real delivery is not validated.
- **Internationalization is path-tested, not exhaustively asserted.** English and Romanian outputs
  are checked directly; the other eight languages share the same code path but are not individually
  asserted (CNF8 = Partial).
- **Avatar upload (CF9)** has no dedicated test.
- **Concurrency / load.** Timings are single-client sequential measurements via the Flask test
  client, not a concurrent load test; absolute numbers will differ from a deployed WSGI server.
- **Dead client code.** The confirmed-dead `SESSION_EXPIRED` broadcast receiver on the Android side
  is not covered (it is unreachable and pending removal).
