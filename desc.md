# SmartPermits — Platformă Digitalizată pentru Permise și Licențe Municipale

> **Document de referință tehnică** pentru partea practică a lucrării de licență.
> Acest document descrie sistemul exact așa cum este implementat în cod (verificat în sursă), pentru a fi citat și parafrazat în redactarea tezei.
> Ultima sincronizare cu codul sursă: iunie 2026.

---

## CUPRINS

1. Introducere și context
2. Problema și soluția propusă
3. Arhitectura generală a sistemului
4. Stiva tehnologică (versiuni exacte)
5. Modelul de date (schema bazei de date)
6. Referința completă a API-ului REST
7. Comunicare în timp real (Socket.IO) și notificări push (FCM)
8. Funcționalități principale (detaliate)
9. Subsisteme avansate (AI, Copilot conversațional, Blockchain, PDF, Predicție)
10. Arhitectura aplicației Android
11. Securitate
12. Internaționalizare (i18n) și temă
13. Fluxuri de utilizare end-to-end
14. Scenarii de testare
15. Deployment și configurare
16. Acoperirea funcționalităților
17. Concluzii și dezvoltări viitoare
18. Fragmente de cod reprezentative (pentru documentație)
19. Bibliografie

---

## 1. Introducere și context

Procesul de obținere a permiselor și licențelor în administrația municipală este, în mod tradițional, consumator de timp și greu de navigat. Cetățenii trebuie să se deplaseze la mai multe departamente, să completeze formulare diferite, să depună documente pe hârtie și să aștepte răspunsuri fără vizibilitate asupra progresului. Inspectorii, la rândul lor, gestionează dosare pe hârtie, fără o evidență digitală unitară a volumului de lucru și a timpilor de procesare.

**SmartPermits** este o platformă full-stack care digitalizează întregul ciclu de viață al unei cereri de permis, oferind:
- o **aplicație mobilă Android** pentru cetățeni (solicitanți);
- o **tablă de control mobilă** pentru inspectori (revizori);
- un **server API REST** (Python/Flask) care orchestrează logica de business, persistența, analiza AI, notarizarea blockchain și generarea certificatelor.

Sistemul integrează autentificare securizată (JWT + bcrypt), analiză automată a documentelor cu inteligență artificială (Google Gemini), notarizare pe blockchain (Ethereum Sepolia), generare de certificate PDF oficiale cu suport Unicode complet, comunicare în timp real (Socket.IO), notificări push (Firebase Cloud Messaging), suport multilingv pentru 10 limbi și mod întunecat.

---

## 2. Problema și soluția propusă

### 2.1 Probleme identificate

1. **Fragmentare administrativă** — cetățeanul contactează mai multe departamente pentru o singură intenție (construcție, afacere, eveniment).
2. **Documente disparate** — cerințele de documente diferă între departamente și sunt comunicate verbal sau pe hârtie.
3. **Lipsă de transparență** — solicitantul nu cunoaște stadiul cererii; inspectorul nu are evidență digitală.
4. **Procese manuale** — verificare vizuală a documentelor, procesare lentă, risc de pierdere a dosarelor.
5. **Lipsa predictibilității** — nici cetățeanul, nici inspectorul nu pot estima timpii de procesare.
6. **Reintroducere de date** — la reînnoiri/reaplicări, datele trebuie reintroduse integral.

### 2.2 Soluția propusă

SmartPermits implementează o platformă unificată care:

- **Centralizează cerințele** — toate cele 8 tipuri de permise sunt definite într-un singur sistem, fiecare cu lista proprie de documente necesare.
- **Digitalizează fluxul** — cetățeanul se înregistrează, aplică online, încarcă documente; inspectorul revizuiește și decide dintr-o tablă de control mobilă.
- **Oferă transparență** — fiecare permis are un *timeline de audit* care înregistrează fiecare eveniment (depunere, analiză AI, revizuire, plată, notarizare, inspecție, emitere certificat).
- **Automatizează verificarea** — Gemini AI analizează imaginile documentelor și emite o recomandare; blockchain-ul notarizează certificatul final.
- **Predictează timpii** — pe baza datelor istorice, sistemul estimează durata de procesare cu un interval de încredere.
- **Refolosește datele** — reînnoirile și reaplicările precompletează automat tipul și descrierea.

---

## 3. Arhitectura generală a sistemului

SmartPermits urmează o arhitectură **client-server pe trei straturi**:

```
┌──────────────────────────────┐         HTTP/REST (JSON)          ┌───────────────────────────────────────┐
│      CLIENT ANDROID (Java)    │  ───────────────────────────────▶ │        SERVER API (Python / Flask)      │
│                               │  ◀─────────────────────────────── │                                         │
│  • Activities (UI)            │         JSON / PDF binar          │  • Endpoints REST (Flask)               │
│  • Retrofit 2 + OkHttp 3      │                                   │  • Autentificare JWT (Flask-JWT-Ext.)   │
│  • Gson (serializare)         │  ◀── WebSocket (Socket.IO) ─────▶ │  • ORM (Flask-SQLAlchemy)               │
│  • Glide (imagini)            │      evenimente în timp real      │  • Logica de business                   │
│  • osmdroid (hartă)           │                                   │                                         │
│  • MPAndroidChart (grafice)   │                                   │      ┌──────────────────────────────┐   │
│  • Socket.IO client           │                                   │      │  SQLite (smartpermits.db)    │   │
└──────────────────────────────┘                                   │      └──────────────────────────────┘   │
            ▲                                                       │                                         │
            │  Notificări push (FCM)                                │   Integrări externe:                    │
            └───────────────────────────────────────────────────── │   • Google Gemini API (AI)              │
                          Firebase Cloud Messaging                  │   • Ethereum Sepolia (web3 / Infura RPC)│
                                                                    │   • Firebase Admin SDK (push)           │
                                                                    └───────────────────────────────────────┘
```

**Trei canale de comunicare client ↔ server:**
1. **REST/HTTP** — operațiile CRUD și acțiunile (login, creare permit, upload, review, plată etc.).
2. **WebSocket (Socket.IO)** — evenimente push instantanee server → client (permit nou, revizuire, comentariu, programare).
3. **Serviciu de prim-plan (foreground service)** — un `NotificationService` Android menține conexiunea Socket.IO activă în fundal, astfel încât notificările sunt livrate în tava de notificări a sistemului **chiar și atunci când aplicația este închisă** (eliminată din recente). Funcționează integral pe LAN, fără chei sau cont extern. *(Opțional, backend-ul include și un canal Firebase Cloud Messaging, folosit doar dacă este configurat un cont de serviciu Firebase.)*

---

## 4. Stiva tehnologică (versiuni exacte)

### 4.1 Frontend — Aplicația Android

| Element | Valoare |
|---|---|
| Limbaj | Java |
| `applicationId` / `namespace` | `project.smartpermits` |
| `minSdk` | 29 (Android 10) |
| `targetSdk` / `compileSdk` | 36 |
| `versionName` / `versionCode` | 1.0 / 1 |
| Compatibilitate Java | Java 11 |
| Arhitectură UI | Activity-based (fără MVVM); apeluri Retrofit direct din Activities |

**Biblioteci (definite în `app/build.gradle.kts` prin version catalog `libs`):**

- AndroidX: `appcompat`, `activity`, `constraintlayout`, `cardview`, `recyclerview`, `swiperefreshlayout`
- Material Components (`material`)
- **Retrofit 2** (`retrofit`, `retrofit-gson`) — client HTTP REST
- **OkHttp 3** (`okhttp-logging`) — interceptori, logging
- **Gson** — serializare/deserializare JSON
- **Glide** — încărcare și caching imagini (documente, avatare)
- **MPAndroidChart** — grafice (pie/bar) pentru analitice
- **osmdroid** — hartă OpenStreetMap (fără cheie API)
- **Socket.IO client** (`socketio-client`) — comunicare în timp real
- Testare: `junit`, `androidx.test.ext:junit`, `espresso-core`

### 4.2 Backend — Server API (Python/Flask)

Versiuni exacte din `requirements.txt`:

| Pachet | Versiune | Rol |
|---|---|---|
| `Flask` | 3.1.0 | Framework web / API REST |
| `Flask-SQLAlchemy` | 3.1.1 | ORM peste SQLite |
| `Flask-JWT-Extended` | 4.7.1 | Autentificare cu token JWT |
| `Flask-Cors` | 5.0.1 | CORS (acces din aplicația mobilă) |
| `Flask-Bcrypt` | 1.0.1 | Hashing parole (bcrypt) |
| `Flask-SocketIO` | 5.3.5 | Comunicare WebSocket în timp real |
| `Werkzeug` | 3.1.3 | Utilitare WSGI, `secure_filename` |
| `reportlab` | 4.1.0 | Generare certificate PDF |
| `qrcode[pil]` | 7.4.2 | Generare cod QR (verificare blockchain) |
| `firebase-admin` | 6.4.0 | Notificări push (FCM) |
| `google-genai` | ≥1.0.0 | Client Google Gemini (analiză AI) |
| `Pillow` | ≥10.0.0 | Procesare imagini pentru AI/PDF |
| `python-dotenv` | ≥1.0.0 | Citire variabile de mediu (`.env`) |
| `web3` | ≥6.0.0 | Notarizare Ethereum (Sepolia) |
| `python-socketio`, `python-engineio` | ≥5.0 / ≥4.0 | Stratul Socket.IO server |
| `eventlet` | ≥0.33.0 | Server async pentru Socket.IO |

**Configurări cheie ale serverului (`app.py`):**
- Bază de date: `sqlite:///smartpermits.db` (în folderul `instance/`)
- `JWT_SECRET_KEY = 'smart-permits-secret-key-2026'`
- Expirare token JWT: **30 de zile**
- Dimensiune maximă upload: `MAX_CONTENT_LENGTH = 16 MB`
- Folder upload: `uploads/`
- CORS: deschis (`CORS(app)`), Socket.IO `cors_allowed_origins="*"`
- Pornire: `socketio.run(app, host='0.0.0.0', port=5000)`

---

## 5. Modelul de date (schema bazei de date)

Sistemul folosește 6 tabele relaționale (definite în `models.py`).

### 5.1 `users`
| Coloană | Tip | Observații |
|---|---|---|
| `id` | Integer (PK) | |
| `username` | String(80) | unic, obligatoriu, min. 3 caractere |
| `email` | String(120) | unic, obligatoriu, validat cu regex |
| `password_hash` | String(200) | bcrypt |
| `role` | String(20) | `citizen` (implicit) sau `inspector` |
| `full_name` | String(150) | |
| `avatar_url` | String(300) | numele fișierului avatar |
| `fcm_token` | String(500) | token Firebase pentru push |
| `created_at` | DateTime | |

### 5.2 `permits` (entitatea centrală)
| Coloană | Tip | Observații |
|---|---|---|
| `id` | Integer (PK) | |
| `user_id` | FK → users.id | solicitantul |
| `permit_type` | String(100) | unul din cele 8 tipuri |
| `description` | Text | descrierea lucrării |
| `status` | String(30) | `submitted` → `approved`/`rejected` → `completed` |
| `fee_amount` | Float | taxa (din `FEE_TABLE`) |
| `is_paid` | Boolean | |
| `reviewer_notes` | Text | nota inspectorului |
| `reviewed_by` | FK → users.id | inspectorul care a revizuit |
| `renewed_from` | Integer | ID-ul permisului-sursă (reînnoire) |
| `latitude`, `longitude` | Float | locația de pe hartă |
| `deleted_at` | DateTime | soft-delete (trash); `NULL` = activ |
| `ai_analysis` | Text | rezultatul analizei Gemini |
| `ai_analysis_lang` | String(10) | limba în care a fost generată analiza |
| `blockchain_hash` | String(66) | SHA-256 al metadatelor + hash-uri documente |
| `blockchain_tx_hash` | String(70) | hash-ul tranzacției Ethereum |
| `blockchain_error` | String(500) | mesaj de eroare (dacă notarizarea eșuează) |
| `expires_at` | DateTime | data de expirare (setată la plată) |
| `created_at`, `updated_at` | DateTime | `updated_at` cu `onupdate` automat |

> **Notă pentru teză:** câmpurile calculate (nu stocate) returnate în `to_dict()`: `applicant_name`, `reviewer_name`, `estimated_processing_time`, `prediction_confidence`, `point_estimate`, `days_to_expiry`, `is_expired`, `days_until_permanent_delete`, `documents`, `timeline`.

### 5.3 `documents`
| Coloană | Tip | Observații |
|---|---|---|
| `id` | Integer (PK) | |
| `permit_id` | FK → permits.id | |
| `file_path` | String(300) | calea pe disc |
| `file_name` | String(200) | numele securizat (`secure_filename`) |
| `document_label` | String(200) | eticheta (ex. „Urban Planning Certificate") |
| `uploaded_at` | DateTime | |

### 5.4 `permit_events` (timeline de audit)
| Coloană | Tip | Observații |
|---|---|---|
| `id` | Integer (PK) | |
| `permit_id` | FK → permits.id | |
| `event_type` | String(50) | ex. „Submitted", „Reviewed by Inspector", „Blockchain Notarized" |
| `actor_name` | String(150) | cine a generat evenimentul |
| `actor_role` | String(20) | `citizen` / `inspector` / `system` |
| `notes` | Text | detalii |
| `created_at` | DateTime | |

### 5.5 `comments`
| Coloană | Tip | Observații |
|---|---|---|
| `id`, `permit_id`, `user_id` | | |
| `message` | Text | |
| `created_at` | DateTime | |
| (calculate) | | `author_name`, `author_role` |

### 5.6 `appointments`
| Coloană | Tip | Observații |
|---|---|---|
| `id`, `permit_id`, `user_id` | | |
| `date` | String(20) | format `YYYY-MM-DD` |
| `time_slot` | String(20) | intervalul orar |
| `status` | String(20) | `scheduled` / `confirmed` / `completed` / `cancelled` |
| `notes` | Text | |
| `created_at` | DateTime | |

### 5.7 Auto-migrații
La pornire, `run_migrations()` inspectează schema existentă și adaugă coloanele lipsă prin `ALTER TABLE` (pentru `avatar_url`, `fcm_token`, `renewed_from`, `deleted_at`, `latitude`, `longitude`, `document_label`, `ai_analysis`, `ai_analysis_lang`, `blockchain_*`, `expires_at`). Astfel baza de date evoluează fără a fi recreată.

### 5.8 Date inițiale (`seed_data`)
La prima pornire se creează două conturi de test:
- **inspector1** / parolă `1q2w3e4r` (rol `inspector`, email `inspector@city.gov`)
- **citizen1** / parolă `1q2w3e4r` (rol `citizen`, email `citizen@email.com`)

---

## 6. Referința completă a API-ului REST

Toate rutele sunt prefixate cu `/api/`. Cele marcate 🔒 necesită antet `Authorization: Bearer <JWT>`.

### 6.1 Autentificare și cont
| Metodă | Rută | Descriere |
|---|---|---|
| POST | `/auth/register` | Înregistrare (validare email/parolă/username); întoarce token + user |
| POST | `/auth/login` | Autentificare; întoarce token + user |
| 🔒 POST | `/auth/change-password` | Schimbare parolă (verifică parola curentă) |
| 🔒 POST | `/auth/fcm-token` | Salvează tokenul FCM al utilizatorului |
| 🔒 GET | `/auth/profile` | Profilul curent |
| 🔒 PUT | `/auth/profile` | Actualizează nume/email |
| 🔒 POST | `/auth/profile/avatar` | Upload avatar (multipart) |
| 🔒 DELETE | `/auth/delete-account` | Șterge contul și toate datele asociate |

### 6.2 Permise — cetățean
| Metodă | Rută | Descriere |
|---|---|---|
| 🔒 GET | `/permits` | Listează permisele proprii (+ `?search=&status=&type=`) |
| 🔒 POST | `/permits` | Creează permis (status `submitted`); emite eveniment Socket.IO către inspectori |
| 🔒 GET | `/permits/{id}` | Detalii permis (cu timeline și documente) |
| 🔒 POST | `/permits/{id}/upload` | Upload document (multipart + `document_label`) |
| 🔒 POST | `/permits/{id}/ai-analyze` | Declanșează/întoarce analiza AI (`?lang=&force=`) |
| 🔒 POST | `/copilot/chat` | Agent AI conversațional (Permit Copilot) — întoarce răspuns + recomandare de permis (`?lang=`) |
| 🔒 POST | `/permits/{id}/pay` | Plată simulată (approved → completed) |
| 🔒 POST | `/permits/{id}/renew` | Reînnoire/reaplicare (din completed/rejected) |
| 🔒 GET | `/permits/{id}/timeline` | Evenimentele de audit |
| 🔒 GET | `/permits/{id}/comments` | Thread comentarii |
| 🔒 POST | `/permits/{id}/comments` | Adaugă comentariu (Socket.IO + push) |
| 🔒 POST | `/permits/{id}/appointment` | Programează inspecție (doar pentru approved) |
| 🔒 GET | `/permits/{id}/certificate` | Descarcă PDF (`?lang=`) |
| GET | `/permits/{id}/verify-blockchain` | **Public** — verificare blockchain (fără auth) |

### 6.3 Trash (coș de gunoi)
| Metodă | Rută | Descriere |
|---|---|---|
| 🔒 POST | `/permits/{id}/trash` | Soft-delete |
| 🔒 POST | `/permits/{id}/restore` | Restaurează |
| 🔒 DELETE | `/permits/{id}/permanent-delete` | Ștergere definitivă (din trash) |
| 🔒 GET | `/permits/trash` | Conținutul coșului |
| 🔒 DELETE | `/permits/trash/empty` | Golește coșul |

### 6.4 Inspector
| Metodă | Rută | Descriere |
|---|---|---|
| 🔒 GET | `/permits/pending` | Permise în așteptare (`?search=&type=`) — *doar inspector* |
| 🔒 GET | `/permits/reviewed` | Permise revizuite de inspectorul curent |
| 🔒 POST | `/permits/{id}/review` | Aprobă/respinge (`action`, `notes`); declanșează blockchain la aprobare |
| 🔒 GET | `/permits/stats/analytics` | Statistici inspector |

### 6.5 Programări și utilitare
| Metodă | Rută | Descriere |
|---|---|---|
| 🔒 GET | `/appointments` | Inspector: toate programările; cetățean: doar ale lui |
| 🔒 PUT | `/appointments/{id}` | Actualizează statusul programării |
| GET | `/permit-types` | Lista tipurilor + taxe |
| GET | `/uploads/{filename}` | Servește fișierele încărcate |

---

## 7. Comunicare în timp real (Socket.IO) și notificări push (FCM)

### 7.1 Camere (rooms) Socket.IO
La conectare, clientul transmite `?user_id=<id>`. Serverul îl alătură la:
- `user_<id>` — camera personală a utilizatorului;
- `inspectors` — dacă rolul este `inspector`;
- `permit_<id>` — la deschiderea unui permit (`join_permit_room`), pentru chat live.

### 7.2 Evenimente emise de server
| Eveniment | Cameră țintă | Declanșator |
|---|---|---|
| `new_permit_submitted` | `inspectors` | cetățean depune un permit nou |
| `permit_status_updated` | `user_<solicitant>` | inspector aprobă/respinge |
| `permit_reviewed` | `permit_<id>` | finalizarea revizuirii |
| `new_comment` | `permit_<id>` | comentariu nou (pentru chat live) |
| `comment_notification` | `user_<destinatar>` | notificare de comentariu |
| `appointment_scheduled` | `inspectors` | programare nouă |
| `appointment_status_updated` | `user_<solicitant>` | schimbare status programare |

Clientul Android (`SocketIOManager`) reconvertește aceste evenimente în **broadcast-uri locale Android** (ex. `project.smartpermits.NEW_PERMIT`, `...PERMIT_STATUS_UPDATED`, `...NEW_COMMENT`), pe care Activitățile le ascultă pentru a-și actualiza UI-ul instant. Reconectarea este automată (delay 1–5 s, încercări nelimitate).

### 7.3 Notificări când aplicația este închisă (serviciu de prim-plan)
Mecanismul principal pentru livrarea notificărilor atunci când aplicația nu este deschisă este un **serviciu de prim-plan** Android, `NotificationService`. Acesta menține procesul aplicației și conexiunea Socket.IO active, astfel încât receptorul de difuzare global din `SmartPermitsApp` poate transforma în continuare evenimentele socket în notificări de sistem (`NotificationHelper`), chiar dacă utilizatorul a închis interfața.

**Caracteristici:**
- Pornit la autentificare (`LoginActivity`), readus la viață la relansarea aplicației dacă există o sesiune (`SmartPermitsApp.onCreate`) și oprit la deconectare (`RetrofitClient.clearSession`).
- `START_STICKY` + `onTaskRemoved` — serviciul este repornit de sistem sub presiune de memorie și supraviețuiește eliminării din aplicațiile recente.
- Afișează o notificare persistentă, de prioritate redusă („Listening for permit updates"), cerință obligatorie pentru rularea unui serviciu de prim-plan. Pe Android 14+ tipul serviciului este `specialUse` (declarat în manifest cu permisiunile `FOREGROUND_SERVICE` și `FOREGROUND_SERVICE_SPECIAL_USE`).
- Nu necesită niciun cont extern, cheie sau fișier de configurare — funcționează integral pe LAN, reutilizând infrastructura Socket.IO existentă.

**Notificări push opționale (Firebase Cloud Messaging):** suplimentar, `send_push_notification(user_id, title, body, data)` poate trimite mesaje prin Firebase Admin SDK, **doar dacă** există fișierul `firebase-service-account.json` și utilizatorul are un `fcm_token`. Dacă Firebase nu este configurat, funcția devine no-op. Această cale (împreună cu SDK-ul `firebase-messaging` și un `FirebaseMessagingService` pe Android) ar fi necesară doar pentru push la nivel de sistem de operare (de ex. supraviețuirea unui restart al dispozitivului) și **nu** este activată în build-ul curent.

> **Limitări:** serviciul de prim-plan livrează fiabil cât timp dispozitivul este pornit; nu supraviețuiește unui restart al dispozitivului și poate fi oprit de managerele agresive de baterie ale unor producători (Xiaomi/Huawei). Pentru aceste cazuri ar fi necesară calea FCM.

---

## 8. Funcționalități principale (detaliate)

### 8.1 Cele 8 tipuri de permise — taxe și valabilitate

| Tip permis | Taxă (`FEE_TABLE`) | Valabilitate (`PERMIT_VALIDITY_DAYS`) |
|---|---|---|
| Construction Permit | $500.00 | 365 zile |
| Renovation Permit | $350.00 | 180 zile |
| Business License | $150.00 | 365 zile |
| Food Service Permit | $200.00 | 365 zile |
| Event Permit | $100.00 | 30 zile |
| Signage Permit | $75.00 | 730 zile |
| Demolition Permit | $450.00 | 180 zile |
| Occupancy Certificate | $120.00 | 0 = **fără expirare** |

*(Taxa implicită pentru un tip necunoscut este $100.00.)*

### 8.2 Documente necesare per tip (`REQUIRED_DOCUMENTS`)
Fiecare tip are o listă proprie, afișată în aplicație ca un chestionar cu checkbox-uri. Exemple:
- **Construction Permit (7 documente):** Urban Planning Certificate, Land Registry Extract, Topographic Survey Plan, Authorized Technical Project, Utility Approvals (Water, Gas, Electricity), Geotechnical Study, Fee Payment Proof.
- **Food Service Permit (6):** Veterinary Sanitary Authorization, HACCP Plan, Pest Control Service Contract, Environmental Approval, Business Registration Certificate, Water Quality Analysis Report.
- **Event Permit (6):** Event Organization Request, Security Plan, Police Approval, Fire Department Approval, Sanitation Service Contract, Liability Insurance Policy.

*(Lista completă pentru toate cele 8 tipuri este definită în `app.py`, dicționarul `REQUIRED_DOCUMENTS`.)*

### 8.3 Procesul de cerere de permit
1. **Selecție tip** dintr-un dropdown cu cele 8 tipuri.
2. **Chestionar documente** — listă cu checkbox + buton de upload; bifa devine verde după încărcare.
3. **Descriere** a lucrării.
4. **Hartă OpenStreetMap (osmdroid)** pentru permisele cu localizare — căutare adresă, deplasare hartă, plasare pin (lat/long salvate).
5. **Previzualizare taxă** (din `FEE_TABLE`).
6. **Depunere** — serverul creează permisul cu status `submitted`, înregistrează evenimentul „Submitted" în timeline și emite `new_permit_submitted` către inspectori. Cetățeanul revine imediat pe dashboard; **aplicația Android declanșează apoi, asincron, endpoint-ul `/ai-analyze`** astfel încât analiza AI nu blochează UI-ul.

### 8.4 Revizuire de către inspector
- **Tablă „pending"** — listă cu căutare și filtru, actualizată instant prin Socket.IO și prin polling silențios.
- **Detalii** — solicitant, descriere, taxă, documente numerotate (galerie „#1 — etichetă"), analiza AI (card colapsabil cu preview), hartă cu locația.
- **Decizie** — aprobare sau respingere, cu notă opțională.
- **Efecte la aprobare** — notarizare blockchain, eveniment în timeline, `permit_status_updated` + push către cetățean.

### 8.5 Plată și finalizare
- Permisul `approved` afișează „Plătește Acum".
- Plata este **simulată**: `is_paid = true`, status → `completed`, `expires_at` calculat din `PERMIT_VALIDITY_DAYS`, evenimente „Payment Received" și „Certificate Issued".
- Apare butonul de descărcare a certificatului PDF.

### 8.6 Timeline de audit
Fiecare permis afișează evenimentele cronologic: Submitted → Documents Analyzed by AI → Reviewed by Inspector → Blockchain Notarized → Payment Received → Certificate Issued → Appointment Scheduled → Inspection Completed (fiecare cu actor, rol, dată/oră, notă).

### 8.7 Comentarii / chat în aplicație
Thread per permis. Cetățeanul vede doar comentariile propriului permis; inspectorul are acces. La fiecare comentariu, celălalt participant primește notificare instant (Socket.IO) + push (FCM).

### 8.8 Programare inspecție
Pentru permisele `approved`, cetățeanul alege o dată viitoare și un interval orar (validare: nu în trecut, fără dublură). Inspectorii primesc notificare; evenimentul intră în timeline. Inspectorul poate marca programarea drept confirmată/finalizată/anulată.

### 8.9 Trash / coș de gunoi (soft-delete cu 30 de zile)
- „Mutați în Trash" setează `deleted_at`; permisul dispare din dashboard.
- În coș se afișează un **countdown de 30 de zile** (`days_until_permanent_delete`).
- **Restaurare** (`deleted_at = NULL`), **ștergere definitivă manuală**, **golire coș**.
- **Curățare automată:** `cleanup_old_trash()` rulează o dată la prima cerere după pornire (`@app.before_request`) și șterge definitiv permisele mai vechi de 30 de zile (inclusiv documentele de pe disc, comentariile, programările și evenimentele).

### 8.10 Reînnoiri și reaplicări
Pentru permise `completed` („Reînnoi") sau `rejected` („Reaplică"), `/renew` creează un permis nou cu același tip și descriere, marcat `renewed_from`, precompletat — fără reintroducere manuală.

### 8.11 Expirare și avertizări
La plată se setează `expires_at`. `to_dict()` calculează `days_to_expiry` și `is_expired`. UI-ul afișează „Expiră în Xd" (ambră) sau „EXPIRAT" (roșu). Occupancy Certificate nu expiră.

---

## 9. Subsisteme avansate

### 9.1 Analiză documente cu AI (Google Gemini)
**Endpoint:** `POST /api/permits/{id}/ai-analyze?lang=<cod>&force=<bool>`

**Logică (din `ai_analyze`):**
1. **Caching inteligent** — dacă analiza există deja, nu este eroare și limba nu s-a schimbat (și fără `force`), se întoarce rezultatul memorat (fără apel costisitor la AI).
2. Dacă `GEMINI_API_KEY` lipsește sau nu sunt documente → mesaj informativ salvat ca rezultat.
3. Se construiește un **prompt expert** care include tipul permisului, lista documentelor încărcate (cu etichetă și nume fișier) și lista documentelor necesare; se cere identificarea tipului fiecărui document, extragerea informațiilor cheie (date, nume, ștampile, semnături), semnalarea problemelor (neclar, expirat, ștampilă lipsă), o evaluare de completitudine și o recomandare pentru inspector.
4. **Suport multilingv** — dacă limba ≠ `en`, se adaugă instrucțiunea „Write your ENTIRE response in <Limbă>".
5. Imaginile sunt încărcate în memorie cu Pillow și trimise împreună cu promptul.
6. **Cascadă de modele + retry:** se încearcă pe rând `gemini-2.5-flash` → `gemini-2.0-flash` → `gemini-2.5-flash-lite`; pentru fiecare model, până la 3 reîncercări cu *exponential backoff* la erori 503/`UNAVAILABLE` (2·n s) și 429/`RESOURCE_EXHAUSTED` (3·n s).
7. Rezultatul (text) și limba se salvează în `ai_analysis` / `ai_analysis_lang`; se adaugă evenimentul „Documents Analyzed by AI".

> Toate erorile sunt prinse și salvate ca text („AI analysis failed: …"), astfel încât analiza nu blochează niciodată fluxul.

### 9.2 Notarizare blockchain (Ethereum Sepolia)
**Modul:** `blockchain.py`. Declanșat automat la aprobarea unui permit.

**Algoritm:**
1. Pentru fiecare document se citește conținutul și se calculează `SHA-256` → `file_hash` (hex, 64 caractere).
2. Se construiește un obiect JSON cu `permit_id`, `permit_type`, `description`, `status`, `timestamp` (data creării) și lista `documents` (cu `file_name`, `document_label`, `file_hash`).
3. JSON-ul se serializează cu **chei sortate** (`sort_keys=True`), se encodează UTF-8 și se face `SHA-256` → `blockchain_hash` (hash-ul unic al permisului).
4. Se construiește o tranzacție Ethereum: `to` = propriul wallet (transfer la sine, 0 ETH), `gas = 50000`, `gasPrice` din rețea, `data = bytes(0x + blockchain_hash)`, `chainId` și `nonce` citite din rețea.
5. Tranzacția se semnează cu `ETH_PRIVATE_KEY` și se trimite prin RPC (`ETH_RPC_URL`, ex. Infura). Se salvează `blockchain_tx_hash`.
6. Se adaugă evenimentul „Blockchain Notarized" în timeline.

**Configurare** prin `.env`: `ETH_PRIVATE_KEY`, `ETH_RPC_URL`, `ETH_WALLET_ADDRESS`. Dacă lipsesc, se salvează doar `blockchain_hash` local și se notează motivul în `blockchain_error` (degradare elegantă).

**Verificare publică** (fără autentificare): `GET /api/permits/{id}/verify-blockchain` întoarce hash-urile și linkul `https://sepolia.etherscan.io/tx/<tx_hash>`. Oricine poate confirma pe Etherscan data/ora blocului și `Input Data`. Pe lanț se scriu **doar 32 de octeți** (hash-ul) — fișierele rămân pe server.

### 9.3 Generare certificate PDF (ReportLab)
**Endpoint:** `GET /api/permits/{id}/certificate?lang=<cod>` (doar pentru status `completed`).

Documentul PDF (A4) este generat cu ReportLab și conține:
- **Decor pagină** (callback `_draw_page_decor`): fundal, card alb, dublu chenar (teal 1.5pt + auriu 0.75pt), watermark diagonal „ISSUED", bară footer teal cu „SmartPermits Platform", diamant auriu, data generării și avertisment anti-falsificare.
- **Antet oficial**: emblemă ⚜ (U+2756), titlu, subtitlu departament, linie separatoare.
- **Număr certificat** formatat `№ SP-00042` într-o cutie teal.
- **Tabel informații** cu rânduri alternante (teal deschis/alb): tip permis (tradus), ID, solicitant, descriere, taxă, status (pill color-coded), date.
- **Pill de status**: verde (`completed`), teal (`approved`), roșu (altele).
- **Bloc autoritate**: disclaimer legal (Cod Municipal §14.2), linie de semnătură (`SigLineFlowable`), sigiliu oficial desenat (`SealFlowable` — cerc dashed cu steluță ★).
- **Secțiune blockchain** (dacă există hash): cutie închisă la culoare cu text verde/alb monospațiat — TX hash trunchiat, document hash, bifă ✔ „Verified on Blockchain", link Etherscan; alături, **cod QR** generat cu `qrcode` care duce la pagina Etherscan a tranzacției.

**Suport Unicode** (`_register_unicode_fonts`): se înregistrează `DejaVuSans.ttf` / `DejaVuSans-Bold.ttf` din `fonts/` (cu fallback la fonturi de sistem Windows/Linux și la `DejaVuSansMono`). Astfel se redau corect diacriticele pentru toate cele 10 limbi (română ăîâșț, polonă łźż, turcă çğışö, chirilice ucrainene etc.). Textele PDF provin din dicționarul `PDF_TRANSLATIONS` (10 limbi), iar tipurile de permis din `PERMIT_TYPE_TRANSLATIONS`.

### 9.4 Predicția timpului de procesare (`compute_predicted_wait`)
Pentru permisele `submitted`, sistemul estimează durata pe baza datelor istorice:
1. Calculează **media orelor** de la creare la `updated_at` pentru permisele de același tip deja finalizate (approved/rejected/completed), plus numărul de mostre.
2. Aplică factori de ajustare:
   - **factor coadă**: `1.0 + (nr_permise_în_așteptare × 0.05)`;
   - **factor documente**: `1.0 + (max(0, nr_documente − 3) × 0.03)`;
   - **factor zi**: `1.15` dacă e vineri–duminică, altfel `1.0`.
3. `predicted = avg_hours × factor_coadă × factor_documente × factor_zi`.
4. **Interval** ± varianță (0.30 / 0.20 / 0.12 după numărul de mostre).
5. **Încredere**: `min(95, 50 + nr_mostre × 3)` %.
6. Formatare prietenoasă („< 1 hour", „N hours", „N days"); rezultatul apare pe permisele în stare `submitted`.

### 9.5 Analitice pentru inspector (`/permits/stats/analytics`)
Întoarce: total revizuite, total aprobate (approved+completed), total respinse, total în așteptare, distribuția pe tipuri de permis (`permit_type_counts`) și timpul mediu de procesare în ore. Vizualizat în Android cu MPAndroidChart (pie/bar).

### 9.6 Asistent conversațional „Permit Copilot" (agent AI cu function-calling)
**Endpoint:** `POST /api/copilot/chat?lang=<cod>`

Spre deosebire de analiza de documente (un singur apel, fără memorie), Copilot este un **agent conversațional** care poartă un dialog pe mai multe ture cu cetățeanul și îl ghidează de la o descriere în limbaj natural („Vreau să adaug un etaj casei mele") până la cererea de permis precompletată.

**Logică (din funcția `copilot_chat`):**
1. Clientul trimite întregul istoric al conversației (`messages`: listă de `{role, content}` cu rolurile `user`/`assistant`) împreună cu limba aplicației.
2. Se construiește o **instrucțiune de sistem** care: prezintă cele 8 tipuri de permise cu taxele lor, cere un ton scurt și prietenos, limitează la **o singură întrebare de clarificare** dacă proiectul e ambiguu și impune ca **fiecare cuvânt** (întrebări, rezumat, descriere sugerată) să fie scris în limba selectată.
3. Se declară un **instrument (tool) de function-calling**, `propose_permit_application`, cu parametri tipați (`permit_type` constrâns prin **enum** la cele 8 tipuri valide, `suggested_description`, `summary`). Astfel modelul nu poate „inventa" un tip inexistent.
4. Istoricul se transformă în `types.Content` (rol `user`/`model`) și se trimite cu `GenerateContentConfig(system_instruction, tools, temperature=0.6)`.
5. Aceeași **cascadă de modele + retry** ca la analiza AI (`gemini-2.5-flash` → `2.0-flash` → `2.5-flash-lite`, 3 reîncercări cu backoff la 503/429).
6. Se inspectează răspunsul:
   - dacă modelul a apelat funcția → se citesc argumentele, se validează `permit_type`, iar serverul **îmbogățește** recomandarea cu taxa (`FEE_TABLE`), valabilitatea (`PERMIT_VALIDITY_DAYS`), lista documentelor necesare (`REQUIRED_DOCUMENTS`) și **estimarea timpului** (`estimate_wait_days` — bază pe tip + factor de coadă din numărul de permise în așteptare). Se întoarce `{reply, recommendation}`;
   - altfel → se întoarce doar `reply` (întrebarea de clarificare, în limba aplicației).
7. Dacă `GEMINI_API_KEY` lipsește sau apare o eroare, se întoarce un mesaj prietenos (degradare elegantă), fără a bloca aplicația.

**Pe partea de Android** (`CopilotActivity` + `CopilotAdapter`): un ecran de chat dedicat (bule utilizator/AI, indicator „se gândește", chip-uri de sugestii inițiale) accesibil dintr-un buton ✨ în antetul dashboard-ului cetățeanului și dintr-un element de meniu. Recomandarea este afișată ca un **card** cu pastile de statistici (Taxă / Valabil / Timp est.) și lista documentelor (localizate). Butonul **„Începe cererea"** deschide `ApplyPermitActivity` cu tipul **preselectat** și descrierea **precompletată** (prin `Intent` extras), reutilizând fluxul existent de aplicare. Limba este preluată din `LocaleHelper` și trimisă ca parametru `lang`, deci atât interfața (din `strings.xml`, 10 limbi) cât și răspunsurile AI sunt în limba utilizatorului.

---

## 10. Arhitectura aplicației Android

### 10.1 Structura pachetului `project.smartpermits`
```
project.smartpermits/
├── api/
│   ├── ApiService.java       — interfața Retrofit (toate endpoint-urile)
│   ├── ApiConfig.java        — IP/URL server (BASE_IP, BASE_API_URL, SOCKET_SERVER_URL)
│   ├── RetrofitClient.java   — singleton Retrofit/OkHttp + gestiune sesiune (SharedPreferences)
│   └── SocketIOManager.java  — client Socket.IO + rebroadcast local
├── models/                   — clase de date (Gson): User, Permit, Document, Comment,
│                               Appointment, PermitEvent, PermitType, + Request/Response DTOs
├── adapters/                 — PermitAdapter, PendingPermitAdapter, ChatAdapter,
│                               CopilotAdapter, TrashAdapter, AppointmentAdapter
├── (Activities)              — LoginActivity, MainActivity, CitizenDashboardActivity,
│                               InspectorDashboardActivity, CopilotActivity,
│                               ApplyPermitActivity,
│                               PermitDetailActivity, PermitReviewActivity,
│                               PermitHistoryActivity, ReviewHistoryActivity,
│                               ChatActivity, ScheduleAppointmentActivity,
│                               InspectorScheduleActivity, AnalyticsActivity,
│                               ProfileActivity, EditProfileActivity,
│                               ChangePasswordActivity, SettingsActivity,
│                               DocumentViewerActivity, TrashActivity
├── SmartPermitsApp.java      — clasa Application (init temă/limbă/Socket.IO)
├── LocaleHelper.java         — schimbare limbă (10 limbi)
├── NotificationService.java  — serviciu de prim-plan (menține Socket.IO viu când app e închisă)
├── NotificationHelper.java   — canale + afișare notificări (socket/local + canal serviciu)
├── CurrencyHelper.java       — formatare sume
└── PermitTypeHelper.java     — mapare tip → documente necesare
```

### 10.2 Networking (`RetrofitClient`)
- **Singleton** cu `OkHttpClient` configurat: timeouts (connect 30 s, read 120 s, write 60 s) — read mare pentru analiza AI și generarea PDF.
- **Interceptor de autentificare**: adaugă automat `Authorization: Bearer <token>` dacă există token salvat.
- **Interceptor de răspuns**: la `401`, șterge tokenul și trimite broadcast `SESSION_EXPIRED` (auto-logout, revenire la login).
- **`HttpLoggingInterceptor`** (nivel BASIC) pentru debugging.
- Sesiunea (token, rol, nume, email, avatar, user_id) este persistată în `SharedPreferences` (`smart_permits_prefs`). La `clearSession()` se păstrează preferințele de temă/limbă/notificări.

### 10.3 Permisiuni Android (Manifest)
INTERNET, CAMERA, READ/WRITE_EXTERNAL_STORAGE, READ_MEDIA_IMAGES/VIDEO/AUDIO, POST_NOTIFICATIONS, ACCESS_FINE/COARSE_LOCATION, ACCESS_NETWORK/WIFI_STATE. `usesCleartextTraffic="true"` (pentru HTTP în dezvoltare LAN). `FileProvider` configurat pentru partajarea certificatelor PDF.

---

## 11. Securitate

- **Parole**: hash bcrypt cu salt; minim 6 caractere. Niciodată stocate în clar.
- **JWT**: semnate cu `JWT_SECRET_KEY`, expirare 30 de zile; identitatea = `user_id`.
- **Transport token**: salvat în `SharedPreferences`, atașat automat de interceptor; la `401` → auto-logout.
- **Autorizare pe rol**: rutele de inspector verifică `role == 'inspector'`; un cetățean accesează doar propriile permise (verificare `permit.user_id == user_id`); inspectorul poate vedea toate permisele.
- **Validare input**: format email (regex), lungime parolă/username, tip permis obligatoriu, dată programare în viitor.
- **Upload securizat**: extensii permise (`jpg,jpeg,png,gif,bmp,webp,pdf,doc,docx`; avatar doar imagini), `secure_filename`, limită 16 MB.
- **Integritate documente**: hashing SHA-256 + notarizare blockchain pentru dovadă imuabilă.
- **Endpoint public controlat**: doar `verify-blockchain` este fără autentificare (verificare publică), expunând exclusiv hash-uri, nu fișiere.

---

## 12. Internaționalizare (i18n) și temă

### 12.1 Cele 10 limbi
English (implicit, `values/`), Română (`values-ro`), Spaniolă (`values-es`), Franceză (`values-fr`), Italiană (`values-it`), Germană (`values-de`), Portugheză (`values-pt`), Poloneză (`values-pl`), Turcă (`values-tr`), Ucraineană (`values-uk`).

Limba afectează: toate textele UI, mesajele de eroare, textul certificatelor PDF (`PDF_TRANSLATIONS`), denumirile tipurilor de permis (`PERMIT_TYPE_TRANSLATIONS`), **limba analizei AI** (parametrul `lang` trimis la `/ai-analyze`) și **limba agentului conversațional Permit Copilot** (parametrul `lang` trimis la `/copilot/chat` — întrebările, rezumatul și descrierea sugerată sunt generate integral în limba aplicației). Limba selectată persistă peste sign-out/sign-in (gestionată de `LocaleHelper`, păstrată în preferințe).

### 12.2 Mod întunecat (Dark Mode)
Toggle manual în Settings + opțiune „urmărește setarea de sistem". Tema persistă peste sesiuni (păstrată la `clearSession`).

---

## 13. Fluxuri de utilizare end-to-end

### 13.1 Cetățean — „Construiesc o casă"
1. Înregistrare cu email + parolă (token JWT salvat).
2. (Opțional) Settings → limba Română + Dark Mode.
3. „Aplică pentru Permit" → „Construction Permit".
4. Se afișează cele 7 documente necesare; se încarcă fișierele (bifele devin verzi).
5. Pe hartă se caută adresa, se deplasează harta, se plasează pinul (lat/long).
6. Descriere + previzualizare taxă $500.00 → „Depune Aplicație".
7. Server: permit `submitted`, eveniment „Submitted", `new_permit_submitted` → inspectori. Aplicația declanșează asincron analiza AI.
8. Inspectorul vede permisul în „pending" (instant prin Socket.IO), îl deschide, citește analiza AI și documentele numerotate, vede locația pe hartă, adaugă notă și **aprobă**.
9. Aprobare → notarizare blockchain (hash permit + hash-uri documente → Sepolia), eveniment „Blockchain Notarized", push + Socket.IO către cetățean.
10. Cetățeanul vede status `approved`, „Plătește Acum" → plată simulată → `completed`, `expires_at` = +365 zile.
11. „Descarcă Certificat" → PDF oficial cu QR și secțiune blockchain (link Etherscan).
12. Chat cu inspectorul; programare inspecție.
13. Timeline final: Submitted → Analyzed by AI → Reviewed → Blockchain Notarized → Payment Received → Certificate Issued → Appointment Scheduled.

### 13.2 Inspector
Pending → deschide permit → citește AI + documente + hartă → aprobă/respinge cu notă → tablă „reviewed" → Analytics (rate aprobare/respingere, timp mediu, tipuri populare).

---

## 14. Scenarii de testare

### 14.1 Test E2E complet
Setup backend (`python app.py`) + setare IP în `ApiConfig.java` → înregistrare → setare limbă/temă → aplicare permis cu documente + hartă → verificare analiză AI → login inspector1 → revizuire/aprobare → verificare TX pe Etherscan → login cetățean → plată → descărcare/inspectare PDF → comentarii (notificare) → verificare timeline complet.

### 14.2 Test Trash
Mută permis în trash → verifică countdown 30 zile → restaurează → verifică revenirea pe dashboard → (opțional) ștergere definitivă / golire coș.

### 14.3 Conturi de test preconfigurate
`inspector1 / 1q2w3e4r` și `citizen1 / 1q2w3e4r`.

---

## 15. Deployment și configurare

### 15.1 Dezvoltare locală — backend
```
cd smart_permits_api
python -m venv venv
venv\Scripts\activate
pip install -r requirements.txt
# .env: GEMINI_API_KEY (+ opțional ETH_PRIVATE_KEY, ETH_RPC_URL, ETH_WALLET_ADDRESS)
python app.py            # http://0.0.0.0:5000
```

### 15.2 Dezvoltare locală — Android
- Editează `ApiConfig.java`: `BASE_IP` = IP-ul LAN al PC-ului (sau `10.0.2.2` pentru emulator).
- Sync Gradle → rulează pe emulator/dispozitiv (același WiFi).

### 15.3 Variabile de mediu (`.env`)
`GEMINI_API_KEY`, `ETH_PRIVATE_KEY`, `ETH_RPC_URL`, `ETH_WALLET_ADDRESS`. Fișierul `firebase-service-account.json` (opțional) activează notificările push.

### 15.4 Containerizare (Docker)
`Dockerfile` bazat pe `python:3.11-slim`, instalează `fonts-dejavu-core` (Unicode PDF), dependențele din `requirements.txt`, creează `uploads/`, expune portul 5000.
```
docker build -t smartpermits-api .
docker run -p 5000:5000 -e GEMINI_API_KEY=... smartpermits-api
```

### 15.5 Compilare APK
```
./gradlew assembleDebug      # APK debug
./gradlew assembleRelease    # APK release
```

---

## 16. Acoperirea funcționalităților

| Caracteristică | Implementat | Testat |
|---|---|---|
| Înregistrare / Login (JWT + bcrypt) | ✅ | ✅ |
| Aplicare permit (8 tipuri) | ✅ | ✅ |
| Încărcare documente (etichetate) | ✅ | ✅ |
| Hartă + locație (osmdroid) | ✅ | ✅ |
| Analiză AI Gemini (10 limbi, fallback de modele) | ✅ | ✅ |
| Asistent conversațional AI „Permit Copilot" (function-calling, 10 limbi) | ✅ | ✅ |
| Revizuire inspector | ✅ | ✅ |
| Notarizare blockchain (Sepolia) | ✅ | ✅ |
| Verificare publică blockchain | ✅ | ✅ |
| Certificate PDF (Unicode, QR) | ✅ | ✅ |
| Timeline de audit | ✅ | ✅ |
| Plată simulată + expirare | ✅ | ✅ |
| Reînnoire / reaplicare | ✅ | ✅ |
| Trash + restaurare + curățare 30 zile | ✅ | ✅ |
| Comentarii / chat (timp real) | ✅ | ✅ |
| Programare inspecție | ✅ | ✅ |
| Predicție timp de procesare | ✅ | ⚠️ (necesită date istorice) |
| Analitice inspector (grafice) | ✅ | ✅ |
| Comunicare în timp real (Socket.IO) | ✅ | ✅ |
| Notificări când app e închisă (serviciu de prim-plan) | ✅ | ✅ |
| Notificări push FCM (opțional, la nivel de OS) | ✅ (scaffold backend) | ⚠️ (opțional — necesită config Firebase) |
| Suport multilingv (10 limbi) | ✅ | ✅ |
| Mod întunecat | ✅ | ✅ |
| Profil, avatar, schimbare parolă, ștergere cont | ✅ | ✅ |

---

## 17. Concluzii și dezvoltări viitoare

### 17.1 Realizări
SmartPermits demonstrează o platformă completă și coerentă care:
- **centralizează** toate tipurile de permise într-un singur flux;
- aduce **transparență** prin timeline de audit și actualizări în timp real;
- **automatizează** verificarea (AI) și asigură **integritate verificabilă public** (blockchain);
- este **accesibilă** (mobil, multilingv, dark mode) și produce **documente oficiale** (PDF cu QR).

Sub aspect tehnic, proiectul ilustrează integrarea unui client Android nativ cu un backend Flask, comunicare hibridă REST + WebSocket + push, și integrarea a trei servicii externe (Gemini, Ethereum, Firebase) cu **degradare elegantă** atunci când acestea nu sunt configurate.

### 17.2 Dezvoltări viitoare
1. Integrare bancară reală (plăți efective în loc de simulare).
2. Notificări SMS pentru utilizatorii fără push.
3. API public pentru integrarea cu alte sisteme municipale.
4. Generator de rapoarte (lunar/management).
5. OCR pentru extragerea automată a textului din documente.
6. Migrare de la SQLite la PostgreSQL pentru producție.
7. Sincronizare offline a permiselor.

### 17.3 Impact așteptat
Reducerea semnificativă a timpilor de procesare, eliminarea hârtiei și a deplasărilor, transparență pentru cetățean și vizibilitate financiară în timp real pentru municipalitate.

---

## 18. Fragmente de cod reprezentative (pentru documentație)

> Această secțiune conține bucățile de cod care merită prezentate și explicate în teză (eventual sub formă de captură de ecran / listing cu legendă). Fiecare fragment este preluat exact din sursă, cu referința fișierului și o explicație a rolului său.

### 18.1 Autentificare securizată — hash bcrypt + token JWT
**Fișier:** `smart_permits_api/app.py` (funcția `login`)

```python
@app.route('/api/auth/login', methods=['POST'])
def login():
    data = request.get_json(silent=True) or {}
    username = data.get('username', '')
    password = data.get('password', '')
    user = User.query.filter_by(username=username).first()
    if user is None or not bcrypt.check_password_hash(user.password_hash, password):
        return jsonify({'error': 'Invalid credentials'}), 401
    token = create_access_token(identity=str(user.id))
    return jsonify({'token': token, 'user': user.to_dict()}), 200
```

**Ce ilustrează:** parola nu este niciodată comparată în clar — se folosește `bcrypt.check_password_hash` peste hash-ul stocat; la succes se emite un token JWT semnat (valabil 30 de zile) cu `user_id` ca identitate. Răspunsul de eroare este identic indiferent dacă username-ul există sau parola e greșită (nu dezvăluie ce a greșit atacatorul).

*Legendă sugerată:* „Fig. X — Autentificarea: verificarea parolei cu bcrypt și emiterea token-ului JWT."

### 18.2 Atașarea automată a token-ului + auto-logout la expirare (client Android)
**Fișier:** `app/.../api/RetrofitClient.java`

```java
Interceptor authInterceptor = chain -> {
    Request original = chain.request();
    String token = prefs.getString("auth_token", "");
    if (token != null && !token.isEmpty()) {
        Request request = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();
        return chain.proceed(request);
    }
    return chain.proceed(original);
};

Interceptor responseInterceptor = chain -> {
    okhttp3.Response response = chain.proceed(chain.request());
    if (response.code() == 401) {
        prefs.edit().remove("auth_token").apply();
        appContext.sendBroadcast(new Intent("project.smartpermits.SESSION_EXPIRED"));
    }
    return response;
};
```

**Ce ilustrează:** mecanismul de sesiune pe partea de client — un interceptor OkHttp adaugă automat antetul `Authorization` la **fiecare** cerere, iar al doilea interceptor detectează răspunsul `401` (token expirat/invalid), șterge token-ul și declanșează auto-logout printr-un broadcast. Astfel logica de securitate este centralizată, nu duplicată în fiecare Activity.

*Legendă sugerată:* „Fig. X — Interceptori OkHttp pentru injectarea token-ului și deconectare automată la expirare."

### 18.3 Cascada de modele AI cu reîncercări (exponential backoff)
**Fișier:** `smart_permits_api/app.py` (funcția `ai_analyze`)

```python
models_to_try = ['gemini-2.5-flash', 'gemini-2.0-flash', 'gemini-2.5-flash-lite']
analysis_text = None
for model_name in models_to_try:
    for attempt in range(3):
        try:
            response = client.models.generate_content(model=model_name, contents=contents)
            analysis_text = response.text
            break
        except Exception as retry_err:
            err_str = str(retry_err)
            if '503' in err_str or 'UNAVAILABLE' in err_str:
                _time.sleep(2 * (attempt + 1))      # backoff la server ocupat
                continue
            elif '429' in err_str or 'RESOURCE_EXHAUSTED' in err_str:
                _time.sleep(3 * (attempt + 1))      # backoff la limită de rată
                continue
            else:
                break
    if analysis_text:
        break
```

**Ce ilustrează:** robustețea integrării cu un serviciu extern. Dacă modelul principal este indisponibil (503) sau s-a atins limita de rată (429), sistemul reîncearcă cu pauze crescătoare și, în cele din urmă, trece la un model alternativ. Aplicația nu eșuează la prima eroare temporară.

*Legendă sugerată:* „Fig. X — Strategia de fallback și reîncercare pentru apelurile către Google Gemini."

### 18.4 Prompt-ul multimodal trimis către AI
**Fișier:** `smart_permits_api/app.py` (funcția `ai_analyze`)

```python
prompt = (
    f"You are an expert municipal permit document reviewer.\n"
    f"This is a \"{permit.permit_type}\" application.\n"
    f"The applicant uploaded {len(docs)} document(s):\n{doc_list}\n\n"
    f"Required documents for this permit type:\n{required_list}\n\n"
    f"Analyze ALL the uploaded document images together. For each document:\n"
    f"1. Identify what type of document it appears to be\n"
    f"2. Extract key visible information (dates, names, addresses, stamps, signatures)\n"
    f"3. Note any issues (blurry, incomplete, expired dates, missing stamps)\n\n"
    f"Then provide:\n"
    f"- Overall completeness assessment (which required documents appear present/missing)\n"
    f"- Any warnings or concerns\n"
    f"- Brief recommendation for the inspector{lang_instruction}"
)
contents = [prompt]
for d in docs:
    img = Image.open(d.file_path)   # imaginile sunt încărcate în memorie
    contents.append(img)
```

**Ce ilustrează:** *prompt engineering* — promptul oferă context (tipul permisului, documentele încărcate vs. cele necesare), instrucțiuni structurate și cere o recomandare pentru inspector. Conținutul este **multimodal**: text + imaginile documentelor în același apel. `lang_instruction` forțează răspunsul în limba selectată.

*Legendă sugerată:* „Fig. X — Construcția prompt-ului multimodal (text + imagini) pentru analiza documentelor."

### 18.5 Calculul hash-ului permisului (notarizare blockchain)
**Fișier:** `smart_permits_api/blockchain.py`

```python
def compute_permit_hash(permit_id, permit_type, description, status, documents_data, created_at_iso=None):
    payload = {
        'permit_id': permit_id,
        'permit_type': permit_type,
        'description': description or '',
        'status': status,
        'timestamp': created_at_iso or '',
        'documents': documents_data        # include SHA-256 al fiecărui document
    }
    raw = json.dumps(payload, sort_keys=True).encode('utf-8')
    return hashlib.sha256(raw).hexdigest()
```

**Ce ilustrează:** modul în care se obține o „amprentă" unică și **deterministă** a permisului. `sort_keys=True` garantează că aceleași date produc mereu același hash (ordinea cheilor nu contează), iar includerea hash-urilor documentelor face ca orice modificare ulterioară a unui fișier să schimbe complet rezultatul — baza dovezii de integritate.

*Legendă sugerată:* „Fig. X — Generarea hash-ului SHA-256 determinist al permisului."

### 18.6 Scrierea hash-ului pe Ethereum Sepolia
**Fișier:** `smart_permits_api/blockchain.py`

```python
tx = {
    'nonce': nonce,
    'to': wallet_address,                       # transfer la sine, 0 ETH
    'value': 0,
    'gas': 50000,
    'gasPrice': gas_price,
    'data': w3.to_bytes(hexstr='0x' + permit_hash),   # hash-ul ca payload (32 bytes)
    'chainId': chain_id
}
signed = w3.eth.account.sign_transaction(tx, private_key)
tx_hash = w3.eth.send_raw_transaction(signed.raw_transaction)
return w3.to_hex(tx_hash), None
```

**Ce ilustrează:** notarizarea efectivă — hash-ul de 32 de octeți este înscris în câmpul `data` al unei tranzacții semnate și trimise pe testnet-ul Sepolia. **Fișierele nu ajung niciodată pe blockchain**, doar amprenta lor; tranzacția poate fi verificată public pe Etherscan.

*Legendă sugerată:* „Fig. X — Construirea, semnarea și trimiterea tranzacției de notarizare pe Ethereum."

### 18.7 Notificare în timp real către inspectori la depunerea unui permis
**Fișier:** `smart_permits_api/app.py` (funcția `create_permit`)

```python
db.session.add(permit)
db.session.flush()
log_permit_event(permit.id, 'Submitted', actor_name=user.full_name,
                 actor_role='citizen', notes=f'{permit_type} application submitted')
db.session.commit()

socketio.emit('new_permit_submitted', {
    'permit': permit.to_dict(),
    'timestamp': datetime.utcnow().isoformat()
}, room='inspectors')          # toți inspectorii conectați primesc evenimentul instant
```

**Ce ilustrează:** integrarea dintre persistență, *audit trail* și comunicarea în timp real. După salvarea permisului și înregistrarea evenimentului în timeline, serverul emite un eveniment Socket.IO doar către camera `inspectors`, astfel încât tablele lor de control se actualizează fără refresh.

*Legendă sugerată:* „Fig. X — Emiterea evenimentului Socket.IO către camera inspectorilor."

### 18.8 Recepția evenimentului pe client și retransmiterea ca broadcast Android
**Fișier:** `app/.../api/SocketIOManager.java`

```java
private final Emitter.Listener onNewPermitSubmitted = args -> {
    try {
        JSONObject data = (JSONObject) args[0];
        Intent intent = new Intent("project.smartpermits.NEW_PERMIT");
        intent.setPackage(context.getPackageName());
        intent.putExtra("permit_data", data.toString());
        context.sendBroadcast(intent);   // Activitatea ascultă acest broadcast și se reîmprospătează
    } catch (Exception e) {
        Log.e(TAG, "onNewPermitSubmitted error: " + e.getMessage());
    }
};
```

**Ce ilustrează:** *podul* dintre evenimentele de rețea și componentele UI. Managerul Socket.IO transformă evenimentul WebSocket într-un `Intent` de broadcast intern aplicației; orice Activity vizibilă îl recepționează și își actualizează lista. Decuplarea face ca rețeaua să nu „știe" nimic despre UI.

*Legendă sugerată:* „Fig. X — Conversia evenimentului Socket.IO în broadcast local Android."

### 18.9 Declanșarea notarizării la aprobarea unui permit
**Fișier:** `smart_permits_api/app.py` (funcția `review_permit`)

```python
permit.status = action          # 'approved' sau 'rejected'
permit.reviewed_by = user_id
if action == 'approved':
    try:
        from blockchain import notarize_permit
        docs = Document.query.filter_by(permit_id=permit_id).all()
        permit_hash, tx_hash, bc_error = notarize_permit(permit, docs, app.config['UPLOAD_FOLDER'])
        permit.blockchain_hash = permit_hash
        permit.blockchain_tx_hash = tx_hash
        permit.blockchain_error = bc_error
        if tx_hash:
            log_permit_event(permit.id, 'Blockchain Notarized',
                             actor_name='Ethereum Sepolia', actor_role='system',
                             notes=f'TX: {tx_hash[:20]}...')
    except Exception as e:
        permit.blockchain_error = f'Notarization failed: {str(e)}'
```

**Ce ilustrează:** punctul în care se leagă revizuirea de blockchain. Notarizarea rulează **doar la aprobare**, iar întregul bloc este protejat de `try/except`: dacă blockchain-ul nu e configurat sau apare o eroare, decizia inspectorului se salvează oricum, iar motivul eșecului se reține în `blockchain_error` (degradare elegantă).

*Legendă sugerată:* „Fig. X — Integrarea revizuirii inspectorului cu notarizarea pe blockchain."

### 18.10 Predicția timpului de procesare pe baza datelor istorice
**Fișier:** `smart_permits_api/models.py` (metoda `compute_predicted_wait`)

```python
predicted = avg_hours * queue_factor * doc_factor * day_factor
# queue_factor = 1.0 + (pending_count * 0.05)        — cu cât coada e mai mare, cu atât crește
# doc_factor   = 1.0 + (max(0, doc_count - 3) * 0.03) — documentele suplimentare cresc durata
# day_factor   = 1.15 if weekend else 1.0            — vinerea/weekendul prelungesc procesarea

variance = 0.3 if sample_count < 5 else 0.2 if sample_count < 15 else 0.12
low, high = predicted * (1 - variance), predicted * (1 + variance)
confidence = min(95, 50 + sample_count * 3)          — încrederea crește cu numărul de mostre
```

**Ce ilustrează:** un model simplu, dar transparent, de estimare. Media istorică este ajustată prin trei factori (coadă, complexitate documentară, ziua săptămânii), iar incertitudinea (interval + procent de încredere) scade pe măsură ce se acumulează date — o abordare explicabilă, potrivită administrației publice.

*Legendă sugerată:* „Fig. X — Modelul de estimare a timpului de procesare cu interval de încredere."

### 18.11 Auto-migrarea schemei la pornire
**Fișier:** `smart_permits_api/app.py` (funcția `run_migrations`)

```python
permit_cols = [c['name'] for c in insp.get_columns('permits')]
if 'latitude' not in permit_cols:
    conn.execute(text("ALTER TABLE permits ADD COLUMN latitude FLOAT"))
if 'blockchain_tx_hash' not in permit_cols2:
    conn.execute(text("ALTER TABLE permits ADD COLUMN blockchain_tx_hash VARCHAR(70)"))
```

**Ce ilustrează:** mecanismul prin care baza de date evoluează fără a fi recreată sau fără a pierde datele. La fiecare pornire, serverul inspectează coloanele existente și adaugă, prin `ALTER TABLE`, doar pe cele lipsă — util pe parcursul dezvoltării incrementale a proiectului.

*Legendă sugerată:* „Fig. X — Migrarea automată, idempotentă, a schemei bazei de date."

### 18.12 Soft-delete cu curățare automată după 30 de zile
**Fișier:** `smart_permits_api/app.py`

```python
def cleanup_old_trash():
    cutoff = datetime.utcnow() - timedelta(days=30)
    old_permits = Permit.query.filter(
        Permit.deleted_at.isnot(None),
        Permit.deleted_at < cutoff
    ).all()
    for permit in old_permits:
        # se șterg și documentele de pe disc, comentariile, programările, evenimentele
        ...
        db.session.delete(permit)

@app.before_request
def run_trash_cleanup():
    if not hasattr(app, '_trash_cleaned'):
        app._trash_cleaned = True
        cleanup_old_trash()
```

**Ce ilustrează:** politica de tip „coș de gunoi" — ștergerea este reversibilă timp de 30 de zile (`deleted_at`), după care datele sunt eliminate definitiv, inclusiv fișierele de pe disc. Curățarea se declanșează o singură dată per pornire, prin hook-ul `before_request`, fără a necesita un planificator separat.

*Legendă sugerată:* „Fig. X — Ștergere temporară (soft-delete) și curățarea automată a coșului."

### 18.13 Agentul conversațional cu function-calling (Permit Copilot)
**Fișier:** `smart_permits_api/app.py` (funcția `copilot_chat`)

```python
propose_decl = types.FunctionDeclaration(
    name='propose_permit_application',
    description='Recommend the single best permit type once you understand the citizen project.',
    parameters=types.Schema(
        type=types.Type.OBJECT,
        properties={
            'permit_type': types.Schema(type=types.Type.STRING, enum=list(FEE_TABLE.keys())),
            'suggested_description': types.Schema(type=types.Type.STRING),
            'summary': types.Schema(type=types.Type.STRING),
        },
        required=['permit_type', 'suggested_description', 'summary'],
    ),
)

config = types.GenerateContentConfig(
    system_instruction=system_text,                       # impune limba + tonul + regulile
    tools=[types.Tool(function_declarations=[propose_decl])],
    temperature=0.6,
)
response = client.models.generate_content(model=model_name, contents=contents, config=config)

# dacă modelul a apelat funcția, serverul îmbogățește recomandarea cu date sigure:
args = dict(function_call.args)
ptype = args.get('permit_type')
recommendation = {
    'permit_type': ptype,
    'suggested_description': args.get('suggested_description', ''),
    'fee': FEE_TABLE.get(ptype, 100.0),
    'validity_days': PERMIT_VALIDITY_DAYS.get(ptype, 365),
    'required_documents': REQUIRED_DOCUMENTS.get(ptype, []),
    'estimated_days_min': low, 'estimated_days_max': high,   # din estimate_wait_days()
}
```

**Ce ilustrează:** diferența dintre un simplu apel la AI și un **agent**. Modelul nu întoarce text liber pentru tipul permisului — este obligat, prin schema funcției cu **enum**, să aleagă unul dintre cele 8 tipuri valide. Serverul nu are încredere oarbă în model: preia doar intenția (tipul + descrierea + rezumatul) și **completează el însuși** taxa, valabilitatea, documentele și estimarea de timp din sursele de adevăr (`FEE_TABLE`, `PERMIT_VALIDITY_DAYS`, `REQUIRED_DOCUMENTS`). Instrucțiunea de sistem forțează întregul răspuns în limba aplicației.

*Legendă sugerată:* „Fig. X — Agent AI cu function-calling: schema constrânsă (enum) și îmbogățirea recomandării pe server."

---

## 19. Bibliografie minimală

1. **Flask** — https://flask.palletsprojects.com/ — framework web Python pentru API REST.
2. **Flask-SQLAlchemy / SQLAlchemy ORM** — https://docs.sqlalchemy.org/ — mapare obiect-relațională.
3. **Flask-JWT-Extended** — https://flask-jwt-extended.readthedocs.io/ — autentificare cu token JWT.
4. **Flask-SocketIO / python-socketio** — https://flask-socketio.readthedocs.io/ — comunicare WebSocket.
5. **Retrofit** — https://square.github.io/retrofit/ — client HTTP pentru Android.
6. **Google Gemini API** — https://ai.google.dev/ — model multimodal pentru analiză de documente.
7. **web3.py** — https://web3py.readthedocs.io/ — interacțiune cu rețele Ethereum.
8. **Ethereum / Sepolia testnet & Etherscan** — https://sepolia.etherscan.io/ — verificare tranzacții.
9. **ReportLab** — https://www.reportlab.com/ — generare documente PDF.
10. **Firebase Cloud Messaging** — https://firebase.google.com/docs/cloud-messaging — notificări push.
11. **osmdroid / OpenStreetMap** — https://github.com/osmdroid/osmdroid — hărți fără cheie API.
12. **MPAndroidChart** — https://github.com/PhilJay/MPAndroidChart — grafice Android.
13. **Material Design** — https://m3.material.io/ — sistem de design.
14. **Android Developers Guide** — https://developer.android.com/ — documentație oficială.

---

*Document redactat ca referință tehnică pentru lucrarea de licență. Conținutul reflectă implementarea reală a codului sursă (backend `smart_permits_api/` și aplicație `app/`). Data sincronizării: iunie 2026.*
