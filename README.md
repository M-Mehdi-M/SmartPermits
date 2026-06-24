# SmartPermits

A modern Android + Flask full-stack platform that digitizes the municipal permit and licensing process. Citizens apply for permits, upload documents, and make payments while inspectors review, approve, or reject applications from their mobile dashboard. Includes AI-powered document verification using Google Gemini, blockchain permit notarization on Ethereum Sepolia, predictive wait time estimation, real-time status timelines, multi-language support, and smart permit expiry tracking.

## Features

### Core Features
- **Online Application** — Single-page permit application with permit type selection, description, fee preview, document upload checklist, and optional map location
- **Permit Location Map** — For Construction and Renovation permits, citizens pin the exact work location on an interactive OpenStreetMap (osmdroid). A crosshair overlay marks the center of the map, users drag the map to position it and press the "Pin Here" button to place the marker. Includes a location search bar where users can type a city or address name and the map navigates there using Android Geocoder. Touch events are properly intercepted so the map scrolls independently from the page. The map pin with coordinates is visible to inspectors on the review screen and to citizens on the permit detail screen. No API key required
- **Document Checklist** — Each permit type displays a checklist of required documents (e.g., Urban Planning Certificate, Authorized Technical Project, Utility Approvals for Construction Permits). Each document has an individual upload button with a checkbox that marks green when uploaded
- **Role-Based Dashboards** — Separate interfaces for citizens and inspectors with tailored workflows
- **Navigation Drawer** — Professional side menu on both dashboards with quick access to all features
- **Permit Tracking** — Real-time status tracking with color-coded chips (Submitted, Approved, Rejected, Completed)
- **Permit History** — Filterable history view with tabs (All, Approved, Completed, Rejected)
- **Inspector Review** — Inspectors view pending applications, review details, see the work location on a map, preview all attached documents in a vertical ordered list with numbered labels (e.g., "#1 — Urban Planning Certificate") and thumbnails, then approve or reject with notes
- **Review History** — Inspectors can browse past reviewed permits filtered by outcome
- **Payment Simulation** — Citizens pay fees for approved permits, changing status to Completed
- **Delete Account** — Users can permanently delete their account and all associated data from the profile screen with confirmation dialog

### AI Features
- **AI Document Verification** — When a citizen submits a permit application with uploaded documents, the system automatically sends all document images to Google Gemini 2.5 Flash in a single API request. The AI analyzes every document together, identifies document types, extracts key information (dates, names, stamps), checks completeness against permit requirements, flags issues (blurry images, expired dates, missing stamps, irrelevant files), and provides a structured recommendation. The AI analysis is rendered with formatted text (bold headings, bullet points, styled sections) using a markdown-to-Spannable converter. The inspector sees a collapsible AI analysis card on the review screen — showing a compact 4-line preview, tappable to open the full analysis in a dialog. Only one AI request is made per permit submission to minimize cost
- **Instant Submission** — Permit submission is instant for the citizen. The AI analysis runs in the background (fire-and-forget) so the user sees the success screen immediately without waiting. The inspector can view the analysis when it completes, or trigger it manually if needed
- **Manual AI Trigger** — If the AI analysis was not available at submission time (e.g., API key not configured), the inspector can manually trigger it from the review screen using the "Run AI Analysis" button
- **AI Language Support** — AI analysis can be requested in any of the 10 supported languages by passing `?lang=ro` (or `es`, `fr`, etc.) to the AI endpoint. Language is cached per permit. Changing the language re-runs analysis in the new language
- **Environment Variable Configuration** — The Gemini API key is stored in a `.env` file inside `smart_permits_api/`, never hardcoded
- **Model Fallback with Retry** — The AI system tries `gemini-2.5-flash`, then `gemini-2.0-flash`, then `gemini-2.5-flash-lite`. Each model retries up to 3 times on 503/429 errors before moving to the next

### Advanced Features
- **Blockchain Permit Notarization** — When an inspector approves a permit, a SHA-256 hash of the permit data and all uploaded documents is computed and written to the Ethereum Sepolia testnet via web3.py. This creates a tamper-proof, publicly verifiable record that the permit was issued at a specific time with specific documents. The blockchain transaction hash and document hash are stored with the permit and displayed on the permit detail screen. Citizens and inspectors can tap "View on Etherscan" to see the on-chain record. The PDF certificate includes the blockchain transaction ID, a clickable Etherscan link, and a QR code linking to the Etherscan page. No real ETH is required (Sepolia testnet uses free test ETH)
- **Real-Time Status Timeline / Audit Trail** — A visual timeline on the permit detail screen shows every state change with timestamps: Submitted → Documents Analyzed by AI → Reviewed by Inspector → Blockchain Notarized → Payment Received → Appointment Scheduled → Inspection Completed → Certificate Issued. Each entry has a timestamp, the actor name, their role, and any notes
- **Predictive Wait Time** — The system uses historical data to predict the specific wait time for each submission based on: permit type historical average, number of documents uploaded, current inspector workload (pending queue size), and day of week submitted. Displayed as a range (e.g., "2–4 days") with a confidence percentage indicator
- **Smart Deadline Reminders & Expiry Tracking** — Permits have expiry dates set when completed based on permit type (e.g., 365 days for Construction, 30 days for Events). Citizens see a countdown on their dashboard cards. Expiring permits (≤30 days) are flagged in amber. Expired permits are flagged in red. Citizens can start a renewal directly from the permit detail screen
- **Push Notifications** — When an inspector approves/rejects a permit, notifications are sent to the citizen via Firebase Cloud Messaging (FCM). Comments and appointment scheduling also trigger notifications. In-app real-time notifications are also delivered via Socket.IO: comment replies ring a system notification on the other party's device without any manual refresh
- **Analytics Dashboard** — Inspector statistics screen showing total permits reviewed, approval vs rejection ratio (pie chart), average review time, and busiest permit types (bar chart) using MPAndroidChart
- **Appointment Scheduling** — After approval, citizens schedule on-site inspection appointments using a date picker and time slot selector
- **In-App Chat / Comments** — Comment thread on each permit where citizens ask questions and inspectors request additional documents
- **Professional PDF Certificate** — When a permit is Completed, a downloadable government-style PDF certificate is generated using ReportLab. The design resembles a real municipal permit document with: a full-page off-white (#F8F9FA) background with white content card, classic double-line decorative border (outer dark teal #005f6b, inner gold #C9A84C offset 6pt), diagonal "ISSUED" watermark at 15% opacity, and a full-width teal footer bar. The header zone shows a gold ⚜ emblem, "OFFICIAL MUNICIPAL PERMIT CERTIFICATE" in bold teal 13pt, department subtitle in italic gray, a thick teal horizontal rule, and the certificate number formatted as № SP-00003 in a light teal box. The permit information zone uses a two-column table with alternating row fills (white / #EAF4F4), no borders, and a colored status pill badge (green for COMPLETED, teal for APPROVED, red for REJECTED). An authority block shows a disclaimer, a dashed signature line with "Municipal Inspector" title, and a dashed-circle official seal drawn in ReportLab. The blockchain verification zone renders a dark-themed inset box (#0D1B2A background, white text) showing the Ethereum Sepolia network label in green monospace, transaction hash and document hash in Courier/monospace at 7.5pt, a green checkmark verification line, and the Etherscan URL. A QR code sits to the right of the dark box with a caption. The footer bar at the bottom carries the platform name, a gold diamond, and generation timestamp in white 7.5pt. Full Unicode support for all 10 languages (Romanian ăîâșț, German äöü, Polish łźż, Turkish çğışö, Ukrainian Cyrillic, etc.) via bundled DejaVuSans fonts. The certificate auto-opens in the device's PDF viewer after download
- **Search & Filter** — Search bar and filter chips on both citizen and inspector dashboards to filter by permit type, status, applicant name, and date
- **Multi-Language Support** — 10 languages supported: English, Romanian, Spanish, French, Italian, German, Portuguese, Polish, Turkish, and Ukrainian. Language can be changed from Settings and affects all menu items, labels, buttons, and UI text throughout the entire app. The selected language persists across sessions and sign-outs
- **Dark Mode** — Toggle in Settings with manual dark mode switch, follow-system option, and proper dark theme colors. Theme preference persists across sign-outs
- **Permit Renewal / Reapply** — For completed permits, Renew button creates a new application pre-filled with previous data (permit type, description, and map location). For rejected permits, Reapply button does the same
- **Change Password** — Change Password option in profile/settings with current password verification
- **User Profile Management** — Edit profile name, email, and upload avatar photo (jpg, jpeg, png, gif, webp, bmp only)
- **Trash / Recycle Bin** — Soft-delete permits to trash with 30-day auto-permanent-delete, restore option, and empty trash

### Infrastructure
- **User Authentication** — Secure JWT-based login and registration with bcrypt password hashing. Tokens expire after 30 days
- **Session Persistence** — Auto-login on app restart using saved tokens in SharedPreferences
- **Auto Session Expiry Handling** — 401 responses automatically clear the token and broadcast `SESSION_EXPIRED` to redirect the user to login
- **Per-Permit Access Control** — Permit-scoped endpoints (detail, timeline, comments, AI analysis, certificate download, appointment updates) enforce that the caller either owns the permit or is an inspector; everyone else receives 403
- **Theme & Language Persistence on Sign-Out** — Dark mode, follow-system, and language preferences are preserved when signing out
- **Pull-to-Refresh** — Swipe down to reload data on all dashboards
- **Real-Time Updates** — Socket.IO (Flask-SocketIO + socket.io-client 2.x) keeps inspector and citizen dashboards live. New permit submissions appear on the inspector's list within seconds. Status changes push notifications to the citizen. Comment events ring system notifications on both parties' devices. A 6-second silent-poll fallback guarantees updates even when the socket is unreachable
- **Shared API/Socket Config** — `ApiConfig.java` holds a single `BASE_API_URL` and `SOCKET_SERVER_URL` constant. Change one file when switching networks
- **Containerized Backend** — Dockerfile included for production-ready deployment
- **FCM Integration** — Backend sends push notifications via Firebase Admin SDK when available
- **Auto Trash Cleanup** — Permits in trash for more than 30 days are automatically purged on server startup

## Blockchain & Hash System — How It Works

This section explains the full blockchain notarization flow in detail.

### Overview

When an inspector **approves** a permit, the backend runs `notarize_permit()` from `blockchain.py`. This does three things in sequence:
1. Computes a SHA-256 hash of the permit metadata combined with hashes of all uploaded document files
2. Writes that hash to the Ethereum Sepolia testnet as transaction data
3. Stores the resulting `blockchain_hash` and `blockchain_tx_hash` in the database

### Step 1 — Hashing the Documents

For each uploaded file on disk, the server reads the raw bytes and computes a SHA-256 hash:

```python
with open(file_path, 'rb') as f:
    file_hash = hashlib.sha256(f.read()).hexdigest()
```

This produces a 64-character hex string (e.g., `a3f4b5c6...`) for each document. This is a **fingerprint** of that file. A single changed pixel produces a completely different hash.

### Step 2 — Hashing the Permit

A JSON payload is assembled containing:
- `permit_id` — the database ID
- `permit_type` — e.g., "Construction Permit"
- `description` — the applicant's description
- `status` — "approved" at the time of notarization
- `timestamp` — the permit's `created_at` datetime in ISO format (deterministic, from the database)
- `documents` — array of `{file_name, document_label, file_hash}` for every uploaded document

This payload is JSON-serialized with sorted keys, encoded to UTF-8, then SHA-256 hashed:

```python
raw = json.dumps(payload, sort_keys=True).encode('utf-8')
permit_hash = hashlib.sha256(raw).hexdigest()
```

The result is the `blockchain_hash` — a 64-character hex string that is a fingerprint of the **entire permit including all its documents**.

The `timestamp` field uses the permit's `created_at` from the database (not the current time). This makes the hash **deterministic** — recomputing it later with the same data always produces the same result, which is necessary for independent verification.

### Step 3 — Writing to the Blockchain

The `permit_hash` is submitted as the `data` field of an Ethereum transaction:

```python
tx = {
    'to': wallet_address,       # self-transfer, 0 ETH sent
    'value': 0,
    'gas': 50000,
    'data': w3.to_bytes(hexstr='0x' + permit_hash),   # 32-byte hash as tx data
    ...
}
signed = w3.eth.account.sign_transaction(tx, private_key)
tx_hash = w3.eth.send_raw_transaction(signed.raw_transaction)
```

The transaction sends 0 ETH from the configured wallet to itself. Its sole purpose is to permanently record the `permit_hash` in the Ethereum blockchain. Once mined, this record is immutable — it cannot be altered or deleted.

The `tx_hash` returned by Ethereum (66 chars: `0x` + 64 hex) is stored as `blockchain_tx_hash`.

### Do the Actual Files Get Uploaded to the Blockchain?

**No.** Uploaded files are stored only on the Flask server in the `uploads/` folder. What goes on-chain is only the 32-byte SHA-256 hash — a mathematical fingerprint of the permit data and document hashes. The blockchain stores proof of existence, not the files themselves. This is called **hash anchoring** or **notarization by hash**.

### How Is a Permit Verified?

1. Call `GET /api/permits/{id}/verify-blockchain` (no authentication required — public verification endpoint)
2. The response includes `blockchain_hash`, `blockchain_tx_hash`, and an Etherscan link
3. To verify independently: look up the `blockchain_tx_hash` on `https://sepolia.etherscan.io/` and check the transaction's Input Data field — it should match the `blockchain_hash`
4. To recompute the hash independently: collect the same permit metadata and document file bytes, run the same SHA-256 computation, and check if the result matches `blockchain_hash`

### What If Blockchain Is Not Configured?

If `ETH_PRIVATE_KEY`, `ETH_RPC_URL`, or `ETH_WALLET_ADDRESS` are not set in `.env`, the permit still receives a `blockchain_hash` (the hash is always computed), but `blockchain_tx_hash` is null. The permit detail screen shows "Hash Recorded Locally" in amber instead of "Verified on Blockchain" in green.

### Two Hashes — What's the Difference?

| Field | What It Is |
|---|---|
| `blockchain_hash` | SHA-256 of the permit metadata + document file hashes. Proves what the permit contained |
| `blockchain_tx_hash` | Ethereum transaction identifier. This is what you look up on Etherscan |

### Why Ethereum Sepolia (Testnet)?

Sepolia is Ethereum's official test network. Transactions are real and permanent but use test ETH which has no monetary value. This allows real blockchain notarization without spending real money. Get free test ETH from https://sepoliafaucet.com/ or https://www.infura.io/faucet/sepolia

## Design

The app uses a clean, minimalist **Teal & Neutral** color palette:
- **Primary**: Teal (#0D9488) with subtle gradient accents
- **Background**: Clean near-white (#FAFAFA) with pure white cards
- **Dark mode**: Deep grey (#111827) with bright teal (#2DD4BF) accents
- **Cards**: Minimal elevation (1dp) for a flat, modern aesthetic
- **Headers**: Subtle teal gradient with rounded bottom corners
- **Status chips**: Amber (submitted), Green (approved), Red (rejected), Teal (completed)

## Tech Stack

- **Mobile**: Java (Android)
- **Backend**: Python (Flask)
- **Database**: SQLite (via Flask-SQLAlchemy)
- **AI**: Google Gemini 2.5 Flash (document analysis, with 2.0-flash and 2.5-flash-lite fallback)
- **Blockchain**: Ethereum Sepolia testnet via web3.py (permit notarization)
- **Authentication**: JWT (Flask-JWT-Extended) — 30-day token expiry
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Architecture**: Activity-based with Retrofit networking layer

### Libraries Used

| Library | Purpose |
|---------|---------|
| [Retrofit2](https://square.github.io/retrofit/) | HTTP client for REST API communication |
| [OkHttp3](https://square.github.io/okhttp/) | HTTP interceptors for auth tokens and logging |
| [Gson](https://github.com/google/gson) | JSON serialization/deserialization |
| [Glide](https://github.com/bumptech/glide) | Image loading and caching |
| [Material Components](https://material.io/develop/android) | UI components (cards, chips, FAB, text inputs, switches) |
| [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart) | Pie charts and bar charts for analytics |
| [osmdroid](https://github.com/osmdroid/osmdroid) | OpenStreetMap map view for permit location pinning (no API key needed) |
| [SwipeRefreshLayout](https://developer.android.com/reference/androidx/swiperefreshlayout/widget/SwipeRefreshLayout) | Pull-to-refresh on dashboards |
| [FileProvider](https://developer.android.com/training/camera) | Secure file sharing |
| [Flask](https://flask.palletsprojects.com/) | Backend web framework |
| [Flask-SQLAlchemy](https://flask-sqlalchemy.palletsprojects.com/) | ORM for database models |
| [Flask-JWT-Extended](https://flask-jwt-extended.readthedocs.io/) | JWT authentication |
| [Flask-Bcrypt](https://flask-bcrypt.readthedocs.io/) | Password hashing |
| [Flask-CORS](https://flask-cors.readthedocs.io/) | Cross-origin request handling |
| [ReportLab](https://www.reportlab.com/) | PDF certificate generation |
| [qrcode](https://pypi.org/project/qrcode/) | QR code generation for certificates |
| [firebase-admin](https://firebase.google.com/docs/admin/setup) | FCM push notifications from server |
| [google-genai](https://pypi.org/project/google-genai/) | Google Gemini AI for document analysis |
| [Pillow](https://pillow.readthedocs.io/) | Image processing for AI analysis |
| [python-dotenv](https://pypi.org/project/python-dotenv/) | Environment variable management from .env files |
| [web3.py](https://web3py.readthedocs.io/) | Ethereum blockchain interaction for permit notarization |

## Project Structure

```
SmartPermits/
+-- app/src/main/java/project/smartpermits/
|   +-- api/
|   |   +-- ApiService.java
|   |   +-- RetrofitClient.java
|   +-- adapters/
|   |   +-- PermitAdapter.java
|   |   +-- PendingPermitAdapter.java
|   |   +-- ChatAdapter.java
|   |   +-- TrashAdapter.java
|   +-- models/
|   |   +-- User.java, Permit.java, Document.java
|   |   +-- Comment.java, Appointment.java, PermitType.java
|   |   +-- PermitEvent.java
|   |   +-- AiAnalysisResponse.java
|   |   +-- AnalyticsResponse.java, FcmTokenRequest.java
|   |   +-- LoginRequest/Response, RegisterRequest
|   |   +-- PermitRequest, ReviewRequest, CommentRequest
|   |   +-- AppointmentRequest, ChangePasswordRequest
|   |   +-- ProfileUpdateRequest, MessageResponse, ErrorResponse
|   +-- LoginActivity.java
|   +-- CitizenDashboardActivity.java
|   +-- InspectorDashboardActivity.java
|   +-- ApplyPermitActivity.java
|   +-- PermitDetailActivity.java
|   +-- PermitReviewActivity.java
|   +-- PermitHistoryActivity.java
|   +-- ReviewHistoryActivity.java
|   +-- ChatActivity.java
|   +-- ScheduleAppointmentActivity.java
|   +-- AnalyticsActivity.java
|   +-- ProfileActivity.java
|   +-- EditProfileActivity.java
|   +-- ChangePasswordActivity.java
|   +-- SettingsActivity.java
|   +-- DocumentViewerActivity.java
|   +-- TrashActivity.java
|   +-- NotificationHelper.java
|   +-- LocaleHelper.java
|   +-- CurrencyHelper.java
|   +-- PermitTypeHelper.java
|   +-- MainActivity.java
+-- app/src/main/res/
|   +-- layout/
|   +-- drawable/
|   +-- values/           (English - default)
|   +-- values-night/     (dark theme colors)
|   +-- values-ro/        (Romanian)
|   +-- values-es/        (Spanish)
|   +-- values-fr/        (French)
|   +-- values-it/        (Italian)
|   +-- values-de/        (German)
|   +-- values-pt/        (Portuguese)
|   +-- values-pl/        (Polish)
|   +-- values-tr/        (Turkish)
|   +-- values-uk/        (Ukrainian)
|   +-- menu/
|   +-- xml/
+-- smart_permits_api/
|   +-- app.py
|   +-- models.py
|   +-- blockchain.py
|   +-- requirements.txt
|   +-- Dockerfile
|   +-- .env
|   +-- fonts/
|   |   +-- DejaVuSans.ttf
|   |   +-- DejaVuSans-Bold.ttf
|   +-- uploads/
|   +-- instance/
+-- TESTING_GUIDE.md
+-- README.md
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or higher
- Python 3.9+ (for backend)
- Android device or emulator (API 29+)

### Backend Setup

1. Navigate to the backend directory:
   ```bash
   cd smart_permits_api
   ```

2. Create and activate a virtual environment:
   ```bash
   python -m venv venv
   # Windows
   venv\Scripts\activate
   # macOS/Linux
   source venv/bin/activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Configure your Gemini API key for AI document analysis. Edit `.env` and set your actual Gemini API key (get one at https://aistudio.google.com/apikey):
   ```
   GEMINI_API_KEY=your-actual-api-key-here
   ```

5. Configure blockchain notarization (optional but recommended). Add to `.env`:
   ```
   ETH_PRIVATE_KEY=your-metamask-private-key
   ETH_RPC_URL=https://sepolia.infura.io/v3/your-project-id
   ETH_WALLET_ADDRESS=your-metamask-wallet-address
   ```
   Get free Sepolia test ETH from https://sepoliafaucet.com/ or https://www.infura.io/faucet/sepolia

6. Start the server:
   ```bash
   python app.py
   ```

7. Server runs on `http://0.0.0.0:5000` with auto-seeded test accounts

### Firebase Setup (Optional — for Push Notifications)

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app with package name `project.smartpermits`
3. Download `google-services.json` and place it in the `app/` directory
4. Download the Firebase service account JSON and place it in `smart_permits_api/` as `firebase-service-account.json`
5. Push notifications will work automatically when both files are configured

### Android Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/SmartPermits.git
   ```

2. Open the project in Android Studio

3. Sync Gradle and build the project

4. **For emulator**: Update `BASE_API_URL` and `SOCKET_SERVER_URL` in `app/src/main/java/project/smartpermits/api/ApiConfig.java` to `http://10.0.2.2:5000/api/` and `http://10.0.2.2:5000` respectively

5. **For physical device**: Update both constants in `ApiConfig.java` to your machine's current Wi-Fi IP (run `ipconfig` → "Wireless LAN adapter Wi-Fi" → IPv4 Address):
   ```java
   public static final String BASE_API_URL    = "http://192.168.1.X:5000/api/";
   public static final String SOCKET_SERVER_URL = "http://192.168.1.X:5000";
   ```

6. Run on device or emulator

### Docker Deployment (Backend)

```bash
cd smart_permits_api
docker build -t smart-permits-api .
docker run -p 5000:5000 \
  -e GEMINI_API_KEY=your-key-here \
  -e ETH_PRIVATE_KEY=your-key \
  -e ETH_RPC_URL=your-url \
  -e ETH_WALLET_ADDRESS=your-address \
  smart-permits-api
```

## Default Test Accounts

| Role      | Username    | Password  | Full Name      |
|-----------|-------------|-----------|----------------|
| Citizen   | citizen1    | 1q2w3e4r  | Test User      |
| Inspector | inspector1  | 1q2w3e4r  | Test Inspector |

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login and get JWT token |
| POST | `/api/auth/change-password` | Yes | Change user password |
| POST | `/api/auth/fcm-token` | Yes | Save FCM token for push notifications |
| GET | `/api/auth/profile` | Yes | Get current user profile |
| PUT | `/api/auth/profile` | Yes | Update profile (name, email) |
| POST | `/api/auth/profile/avatar` | Yes | Upload profile avatar (image files only) |
| DELETE | `/api/auth/delete-account` | Yes | Permanently delete user account |
| GET | `/api/permits` | Yes | Get user's permits (with search/filter) |
| POST | `/api/permits` | Yes | Create new permit application |
| GET | `/api/permits/{id}` | Yes | Get permit details (includes timeline) |
| POST | `/api/permits/{id}/upload` | Yes | Upload document to permit |
| POST | `/api/permits/{id}/ai-analyze` | Yes | Trigger AI document analysis (own permit or inspector, `?lang=en`) |
| POST | `/api/permits/{id}/pay` | Yes | Simulate payment (sets expiry date) |
| POST | `/api/permits/{id}/renew` | Yes | Renew/reapply for a permit |
| POST | `/api/permits/{id}/trash` | Yes | Move permit to trash |
| POST | `/api/permits/{id}/restore` | Yes | Restore permit from trash |
| DELETE | `/api/permits/{id}/permanent-delete` | Yes | Permanently delete permit |
| GET | `/api/permits/trash` | Yes | Get trashed permits |
| DELETE | `/api/permits/trash/empty` | Yes | Empty trash |
| GET | `/api/permits/pending` | Yes | Get pending permits (inspector only) |
| GET | `/api/permits/reviewed` | Yes | Get reviewed permits (inspector only) |
| POST | `/api/permits/{id}/review` | Yes | Approve/reject permit (triggers blockchain + timeline) |
| GET | `/api/permits/{id}/comments` | Yes | Get permit comments (own permit or inspector) |
| POST | `/api/permits/{id}/comments` | Yes | Add comment to permit (own permit or inspector) |
| POST | `/api/permits/{id}/appointment` | Yes | Schedule inspection appointment |
| GET | `/api/permits/{id}/timeline` | Yes | Get permit audit trail (own permit or inspector) |
| GET | `/api/appointments` | Yes | Get appointments |
| PUT | `/api/appointments/{id}` | Yes | Update appointment status (own permit or inspector) |
| GET | `/api/permits/{id}/certificate` | Yes | Download professional PDF certificate (own permit or inspector, `?lang=en`) |
| GET | `/api/permits/stats/analytics` | Yes | Get analytics data (inspector only) |
| GET | `/api/permit-types` | No | List available permit types with fees |
| GET | `/api/uploads/{filename}` | No | Serve uploaded file |
| GET | `/api/permits/{id}/verify-blockchain` | No | Public blockchain verification for a permit |

## Supported Languages

| Language | Code | Resource Folder |
|----------|------|-----------------|
| English | en | values/ (default) |
| Romanian | ro | values-ro/ |
| Spanish | es | values-es/ |
| French | fr | values-fr/ |
| Italian | it | values-it/ |
| German | de | values-de/ |
| Portuguese | pt | values-pt/ |
| Polish | pl | values-pl/ |
| Turkish | tr | values-tr/ |
| Ukrainian | uk | values-uk/ |

Language can be changed from Settings > Language. The selection persists across app restarts and sign-outs.

## Permit Statuses

| Status | Color | Description |
|--------|-------|-------------|
| submitted | Amber | Awaiting inspector review |
| approved | Emerald | Approved, awaiting payment |
| rejected | Red | Rejected by inspector |
| completed | Teal | Approved and paid |

## Permit Validity / Expiry

| Permit Type | Fee | Validity |
|-------------|-----|---------|
| Construction Permit | $500.00 | 365 days |
| Renovation Permit | $350.00 | 180 days |
| Business License | $150.00 | 365 days |
| Food Service Permit | $200.00 | 365 days |
| Event Permit | $100.00 | 30 days |
| Signage Permit | $75.00 | 730 days |
| Demolition Permit | $450.00 | 180 days |
| Occupancy Certificate | $120.00 | Non-expiring |

## Status Timeline Events

Every permit action is logged as an audit trail event with timestamp, actor name, and notes:

| Event Type | Triggered When |
|---|---|
| Submitted | Citizen submits a new permit application |
| Documents Analyzed by AI | Gemini AI completes document analysis |
| Reviewed by Inspector | Inspector approves or rejects the permit |
| Blockchain Notarized | Permit hash is written to Ethereum Sepolia |
| Payment Received | Citizen pays the permit fee |
| Appointment Scheduled | Citizen schedules an inspection appointment |
| Inspection Completed | Inspector marks the inspection as completed |
| Certificate Issued | Permit is completed and certificate is available |

## Document Upload

Supported file types for permit documents: `jpg`, `jpeg`, `png`, `gif`, `bmp`, `webp`, `pdf`, `doc`, `docx`

Supported file types for profile avatars: `jpg`, `jpeg`, `png`, `gif`, `webp`, `bmp`

Maximum upload size: 16 MB per file

Note: The AI document analysis reads image files directly via Pillow. PDFs and office documents are accepted for upload and mentioned to the AI by filename/label, but their content is not pixel-read.

## Known Limitations

- `GET /api/uploads/{filename}` does not require authentication. Filenames are generated with a permit ID prefix via `werkzeug.secure_filename`, providing some obscurity but not cryptographic access control
- AI analysis only directly reads image-type files. PDFs and office documents are acknowledged to the AI by name and label but pages are not extracted
- `cleanup_old_trash` runs once per server process startup, not on a recurring timer
- Comment notifications are only sent when the permit has a `reviewed_by` inspector assigned. Comments on fresh (not yet reviewed) permits do not trigger a notification to any inspector
- The Socket.IO client uses v2.x protocol which requires Flask-SocketIO 5.x on the server. Do not downgrade the `socket.io-client` dependency below `2.1.0`
- Server IP must be updated in `ApiConfig.java` whenever the development machine's Wi-Fi IP changes (common after reconnecting to a hotspot)

## Building

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
