# SmartPermits

A modern Android + Flask full-stack platform that digitizes the municipal permit and licensing process. Citizens can apply for permits, upload documents, and make payments while inspectors review, approve, or reject applications from their mobile dashboard. Includes AI-powered document verification using Google Gemini.

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
- **AI Document Verification** - When a citizen submits a permit application with uploaded documents, the system automatically sends all document images to Google Gemini 2.5 Flash in a single API request. The AI analyzes every document together, identifies document types, extracts key information (dates, names, stamps), checks completeness against permit requirements, flags issues (blurry images, expired dates, missing stamps, irrelevant files), and provides a structured recommendation. The AI analysis text is rendered with proper formatting (bold headings, bullet points, styled sections) using a markdown-to-Spannable converter. The inspector sees the full AI analysis card on the review screen before making a decision. Only one AI request is made per permit submission to minimize cost
- **Manual AI Trigger** - If the AI analysis was not available at submission time (e.g., API key not configured), the inspector can manually trigger it from the review screen using the "Run AI Analysis" button
- **Environment Variable Configuration** - The Gemini API key is stored in a `.env` file inside `smart_permits_api/`, never hardcoded

### Advanced Features
- **Push Notifications** - When an inspector approves/rejects a permit, notifications are sent to the citizen via Firebase Cloud Messaging (FCM). Comments and appointment scheduling also trigger notifications
- **Analytics Dashboard** - Inspector statistics screen showing total permits reviewed, approval vs rejection ratio (pie chart), average review time, and busiest permit types (bar chart) using MPAndroidChart
- **Appointment Scheduling** - After approval, citizens schedule on-site inspection appointments using a date picker and time slot selector
- **In-App Chat / Comments** - Comment thread on each permit where citizens ask questions and inspectors request additional documents
- **PDF Permit Certificate** - When a permit is Completed, a downloadable PDF certificate with QR code is generated using ReportLab
- **Search & Filter** - Search bar and filter chips on both citizen and inspector dashboards to filter by permit type, status, applicant name, and date
- **Map Location Search** - Geocoder-based search field on the permit application map allowing users to type a city, address, or landmark and navigate the map directly to that location
- **Estimated Processing Time** - Shows citizens average review time based on historical data for each permit type
- **Dark Mode** - Toggle in Settings with manual dark mode switch, follow-system option, and proper dark theme colors
- **Permit Renewal / Reapply** - For completed permits, Renew button creates a new application pre-filled with previous data. For rejected permits, Reapply button does the same
- **Change Password** - Change Password option in profile/settings with current password verification
- **User Profile Management** - Edit profile name, email, and upload avatar photo
- **Trash / Recycle Bin** - Soft-delete permits to trash with 30-day auto-permanent-delete, restore option, and empty trash

### Infrastructure
- **User Authentication** - Secure JWT-based login and registration with bcrypt password hashing
- **Session Persistence** - Auto-login on app restart using saved tokens in SharedPreferences
- **Pull-to-Refresh** - Swipe down to reload data on all dashboards
- **Containerized Backend** - Dockerfile included for production-ready deployment
- **FCM Integration** - Backend sends push notifications via Firebase Admin SDK when available

## Design

The app uses a modern **Indigo & Slate** color palette inspired by contemporary productivity apps:
- **Primary**: Indigo (#6366F1) with violet gradient accents
- **Background**: Clean slate-white (#F8FAFC) with pure white cards
- **Dark mode**: Deep slate (#0F172A) with indigo/violet accents
- **Cards**: Minimal elevation (2dp) for a flat, modern aesthetic
- **Typography**: Clean hierarchy with slate-900 primary text and slate-500 secondary text

## Tech Stack

- **Mobile**: Java (Android)
- **Backend**: Python (Flask)
- **Database**: SQLite (via Flask-SQLAlchemy)
- **AI**: Google Gemini 2.5 Flash (document analysis)
- **Authentication**: JWT (Flask-JWT-Extended)
- **Min SDK**: 33 (Android 13)
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
|   +-- MainActivity.java
+-- app/src/main/res/
|   +-- layout/
|   +-- drawable/
|   +-- values/
|   +-- values-night/
|   +-- menu/
|   +-- xml/
+-- smart_permits_api/
|   +-- app.py
|   +-- models.py
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
- Android device or emulator (API 33+)

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

5. Start the server:
   ```bash
   python app.py
   ```

6. Server runs on `http://0.0.0.0:5000` with auto-seeded test accounts

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
docker run -p 5000:5000 -e GEMINI_API_KEY=your-key-here smart-permits-api
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
| GET    | `/api/permits/{id}`              | Yes  | Get permit details                   |
| POST   | `/api/permits/{id}/upload`       | Yes  | Upload document to permit            |
| POST   | `/api/permits/{id}/ai-analyze`   | Yes  | Trigger AI document analysis (once per permit) |
| POST   | `/api/permits/{id}/pay`          | Yes  | Simulate payment                     |
| POST   | `/api/permits/{id}/renew`        | Yes  | Renew/reapply for a permit           |
| POST   | `/api/permits/{id}/trash`        | Yes  | Move permit to trash                 |
| POST   | `/api/permits/{id}/restore`      | Yes  | Restore permit from trash            |
| DELETE | `/api/permits/{id}/permanent-delete` | Yes | Permanently delete permit         |
| GET    | `/api/permits/trash`             | Yes  | Get trashed permits                  |
| DELETE | `/api/permits/trash/empty`       | Yes  | Empty trash                          |
| GET    | `/api/permits/pending`           | Yes  | Get pending permits (inspector)      |
| GET    | `/api/permits/reviewed`          | Yes  | Get reviewed permits (inspector)     |
| POST   | `/api/permits/{id}/review`       | Yes  | Approve/reject permit                |
| GET    | `/api/permits/{id}/comments`     | Yes  | Get permit comments                  |
| POST   | `/api/permits/{id}/comments`     | Yes  | Add comment to permit                |
| POST   | `/api/permits/{id}/appointment`  | Yes  | Schedule inspection appointment      |
| GET    | `/api/appointments`              | Yes  | Get appointments                     |
| PUT    | `/api/appointments/{id}`         | Yes  | Update appointment status            |
| GET    | `/api/permits/{id}/certificate`  | Yes  | Download PDF certificate             |
| GET    | `/api/permits/stats/analytics`   | Yes  | Get analytics data (inspector)       |
| GET    | `/api/permit-types`              | No   | List available permit types          |
| GET    | `/api/uploads/{filename}`        | No   | Download uploaded file               |

## AI Document Analysis

When a citizen submits a permit application with uploaded documents, the system automatically triggers AI analysis using Google Gemini 2.5 Flash. The flow:

1. Citizen fills out permit details and uploads required documents from the checklist on a single page
2. On submission, all documents are uploaded then a single POST request is sent to `/api/permits/{id}/ai-analyze`
3. The backend collects all document images for the permit and sends them to Gemini in one request
4. Gemini analyzes all documents together with a prompt that includes the permit type and required document list
5. The analysis is stored in the permit record and displayed to the inspector on the review screen
6. The AI response text is rendered with proper formatting (bold headings, bullet points, italic sections) using a markdown-to-Spannable converter

The AI analysis includes:
- Document type identification for each uploaded file
- Key information extraction (dates, names, addresses, stamps, signatures)
- Completeness check against required documents for the permit type
- Detection of irrelevant or incorrect documents
- Warnings about issues (blurry images, expired dates, missing stamps)
- Overall recommendation for the inspector

If the API key was not configured at submission time, the inspector can manually trigger AI analysis from the review screen using the "Run AI Analysis" button.

Cost: one Gemini API call per permit submission.

## Permit Statuses

| Status    | Color   | Description                          |
|-----------|---------|--------------------------------------|
| submitted | Amber   | Awaiting inspector review            |
| approved  | Emerald | Approved, awaiting payment           |
| rejected  | Red     | Rejected by inspector                |
| completed | Indigo  | Approved and paid                    |

## Required Documents per Permit Type

Each permit type requires specific documents. The app presents a checklist with individual upload buttons.

| Permit Type | Required Documents |
|---|---|
| Construction Permit | Urban Planning Certificate, Land Registry Extract, Topographic Survey Plan, Authorized Technical Project, Utility Approvals (Water, Gas, Electricity), Geotechnical Study, Fee Payment Proof |
| Renovation Permit | Urban Planning Certificate, Existing Condition Survey, Renovation Technical Project, Homeowners Association Approval (if applicable), Affected Utility Approvals, Fee Payment Proof |
| Business License | Business Registration Certificate, Articles of Incorporation, Office Space Lease Agreement, Fire Safety Approval, Tax Clearance Certificate, Business Registry Certificate |
| Food Service Permit | Veterinary Sanitary Authorization, HACCP Plan, Pest Control Service Contract, Environmental Approval, Business Registration Certificate, Water Quality Analysis Report |
| Event Permit | Event Organization Request, Security Plan, Police Approval, Fire Department Approval, Sanitation Service Contract, Liability Insurance Policy |
| Signage Permit | Signage Placement Request, Site Sketch, Urban Planning / Architecture Approval, Property Owner Agreement, Photo Simulation / Mockup |
| Demolition Permit | Urban Planning Certificate, Land Registry Extract, Demolition Technical Project, Demolition Plan, Environmental Approval, Waste Management Study, Fee Payment Proof |
| Occupancy Certificate | Work Completion Inspection Report, Energy Performance Certificate, Cadastral Documentation, Project Verifier Reports, Installation Compliance Declaration, Fee Payment Proof |

## Permit Location Map

For **Construction Permit** and **Renovation Permit** types, the application includes an interactive OpenStreetMap (via osmdroid) where citizens can:
1. **Search for a location** using the search bar above the map - type a city, address, or landmark and tap "Search" to navigate the map there (powered by Android Geocoder)
2. **Drag the map** to position a crosshair at the desired work location
3. **Press "Pin Here"** to place a marker at the crosshair position

The map properly intercepts touch events so scrolling the map does not scroll the page. The coordinates (latitude/longitude) are stored with the permit and displayed on both the citizen's permit detail screen and the inspector's review screen. No Google Maps API key is required.

## Building

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
