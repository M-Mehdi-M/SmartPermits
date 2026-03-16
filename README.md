# SmartPermits

A modern Android + Flask full-stack platform that digitizes the municipal permit and licensing process. Citizens can apply for permits, upload documents, and make payments while inspectors review, approve, or reject applications from their mobile dashboard.

## Features

### Core Features
- **Online Application Wizard** - Step-by-step permit application with permit type selection, description, and fee preview
- **Permit Location Map** - For Construction and Renovation permits, citizens pin the exact work location on an interactive OpenStreetMap (osmdroid). A crosshair overlay marks the center of the map — users drag the map to position it and press the "Pin Here" button to place the marker. The map pin with coordinates is visible to inspectors on the review screen and to citizens on the permit detail screen. No API key required
- **Romanian-Law Document Checklist** - Each permit type displays a checklist of required documents based on Romanian legislation (e.g., Certificat de urbanism, Proiect tehnic DTAC, Avize utilități for Construction Permits). Each document has an individual upload button with a checkbox that marks green when uploaded
- **Multi-Document Upload** - Select multiple documents (ID, property deed, floor plans) from gallery or capture via camera; displayed as scrollable carousel. Documents are tagged with labels matching the Romanian legal requirements
- **Role-Based Dashboards** - Separate interfaces for citizens and inspectors with tailored workflows
- **Navigation Drawer** - Professional side menu on both dashboards with quick access to all features
- **Permit Tracking** - Real-time status tracking with color-coded chips (Submitted, Approved, Rejected, Completed)
- **Permit History** - Filterable history view with tabs (All, Approved, Completed, Rejected)
- **Inspector Review** - Inspectors view pending applications, review details, see the work location on a map, preview all attached documents in a vertical ordered list with numbered labels (e.g., "#1 — Certificat de urbanism") and thumbnails, then approve or reject with notes
- **Review History** - Inspectors can browse past reviewed permits filtered by outcome
- **Payment Simulation** - Citizens pay fees for approved permits, changing status to Completed
- **Delete Account** - Users can permanently delete their account and all associated data from the profile screen with confirmation dialog

### Advanced Features
- **Push Notifications** - When an inspector approves/rejects a permit, notifications are sent to the citizen via Firebase Cloud Messaging (FCM). Comments and appointment scheduling also trigger notifications
- **Analytics Dashboard** - Inspector statistics screen showing total permits reviewed, approval vs rejection ratio (pie chart), average review time, and busiest permit types (bar chart) using MPAndroidChart
- **Appointment Scheduling** - After approval, citizens schedule on-site inspection appointments using a date picker and time slot selector
- **In-App Chat / Comments** - Comment thread on each permit where citizens ask questions and inspectors request additional documents
- **PDF Permit Certificate** - When a permit is Completed, a downloadable PDF certificate with QR code is generated using ReportLab
- **Search & Filter** - Search bar and filter chips on both citizen and inspector dashboards to filter by permit type, status, applicant name, and date
- **Estimated Processing Time** - Shows citizens average review time based on historical data for each permit type
- **Dark Mode** - Toggle in Settings with manual dark mode switch, follow-system option, and proper dark theme colors for gradients and cards
- **Permit Renewal / Reapply** - For completed permits, Renew button creates a new application pre-filled with previous data. For rejected permits, Reapply button does the same
- **Change Password** - Change Password option in profile/settings with current password verification
- **User Profile Management** - Edit profile name, email, and upload avatar photo

### Infrastructure
- **User Authentication** - Secure JWT-based login and registration with bcrypt password hashing
- **Session Persistence** - Auto-login on app restart using saved tokens in SharedPreferences
- **Pull-to-Refresh** - Swipe down to reload data on all dashboards
- **Containerized Backend** - Dockerfile included for production-ready deployment
- **FCM Integration** - Backend sends push notifications via Firebase Admin SDK when available

## Tech Stack

- **Mobile**: Java (Android)
- **Backend**: Python (Flask)
- **Database**: SQLite (via Flask-SQLAlchemy)
- **Authentication**: JWT (Flask-JWT-Extended)
- **Min SDK**: 34 (Android 14)
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
| [CameraX / FileProvider](https://developer.android.com/training/camera) | Camera capture and secure file sharing |
| [Flask](https://flask.palletsprojects.com/) | Backend web framework |
| [Flask-SQLAlchemy](https://flask-sqlalchemy.palletsprojects.com/) | ORM for database models |
| [Flask-JWT-Extended](https://flask-jwt-extended.readthedocs.io/) | JWT authentication |
| [Flask-Bcrypt](https://flask-bcrypt.readthedocs.io/) | Password hashing |
| [Flask-CORS](https://flask-cors.readthedocs.io/) | Cross-origin request handling |
| [ReportLab](https://www.reportlab.com/) | PDF certificate generation |
| [qrcode](https://pypi.org/project/qrcode/) | QR code generation for certificates |
| [firebase-admin](https://firebase.google.com/docs/admin/setup) | FCM push notifications from server |

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
|   +-- models/
|   |   +-- User.java, Permit.java, Document.java
|   |   +-- Comment.java, Appointment.java, PermitType.java
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
- Android device or emulator (API 34+)

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

4. Start the server:
   ```bash
   python app.py
   ```

5. Server runs on `http://0.0.0.0:5000` with auto-seeded test accounts

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
docker run -p 5000:5000 smart-permits-api
```

## Default Test Accounts

| Role      | Username    | Password  |
|-----------|-------------|-----------|
| Citizen   | citizen1    | 1q2w3e4r  |
| Inspector | inspector1  | 1q2w3e4r  |

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
| POST   | `/api/permits/{id}/pay`          | Yes  | Simulate payment                     |
| POST   | `/api/permits/{id}/renew`        | Yes  | Renew/reapply for a permit           |
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

## Permit Statuses

| Status    | Color  | Description                          |
|-----------|--------|--------------------------------------|
| submitted | Orange | Awaiting inspector review            |
| approved  | Green  | Approved, awaiting payment           |
| rejected  | Red    | Rejected by inspector                |
| completed | Blue   | Approved and paid                    |

## Required Documents per Permit Type (Romanian Law)

Each permit type requires specific documents based on Romanian legislation. The app presents a checklist with individual upload buttons.

| Permit Type | Required Documents |
|---|---|
| Construction Permit | Certificat de urbanism, Extras carte funciară (CF), Plan topografic vizat de OCPI, Proiect tehnic (DTAC) autorizat, Avize utilități (apă, gaz, electricitate), Studiu geotehnic, Dovada achitării taxei |
| Renovation Permit | Certificat de urbanism, Releveu stare existentă, Proiect tehnic renovare, Acord asociație proprietari, Avize utilități afectate, Dovada achitării taxei |
| Business License | Certificat înregistrare ORC, Act constitutiv societate, Contract spațiu / sediu social, Aviz PSI / ISU, Cazier fiscal, Certificat constatator ORC |
| Food Service Permit | Autorizație sanitară veterinară (DSVSA), Plan HACCP, Contract dezinsecție și deratizare, Aviz de mediu, Certificat înregistrare ORC, Buletin analiză apă |
| Event Permit | Cerere organizare eveniment, Plan de securitate, Aviz Poliție, Aviz ISU (pompieri), Contract salubrizare, Poliță asigurare răspundere civilă |
| Signage Permit | Cerere amplasare firmă, Schița amplasament, Aviz urbanism / arhitectură, Acord proprietar imobil, Simulare foto montaj |
| Demolition Permit | Certificat de urbanism, Extras carte funciară (CF), Proiect tehnic desființare (DTAD), Plan de demolare, Aviz de mediu, Studiu gestionarea deșeurilor, Dovada achitării taxei |
| Occupancy Certificate | Proces verbal recepție la terminarea lucrărilor, Certificat de performanță energetică, Documentație cadastrală, Referatele verificatorilor de proiecte, Declarație conformitate instalații, Dovada achitării taxei |

## Permit Location Map

For **Construction Permit** and **Renovation Permit** types, the application includes an interactive OpenStreetMap (via osmdroid) where citizens drag the map to position a crosshair at the desired work location, then press the "Pin Here" button to place a marker. The coordinates (latitude/longitude) are stored with the permit and displayed on both the citizen's permit detail screen and the inspector's review screen. No Google Maps API key is required.

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
