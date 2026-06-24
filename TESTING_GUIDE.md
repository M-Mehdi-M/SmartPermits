# SmartPermits — Complete Testing Guide

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

4. Configure the Gemini API key for AI document analysis. Edit `.env` and set your actual Gemini API key from https://aistudio.google.com/apikey:
   ```
   GEMINI_API_KEY=your-actual-api-key-here
   ```

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

7. Server runs on `http://0.0.0.0:5000`. Verify by visiting `http://localhost:5000/api/permit-types` in a browser — you should see a JSON list of permit types with fees.

8. Leave this terminal open.

### Step 2: Run the Android App

1. Open Android Studio with the SmartPermits project
2. Sync Gradle and build
3. For emulator: set `BASE_API_URL` to `http://10.0.2.2:5000/api/` and `SOCKET_SERVER_URL` to `http://10.0.2.2:5000` in `app/src/main/java/project/smartpermits/api/ApiConfig.java`
4. For physical phone: set both constants to your PC's current Wi-Fi IP, e.g. `http://YOUR_PC_IP:5000/api/` and `http://YOUR_PC_IP:5000` (same Wi-Fi network required)
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
6. Navigate back to dashboard — all menu items, labels, and buttons are translated
7. Open drawer menu — all navigation items are in the selected language
8. Sign out and sign back in — the language is preserved
9. Change back to "English" or "System Default" to restore

### 4. Dark Mode Persistence on Sign-Out

1. Open Settings and toggle "Dark Mode" ON
2. Verify the app switches to dark theme
3. Sign out from the drawer menu
4. Sign back in with the same or different account
5. The app should still be in dark mode (theme is preserved across sign-outs)
6. Toggle dark mode OFF and sign out again — it stays light

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
13. **The permit appears immediately on the dashboard with "Submitted" status** — no waiting for AI analysis
14. AI analysis runs in the background; when it finishes the inspector sees it in the review screen
15. If the inspector app is open, a "New Permit Application" system notification appears within ≤6 seconds

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
5. Submit a new permit, have it approved, pay it — watch the timeline grow

### 8. Predictive Wait Time

1. Submit a few permits and have the inspector approve/reject them to build historical data
2. Submit a new permit
3. Open the permit detail
4. See the "Estimated Processing Time" card showing a predicted range like "2–4 days"
5. A confidence percentage is shown (e.g., "65% Confidence")
6. The prediction accounts for: permit type history, document count, pending queue size, and day of week
7. Note: the prediction only appears for "submitted" status permits and requires at least one previously reviewed permit of the same type

### 9. Permit Expiry Tracking

1. Complete the full flow: submit permit → inspector approves → citizen pays
2. Open the completed permit detail
3. See the "Permit Validity" card showing "Valid until YYYY-MM-DD" in green
4. On the dashboard, completed permits show the fee amount normally
5. When a permit is within 30 days of expiry, the dashboard card shows "Expires in Xd" in amber
6. Expired permits show "EXPIRED" in red on the dashboard card
7. The expiry period varies by permit type (365 days for Construction, 30 days for Events, non-expiring for Occupancy Certificate)

### 10. Permit Details, Map, and Processing Time

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
4. Tap the AI card to open full analysis dialog with formatted text (bold headings, bullet points)
5. If no AI analysis exists, a "Run AI Analysis" button appears — tap it to manually trigger analysis
6. See applicant info, description, and attached documents
7. For Construction/Renovation permits: map card shows pinned work location
8. Add optional review notes
9. Tap "Approve" or "Reject"
10. A timeline event is created automatically for the review action

### 12. Blockchain Permit Notarization

**What it does:** When a permit is approved, the backend:
1. Reads each uploaded document file from disk and computes a SHA-256 hash of its bytes
2. Builds a JSON payload with permit metadata + document hashes, and computes a SHA-256 hash of that payload → this is the `blockchain_hash`
3. Sends the `blockchain_hash` as the `data` field of an Ethereum transaction on Sepolia testnet → this is the `blockchain_tx_hash`
4. No files are uploaded to the blockchain — only the 32-byte hash fingerprint

**To test:**
1. Configure blockchain in `.env` (optional — see setup)
2. Login as inspector and approve a pending permit
3. Login as citizen and open the approved permit
4. See the "Blockchain Notarization" card showing:
   - "Verified on Blockchain" (green) if on-chain transaction succeeded
   - "Hash Recorded Locally" (amber) if blockchain was not configured — the `blockchain_hash` still exists but there is no on-chain TX
   - Transaction hash in monospace font
   - Document hash (SHA-256)
   - "View on Etherscan" button
5. Tap "View on Etherscan" to open the transaction on https://sepolia.etherscan.io/
6. On Etherscan, look at the "Input Data" field of the transaction — it should show the `blockchain_hash` value, proving the permit was notarized at that exact block time

**Public verification endpoint (no login needed):**
```
GET http://localhost:5000/api/permits/{id}/verify-blockchain
```
Returns: `blockchain_hash`, `blockchain_tx_hash`, `etherscan_url`, and `verified` (true/false)

### 13. PDF Certificate (Professional Government Design)

1. Open a "Completed" permit (approved + paid)
2. Tap "Download Certificate"
3. PDF is saved to Downloads folder and automatically opens in the device's PDF viewer
4. Verify the overall page layout:
   - Off-white (#F8F9FA) background with white content card
   - Classic double-line decorative border — outer dark teal (#005f6b) and inner gold (#C9A84C) offset 6pt inward
   - Diagonal "ISSUED" watermark lightly visible across the page at ~15% opacity
5. Verify the **Header Zone**:
   - Gold ⚜ emblem centered at top
   - "OFFICIAL MUNICIPAL PERMIT CERTIFICATE" in bold teal 13pt all-caps
   - Department subtitle "Issued by the Department of Municipal Affairs & Urban Development" in italic gray 9pt
   - Thick teal horizontal rule separator
   - Certificate number formatted as **№ SP-00001** (using `№` symbol) in a light teal background box
6. Verify the **Permit Information section**:
   - "PERMIT INFORMATION" section header with a teal left accent bar
   - Two-column table with label column (bold, dark gray) and value column (regular black)
   - Alternating row backgrounds: white and light teal (#EAF4F4), no visible cell borders
   - Rows include: Permit Type, Permit ID, Applicant, Description, Fee Amount, Status, Application Date, Issued Date, Validity Period
   - **STATUS row** shows a colored pill badge — green for COMPLETED, teal for APPROVED, red for REJECTED
   - **Validity Period row** shows issued date → expiry date with arrow separator
7. Verify the **Authority Block**:
   - Italic disclaimer text: "This permit has been reviewed and approved by a certified municipal inspector..."
   - Dashed signature line with "Municipal Inspector" and "Department of Urban Development" below
   - Official seal circle drawn to the right with dashed circumference, inner ring, star, and "OFFICIAL SEAL" text in teal
8. Verify the **Blockchain Verification section** (if blockchain is configured):
   - "BLOCKCHAIN VERIFICATION" header with teal accent bar
   - Dark-themed inset box with #0D1B2A background and white/green text
   - "VERIFIED ON ETHEREUM SEPOLIA TESTNET" in green monospace at the top
   - Transaction Hash and Document Hash in monospace 7.5pt (Courier or DejaVuSansMono)
   - Green checkmark ✔ status line and Etherscan URL in light blue
   - QR code to the right of the dark box with "Scan to Verify on Etherscan" caption below
9. Verify the **Footer Zone**:
   - Full-width dark teal bar at the very bottom of the page
   - Left side: "SmartPermits Platform | smartpermits.gov" in white 7.5pt
   - Center: gold diamond symbol ◆
   - Right side: "Generated: [date] at [time] UTC" in white 7.5pt
   - Just above the bar: tamper disclaimer in gray 7pt italic
10. Change language to Romanian in Settings, then download the certificate again — verify Romanian characters (ăîâșț) render correctly in all zones including the authority disclaimer and section headers
11. Try Polish (łźż), Turkish (çğışö), Ukrainian (Кирилиця) — all special characters must display without black squares or boxes
12. The PDF language applies to all text: section headers, table labels, authority disclaimer, official seal label, scan caption, footer disclaimer

### 14. In-App Chat / Comments with Live Notifications

1. Open any permit detail (citizen or inspector)
2. Tap "Comments" button
3. Type a message and tap send
4. Messages appear in a chat-style interface
5. Your messages appear on the right, others on the left
6. Comments are restricted to participants — citizens can only see their own permit's comments, inspectors can see all comments
7. **Comment notification (citizen → inspector)**: While the inspector app is open on the dashboard, send a comment from the citizen app — within seconds the inspector receives a system notification showing "New Comment from [citizen name]" and the message preview, without any manual refresh
8. **Comment notification (inspector → citizen)**: While the citizen app is open on the dashboard, send a comment from the inspector app — the citizen receives a system notification showing the inspector's name and message preview

### 15. Payment and Expiry

1. Have an inspector approve a permit
2. Login as citizen and open the approved permit
3. Tap "Pay Now"
4. Status changes to "Completed"
5. An expiry date is set based on permit type (e.g., 365 days for Business License)
6. Timeline shows "Payment Received" and "Certificate Issued" entries

### 16. Permit Renewal / Reapply

1. Open a "Completed" permit
2. Tap "Renew" button
3. A new permit is created with the same type, description, and pinned map location, status "Submitted"
4. Timeline shows "Submitted" with note "Renewed from permit #X"
5. For rejected permits, tap "Reapply" — works the same way

### 17. Appointment Scheduling

1. Open an "Approved" permit (as citizen)
2. Tap "Schedule Inspection"
3. Select a future date using the date picker
4. Select a time slot
5. Tap "Schedule"
6. Timeline shows "Appointment Scheduled" entry
7. Inspector can see all scheduled appointments from their dashboard

### 18. Analytics Dashboard (Inspector)

1. Login as inspector
2. Open drawer menu and tap "Analytics"
3. See statistics cards: Total Reviewed, Approved, Rejected, Pending
4. Average processing time displayed (computed from historical data)
5. Pie chart shows approval vs rejection ratio
6. Bar chart shows busiest permit types

### 19. Dark Mode

1. Open drawer menu and tap "Settings"
2. Toggle "Dark Mode" switch
3. App switches to dark theme
4. Toggle "Follow System" to auto-detect system dark mode
5. Close and reopen app to verify dark mode persists
6. Sign out and sign back in — dark mode is still active

### 20. Profile Management

1. Open drawer and tap "My Profile"
2. See profile info: name, role, email
3. Tap "Edit Profile" to change name and email
4. Tap avatar change button to upload a new profile photo (image files only: jpg, jpeg, png, gif, webp, bmp)

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
5. See the trashed permit with "X days until permanent deletion" label (30-day countdown)
6. Tap "Restore" to move it back to active permits
7. Tap "Delete" to permanently delete it immediately
8. Permits left in trash for 30 days are automatically purged when the server starts

### 23. Change Password

1. Open drawer and tap "Change Password"
2. Enter current password incorrectly — verify error message
3. Enter correct current password and a new password
4. Tap Save
5. Sign out and sign back in with the new password to confirm it worked

### 24. Delete Account

1. Open "My Profile" from the drawer
2. Scroll to the bottom and tap "Delete Account"
3. Confirm in the dialog
4. All permits, documents, comments, and appointments are permanently deleted
5. You are redirected to the login screen

### 25. Per-Permit Access Control (403)

1. As `citizen1`, create a permit and note its ID
2. Register or log in as a different citizen account (not an inspector)
3. Using that second account, call any permit-scoped endpoint for the first citizen's permit ID, e.g. `GET /api/permits/{id}`, `GET /api/permits/{id}/timeline`, `GET /api/permits/{id}/certificate`, `POST /api/permits/{id}/comments`, `POST /api/permits/{id}/ai-analyze`, or `PUT /api/appointments/{id}`
4. Every call must return `403 {"error": "Unauthorized"}` — a non-owner citizen cannot read or modify another citizen's permit
5. Log in as `inspector1` and repeat the same calls — they succeed, because inspectors may act on all permits

### 26. AI Permit Copilot (Conversational Agent)

The Permit Copilot is an AI chat assistant (Google Gemini with function-calling) that lets a citizen describe a project in plain language and then recommends the right permit type, lists the required documents, estimates the fee and processing time, and pre-fills the application form.

1. Login as `citizen1`
2. Open the Copilot from either entry point:
   - The **✨ sparkle button** in the top-right of the Citizen Dashboard header, or
   - The **Permit Copilot** item at the top of the navigation drawer
3. The intro screen shows a welcome message and three tappable suggestion chips (e.g. "I want to add a second floor to my house")
4. Tap a suggestion **or** type your own project description (e.g. "I want to open a small restaurant downtown") and tap send
5. A "Thinking…" indicator appears while the agent works
6. The Copilot replies conversationally. It may ask **one short clarifying question** first — answer it in natural language and the conversation continues with full context
7. Once it understands the project, the Copilot returns a **recommendation card** showing:
   - The recommended permit type (localized)
   - Three stat pills: **Fee**, **Valid for** (validity), and **Est. time** (predicted processing range, e.g. "3–5 days")
   - The full **required-documents** checklist for that permit type
   - A **"Start application"** button and an **"Ask something else"** link
8. Tap **"Start application"** → the Apply for Permit screen opens with the **permit type already selected** and the **description pre-filled** with the AI-drafted text. The required-document checklist for that type is shown, ready for uploads
9. Tap **"Ask something else"** to keep chatting (e.g. ask about a different project) without leaving the screen

**Multi-language test (all 10 languages):**
1. Go to Settings → App Language → select **Romanian** (or Spanish, French, Italian, German, Portuguese, Polish, Turkish, Ukrainian)
2. Open the Copilot again — the title, subtitle, intro text, suggestion chips, stat labels, and buttons are all in the selected language
3. Type your project in that language (or any language) — **the AI replies, asks questions, writes the summary, and drafts the suggested description entirely in the selected app language**
4. The recommendation card's permit type and required documents are also shown localized
5. Switch back to English and verify the replies return in English

**Notes:**
- The Copilot requires `GEMINI_API_KEY` in `smart_permits_api/.env`. If it is missing, the chat returns a friendly "currently unavailable" message instead of crashing
- The processing-time estimate adapts to the live inspector queue (more pending permits → longer estimate)
- Endpoint used: `POST /api/copilot/chat?lang=<code>` (see API reference)

### Real-Time Updates & Notifications

SmartPermits uses Socket.IO (via the Flask-SocketIO backend) for live updates plus a 6-second silent-polling fallback:

| Event | Who receives it | Delivery |
|---|---|---|
| New permit submitted | Inspector dashboard | Socket broadcast + poll fallback |
| Permit status changed (approved/rejected) | Citizen dashboard | Socket broadcast + poll fallback |
| Comment posted by citizen | Inspector (system notification) | Socket `comment_notification` event |
| Comment posted by inspector | Citizen (system notification) | Socket `comment_notification` event |
| Appointment scheduled | Inspector (system notification) | Socket `appointment_scheduled` event |

**Notifications keep working when the app is closed:** A foreground service (`NotificationService`) keeps the Socket.IO connection alive in the background, so the events above are delivered to the device's notification tray even after the receiving app is swiped away from recents. While logged in, a quiet persistent "SmartPermits — Listening for permit updates" notification indicates the service is running. No Firebase or keys are involved.

**Requirements for live notifications:**
- Both devices must be on the same Wi-Fi network as the Flask server
- The server IP in `ApiConfig.java` must match your current Wi-Fi IP (`ipconfig` → Wi-Fi adapter IPv4)
- Grant notification permission when the app prompts on first launch (Android 13+)
- Sign out and sign back in if you installed a fresh build — this re-establishes the socket connection with the correct user ID

**Test: notification while the app is closed**
1. Log in on both devices (inspector + citizen); confirm the persistent "Listening for permit updates" notification appears on each
2. On the **receiving** device, swipe the app away from recents (fully close the UI)
3. On the **other** device, perform an action (approve/reject a permit, post a comment, or schedule an appointment)
4. ✅ The closed device still rings a system notification within a few seconds; tapping it opens the relevant permit
5. (Limitations: device reboot and aggressive OEM battery managers — Xiaomi/Huawei — can stop the service; those cases need the optional FCM path)

---

## Complete End-to-End Test

1. Start the Flask backend
2. Change language to Romanian in Settings
3. Verify all UI text changed to Romanian
4. Change back to English
5. Toggle dark mode ON
6. Register a new citizen account
7. Apply for a "Construction Permit" with description
8. Upload required documents from the checklist (at least 2–3 files)
9. Use the map search to navigate to a city, then drag map and pin a location
10. Tap "Submit Application" — success screen appears immediately
11. Verify permit appears with "Submitted" status on dashboard
12. Open permit detail and see the Status Timeline with "Submitted" entry
13. See estimated processing time with confidence percentage (may need historical data first)
14. Logout
15. Login as inspector (`inspector1` / `1q2w3e4r`)
16. Open the pending permit
17. See the AI analysis card (if Gemini key configured, analysis will be present; otherwise click "Run AI Analysis")
18. See the map showing the pinned work location
19. See all uploaded documents in the numbered list with labels
20. Add review notes and tap "Approve"
21. If blockchain is configured, verify Flask terminal shows blockchain notarization logs
22. Logout
23. Login as citizen
24. Open the approved permit
25. See the Status Timeline with: Submitted → AI Analysis → Reviewed by Inspector → (Blockchain Notarized if configured)
26. See the Blockchain Notarization card:
    - If configured: "Verified on Blockchain" in green with TX hash and Etherscan link
    - If not configured: "Hash Recorded Locally" in amber with document hash
27. Schedule an inspection appointment (select future date + time slot)
28. See "Appointment Scheduled" added to timeline
29. Pay the permit fee — status changes to Completed
30. See "Payment Received" and "Certificate Issued" in timeline
31. See the Permit Validity card showing expiry date in green
32. Open Comments, send a message as citizen
33. Download the PDF certificate
34. Verify certificate has: branded header, info table, blockchain section, QR code, gold seal
35. Renew the permit — see new permit created with "Renewed from permit #X" in timeline
36. Move the original permit to Trash, verify countdown shows
37. Sign out
38. Verify dark mode is still active on login screen
39. Sign back in and change language to Spanish — verify all text changes
40. Change back to English

---

## Blockchain Verification Manual Test

To manually verify a permit's blockchain notarization without the app:

1. Get the permit ID (visible in the app or from the server database)
2. Open in browser: `http://localhost:5000/api/permits/{id}/verify-blockchain` (no auth needed)
3. Note the `blockchain_hash` and `blockchain_tx_hash` values
4. Open `https://sepolia.etherscan.io/tx/{blockchain_tx_hash}`
5. On Etherscan, click "Click to see More" under Input Data
6. Decode as UTF-8 — you should see the `blockchain_hash` value embedded in the transaction
7. This proves the permit was notarized at the block's timestamp on the public blockchain

---

## Troubleshooting

### App Shows "Connection error"
- Ensure Flask server is running
- For emulator: use `10.0.2.2:5000`
- For physical device: use your PC's current Wi-Fi IP (run `ipconfig`, look at "Wireless LAN adapter Wi-Fi" → IPv4 Address). Update `BASE_API_URL` and `SOCKET_SERVER_URL` in `app/src/main/java/project/smartpermits/api/ApiConfig.java`
- Ensure both phone and PC are on the same Wi-Fi network

### Login Fails
- Restart Flask server to re-seed test accounts
- Delete `instance/smartpermits.db` and restart to reset the database

### Language Not Changing
- Ensure you selected a language from Settings > Language
- The app recreates the activity so the language applies immediately
- Language persists across sign-outs

### Dark Mode Resets on Sign-Out
- This is fixed. Theme preferences are preserved across sign-outs via SharedPreferences clear with selective restore

### Timeline Not Showing
- Timeline appears after at least one event (e.g., submission)
- Ensure the backend has the `permit_events` table — delete old database and restart if migrating

### Estimated Wait Time Not Showing
- Historical data is needed — submit and review a few permits first
- Prediction only appears for "submitted" status permits

### Permit Expiry Not Showing
- Expiry is set when a permit is paid (completed)
- Only completed permits have expiry dates
- Occupancy Certificate has no expiry (non-expiring)

### PDF Certificate Issues
- Ensure `reportlab` and `qrcode` are installed: `pip install -r requirements.txt`
- Permit must be in "Completed" status
- Check server logs for certificate generation errors — the endpoint returns `{"error": "Certificate generation failed: ..."}` with the exact exception
- For proper Unicode rendering (Romanian ăîâșț, Polish łźż, etc.), the `fonts/` directory with `DejaVuSans.ttf` and `DejaVuSans-Bold.ttf` must exist in `smart_permits_api/fonts/`
- For monospace hash rendering, `DejaVuSansMono.ttf` is used if present; otherwise `Courier` (built-in) is used automatically — no action needed
- If the decorative border or watermark is missing, your ReportLab version may not support `setFillAlpha()` — update with `pip install --upgrade reportlab`
- The dark blockchain inset box requires ReportLab ≥ 3.3 for correct background rendering on nested tables
- QR code to the right of the blockchain box requires the `qrcode` package with Pillow: `pip install qrcode[pil]`

### AI Analysis Not Working
- Ensure `GEMINI_API_KEY` is set in the `.env` file
- Check the Flask server terminal for error messages
- The AI tries 3 models (gemini-2.5-flash → gemini-2.0-flash → gemini-2.5-flash-lite) with retries — check logs for which model responded

### Blockchain Notarization Not Working
- Ensure `ETH_PRIVATE_KEY`, `ETH_RPC_URL`, and `ETH_WALLET_ADDRESS` are set in `.env`
- Ensure your wallet has Sepolia test ETH (get from https://sepoliafaucet.com/)
- Ensure the RPC URL is active (Infura, Alchemy, or other provider)
- Without blockchain config: the permit still gets a `blockchain_hash` locally; only the on-chain TX is skipped
- Check the `blockchain_error` field in the permit response for the exact error

### Notifications Not Working
- Grant notification permission: Android Settings → Apps → SmartPermits → Notifications → Allow All
- Ensure the socket connects: sign out and sign back in on both citizen and inspector apps to force a fresh socket handshake
- Verify the server IP in `ApiConfig.java` matches your current Wi-Fi IP (re-check with `ipconfig` — the IP can change when reconnecting to Wi-Fi)
- Socket events require the backend to be running with `python app.py` (not inside Docker unless ports are forwarded)
- Comment notifications require the permit to have a `reviewed_by` inspector assigned (the backend only sends `comment_notification` to `permit.reviewed_by` when the commenter is the citizen). If the permit hasn't been touched by an inspector yet, no `reviewed_by` is set and the comment notification is not sent

### Inspector Dashboard Not Updating Without Refresh
- The dashboard polls every 6 seconds silently in the background — new permits appear automatically within ≤6 s
- If the list never updates, the server IP in `ApiConfig.java` may be stale — update it to your current Wi-Fi IP and rebuild
- The socket path `/socket.io/` must match the Flask-SocketIO mount path

### "Unauthorized" (403) on a Permit Action
- Permit-scoped endpoints (detail, timeline, comments read/write, AI analysis, certificate download, appointment status update) require the caller to either own the permit or be an inspector
- Citizens can only act on their own permits; inspectors can act on all permits
- If you get a 403, ensure you are logged in as the correct role for that permit

### Avatar Upload Fails
- Only image files are accepted: jpg, jpeg, png, gif, webp, bmp
- PDFs and documents are rejected for avatars (use the document upload for permit files instead)

---

## Database Tables

| Table | Columns |
|-------|---------|
| users | id, username, email, password_hash, role, full_name, avatar_url, fcm_token, created_at |
| permits | id, user_id, permit_type, description, status, fee_amount, is_paid, reviewer_notes, reviewed_by, renewed_from, latitude, longitude, ai_analysis, ai_analysis_lang, blockchain_hash, blockchain_tx_hash, blockchain_error, expires_at, created_at, updated_at, deleted_at |
| permit_events | id, permit_id, event_type, actor_name, actor_role, notes, created_at |
| documents | id, permit_id, file_path, file_name, document_label, uploaded_at |
| comments | id, permit_id, user_id, message, created_at |
| appointments | id, permit_id, user_id, date, time_slot, status, notes, created_at |

## Permit Types and Fees

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

## File Locations

- **APK Output**: `app/build/outputs/apk/debug/app-debug.apk`
- **Backend Database**: `smart_permits_api/instance/smartpermits.db`
- **Uploaded Documents**: `smart_permits_api/uploads/`
- **Backend Server**: `smart_permits_api/app.py`
- **Blockchain Module**: `smart_permits_api/blockchain.py`
- **AI + Blockchain Config**: `smart_permits_api/.env`
- **Language Resources**: `app/src/main/res/values-{lang}/strings.xml`
- **Locale Helper**: `app/src/main/java/project/smartpermits/LocaleHelper.java`
- **PDF Unicode Fonts**: `smart_permits_api/fonts/DejaVuSans.ttf`, `DejaVuSans-Bold.ttf`
- **API / Socket Config**: `app/src/main/java/project/smartpermits/api/ApiConfig.java` (change `BASE_API_URL` and `SOCKET_SERVER_URL` here)
