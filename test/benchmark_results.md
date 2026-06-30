# SmartPermits — Backend Operation Timing

Measured via the Flask test client over **10 iterations** of the main end-to-end flow (login → list → create → detail → pending → AI-analyze → review → pay → certificate).
External services (Gemini, Ethereum/Sepolia, Socket.IO) are mocked, so no network call is made.

| HTTP Method | Route | Avg (ms) | Min (ms) | Max (ms) | Category |
|---|---|---:|---:|---:|---|
| POST | `/api/auth/login` | 207.36 | 205 | 218 | CRUD |
| GET | `/api/permits` | 12.95 | 4 | 28 | CRUD |
| POST | `/api/permits` | 14.14 | 11 | 23 | CRUD |
| GET | `/api/permits/<id>` | 6.11 | 5 | 10 | CRUD |
| GET | `/api/permits/pending` | 6.99 | 5 | 13 | CRUD |
| POST | `/api/permits/<id>/ai-analyze` | 9.20 | 8 | 13 | AI (mocked) |
| POST | `/api/permits/<id>/review` | 12.33 | 11 | 15 | CRUD |
| POST | `/api/permits/<id>/pay` | 11.02 | 10 | 12 | CRUD |
| GET | `/api/permits/<id>/certificate` | 45.50 | 30 | 159 | PDF render |

**CRUD average:** 38.70 ms — reference threshold **500 ms** (within threshold).

Heavy operations: `certificate` (ReportLab PDF generation) is the most expensive route; `ai-analyze` would dominate in production because of the real Gemini round-trip, which is mocked here. Plain GETs and CRUD writes are well under the threshold.
