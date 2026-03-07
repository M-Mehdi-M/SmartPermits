# SmartPermits - Complete Testing Guide

## Quick Start

### Step 1: Start the Flask Backend Server

1. Open a terminal and navigate to the backend folder:
   ```
   cd D:\android_project\SmartPermits\smart_permits_api
   ```

2. Activate the Python virtual environment:
   ```
   .\venv\Scripts\activate
   ```

3. Install dependencies (first time only):
   ```
   pip install -r requirements.txt
   ```

4. Start the server:
   ```
   python app.py
   ```

5. Server runs on `http://0.0.0.0:5000`. Verify by visiting `http://localhost:5000/api/permit-types` in a browser.

6. Leave this terminal open.

### Step 2: Run the Android App

1. Open Android Studio with the SmartPermits project
2. Sync Gradle and build
3. For emulator: change `BASE_URL` in `RetrofitClient.java` to `http://10.0.2.2:5000/api/`
4. For physical phone: change `BASE_URL` to `http://YOUR_PC_IP:5000/api/` (same Wi-Fi network required)
5. Click Run

### Step 3: Test Login

1. App opens to login screen
2. Enter: Username = `citizen1`, Password = `citizen123`
3. Tap Login
4. You land on the Citizen Dashboard showing "Welcome, Maria Popescu"

---

## Default Test Accounts

| Role      | Username    | Password      |
|-----------|-------------|---------------|
| Citizen   | citizen1    | citizen123    |
| Inspector | inspector1  | inspector123  |

---

## Feature Testing Guide

### 1. User Registration

1. Open the app and tap "Register" at the bottom
2. Fill in: Full Name, Email, Username, Password
3. Select role (Citizen or Inspector)
4. Tap Register
5. You are redirected to the appropriate dashboard

### 2. User Login

1. Enter `citizen1` / `citizen123` and tap Login
2. You land on Citizen Dashboard
3. Try `inspector1` / `inspector123` for Inspector Dashboard
4. Invalid credentials show error toast
5. Close and reopen app to verify auto-login (session persistence)

### 3. Search and Filter (Citizen Dashboard)

1. Login as citizen
2. Use the search bar at the top to type a permit type (e.g., "Construction")
3. Results filter in real-time as you type
4. Use the filter chips below the search bar:
   - Tap "Submitted" to show only submitted permits
   - Tap "Approved" to show only approved permits
   - Tap "Rejected" to show only rejected permits
   - Tap "Completed" to show only completed permits
   - Tap the active chip again to deselect and show all
5. Search and filter work together (e.g., search "Business" + filter "Approved")

### 4. Search and Filter (Inspector Dashboard)

1. Login as inspector
2. Use the search bar at the top to search by applicant name, permit type, or description
3. Results filter in real-time
4. Pull-to-refresh to reload pending permits

### 5. Apply for Permit (Multi-Document Upload)

1. From Citizen Dashboard, tap "Apply for Permit" FAB
2. Step 1: Select permit type, enter description, see fee preview
3. Tap "Next: Upload Documents"
4. Step 2: Tap "Gallery" and select multiple images (hold to multi-select)
5. Document count shows (e.g., "3 document(s) attached")
6. First selected image shows as preview
7. Tap "Camera" to capture a photo
8. Tap "Submit Application"
9. Step 3: Success screen appears
10. Tap "Back to Dashboard" and see new permit

### 6. Permit Details and Estimated Processing Time

1. Tap any permit in the list
2. See permit type, date, fee, payment status
3. For "submitted" permits, see "Estimated Processing Time" card showing average processing time (e.g., "2.5 days")
4. Description card shows if description was provided
5. Documents card shows a horizontal carousel of all uploaded documents
6. Tap any document thumbnail to open full-screen viewer

### 7. Inspector Review with Document Carousel

1. Login as inspector
2. Tap a pending permit
3. See applicant info, description, and attached documents list
4. Scroll through the document carousel (horizontal scroll)
5. Tap any document to view full-size
6. Add optional review notes
7. Tap "Approve" or "Reject"
8. Notification is sent to the citizen (if FCM is configured)

### 8. In-App Chat / Comments

1. Open any permit detail (citizen or inspector)
2. Tap "Comments" button
3. Type a message and tap send
4. Messages appear in a chat-style interface
5. Your messages appear on the right, others on the left
6. Author name and role shown for each message
7. Both citizen and inspector can send messages on the same permit
8. New comment triggers notification to the other party

### 9. Payment

1. Have an inspector approve a permit
2. Login as citizen and open the approved permit
3. Tap "Pay Now"
4. Status changes to "Completed" (blue chip)
5. Payment status shows "Paid"

### 10. PDF Certificate Download

1. Open a "Completed" permit (approved + paid)
2. Tap "Download Certificate"
3. PDF is saved to Downloads folder
4. PDF contains: permit details table, applicant info, QR code for verification
5. Open the PDF from Downloads to verify content

### 11. Permit Renewal / Reapply

1. Open a "Completed" permit
2. Tap "Renew" button
3. A new permit is created with the same type and description
4. You are navigated to the new permit detail
5. For "Rejected" permits, tap "Reapply" button (same behavior)

### 12. Appointment Scheduling

1. Open an "Approved" permit (as citizen)
2. Tap "Schedule Inspection"
3. Tap the date field to open date picker
4. Select a future date
5. Select a time slot from the dropdown
6. Add optional notes
7. Tap "Schedule"
8. Toast shows "Appointment scheduled!"
9. Notification sent to inspectors

### 13. Analytics Dashboard (Inspector)

1. Login as inspector
2. Open drawer menu and tap "Analytics"
3. See statistics cards: Total Reviewed, Approved, Rejected, Pending
4. Average processing time displayed
5. Pie chart shows approval vs rejection ratio
6. Bar chart shows busiest permit types
7. Charts animate on load

### 14. Change Password

1. Open drawer menu and tap "Change Password"
2. Enter current password (`citizen123` or `inspector123`)
3. Enter new password (min 6 characters)
4. Confirm new password
5. Tap "Change Password"
6. Toast shows "Password changed successfully"
7. Login with new password to verify

### 15. Dark Mode

1. Open drawer menu and tap "Settings"
2. Toggle "Dark Mode" switch
3. App immediately switches to dark theme
4. Background becomes dark, cards become dark gray, text becomes light
5. Gradient headers use darker teal/blue
6. Toggle "Follow System" to auto-detect system dark mode
7. When Follow System is on, Dark Mode toggle is disabled
8. Close and reopen app to verify dark mode persists

### 16. Notifications Settings

1. In Settings, see "Notifications" section
2. Toggle "Push Notifications" to enable/disable
3. When enabled, you receive notifications for:
   - Permit approved/rejected
   - New comments on your permits
   - Appointment scheduling

### 17. Profile Management

1. Open drawer and tap "My Profile"
2. See profile info: name, role, email
3. Tap "Edit Profile" to change name and email
4. Tap avatar change button to upload a new profile photo
5. Changes reflect immediately across the app

### 18. Edit Profile

1. Open drawer and tap "Edit Profile"
2. Change full name or email
3. Tap "Save Changes"
4. Tap the camera FAB to change avatar
5. Select an image from gallery
6. Avatar uploads automatically

### 19. Navigation Drawer (Citizen)

1. Tap the menu icon (top left)
2. Test all menu items:
   - Dashboard
   - Permit History (with tabs: All, Approved, Completed, Rejected)
   - My Profile
   - Edit Profile
   - Change Password
   - Settings
   - About (shows dialog)
   - Sign Out (with confirmation)

### 20. Navigation Drawer (Inspector)

1. Login as inspector
2. Tap the menu icon
3. Test all menu items:
   - Dashboard
   - Pending Reviews
   - Review History (with tabs: All, Approved, Rejected)
   - Analytics
   - My Profile
   - Edit Profile
   - Change Password
   - Settings
   - About
   - Sign Out

### 21. Document Viewer

1. Open a permit with uploaded documents
2. Tap a document thumbnail
3. Full-screen viewer opens
4. File name shown in header
5. Loading indicator during image load
6. Error state with retry button if load fails
7. Back button returns to previous screen

### 22. Push Notifications

1. Configure Firebase (optional):
   - Place `google-services.json` in `app/` directory
   - Place `firebase-service-account.json` in `smart_permits_api/` directory
2. Without Firebase: notifications work locally within the app
3. With Firebase: cross-device push notifications work
4. Notifications appear when:
   - Inspector approves/rejects a permit
   - Someone comments on a permit
   - An appointment is scheduled

### 23. Trash / Recycle Bin

1. Open any permit detail as a citizen
2. Scroll down and tap "Move to Trash"
3. Confirm in the dialog
4. Permit disappears from dashboard
5. Open drawer menu and tap "Trash"
6. See the trashed permit with "X days until permanent deletion" label
7. Tap "Restore" to move it back to your permits
8. Tap "Delete" to permanently delete a single permit (confirmation required)
9. Tap the trash icon in the header to "Empty Trash" (deletes all items permanently)
10. Pull-to-refresh to reload trash contents
11. Items in trash are automatically permanently deleted after 30 days

---

## Complete End-to-End Test Flow

1. Register a new citizen account
2. Apply for a "Construction Permit" with description and multiple photos
3. Verify permit appears with "Submitted" status
4. Search for it using the search bar
5. Filter by "Submitted" status
6. Logout
7. Login as inspector (`inspector1` / `1q2w3e4r`)
8. Search for the pending permit by applicant name
9. Review the permit, view all documents in carousel
10. Add a comment asking for more info
11. Approve the permit with notes
12. Logout
13. Login as citizen
14. See notification (if FCM configured)
15. Open the approved permit, see reviewer notes
16. Read and reply to the comment
17. Schedule an inspection appointment
18. Pay the permit fee
19. Download the PDF certificate
20. Verify certificate has QR code and correct details
21. Renew the permit (creates new application)
22. Change password in Settings
23. Toggle dark mode on/off
24. Edit profile name and avatar
25. Move one permit to Trash
26. Open Trash, verify permit is there
27. Restore the permit from Trash
28. Move it to Trash again, permanently delete it
29. Logout and login with new password

---

## Troubleshooting

### App Shows "Connection error"
- Ensure Flask server is running
- For emulator: use `10.0.2.2:5000`
- For physical device: use LAN IP
- Check `usesCleartextTraffic="true"` in AndroidManifest.xml

### Login Fails
- Restart Flask server to re-seed test accounts
- Delete `instance/smartpermits.db` and restart to reset database

### Camera Not Working
- Grant camera permission in device Settings
- Emulator: configure virtual camera in AVD settings
- Use Gallery option as alternative

### Permits Not Showing
- Pull-to-refresh on dashboard
- Check server logs for errors
- Re-login if JWT token expired

### PDF Certificate Not Downloading
- Ensure `reportlab` and `qrcode` are installed: `pip install reportlab qrcode[pil]`
- Permit must be in "Completed" status (approved + paid)
- Check storage permission on device

### Dark Mode Not Applying
- Toggle the switch off and on again
- Restart the app
- Check if "Follow System" is enabled

### Search Not Finding Results
- Search is case-insensitive
- Search checks permit type, description, and dates
- Clear the search field to show all permits
- Deselect filter chips to remove status filters

---

## Database Tables

| Table        | Columns                                                                         |
|--------------|---------------------------------------------------------------------------------|
| users        | id, username, email, password_hash, role, full_name, avatar_url, fcm_token      |
| permits      | id, user_id, permit_type, description, status, fee_amount, is_paid, reviewer_notes, reviewed_by, renewed_from, created_at, updated_at |
| documents    | id, permit_id, file_path, file_name, uploaded_at                                |
| comments     | id, permit_id, user_id, message, created_at                                     |
| appointments | id, permit_id, user_id, date, time_slot, status, notes, created_at              |

## Permit Types and Fees

| Permit Type           | Fee     |
|-----------------------|---------|
| Business License      | $150.00 |
| Construction Permit   | $500.00 |
| Food Service Permit   | $200.00 |
| Signage Permit        | $75.00  |
| Event Permit          | $100.00 |
| Renovation Permit     | $350.00 |
| Demolition Permit     | $450.00 |
| Occupancy Certificate | $120.00 |

## File Locations
- **APK Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Backend Database**: `smart_permits_api/instance/smartpermits.db`
- **Uploaded Documents**: `smart_permits_api/uploads/`
- **Backend Server**: `smart_permits_api/app.py`
