# SmartPermits - Platformă Digitalizată pentru Permise și Licență Municipale

## 1. Introducere și Context

Procesul de obținere a permiselor și licențelor în administrația municipală este, de obicei, consumator de timp și dificil de navigat. Cetățenii trebuie să se deplaseze la mai multe departamente, să completeze formulare diferite, să ridice documente și să aștepte răspunsuri fără o vizibilitate asupra progresului. Inspectorii, la rândul lor, gestionează solicitări pe hârtie, documente detașate și nu au o perspectivă asupra volumului de lucru și a timpului de procesare.

SmartPermits este o platformă full-stack care digitalizează întregul proces de cereri de permise și licență, oferind o interfață mobilă pentru cetățeni și o tablă de control pentru inspectori. Sistemul combină autentificare securizată, analiză de documente cu inteligență artificială, notarizare blockchain, generare de certificate PDF profesionale și alte funcționalități avansate care vin în sprijinul unei administrații moderne și eficiente.

## 2. Problemă și Soluție

### Probleme Identificate

1. **Fragmentare administrativă**: Cetățenii trebuie să contacteze mai multe departamente pentru o singură intenție (construcție, afacere, eveniment)
2. **Documente disparate**: Cerințele de documente diferă între departamente și sunt comunicate oral sau pe hârtie
3. **Fără transparență**: Solicitanții nu știu în ce stadiu se află cererea; inspectorii nu au o evidență digitală unitară
4. **Procese manuale**: Verificare vizuală a documente, procesare lentă, riscuri de pierdere a dosarelor
5. **Fără planificare**: Nici cetățenii nu știu cât de mult vor aștepta, nici inspectorii nu știu ce vin pe teren
6. **Documente duplicate**: Pentru reînnoiri sau reamplasări, dovezi trebuie re-încărcate integral

### Soluția Propusă

SmartPermits implementează o platformă unificată care:

- **Centralizeaza cerințele**: Toate tipurile de permise sunt definite într-un singur sistem cu documente specifice pentru fiecare tip
- **Digitalizează fluxul**: Cetățenii se înregistrează, aplică on-line, încarcă documente, iar inspectorii revizuiesc și aproba din tablă de control mobilă
- **Oferă transparență**: Un calendar cu stări și timeline de audit arată fiecare eveniment (depunere, analiză AI, revizuire, plată, notarizare blockchain)
- **Automatizează verificarea**: Gemini AI analizează documente și extrage informații cheie; blockchain notarizează certificatele finale
- **Predictează timpii**: Bazat pe date istorice, sistemul prezice durata medie de procesare
- **Refolosește date**: Reînnouiri și reamplasări preompletează aplicația cu datele anterioare

## 3. Caracteristici Principale și Avansate

### 3.1 Autentificare și Gestionare Utilizatori

- **Înregistrare și autentificare sigură**: Parolele sunt hash-uizate cu bcrypt, tokenuri JWT cu expirare de 30 de zile
- **Persistență sesiune**: Tokenul salvat pe telefon permite auto-login la redeschiderea aplicației
- **Gestionare rol**: Doi roluri principale — cetățean (solicitant) și inspector (revisionist)
- **Profil utilizator**: Nume, email, avatar, opțiuni de schimbare parolă
- **Ștergere cont**: Cetățenii pot șterge permanent contul și toate datele asociate

### 3.2 Procesul de Cerere de Permit

1. **Selecție tip de permit**: Aplicația listează 8 tipuri: Autorizație Construcție, Renovare, Licență Afaceri, Autorizație Alimentară, Eveniment, Semnalistică, Demolare, Certificat Ocupare
2. **Chestionar documente**: Pentru fiecare tip, se afișează o listă cu documente necesare (de ex., Certificat Urbanistic, Extrat Registru Fonciar, Certificat Tehnic Autorizat etc.)
3. **Control upload**: Fiecare document are o casuță și buton de încărcare; checkbox devine verde după încarcare
4. **Descriere detaliat**: Cetățeanul completează descrierea lucrării
5. **Hartă pentru locație**: Pentru permise de construcție/renovare, se afișează o hartă OpenStreetMap interactiv unde citadinos poate căuta o locație, trageți harta și pune pina pe locul lucrării
6. **Previzualizare taxă**: Se afișează suma datorată (ex., 500$ pentru construcție, 100$ pentru eveniment)
7. **Depunere instant**: După apăsarea „Depune Aplicație", serverul creează cererea și cetățeanul revine imediat pe dashboard unde permisul apare cu statusul „Depus" — analiza AI rulează în fundal fără a bloca UI. Tabloul de bord al inspectorului se actualizează automat în ≤6 secunde prin polling silențios și Socket.IO

### 3.3 Analiză Documente cu AI (Google Gemini)

- **Declanșare automată**: După depunere, serverul trimite toate imaginile încărcate la Gemini 2.5 Flash
- **Instrucțiuni detaliate**: AI analizează tip document, extrage informații (date, nume, stampile), detectează probleme (imagine neclar, data expirată, stampilă lipsă)
- **Comparație cu lista**: AI verific ce documente din lista necesară sunt prezente și care lipsesc
- **Recomandare**: Oferă o recomandare pentru inspector privind acceptabilitate
- **Suport multilingv**: Analiza se poate cere în orice din 10 limbile suportate
- **Fără cost excesiv**: Se face o singur cerere per depunere; inspectorul o vede într-un card colapsabil

### 3.4 Revizuire Inspector

- **Tablă de control pending**: Inspectorul vede lista permiselor în așteptare cu căutare și filtru; lista se actualizează automat la fiecare 6 secunde (polling silențios) și instant prin Socket.IO când un cetățean depune o nouă cerere
- **Detalii aplicație**: Nume solicitant, descriere, fee, documente încărcate, analiza AI, hartă cu locația
- **Documente numerotate**: Se afișează o galerie orizontal cu toate documentele, etichetate ca "#1 — Certificat Urbanistic" etc.
- **Analiză AI colapsabilă**: Un card cu un preview pe 4 linii; click pentru a vedea analiza completă cu text formatat
- **Notițe revizor**: Inspector adaug note opționale
- **Decizie**: Aprobă sau respinge, cu notă opțional

### 3.5 Plată și Completare

- **Stare Aprobat**: După revizuire pozitiv, statutul devine „Aprobat" și cetățeanul vede butonul „Plătește Acum"
- **Simulare plată**: Click pe „Plătește" simulează o tranzacție și schimbă statutul în „Finalizat"
- **Expirare permit**: Datele de valabilitate sunt setate în funcție de tip (365 zile pentru Construcție, 30 zile pentru Eveniment, fără expirare pentru Certificat Ocupare)
- **Certificat Professional**: Un fișier PDF cu design oficial municipal este generat și poate fi descărcat

### 3.6 Timeline-ul de Audit

Fiecare permit are o demonstrație vizuală a tuturor evenimentelor:

- **Depus**: Cetățean, data și ora
- **Analizat de AI**: Sistem, notă despre analiză
- **Revizuit de Inspector**: Nume inspector, aprobat/respins, notă
- **Notarizat Blockchain**: Ethereum Sepolia, hash TX
- **Plată Primită**: Suma, data
- **Programare Inspecție**: Data și ora ternei
- **Inspecție Completă**: Inspector, data
- **Certificat Emis**: Sistem, link descărcare

### 3.7 Notarizare Blockchain (Ethereum Sepolia)

- **Hash dokument**: Fiecare fișier încărcat este hash-uit (SHA-256)
- **Hash permit**: Metadate permit + hashes documente = hash unic al permisului
- **Scriere pe lanț**: Hash-ul este trimis ca data dintr-o tranzacție Ethereum pe testnet Sepolia
- **Tablă publică**: Linkul Etherscan permite oricui să verific online că permisul a fost emis la o dată și oră specific
- **Geen fișiere pe lanț**: Doar hash-ul (32 de octeți) este pe lanț; fișierele rămân pe server
- **Redundanț**: Dacă blockchain nu este configurat, se salvează hash-ul local; TX-ul blockchain nu se face

### 3.8 Certificate PDF Professional

Atunci când un permit este Finalizat, un PDF cu design oficial este gerat cu:

- **Decor pagină**: Fundal alb-gălbui cu border decorativ cu linie dublă (teal și auriu)
- **Header official**: Emblemă ⚜, titlu bold, subtitlu departament, linie separatorie
- **Număr certificat**: Formatat ca „№ SP-00003" într-o cutie teal deschis
- **Tabel informații**: Tip permit, ID, solicitant, descriere, taxă, stare, date
- **Status pil**: Roșu (Respins), Teal (Aprobat), Verde (Finalizat)
- **Bloc autoritate**: Disclaimer, linie semnătură, sigiliu oficial ( cerc dashed cu steluță teal)
- **Blockchain section**: Cutie neagr cu text alb/verde care arată TX hash, document hash, verificare checkmark, link Etherscan
- **Cod QR**: Scaneaza pentru verificare Blockchain (link sepolia.etherscan.io)
- **Footer**: Teal pe tot lățimea, logo platformă, diamant auriu, data generare
- **Unicode complet**: Suport pentru română (ăîâșț), polonez (łźż), turcă (çğışö), ucraineană (Kirilic) cu fonturi DejaVuSans

### 3.9 Sistem Trash / Coş de Gunoi

- **Ștergere soft**: Cetățean apasă „Mutați în Trash" pe un permit
- **Contdown 30 zile**: Permisul trece în coș cu un timer care arată cât timp mai dureaza până la ștergere permanent
- **Restaurare**: Înainte de 30 zile, cetățean poate „Restaura" și permitele revin în dashboard
- **Ștergere permanent manually**: Buton pentru ștergere imediat din coș
- **Ștergere automată**: După 30 zile, serverul șterge permanent la fiecare start
- **Golire coș**: O opțiune pentru a șterge toate dintr-o dată

### 3.10 Schimbare Parolă

- **Verificare curent**: Sistemul cere parola actuală
- **Validare**: Minimum 6 caractere pentru noua parolă
- **Hashing bcrypt**: Noua parolă este hash-uizată și salvată

### 3.11 Reînnoiri și Reamplasări

- **Permit Completat**: Buton „Reînnoi" creează un nou permit cu același tip și descriere
- **Permit Respins**: Buton „Reamplasă" face acelaș lucru, permițând re-aplicare
- **Precompletare**: Datele vechi sunt precompletate, cetățeanul nu trebuie să rescrie

### 3.12 Programare Inspecție

- **După aprobare**: Un permit aprobat poate fi programat pentru inspecție pe teren
- **Selector dată + timp**: Cetățean alege o dată viitoare și o franjeră orară
- **Notificare inspector**: Inspector vede programările într-o tablă de control
- **Timeline**: Un eveniment „Programare Inspecție" este adăugat la timeline-ul permitului

### 3.13 Comentarii și Chat în aplicație

- **Thread comentarii**: Sub fiecare permit, cetățean și inspector (dacă i-a fost atribuit) pot lăsa mesaje
- **Restricții acces**: Cetățean vede doar comentariile propriului permit; inspector vede toate
- **Notificări în timp real**: Când cetățeanul trimite un comentariu, inspectorul primește imediat o notificare de sistem (fără refresh) cu numele cetățeanului și previzualizarea mesajului. La fel invers — când inspectorul comentează, cetățeanul primește notificarea
- **Livrare dublă**: Notificările sunt trimise atât prin Socket.IO (instant) cât și prin Firebase Cloud Messaging (FCM) ca fallback
- **Tip colocvial**: Permitele pot cere documente suplimentare fără a respinge, direct prin comentariu

### 3.14 Tip Timp Estimat de Procesare

- **Date istorice**: Se calculeaza media timpului de la depunere până la revizuire pentru fiecare tip
- **Variabile**: Se ia în considere tip permit, numărul de documente, cateoga în așteptare a inspecrotului, ziua săptămânii
- **Ranger și procecentă**: Se arată o gamă (ex. 2–4 zile) cu asigurând procent (ex. 65%)
- **Display**: Apare pe permitele în stare „Depus"

### 3.15 Expirare și Avertizări

- **Dată expirare**: Setată după plată în funcție de tip (ex. 365 zile)
- **Countdown: La 30 de zile de nici, permitele sunt marcate ca „Expiră în Xd" cu culoare ambra
- **Expirare**: După expirare, sunt marcate „EXPIRAT" în roșu
- **Non-expiring**: Certificatul de Ocupare nu expir
- **Reînnoi**: Butoane pentru reînnoi pe permitele completate

### 3.16 Suport Multilingv (10 Limbi)

Aplicația suportă:
- English (implicit)
- Român (valori-ro)
- Spaniol (valori-es)
- Francez (valori-fr)
- Italian (valori-it)
- German (valori-de)
- Portughez (valori-pt)
- Polonez (valori-pl)
- Turcă (valori-tr)
- Ucraineană (valori-uk)

Limba afectează:
- Toate labelele UI, butoanele, meniuri
- Mesajele de eroare
- Textul PDF certificatelor
- Instrucțiuni de prompt AI (análisis în limba selectată)

**Persistență**: Limba selectată rămâne și după sign-out/sign-in

### 3.17 Mod Intunecat (Dark Mode)

- **Toggle manual**: Setări > Aspect > Activează Mod Intunecat
- **Urmărire sistem**: Opțiune pentru a urmări setarea de sistem
- **Culori corespunzâtoare**: Fundal gri profund, accent teal deschis, text alb
- **Persistență sesiune**: Tema selectată rămâne și după sign-out/sign-in

### 3.18 Notificări Push (Firebase Cloud Messaging)

- **Declanșare: Inspector aprobă/respinge → notificare pentru cetățean
- **Comentarii**: Fiecare comentariu nou declanșează o notificare
- **Programare inspecție**: Inspectorii sunt notificați când o inspecție este programată
- **Backend Firebase**: Serverul Flask trimite mesaje prin Firebase Admin SDK
- **Opțional**: Dacă nu este configurat firebase-service-account.json, nici o notificare

## 4. Arhitectura Sistem

### 4.1 Stivă Tehnologică

#### Frontend (Android)

- **Limbaj**: Java
- **Min SDK**: 29 (Android 10+)
- **Target SDK**: 36
- **Arhitectură**: Activity-based (nu MVVM, ci direct în Activities cu Retrofit calls)
- **HTTP Client**: Retrofit 2 cu OkHttp 3
- **Serializare JSON**: Gson
- **Încârcire imagini**: Glide
- **UI Components**: Material Components (CardView, Chip, FAB, TextInputLayout, Switch)
- **Grafice**: MPAndroidChart (pie charts, bar charts)
- **Hartă**: osmdroid (OpenStreetMap, fără cheie API)
- **Pull-to-refresh**: SwipeRefreshLayout
- **File Sharing**: FileProvider (pentru certificate PDF)

#### Backend (Python + Flask)

- **Framework**: Flask
- **ORM**: Flask-SQLAlchemy
- **Autentificare**: Flask-JWT-Extended
- **Hashing parolă**: Flask-Bcrypt
- **CORS**: Flask-CORS
- **Bază date**: SQLite (cu suport auto-migrații)
- **AI**: google-genai (Gemini 2.5 Flash, cu fallback 2.0-flash, 2.5-flash-lite)
- **Blockchain**: web3.py (Ethereum Sepolia)
- **PDF**: ReportLab (cu Unicode DejaVuSans fonts)
- **QR codes**: qrcode module cu Pillow
- **Imagini**: Pillow (pentru redimensionare, citire în AI)
- **Variabile env**: python-dotenv
- **Push notificări**: firebase-admin
- **Containerizare**: Docker dockerfile inclus

### 4.2 Diagrama Flux Principal

#### Flux Cetățean

1. **Înregistrare/Login** → JWT token salvat
2. **Tablă de control**: Lista permitelor propriului utilizator, căutare, filtru
3. **Cerere nouă**: Selectez tip → Iau documente → Încarc fișiere → Harță opțional → Submit
   - Serverul creează Permit în stare „Depus"
   - AI analizie în fundal
4. **Detaliu permit**: Vad stare, timeline, hartă, documente, analiză AI, comentarii
5. **Aprobat**: Apare un buton „Plătește Acum"
6. **Plă**: Status → Finalizat, dată expirare setată, timeline updated
7. **Descărcare certificat**: PDF professional se deschide
8. **Reînnoi**: Buton pentru a crea o nouă aplicație cu date precompletate

#### Flux Inspector

1. **Tablă de control pending**: Lista permitelor în stare „Depus"
2. **Deschid permit**: Vad solicitant, documente numerotate, analiză AI, hartă
3. **Revizuire**: Adaug notă opțional, aprobă sau respinge
4. **Efecte**:
   - Dacă aprobat → Blockchain notarizare se declanșează, timeline updated, notificație cetățean
   - Dacă respins → Status se schimbă, notificație cetățean
5. **Tablă de control completate**: Vad permisele revizuite anterior
6. **Analitics**: Statistici — total revizuite, rație aprobări/respingeri, timp mediu, tipuri popular

### 4.3 Bază Date

#### Tabele Principale

- **users**: id, username, email, password_hash, role, full_name, avatar_url, fcm_token, created_at
- **permits**: id, user_id, permit_type, description, status, fee_amount, is_paid, reviewer_notes, reviewed_by, renewed_from, latitude, longitude, ai_analysis, ai_analysis_lang, blockchain_hash, blockchain_tx_hash, blockchain_error, expires_at, deleted_at, created_at, updated_at
- **documents**: id, permit_id, file_path, file_name, document_label, uploaded_at
- **permit_events**: id, permit_id, event_type, actor_name, actor_role, notes, created_at
- **comments**: id, permit_id, user_id, message, created_at
- **appointments**: id, permit_id, user_id, date, time_slot, status, notes, created_at

### 4.4 Puncte Terminale API Esențiale

- **POST /api/auth/register** → Înregistrare
- **POST /api/auth/login** → Autentificare
- **GET /api/permits** → Lista permiselor utilizatorului curent (cu căutare/filtru)
- **POST /api/permits** → Creează permit nou
- **GET /api/permits/{id}** → Detalii permit (inclusiv timeline)
- **POST /api/permits/{id}/upload** → Încarcă document
- **POST /api/permits/{id}/ai-analyze** → Declanșează analiză AI
- **POST /api/permits/pending** → Lista pending (inspect-only)
- **POST /api/permits/{id}/review** → Aprobă/respinge (inspect-only)
- **POST /api/permits/{id}/pay** → Simulează plată
- **POST /api/permits/{id}/trash** → Mută în trash
- **DELETE /api/permits/trash/empty** → Golește trash
- **GET /api/permits/{id}/certificate** → Descarcă PDF
- **GET /api/permits/{id}/verify-blockchain** → Verifică public blockchain
- **POST /api/permits/{id}/appointment** → Programează inspecție
- **POST /api/permits/{id}/comments** → Adaugă comentariu
- **GET /api/permits/stats/analytics** → Statistici inspector

## 5. Flow-uri de Caz de Utilizare

### 5.1 Caz: Construire Casa - Flux Complet

1. Cetățean se înregistrează cu email și parolă
2. Din tablă de control, apasă „Aplică pentru Permit"
3. Selectează „Construction Permit" din dropdown
4. Vede lista de documente necesare: Certificat Urbanistic, Extrat Fonciar, Plan Topografic, Certificat Tehnic Autorizat, etc.
5. Apasă „Încarcă" pentru fiecare document și selectează un fișier de pe telefon
6. Casuțele devin verzi după încărcare
7. Harta OpenStreetMap apare — tipuri o adresă și apasă căutare, hartă se deplasează
8. Târg hartele cu degetul pentru a centra pe locul construcției, apoi apasă „Pun Pina Aici"
9. Completează descriere: „Construiesc o casă de 200 mp pe parcela mea"
10. Vede taxa: $500.00
11. Apasă „Depune Aplicație" — succes imediat, AI analizie în fundal
12. După ~ 1 minut, AI-ul a analizat documentele si a oferit o recomandare
13. Inspector vede permitele în așteptare; deschide aceasta, vede analizia AI pe 4 linii, click pentru a vedea completă
14. Deschide hartă și vede locul constrcției
15. Citește documente numerotate: #1 Certificat (scan bun) #2 Extrat Fonciar (ok), etc.
16. Adaug notă: „Aprob — certificatele sunt în ordine" și apasă Aproba
17. Blockchain notarizare se activează: hash-ul permit cu hashes-ul fiecărui document se scrie pe Ethereum Sepolia
18. Cetățean primește notificare push: „Permitea dumneavoastră a fost aprobat"
19. Deschide aplicația, vede stare Aprobat, buton "Plătește Acum"
20. Apasă plată → status devine Completat, data expirării setată la 365 zile
21. Apare buton „Descarcă Certificat"
22. PDF-ul se deschide cu design oficial, conține TX blockchain Etherscan și QR code
23. Deschide Comentarii, trimite mesaj inspectorului cu o întrebare
24. Inspector răspunde — Cetățeanul primește notificare
25. Apasă „Programează Inspecție", selectează o dată și oră
26. Timeline-ul său afișează acum: Depus → Analizat AI → Revizuit → Notarizat Blockchain → Plătit → Certificat Emis → Programat

## 6. Detalii de Implementare Tehnică

### 6.1 Securitate

- **Parolă**: Bcrypt cu salt, minim 6 caractere
- **JWT**: Semnate cu cheie secretă, expirare 30 zile
- **Token auto-refresh**: Token salvat în SharedPreferences (Android), auto-trimitere în Authorization header
- **CORS**: Server acceptă cereri din IP local (dev) și desert configurabil
- **Validare input**: Email format, lungimea parolei, nume permit
- **Authorization**: Cetățean vede doar propriile permitele; Inspector poate vedea toate; Comentarii și timeline restricționate corespunzător

### 6.2 Analiză AI - Detalii

#### Secvență

1. Cetățean depune permit cu documente
2. Endpoint `/api/permits/{id}/ai-analyze` este apelat asincron (fire-and-forget)
3. Flask descarcă fiecare imagine document de pe disk
4. Construiește prompt detalit incluzând tip permit și lista documente necesare
5. Trimite `gemini-2.5-flash` cu imagini în URL (în memorie, nu încărcate separate)
6. Dacă 503/429 -> retry până la 3 ori cu exponential backoff
7. Dacă model nu răspunde -> încearcă `gemini-2.0-flash`, apoi `gemini-2.5-flash-lite`
8. Scrieți răspunsul text în database sub `ai_analysis` și respectivul limbă

#### Prompt Exemplu

```
You are an expert municipal permit document reviewer.
This is a "Construction Permit" application.
The applicant uploaded 3 document(s):
  Document 1: "Certificat Urbanistic" (file: 3_Certificat_Urbanistic.jpg)
  Document 2: "Extrat Fonciar" (file: 3_Extrat.jpg)
  Document 3: "Plan Topografic" (file: 3_Plan.jpg)

Required documents for this permit type:
  - Urban Planning Certificate
  - Land Registry Extract
  - Topographic Survey Plan
  - Authorized Technical Project
  ...

Analyze ALL the uploaded document images together. For each document:
1. Identify what type of document it appears to be
2. Extract key visible information (dates, names, addresses, stamps, signatures)
3. Note any issues (blurry, incomplete, expired dates, missing stamps)

Then provide:
- Overall completeness assessment (which required documents appear present/missing)
- Any warnings or concerns
- Brief recommendation for the inspector
```

### 6.3 Blockchain Notarizare - Detalii

#### Algoritm

1. Pentru fiecare document încărcat, citește octeții și calculează SHA-256: `file_hash = hashlib.sha256(file_bytes).hexdigest()` → string hex 64 caractere
2. Construiește obiect JSON:
   ```json
   {
     "permit_id": 123,
     "permit_type": "Construction Permit",
     "description": "Building a house",
     "status": "approved",
     "timestamp": "2026-06-16T14:32:11.123456",
     "documents": [
       {"file_name": "cert.jpg", "document_label": "Certificate", "file_hash": "a3f4b5..."},
       {"file_name": "extract.jpg", "document_label": "Extract", "file_hash": "c9e2d1..."}
     ]
   }
   ```
3. Serializeaza JSON cu chei sortate, encode UTF-8, calculeaza SHA-256 → `blockchain_hash`
4. Construiește tranzacție Ethereum:
   - **from**: wallet address din .env
   - **to**: aceeași wallet (transfer la sine, 0 ETH)
   - **value**: 0
   - **gas**: 50000
   - **data**: `0x` + `blockchain_hash` (32 bytes)
5. Semneaza cu cheie privat din .env
6. Trimite la netowkr Sepolia (via Infura RPC)
7. Primite TX hash, salvează în `blockchain_tx_hash`
8. Pe Etherscan, data tranzacției și inputul se pot verifica public

#### Verificare Publica

Oricine poate verifica:
1. Merge la https://sepolia.etherscan.io/tx/{blockchain_tx_hash}
2. Vede data-ora exactă a blocului
3. Vede "Input Data" care conține `blockchain_hash`
4. Deschide API endpoint /api/permits/{id}/verify-blockchain (fără autentificare) pentru a vedea hash-urile

### 6.4 Generare PDF Certificate

#### Procès

1. ClientAndroid cere `/api/permits/{id}/certificate?lang=ro`
2. Flask verifca cert este în stare Completat
3. Construiește un document ReportLab cu:
   - Pagină background: A4, fundal alb-gălbui
   - Decor: Border dublu teal+auriu, watermark diagonal „ISSUED"
   - Header: Emblemă, titlu, dept, HR, num certificat în cutie
   - Tabel info: 2 coloane, rânduri cu culori alternate, pill status color-coded
   - Bloc autoritate: Disclaimer, linie semnătură dashed, sigiliu dashed cu steluță
   - Blockchain section (dacă configurată): Cutie neagră cu verde checkmark, hash-uri monospace, QR code
   - Footer: Teal bar cu text alb, logo, diamant, dată generare
4. Unicode fonts: Caută DejaVuSans.ttf în fonts/, registează cu pdfmetrics
5. Renderizează Flowables în PDF buffer (io.BytesIO)
6. Returneaza PDF cu Content-Disposition: attachment; filename=permit_certificate_{id}.pdf
7. Android-ul imediat abre PDF în aplicația sistem

#### Limbă și Unicode

- Limbă selectată determină texte din `PDF_TRANSLATIONS` dict
- Fiecare limbă are traduceri pentru toți termenii (Certificat Nr., Solicitant, Descriere, etc.)
- Font DejaVuSans suportă română (ăîâșț), germană (äöü), polonez (łźż), turcă (çğışö), ucraineană (Kirilic)
- Fără font Unicode, PDF apare cu patrate negre

## 7. Fluxuri de Testare Esențiale

### 7.1 Test End-to-End Complet

1. **Setup**: Start Flask backend, schimbă API URL în RetrofitClient.java pentru LAN IP, sync gradle
2. **Înregistrare**: Create ciudadino nou (ex. user1@email.com)
3. **Limbă + Dark**: Mergeți Settings, selectați Român și Mod Intunecat ON
4. **Aplicare**: Mergeți pe Apply, selectați Construction Permit, încârcați 3-4 documente, căutați hartă (ex. București), pinați o locație, depuneți
5. **AI Analize**: Așteptați ~30 secunde, deschideți permitul, expandeți AI card, verificați analiza completă
6. **Inspector**: Logout, login ca inspector1
7. **Revizuire**: Mergeți Pending, deschideți permit, citiți AI, aprobați cu notă
8. **Blockchain**: În Flask terminal, verificați TX hash; pe Etherscan, caută TX, vede hash-ul în Input Data
9. **Cetățean**: Logout, login ciudadino original
10. **Plată**: Apasă "Plătește Acum", status → Completat
11. **Certificate**: Descarcă PDF, verifică design profesional, QR code, info bloc, blockchain section cu Etherscan
12. **Comentarii**: Deschide Comments, trimite un mesaj, revino inspector, răspunde, cetățean primește notificare
13. **Timeline**: Scrolleaza timeline, vede: Depus → Analizat AI → Revizuit → Blockchain Notarizat → Plătit → Certificat

### 7.2 Test Trash

1. Deschide un permit depus
2. Scrollează jos, apasă "Mutați în Trash"
3. Confirma
4. Mergeți Drawer > Trash
5. Vedeți permitele în trash cu timer 30 zile
6. Apasă "Restaura" pentru a-l readuce
7. Mergeți Dashboard, permitele nu mai este în trash

## 8. Rezultate și Validare

### 8.1 Testare Manuală

Testele manuale au demonstrat:

- ✅ Flux complet de cerere funcționează: depunere → revizuire → aprobare → plată → certificat
- ✅ AI Gemini analizează documente și oferă recomandări în limbile selectate
- ✅ Blockchain notarizare scrie pe Ethereum Sepolia și poate fi verificată public
- ✅ PDF certificate sunt generate cu design profesional și Unicode corect
- ✅ Timeline-ul arată toți pașii cu date/ore
- ✅ Căutarea și filtrarea lucrează în timp real
- ✅ Dark mode și limbile sunt persistente peste sign-outs
- ✅ Trash-ul șterge automat după 30 zile
- ✅ Comentariile sunt restricționate corect pe rol

### 8.2 Acoperire Funcționalități

| Caracteristică | Implementat | Testat |
|---|---|---|
| Înregistrare și Login | ✅ | ✅ |
| Aplicare Permit | ✅ | ✅ |
| Încărcare Documente | ✅ | ✅ |
| Hartă és Locație | ✅ | ✅ |
| AI Gemini Analizie | ✅ | ✅ |
| Revizuire Inspector | ✅ | ✅ |
| Blockchain Notarizare | ✅ | ✅ |
| PDF Certificate | ✅ | ✅ |
| Timeline Audit | ✅ | ✅ |
| Plată Simulare | ✅ | ✅ |
| Expirare Permit | ✅ | ✅ |
| Reînnoi/Reamplasă | ✅ | ✅ |
| Trash și Restaurare | ✅ | ✅ |
| Comentarii | ✅ | ✅ |
| Dark Mode | ✅ | ✅ |
| Multilingv Suport | ✅ | ✅ |
| Push Notificări | ✅ | ⚠️ (opțional FCM) |
| Programare Inspecție | ✅ | ✅ |
| Timp Estimat | ✅ | ⚠️ (necesare date istorice) |

## 9. Arhitectură Aplicație

### 9.1 Structura Folder Android

```
app/src/main/java/project/smartpermits/
├── api/
│   ├── ApiService.java
│   └── RetrofitClient.java
├── models/
│   ├── User.java
│   ├── Permit.java
│   ├── Document.java
│   ├── Comment.java
│   ├── Appointment.java
│   └── ... (rate data classes)
├── adapters/
│   ├── PermitAdapter.java
│   ├── PendingPermitAdapter.java
│   ├── ChatAdapter.java
│   └── TrashAdapter.java
├── Activities/
│   ├── LoginActivity.java
│   ├── CitizenDashboardActivity.java
│   ├── InspectorDashboardActivity.java
│   ├── ApplyPermitActivity.java
│   ├── PermitDetailActivity.java
│   ├── PermitReviewActivity.java
│   ├── PermitHistoryActivity.java
│   ├── ReviewHistoryActivity.java
│   ├── ChatActivity.java
│   ├── ScheduleAppointmentActivity.java
│   ├── AnalyticsActivity.java
│   ├── ProfileActivity.java
│   ├── EditProfileActivity.java
│   ├── ChangePasswordActivity.java
│   ├── SettingsActivity.java
│   ├── DocumentViewerActivity.java
│   ├── TrashActivity.java
│   └── MainActivity.java
├── Helpers/
│   ├── LocaleHelper.java (gestionare limbă)
│   ├── NotificationHelper.java (FCM)
│   ├── CurrencyHelper.java (format $)
│   └── PermitTypeHelper.java (documente necesare)
```

### 9.2 Structura Backend

```
smart_permits_api/
├── app.py (Flask app, API endpoints)
├── models.py (SQLAlchemy ORM)
├── blockchain.py (web3.py notarizare)
├── requirements.txt (pip dependencies)
├── .env (variabile de mediu: API keys, RPC, blockchain)
├── Dockerfile (containerizare)
├── fonts/ (DejaVuSans.ttf pentru PDF Unicode)
├── uploads/ (fișiere încărcate ciudadini)
└── instance/ (database smartpermits.db)
```

## 10. Instrucțiuni de Deployment

### 10.1 Dezvoltare Locală

1. **Backend**:
   ```
   cd smart_permits_api
   python -m venv venv
   venv\Scripts\activate
   pip install -r requirements.txt
   # Edit .env cu GEMINI_API_KEY (optional: ETH_PRIVATE_KEY, ETH_RPC_URL)
   python app.py
   ```
   Server pe http://localhost:5000

2. **Android**:
   - Deschide Android Studio
   - Sync Gradle
   - Edit RetrofitClient.java BASE_URL → http://10.0.2.2:5000/api/ (emulator) sau http://{PC_IP}:5000/api/ (device)
   - Run pe emulator sau device

### 10.2 Containerizare Production

```bash
cd smart_permits_api
docker build -t smartpermits-api .
docker run -p 5000:5000 \
  -e GEMINI_API_KEY=... \
  -e ETH_PRIVATE_KEY=... \
  -e ETH_RPC_URL=... \
  -e ETH_WALLET_ADDRESS=... \
  smartpermits-api
```

### 10.3 APK Compilare (Android)

```bash
./gradlew assembleDebug  # Debug APK
./gradlew assembleRelease  # Release APK (kell signing config)
```

APK în `app/build/outputs/apk/debug/` sau `release/`

## 11. Concluzii și Perspective Viitoare

### 11.1 Realizări Principale

SmartPermits aduce modernizare într-un domeniu tradițional:

- **Centralizare**: O singur platformă pentru toți tipurile de permise
- **Transparență**: Citizens și inspectori au complet vizibilitate
- **Automatizare**: AI analizează documente, blockchain notarizează
- **Acces**: De pe telefon, oricând, oriunde
- **Conformitate**: Audit trail complet, certificate profesionale

### 11.2 Extensii Viitoare

1. **Integrare Bancă**: Plată reală în loc de simulare
2. **SMS Notificări**: Pentru persoane fără push
3. **Acces API Public**: Integrări cu alte sisteme municipale
4. **Report Generator**: Rapoarte lunare pentru management
5. **OCR Documentare**: Extracție text automat din PDF-uri
6. **Video Cer**: Pentru permise speciale
7. **Sincronizare Offline**: Salvare locală permise

### 11.3 Impactul Așteptat

- **Reducere înregistrat**: Economia ~ 70% timp în procesare
- **Costuri scădute**: Fără hârtie, fără deplasări
- **Satisfacție cetățean**: Progres transparent, feedback rapid
- **Revenue clarity**: Municipalitate vede colectare taxe în timp real

## Bibliografie Minimală

1. **Flask Framework**: https://flask.palletsprojects.com/ — web framework Python pentru develop API REST
2. **SQLAlchemy ORM**: https://docs.sqlalchemy.org/ — Object-Relational Mapping pentru bază date
3. **JWT Authentication**: https://flask-jwt-extended.readthedocs.io/ — autentificare sigură cu tokeniu
4. **Retrofit Android**: https://square.github.io/retrofit/ — HTTP client pentru Android
5. **Google Gemini AI**: https://ai.google.dev/ — general inteligență artificială API
6. **Web3.py**: https://web3py.readthedocs.io/ — bibliotecă Python pentru Ethereum
7. **ReportLab PDF**: https://www.reportlab.com/ — generare de documente PDF
8. **Firebase Cloud Messaging**: https://firebase.google.com/docs/cloud-messaging — push notificări mobile
9. **Material Design**: https://material.io/develop/android — design sistem pentru Android
10. **Android Developers Guide**: https://developer.android.com/ — documentație oficială Android

---

*Document redactat conform cerințelor de teză de licență. Data redactării: iunie 2026.*

