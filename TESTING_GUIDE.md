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

4. Configure the Gemini API key for AI document analysis. Edit `.env` and set your actual Gemini API key from https://aistudio.google.com/apikey

5. Configure blockchain notarization (optional). Add to `.env`:
   ```
   ETH_PRIVATE_KEY=your-metamask-private-key
   ETH_RPC_URL=https://sepolia.infura.io/v3/your-project-id
   ETH_WALLET_ADDRESS=your-metamask-wallet-address
   ```

6. Start the server:
   ```
   python app.py
   ```

7. Server runs on `http://0.0.0.0:5000`. Verify by visiting `http://localhost:5000/api/permit-types` in a browser.

8. Leave this terminal open.

### Step 2: Run the Android App

1. Open Android Studio with the SmartPermits project
2. Sync Gradle and build
3. For emulator: change `BASE_URL` in `RetrofitClient.java` to `http://10.0.2.2:5000/api/`
4. For physical phone: change `BASE_URL` to `http://YOUR_PC_IP:5000/api/` (same Wi-Fi network required)
5. Click Run

### Step 3: Test Login

1. App opens to login screen with teal gradient background
2. Enter: Username = `citizen1`, Password = `1q2w3e4r`
3. Tap Login
4. You land on the Citizen Dashboard showing "Welcome, Test User"

---

## Default Test Accounts

| Role      | Username    | Password  | Full Name      |
|-----------|-------------|-----------|----------------|
| Citizen   | citizen1    | 1q2w3e4r  | Test User      |
| Inspector | inspector1  | 1q2w3e4r  | Test Inspector |

---

## Feature Testing Guide

### 1. User Registration

1. Open the app and tap "Register" at the bottom
2. Fill in: Full Name, Email, Username, Password
3. Select role (Citizen or Inspector)
4. Tap Register
5. You are redirected to the appropriate dashboard

### 2. User Login

1. Enter `citizen1` / `1q2w3e4r` and tap Login
2. You land on Citizen Dashboard
3. Try `inspector1` / `1q2w3e4r` for Inspector Dashboard
4. Invalid credentials show error toast
5. Close and reopen app to verify auto-login (session persistence)

### 3. Multi-Language Support

1. Login and open drawer menu, tap "Settings"
2. Tap the "App Language" row in the Language section
3. A dialog appears with 10 language options: System Default, English, Romanian, Spanish, French, Italian, German, Portuguese, Polish, Turkish, Ukrainian
4. Select "Romanian" (or any language)
5. The Settings screen immediately refreshes with all text in the selected language
6. Navigate back to dashboard - all menu items, labels, and buttons are translated
7. Open drawer menu - all navigation items are in the selected language
8. Sign out and sign back in - the language is preserved
9. Change back to "English" or "System Default" to restore

### 4. Dark Mode Persistence on Sign-Out

1. Open Settings and toggle "Dark Mode" ON
2. Verify the app switches to dark theme
3. Sign out from the drawer menu
4. Sign back in with the same or different account
5. The app should still be in dark mode (theme is preserved across sign-outs)
6. Toggle dark mode OFF and sign out again - it stays light

### 5. Search and Filter (Citizen Dashboard)

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

### 6. Apply for Permit (Instant Submission)

1. From Citizen Dashboard, tap "Apply for Permit" FAB
2. Select permit type from the dropdown
3. A checklist of required documents appears below the fee preview
4. Each document in the checklist has a checkbox and "Upload" button
5. Tap "Upload" next to any document to pick a file from device storage
6. After upload, the checkbox turns green with a checkmark
7. For "Construction Permit" or "Renovation Permit": an interactive map appears with a crosshair overlay and a search bar
8. Type a city or address in the search field above the map and tap "Search" to navigate
9. Drag the map to position the crosshair at the desired work location
10. Press the "Pin Here" button to place the marker
11. Enter a description and see the fee preview
12. Tap "Submit Application"
13. Success screen appears immediately (AI analysis runs in the background)
14. Tap "Back to Dashboard" and see new permit

### 7. Status Timeline / Audit Trail

1. Tap any permit in the list to open permit details
2. Scroll down to find the "Status Timeline" card
3. See a visual timeline with dots and connecting lines showing every state change:
   - "Submitted" with the citizen's name and timestamp
   - "Documents Analyzed by AI" (if AI ran) with Gemini AI as actor
   - "Reviewed by Inspector" with the inspector's name
   - "Blockchain Notarized" (if blockchain configured)
   - "Payment Received" after paying
   - "Certificate Issued" after completion
4. Each entry shows: event type (bold), actor name, notes, and timestamp
5. Submit a new permit, have it approved, pay it - watch the timeline grow

### 8. Predictive Wait Time with ML

1. Submit a few permits and have the inspector approve/reject them to build historical data
2. Submit a new permit
3. Open the permit detail
4. See the "Estimated Processing Time" card showing a predicted range like "2-4 days"
5. A confidence percentage is shown (e.g., "65% Confidence")
6. The prediction accounts for: permit type history, document count, pending queue size, and day of week

### 9. Permit Expiry Tracking

1. Complete the full flow: submit permit → inspector approves → citizen pays
2. Open the completed permit detail
3. See the "Permit Validity" card showing "Valid until YYYY-MM-DD" in green
4. On the dashboard, completed permits show the fee amount normally
5. When a permit is within 30 days of expiry, the dashboard card shows "Expires in Xd" in amber
6. Expired permits show "EXPIRED" in red on the dashboard card
7. The expiry period varies by permit type (365 days for Construction, 30 days for Events, etc.)

### 10. Permit Details, Map, and Enhanced Processing Time

1. Tap any permit in the list
2. See permit type, date, fee, payment status
3. For Construction/Renovation permits with a pinned location: a map card shows the work location with a marker
4. For "submitted" permits, see "Estimated Processing Time" card with range and confidence
5. Description card shows if description was provided
6. Documents card shows a horizontal carousel of all uploaded documents
7. Tap any document thumbnail to open full-screen viewer

### 11. Inspector Review with Collapsible AI Analysis

1. Login as inspector
2. Tap a pending permit
3. See the AI Document Analysis card showing a compact 4-line preview
4. Tap the AI card to open full analysis dialog with formatted text
5. If no AI analysis exists, a "Run AI Analysis" button appears
6. See applicant info, description, and attached documents
7. For Construction/Renovation permits: map card shows pinned work location
8. Add optional review notes
9. Tap "Approve" or "Reject"
10. A timeline event is created automatically for the review action

### 12. Blockchain Permit Notarization

1. Configure blockchain in `.env` (optional)
2. Login as inspector and approve a pending permit
3. Login as citizen and open the approved permit
4. See the "Blockchain Notarization" card showing:
   - "Verified on Blockchain" (green) if on-chain transaction succeeded
   - "Hash Recorded Locally" (amber) if blockchain was not configured
   - Transaction hash in monospace font
   - Document hash (SHA-256)
   - "View on Etherscan" button
5. Tap "View on Etherscan" to open the transaction on https://sepolia.etherscan.io/

### 13. PDF Certificate (Professional Design)

1. Open a "Completed" permit (approved + paid)
2. Tap "Download Certificate"
3. PDF is saved to Downloads folder and automatically opens in the device's PDF viewer
4. PDF contains:
   - Teal-branded header with diamond icon and "SmartPermits" title
   - Certificate number (e.g., "SP-00001")
   - Permit information table with alternating row colors
   - Application date, issued date, and valid-until date
   - Blockchain verification section with clickable Etherscan link
   - QR code that links to Etherscan (or verification URL)
   - Gold footer seal with tamper warning
5. If blockchain was configured, the PDF includes a clickable "View on Etherscan" link
6. Change language to Romanian in Settings, then download the certificate again - verify Romanian characters (ăîâșț) render correctly without black squares
7. Try other languages (Polish, Turkish, Ukrainian) to confirm all special characters display properly

### 14. In-App Chat / Comments

1. Open any permit detail (citizen or inspector)
2. Tap "Comments" button
3. Type a message and tap send
4. Messages appear in a chat-style interface
5. Your messages appear on the right, others on the left

### 15. Payment and Expiry

1. Have an inspector approve a permit
2. Login as citizen and open the approved permit
3. Tap "Pay Now"
4. Status changes to "Completed"
5. An expiry date is set based on permit type
6. Timeline shows "Payment Received" and "Certificate Issued" entries

### 16. Permit Renewal / Reapply

1. Open a "Completed" permit
2. Tap "Renew" button
3. A new permit is created with the same type and description
4. Timeline shows "Submitted" with note "Renewed from permit #X"

### 17. Appointment Scheduling

1. Open an "Approved" permit (as citizen)
2. Tap "Schedule Inspection"
3. Select a future date and time slot
4. Tap "Schedule"
5. Timeline shows "Appointment Scheduled" entry

### 18. Analytics Dashboard (Inspector)

1. Login as inspector
2. Open drawer menu and tap "Analytics"
3. See statistics cards: Total Reviewed, Approved, Rejected, Pending
4. Average processing time displayed
5. Pie chart shows approval vs rejection ratio
6. Bar chart shows busiest permit types

### 19. Dark Mode

1. Open drawer menu and tap "Settings"
2. Toggle "Dark Mode" switch
3. App switches to dark theme
4. Toggle "Follow System" to auto-detect system dark mode
5. Close and reopen app to verify dark mode persists
6. Sign out and sign back in - dark mode is still active

### 20. Profile Management

1. Open drawer and tap "My Profile"
2. See profile info: name, role, email
3. Tap "Edit Profile" to change name and email
4. Tap avatar change button to upload a new profile photo

### 21. Navigation Drawer (Citizen)

1. Tap the menu icon (top left)
2. Test all menu items:
   - Dashboard
   - Permit History (with tabs: All, Approved, Completed, Rejected)
   - Trash
   - My Profile
   - Edit Profile
   - Change Password
   - Settings (with Language and Appearance)
   - About (shows dialog)
   - Sign Out (with confirmation)

### 22. Trash / Recycle Bin

1. Open any permit detail as a citizen
2. Scroll down and tap "Move to Trash"
3. Confirm in the dialog
4. Open drawer menu and tap "Trash"
5. See the trashed permit with "X days until permanent deletion" label
6. Tap "Restore" to move it back
7. Tap "Delete" to permanently delete

---

## Complete End-to-End Test Flow

1. Start the Flask backend
2. Change language to Romanian in Settings
3. Verify all UI text changed to Romanian
4. Change back to English
5. Toggle dark mode ON
6. Register a new citizen account
7. Apply for a "Construction Permit" with description
8. Upload required documents from the checklist
9. Use the map search to navigate to a city, then pin a location
10. Tap "Submit Application" - success screen appears immediately
11. Verify permit appears with "Submitted" status
12. Open permit detail and see the Status Timeline with "Submitted" entry
13. See estimated processing time with confidence percentage
14. Logout
15. Login as inspector (`inspector1` / `1q2w3e4r`)
16. Open the pending permit and see the AI analysis card
17. Approve the permit with notes
18. Verify the Flask terminal shows blockchain notarization logs
19. Logout
20. Login as citizen
21. Open the approved permit
22. See the Status Timeline with: Submitted → AI Analysis → Reviewed by Inspector → Blockchain Notarized
23. See the Blockchain Notarization card with transaction hash
24. Schedule an inspection appointment
25. See "Appointment Scheduled" added to timeline
26. Pay the permit fee
27. See "Payment Received" and "Certificate Issued" in timeline
28. See the Permit Validity card showing expiry date
29. Download the PDF certificate
30. Verify certificate has: branded header, info table, blockchain section with clickable link, QR code, gold seal
31. Renew the permit - see timeline shows "Renewed from permit #X"
32. Sign out
33. Verify dark mode is still active on login screen
34. Login with new password
35. Change language to Spanish - verify all text changes
36. Change back to English

---

## Troubleshooting

### App Shows "Connection error"
- Ensure Flask server is running
- For emulator: use `10.0.2.2:5000`
- For physical device: use LAN IP

### Login Fails
- Restart Flask server to re-seed test accounts
- Delete `instance/smartpermits.db` and restart to reset database

### Language Not Changing
- Ensure you selected a language from Settings > Language
- The app recreates the activity so the language applies immediately
- Language persists across sign-outs

### Dark Mode Resets on Sign-Out
- This has been fixed. Theme preferences are now preserved when signing out

### Timeline Not Showing
- Timeline appears after at least one event (e.g., submission)
- Ensure the backend is updated with the PermitEvent model
- Delete old database and restart if migrating from an older version

### Estimated Wait Time Not Showing
- Historical data is needed - submit and review a few permits first
- Only shows for "submitted" status permits

### Permit Expiry Not Showing
- Expiry is set when a permit is paid (completed)
- Only completed permits have expiry dates

### PDF Certificate Issues
- Ensure `reportlab` and `qrcode` are installed
- Permit must be in "Completed" status
- Check server logs for certificate generation errors
- For proper Unicode rendering (Romanian ăîâșț, Polish łźż, etc.), the `fonts/` directory with DejaVuSans TTF files must exist in `smart_permits_api/`. The Dockerfile also installs `fonts-dejavu-core` as a fallback

### AI Analysis Not Working
- Ensure `GEMINI_API_KEY` is set in the `.env` file
- Check the Flask server terminal for error messages

### Blockchain Notarization Not Working
- Ensure `ETH_PRIVATE_KEY`, `ETH_RPC_URL`, and `ETH_WALLET_ADDRESS` are set in `.env`
- Ensure your wallet has Sepolia test ETH

---

## Database Tables

| Table         | Columns                                                                         |
|---------------|---------------------------------------------------------------------------------|
| users         | id, username, email, password_hash, role, full_name, avatar_url, fcm_token      |
| permits       | id, user_id, permit_type, description, status, fee_amount, is_paid, reviewer_notes, reviewed_by, renewed_from, latitude, longitude, ai_analysis, blockchain_hash, blockchain_tx_hash, blockchain_error, expires_at, created_at, updated_at, deleted_at |
| permit_events | id, permit_id, event_type, actor_name, actor_role, notes, created_at            |
| documents     | id, permit_id, file_path, file_name, document_label, uploaded_at                |
| comments      | id, permit_id, user_id, message, created_at                                     |
| appointments  | id, permit_id, user_id, date, time_slot, status, notes, created_at              |

## Permit Types and Fees

| Permit Type           | Fee     | Validity    |
|-----------------------|---------|-------------|
| Business License      | $150.00 | 365 days    |
| Construction Permit   | $500.00 | 365 days    |
| Food Service Permit   | $200.00 | 365 days    |
| Signage Permit        | $75.00  | 730 days    |
| Event Permit          | $100.00 | 30 days     |
| Renovation Permit     | $350.00 | 180 days    |
| Demolition Permit     | $450.00 | 180 days    |
| Occupancy Certificate | $120.00 | Non-expiring|

## File Locations
- **APK Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Backend Database**: `smart_permits_api/instance/smartpermits.db`
- **Uploaded Documents**: `smart_permits_api/uploads/`
- **Backend Server**: `smart_permits_api/app.py`
- **AI Config**: `smart_permits_api/.env`
- **Blockchain Config**: `smart_permits_api/.env` (ETH_PRIVATE_KEY, ETH_RPC_URL, ETH_WALLET_ADDRESS)
- **Blockchain Module**: `smart_permits_api/blockchain.py`
- **Language Resources**: `app/src/main/res/values-{lang}/strings.xml`
- **Locale Helper**: `app/src/main/java/project/smartpermits/LocaleHelper.java`
- **PDF Unicode Fonts**: `smart_permits_api/fonts/DejaVuSans.ttf`, `DejaVuSans-Bold.ttf`
