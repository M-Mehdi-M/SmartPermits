# SmartPermits

A modern Android + Flask full-stack platform that digitizes the municipal permit and licensing process. Citizens can apply for permits, upload documents, and make payments while inspectors review, approve, or reject applications from their mobile dashboard. Includes AI-powered document verification using Google Gemini, blockchain permit notarization on Ethereum Sepolia, predictive wait time estimation, real-time status timelines, multi-language support, and smart permit expiry tracking.

## Features

### Core Features
- **Online Application** - Single-page permit application with permit type selection, description, fee preview, document upload checklist, and optional map location
- **Permit Location Map** - For Construction and Renovation permits, citizens pin the exact work location on an interactive OpenStreetMap (osmdroid). A crosshair overlay marks the center of the map, users drag the map to position it and press the "Pin Here" button to place the marker. Includes a location search bar where users can type a city or address name and the map navigates there using Android Geocoder. Touch events are properly intercepted so the map scrolls independently from the page. The map pin with coordinates is visible to inspectors on the review screen and to citizens on the permit detail screen. No API key required
- **Document Checklist** - Each permit type displays a checklist of required documents (e.g., Urban Planning Certificate, Authorized Technical Project, Utility Approvals for Construction Permits). Each document has an individual upload button with a checkbox that marks green when uploaded
- **Role-Based Dashboards** - Separate interfaces for citizens and inspectors with tailored workflows
- **Navigation Drawer** - Professional side menu on both dashboards with quick access to all features
- **Permit Tracking** - Real-time status tracking with color-coded chips (Submitted, Approved, Rejected, Completed)
- **Permit History** - Filterable history view with tabs (All, Approved, Completed, Rejected)
- **Inspector Review** - Inspectors view pending applications, review details, see the work location on a map, preview all attached documents in a vertical ordered list with numbered labels (e.g., "#1 — Urban Planning Certificate") and thumbnails, then approve or reject with notes
- **Review History** - Inspectors can browse past reviewed permits filtered by outcome
- **Payment Simulation** - Citizens pay fees for approved permits, changing status to Completed
- **Delete Account** - Users can permanently delete their account and all associated data from the profile screen with confirmation dialog

### AI Features
- **AI Document Verification** - When a citizen submits a permit application with uploaded documents, the system automatically sends all document images to Google Gemini 2.5 Flash in a single API request. The AI analyzes every document together, identifies document types, extracts key information (dates, names, stamps), checks completeness against permit requirements, flags issues (blurry images, expired dates, missing stamps, irrelevant files), and provides a structured recommendation. The AI analysis is rendered with formatted text (bold headings, bullet points, styled sections) using a markdown-to-Spannable converter. The inspector sees a collapsible AI analysis card on the review screen — showing a compact 4-line preview, tappable to open the full analysis in a dialog. Only one AI request is made per permit submission to minimize cost
- **Instant Submission** - Permit submission is instant for the citizen. The AI analysis runs in the background (fire-and-forget) so the user sees the success screen immediately without waiting. The inspector can view the analysis when it completes, or trigger it manually if needed
- **Manual AI Trigger** - If the AI analysis was not available at submission time (e.g., API key not configured), the inspector can manually trigger it from the review screen using the "Run AI Analysis" button
- **Environment Variable Configuration** - The Gemini API key is stored in a `.env` file inside `smart_permits_api/`, never hardcoded

### Advanced Features
- **Blockchain Permit Notarization** - When an inspector approves a permit, a SHA-256 hash of the permit data and all uploaded documents is computed and written to the Ethereum Sepolia testnet via web3.py. This creates a tamper-proof, publicly verifiable record that the permit was issued at a specific time with specific documents. The blockchain transaction hash and document hash are stored with the permit and displayed on the permit detail screen. Citizens and inspectors can tap "View on Etherscan" to see the on-chain record. The PDF certificate includes the blockchain transaction ID, a clickable Etherscan link, and a QR code linking to the Etherscan page. No real ETH is required (Sepolia testnet uses free test ETH)
- **Real-Time Status Timeline / Audit Trail** - A visual timeline on the permit detail screen shows every state change with timestamps: "Submitted → Documents Analyzed by AI → Reviewed by Inspector → Approved → Payment Received → Appointment Scheduled → Inspection Completed → Certificate Issued". Each entry has a timestamp, the actor name, their role, and any notes. This blockchain-inspired transparency feature logs and displays every action for full accountability and auditability
- **Predictive Wait Time with ML** - Instead of a simple average processing time, the system uses historical data to predict the specific wait time for each submission based on: permit type historical average, number of documents uploaded, current inspector workload (pending queue size), day of week submitted, and historical patterns. Displayed as a range (e.g., "2–4 days") with a confidence percentage indicator
- **Smart Deadline Reminders & Expiry Tracking** - Permits have expiry dates set when completed based on permit type (e.g., 365 days for Construction, 30 days for Events). Citizens see a countdown on their dashboard cards. Expiring permits (≤30 days) are flagged in amber. Expired permits are flagged in red. Citizens can start a renewal directly from the permit detail screen. The permit detail screen shows a validity card with the expiry date
- **Push Notifications** - When an inspector approves/rejects a permit, notifications are sent to the citizen via Firebase Cloud Messaging (FCM). Comments and appointment scheduling also trigger notifications
- **Analytics Dashboard** - Inspector statistics screen showing total permits reviewed, approval vs rejection ratio (pie chart), average review time, and busiest permit types (bar chart) using MPAndroidChart
- **Appointment Scheduling** - After approval, citizens schedule on-site inspection appointments using a date picker and time slot selector
- **In-App Chat / Comments** - Comment thread on each permit where citizens ask questions and inspectors request additional documents
- **Professional PDF Certificate** - When a permit is Completed, a downloadable PDF certificate is generated using ReportLab with: teal-branded header with certificate number, permit information table with alternating row colors, blockchain verification section with clickable Etherscan link, QR code for scanning, gold footer seal, proper typography and formatting. The certificate auto-opens in the device's PDF viewer after download
- **Search & Filter** - Search bar and filter chips on both citizen and inspector dashboards to filter by permit type, status, applicant name, and date
- **Map Location Search** - Geocoder-based search field on the permit application map allowing users to type a city, address, or landmark and navigate the map directly to that location
- **Multi-Language Support** - 10 languages supported: English, Romanian, Spanish, French, Italian, German, Portuguese, Polish, Turkish, and Ukrainian. Language can be changed from Settings and affects all menu items, labels, buttons, and UI text throughout the entire app. The selected language persists across sessions and sign-outs
- **Dark Mode** - Toggle in Settings with manual dark mode switch, follow-system option, and proper dark theme colors. Theme preference persists across sign-outs
- **Permit Renewal / Reapply** - For completed permits, Renew button creates a new application pre-filled with previous data. For rejected permits, Reapply button does the same
- **Change Password** - Change Password option in profile/settings with current password verification
- **User Profile Management** - Edit profile name, email, and upload avatar photo
- **Trash / Recycle Bin** - Soft-delete permits to trash with 30-day auto-permanent-delete, restore option, and empty trash

### Infrastructure
- **User Authentication** - Secure JWT-based login and registration with bcrypt password hashing
- **Session Persistence** - Auto-login on app restart using saved tokens in SharedPreferences
- **Theme & Language Persistence on Sign-Out** - Dark mode, follow-system, and language preferences are preserved when signing out
- **Pull-to-Refresh** - Swipe down to reload data on all dashboards
- **Containerized Backend** - Dockerfile included for production-ready deployment
- **FCM Integration** - Backend sends push notifications via Firebase Admin SDK when available

## Design

The app uses a clean, minimalist **Teal & Neutral** color palette inspired by modern apps like WhatsApp and Facebook:
- **Primary**: Teal (#0D9488) with subtle gradient accents
- **Background**: Clean near-white (#FAFAFA) with pure white cards
- **Dark mode**: Deep grey (#111827) with bright teal (#2DD4BF) accents
- **Cards**: Minimal elevation (1dp) for a flat, modern aesthetic
- **Headers**: Subtle teal gradient with rounded bottom corners
- **Typography**: Clean hierarchy with grey-900 primary text and grey-500 secondary text
- **Status chips**: Amber (submitted), Green (approved), Red (rejected), Teal (completed)

## Tech Stack

- **Mobile**: Java (Android)
- **Backend**: Python (Flask)
- **Database**: SQLite (via Flask-SQLAlchemy)
- **AI**: Google Gemini 2.5 Flash (document analysis)
- **Blockchain**: Ethereum Sepolia testnet via web3.py (permit notarization)
- **Authentication**: JWT (Flask-JWT-Extended)
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
|   +-- MainActivity.java
+-- app/src/main/res/
|   +-- layout/
|   +-- drawable/
|   +-- values/ (English - default)
|   +-- values-night/ (dark theme colors)
|   +-- values-ro/ (Romanian)
|   +-- values-es/ (Spanish)
|   +-- values-fr/ (French)
|   +-- values-it/ (Italian)
|   +-- values-de/ (German)
|   +-- values-pt/ (Portuguese)
|   +-- values-pl/ (Polish)
|   +-- values-tr/ (Turkish)
|   +-- values-uk/ (Ukrainian)
|   +-- menu/
|   +-- xml/
+-- smart_permits_api/
|   +-- app.py
|   +-- models.py
|   +-- blockchain.py
|   +-- requirements.txt
|   +-- Dockerfile
|   +-- .env
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

### Firebase Setup (Optional - for Push Notifications)

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

4. **For emulator**: Update `BASE_URL` in `RetrofitClient.java` to `http://10.0.2.2:5000/api/`

5. **For physical device**: Update `BASE_URL` in `RetrofitClient.java` to your machine's LAN IP:
   ```java
   private static final String BASE_URL = "http://192.168.1.X:5000/api/";
   ```

6. Run on device or emulator

### Docker Deployment (Backend)

```bash
cd smart_permits_api
docker build -t smart-permits-api .
docker run -p 5000:5000 -e GEMINI_API_KEY=your-key-here -e ETH_PRIVATE_KEY=your-key -e ETH_RPC_URL=your-url -e ETH_WALLET_ADDRESS=your-address smart-permits-api
```

## Default Test Accounts

| Role      | Username    | Password  | Full Name      |
|-----------|-------------|-----------|----------------|
| Citizen   | citizen1    | 1q2w3e4r  | Test User      |
| Inspector | inspector1  | 1q2w3e4r  | Test Inspector |

## API Endpoints

| Method | Endpoint                         | Auth | Description                          |
|--------|----------------------------------|------|--------------------------------------|
| POST   | `/api/auth/register`             | No   | Register new user                    |
| POST   | `/api/auth/login`                | No   | Login and get JWT token              |
| POST   | `/api/auth/change-password`      | Yes  | Change user password                 |
| POST   | `/api/auth/fcm-token`            | Yes  | Save FCM token for push notifications|
| GET    | `/api/auth/profile`              | Yes  | Get current user profile             |
| PUT    | `/api/auth/profile`              | Yes  | Update profile (name, email)         |
| POST   | `/api/auth/profile/avatar`       | Yes  | Upload profile avatar                |
| DELETE | `/api/auth/delete-account`       | Yes  | Permanently delete user account      |
| GET    | `/api/permits`                   | Yes  | Get user's permits (with search/filter) |
| POST   | `/api/permits`                   | Yes  | Create new permit application        |
| GET    | `/api/permits/{id}`              | Yes  | Get permit details (includes timeline)|
| POST   | `/api/permits/{id}/upload`       | Yes  | Upload document to permit            |
| POST   | `/api/permits/{id}/ai-analyze`   | Yes  | Trigger AI document analysis         |
| POST   | `/api/permits/{id}/pay`          | Yes  | Simulate payment (sets expiry date)  |
| POST   | `/api/permits/{id}/renew`        | Yes  | Renew/reapply for a permit           |
| POST   | `/api/permits/{id}/trash`        | Yes  | Move permit to trash                 |
| POST   | `/api/permits/{id}/restore`      | Yes  | Restore permit from trash            |
| DELETE | `/api/permits/{id}/permanent-delete` | Yes | Permanently delete permit         |
| GET    | `/api/permits/trash`             | Yes  | Get trashed permits                  |
| DELETE | `/api/permits/trash/empty`       | Yes  | Empty trash                          |
| GET    | `/api/permits/pending`           | Yes  | Get pending permits (inspector)      |
| GET    | `/api/permits/reviewed`          | Yes  | Get reviewed permits (inspector)     |
| POST   | `/api/permits/{id}/review`       | Yes  | Approve/reject permit (triggers blockchain + timeline event) |
| GET    | `/api/permits/{id}/comments`     | Yes  | Get permit comments                  |
| POST   | `/api/permits/{id}/comments`     | Yes  | Add comment to permit                |
| POST   | `/api/permits/{id}/appointment`  | Yes  | Schedule inspection appointment      |
| GET    | `/api/permits/{id}/timeline`     | Yes  | Get permit audit trail timeline      |
| GET    | `/api/appointments`              | Yes  | Get appointments                     |
| PUT    | `/api/appointments/{id}`         | Yes  | Update appointment status            |
| GET    | `/api/permits/{id}/certificate`  | Yes  | Download professional PDF certificate|
| GET    | `/api/permits/stats/analytics`   | Yes  | Get analytics data (inspector)       |
| GET    | `/api/permit-types`              | No   | List available permit types          |
| GET    | `/api/uploads/{filename}`        | No   | Download uploaded file               |
| GET    | `/api/permits/{id}/verify-blockchain` | No | Verify permit blockchain record    |

## Supported Languages

| Language   | Code | Resource Folder |
|------------|------|-----------------|
| English    | en   | values/ (default) |
| Romanian   | ro   | values-ro/      |
| Spanish    | es   | values-es/      |
| French     | fr   | values-fr/      |
| Italian    | it   | values-it/      |
| German     | de   | values-de/      |
| Portuguese | pt   | values-pt/      |
| Polish     | pl   | values-pl/      |
| Turkish    | tr   | values-tr/      |
| Ukrainian  | uk   | values-uk/      |

Language can be changed from Settings > Language. The selection persists across app restarts and sign-outs.

## Permit Statuses

| Status    | Color   | Description                          |
|-----------|---------|--------------------------------------|
| submitted | Amber   | Awaiting inspector review            |
| approved  | Emerald | Approved, awaiting payment           |
| rejected  | Red     | Rejected by inspector                |
| completed | Teal    | Approved and paid                    |

## Permit Validity / Expiry

| Permit Type           | Validity Period |
|-----------------------|-----------------|
| Construction Permit   | 365 days        |
| Renovation Permit     | 180 days        |
| Business License      | 365 days        |
| Food Service Permit   | 365 days        |
| Event Permit          | 30 days         |
| Signage Permit        | 730 days        |
| Demolition Permit     | 180 days        |
| Occupancy Certificate | Non-expiring    |

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

## Building

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
