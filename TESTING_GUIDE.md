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

### 5. Apply for Permit (Instant Submission)

1. From Citizen Dashboard, tap "Apply for Permit" FAB
2. Select permit type from the dropdown
3. A checklist of required documents appears below the fee preview
4. Each document in the checklist has a checkbox and "Upload" button
5. Tap "Upload" next to any document to pick a file from device storage
6. After upload, the checkbox turns green with a checkmark
7. The view stays on the document checklist area (does not scroll to map or open keyboard)
8. For "Construction Permit" or "Renovation Permit": an interactive map appears with a crosshair overlay and a search bar
9. Type a city or address in the search field above the map and tap "Search" to navigate
10. Drag the map to position the crosshair at the desired work location
11. Press the "Pin Here" button to place the marker
12. Enter a description and see the fee preview
13. Tap "Submit Application"
14. Success screen appears immediately (AI analysis runs in the background)
15. Tap "Back to Dashboard" and see new permit

### 6. Permit Details, Map, and Estimated Processing Time

1. Tap any permit in the list
2. See permit type, date, fee, payment status
3. For Construction/Renovation permits with a pinned location: a map card shows the work location with a marker
4. Coordinates displayed below the map
5. For "submitted" permits, see "Estimated Processing Time" card
6. Description card shows if description was provided
7. Documents card shows a horizontal carousel of all uploaded documents
8. Tap any document thumbnail to open full-screen viewer

### 7. Inspector Review with Collapsible AI Analysis

1. Login as inspector
2. Tap a pending permit
3. See the **AI Document Analysis** card showing a compact 4-line preview of the analysis
4. The card displays "Tap to read full analysis" hint
5. Tap the AI card to open a dialog with the full formatted analysis:
   - Bold headings and section titles
   - Bullet points as proper list items
   - Document type identification for each uploaded file
   - Completeness assessment against required documents
   - Warnings or concerns
   - Overall recommendation
6. Close the dialog and continue reviewing
7. If no AI analysis exists, a "Run AI Analysis" button appears to manually trigger it
8. See applicant info, description, and attached documents list
9. For Construction/Renovation permits: map card shows pinned work location
10. Add optional review notes
11. Tap "Approve" or "Reject"

### 8. AI Document Analysis Details

1. The AI analysis runs in the background when a permit is submitted (citizen does not wait)
2. If no GEMINI_API_KEY is configured, the inspector sees a "Run AI Analysis" button
3. The analysis is collapsible on the review screen (4-line preview, tap to expand)
4. The full analysis opens in a scrollable dialog
5. The analysis is stored permanently with the permit
6. Subsequent views show the cached analysis (no additional API calls)

### 9. Blockchain Permit Notarization

1. Configure blockchain in `.env` (optional):
   ```
   ETH_PRIVATE_KEY=your-metamask-private-key
   ETH_RPC_URL=https://sepolia.infura.io/v3/your-project-id
   ETH_WALLET_ADDRESS=your-metamask-wallet-address
   ```
2. Get free Sepolia test ETH from https://sepoliafaucet.com/ or https://www.infura.io/faucet/sepolia
3. Install web3: `pip install web3`
4. Restart the Flask server
5. Login as inspector and approve a pending permit
6. The backend computes a SHA-256 hash of the permit data + all document files
7. If blockchain is configured, the hash is written to the Ethereum Sepolia testnet
8. Login as citizen and open the approved permit
9. See the "Blockchain Notarization" card showing:
   - "Verified on Blockchain" (green) if the on-chain transaction succeeded
   - "Hash Recorded Locally" (amber) if blockchain was not configured
   - Transaction hash in monospace font
   - Document hash (SHA-256)
   - "View on Etherscan" button
10. Tap "View on Etherscan" to open the transaction on https://sepolia.etherscan.io/
11. Download the PDF certificate and verify it includes the blockchain TX and QR code
12. Without blockchain configured: the SHA-256 hash is still computed and stored
13. Verify via API: visit `http://localhost:5000/api/permits/{id}/verify-blockchain` in a browser

### 10. In-App Chat / Comments

1. Open any permit detail (citizen or inspector)
2. Tap "Comments" button
3. Type a message and tap send
4. Messages appear in a chat-style interface
5. Your messages appear on the right, others on the left
6. Author name and role shown for each message
7. Both citizen and inspector can send messages on the same permit

### 11. Payment

1. Have an inspector approve a permit
2. Login as citizen and open the approved permit
3. Tap "Pay Now"
4. Status changes to "Completed" (teal chip)
5. Payment status shows "Paid"

### 12. PDF Certificate Download

1. Open a "Completed" permit (approved + paid)
2. Tap "Download Certificate"
3. PDF is saved to Downloads folder and automatically opens in the device's PDF viewer
4. PDF contains: permit details table, applicant info, QR code for verification, blockchain transaction ID (if available)
5. If no PDF viewer is installed, the file is still saved to Downloads

### 13. Permit Renewal / Reapply

1. Open a "Completed" permit
2. Tap "Renew" button
3. A new permit is created with the same type and description
4. You are navigated to the new permit detail
5. For "Rejected" permits, tap "Reapply" button (same behavior)

### 14. Appointment Scheduling

1. Open an "Approved" permit (as citizen)
2. Tap "Schedule Inspection"
3. Tap the date field to open date picker
4. Select a future date
5. Select a time slot from the dropdown
6. Add optional notes
7. Tap "Schedule"
8. Toast shows "Appointment scheduled!"

### 15. Analytics Dashboard (Inspector)

1. Login as inspector
2. Open drawer menu and tap "Analytics"
3. See statistics cards: Total Reviewed, Approved, Rejected, Pending
4. Average processing time displayed
5. Pie chart shows approval vs rejection ratio
6. Bar chart shows busiest permit types

### 16. Change Password

1. Open drawer menu and tap "Change Password"
2. Enter current password (`1q2w3e4r`)
3. Enter new password (min 6 characters)
4. Confirm new password
5. Tap "Change Password"
6. Toast shows "Password changed successfully"

### 17. Dark Mode

1. Open drawer menu and tap "Settings"
2. Toggle "Dark Mode" switch
3. App switches to dark theme with deep grey background and teal accents
4. Cards become dark grey, text becomes light
5. Headers use dark teal tones
6. Toggle "Follow System" to auto-detect system dark mode
7. When Follow System is on, Dark Mode toggle is disabled
8. Close and reopen app to verify dark mode persists

### 18. Notifications Settings

1. In Settings, see "Notifications" section
2. Toggle "Push Notifications" to enable/disable

### 19. Profile Management

1. Open drawer and tap "My Profile"
2. See profile info: name, role, email
3. Tap "Edit Profile" to change name and email
4. Tap avatar change button to upload a new profile photo

### 20. Edit Profile

1. Open drawer and tap "Edit Profile"
2. Change full name or email
3. Tap "Save Changes"
4. Tap the camera FAB to change avatar
5. Select an image from gallery

### 21. Navigation Drawer (Citizen)

1. Tap the menu icon (top left)
2. Test all menu items:
   - Dashboard
   - Permit History (with tabs: All, Approved, Completed, Rejected)
   - Trash
   - My Profile
   - Edit Profile
   - Change Password
   - Settings
   - About (shows dialog)
   - Sign Out (with confirmation)

### 22. Navigation Drawer (Inspector)

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

### 23. Document Viewer

1. Open a permit with uploaded documents
2. Tap a document thumbnail
3. Full-screen viewer opens
4. File name shown in header
5. Loading indicator during image load
6. Error state with retry button if load fails

### 24. Push Notifications

1. Configure Firebase (optional):
   - Place `google-services.json` in `app/` directory
   - Place `firebase-service-account.json` in `smart_permits_api/` directory
2. Without Firebase: notifications work locally within the app
3. With Firebase: cross-device push notifications work

### 25. Trash / Recycle Bin

1. Open any permit detail as a citizen
2. Scroll down and tap "Move to Trash"
3. Confirm in the dialog
4. Permit disappears from dashboard
5. Open drawer menu and tap "Trash"
6. See the trashed permit with "X days until permanent deletion" label
7. Tap "Restore" to move it back
8. Tap "Delete" to permanently delete (confirmation required)
9. Tap the trash icon in the header to "Empty Trash"

### 26. Delete Account

1. Login as any user (create a test account to try this)
2. Open drawer and tap "My Profile"
3. Scroll down and tap "Delete Account"
4. A confirmation dialog appears
5. Tap "Delete" to confirm
6. App returns to login screen
7. Try logging in with deleted credentials - login fails

### 27. Map Location Search

1. From Citizen Dashboard, tap "Apply for Permit"
2. Select "Construction Permit" or "Renovation Permit"
3. The map section appears with a search bar above it
4. Type a city name (e.g., "London") in the search field
5. Tap "Search" button
6. The map navigates to the searched location
7. Type another address and search again
8. If the location is not found, a "Location not found" toast appears
9. After navigating, drag the map and press "Pin Here" to set the exact location

---

## Complete End-to-End Test Flow

1. Register a new citizen account
2. Apply for a "Construction Permit" with description
3. Upload required documents from the checklist
4. Use the map search to navigate to a city, then pin a location
5. Tap "Submit Application" - success screen appears immediately
6. Verify permit appears with "Submitted" status
7. Search for it using the search bar
8. Filter by "Submitted" status
9. Logout
10. Login as inspector (`inspector1` / `1q2w3e4r`)
11. Search for the pending permit by applicant name
12. Open the permit and see the collapsible AI analysis card (4-line preview)
13. Tap the AI card to open full analysis dialog with formatted text
14. Close dialog and review the permit, see the map, view documents
15. Add a comment asking for more info
16. Approve the permit with notes
17. Verify the Flask terminal shows blockchain notarization logs
18. Logout
19. Login as citizen
20. Open the approved permit, see reviewer notes
21. See the "Blockchain Notarization" card with transaction hash
22. Read and reply to the comment
23. Schedule an inspection appointment
24. Pay the permit fee
25. Download the PDF certificate (auto-opens in PDF viewer)
26. Verify certificate has QR code, correct details, and blockchain TX ID
27. Renew the permit
28. Change password in Settings
29. Toggle dark mode on/off (verify teal color scheme in both modes)
30. Edit profile name and avatar
31. Move one permit to Trash
32. Restore it from Trash
33. Move it to Trash again, permanently delete it
34. Logout and login with new password

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

### Permits Not Showing
- Pull-to-refresh on dashboard
- Check server logs for errors
- Re-login if JWT token expired

### PDF Certificate Not Opening
- Ensure `reportlab` and `qrcode` are installed: `pip install reportlab qrcode[pil]`
- Permit must be in "Completed" status (approved + paid)
- The PDF auto-opens after download; ensure a PDF viewer is installed on the device
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

### AI Analysis Not Working
- Ensure `GEMINI_API_KEY` is set in the `.env` file
- The `.env` file must be in the `smart_permits_api/` directory
- Check that `google-genai` and `Pillow` are installed
- Check the Flask server terminal for error messages
- If no key is configured at submission time, the inspector can trigger AI analysis manually
- Restart the Flask server after editing the `.env` file

### Upload Goes to Wrong Place or Opens Keyboard
- This has been fixed. After uploading a document, focus is cleared and keyboard is hidden
- If the issue persists, restart the app

### Map Search Not Working
- Ensure the device has internet connectivity
- Try a well-known city name (e.g., "London", "Tokyo")
- If search fails, manually drag the map to the desired location

### Blockchain Notarization Not Working
- Ensure `ETH_PRIVATE_KEY`, `ETH_RPC_URL`, and `ETH_WALLET_ADDRESS` are set in `.env`
- Check that `web3` is installed: `pip install web3`
- Ensure your MetaMask wallet has Sepolia test ETH
- Verify your Infura/Alchemy Sepolia RPC URL is correct
- If blockchain is not configured, the SHA-256 hash is still computed and stored locally
- Restart the Flask server after editing `.env`

---

## Database Tables

| Table        | Columns                                                                         |
|--------------|---------------------------------------------------------------------------------|
| users        | id, username, email, password_hash, role, full_name, avatar_url, fcm_token      |
| permits      | id, user_id, permit_type, description, status, fee_amount, is_paid, reviewer_notes, reviewed_by, renewed_from, latitude, longitude, ai_analysis, blockchain_hash, blockchain_tx_hash, blockchain_error, created_at, updated_at, deleted_at |
| documents    | id, permit_id, file_path, file_name, document_label, uploaded_at                |
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

## Required Documents per Permit Type

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

## File Locations
- **APK Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Backend Database**: `smart_permits_api/instance/smartpermits.db`
- **Uploaded Documents**: `smart_permits_api/uploads/`
- **Backend Server**: `smart_permits_api/app.py`
- **AI Config**: `smart_permits_api/.env`
- **Blockchain Config**: `smart_permits_api/.env` (ETH_PRIVATE_KEY, ETH_RPC_URL, ETH_WALLET_ADDRESS)
- **Blockchain Module**: `smart_permits_api/blockchain.py`
