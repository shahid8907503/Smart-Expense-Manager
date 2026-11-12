# Voice-Enabled Smart Expense Manager 🎙️💰

A modern, production-grade Android personal finance management application that empowers users to effortlessly track daily expenses and income using natural speech in **English**, **Telugu**, and **Hindi**, coupled with an intelligent **Node.js/Express/MongoDB** REST backend, **Room Database** offline sync, and **Text-to-Speech** financial queries.

---

## 🌟 Key Features

1. **Voice-Based Expense & Income Entry**
   - Live microphone recording with **real-time animated audio waveform**.
   - Multilingual NLP parsing for **English**, **Telugu**, and **Hindi** (both native script and romanized transliteration).
   - Automatic extraction of:
     - **Amount** (e.g. `250`, `₹1200`, `paanch hazaar`, `rendu vandhalu`)
     - **Transaction Type** (`Expense` vs `Income`)
     - **Category** (`Food`, `Travel`, `Shopping`, `Bills`, `Health`, `Education`, `Entertainment`, `Rent`, `Other`)
     - **Date** (`Today`, `Yesterday`, `Day before yesterday`)
     - **Clean Description** (e.g. `Lunch`, `Petrol`, `Shirt`, `Tea`, `Salary`)
   - **Confirmation Dialog**: Never auto-saves without user review; allows 1-tap editing before committing.

2. **Spoken Financial Q&A Assistant**
   - Natural voice querying:
     - *"How much did I spend on food this month?"*
     - *"What is my total spending today?"*
     - *"How much money did I spend last week?"*
     - *"Which category has the highest expenses?"*
     - *"What is my remaining food budget?"*
     - *"Compare my spending this month with last month."*
   - Real-time calculation from actual database records (no hallucinated AI guesses).
   - Responds visually with detailed financial cards and reads out the exact result using **Android TextToSpeech (TTS)**.

3. **Production Startup-Quality UI/UX (Jetpack Compose & Material Design 3)**
   - **Deep Forest Green** (`#0B3B24`), **Emerald** (`#10B981`), and **Warm Off-White** (`#F8FAF8`).
   - Available balance hero focus with tabular numeral formatting (`₹250.00`).
   - Custom canvas **interactive Donut/Pie chart** and **Income vs Expense dual bar chart**.
   - No generic template feel or tacky neon gradients—designed like a real consumer finance app.

4. **Offline-First Resilience & Background WorkManager Sync**
   - Full offline transaction entry and history browsing using **Room Database**.
   - Persistent `sync_queue` table tracking offline mutations.
   - Background **Android WorkManager** automatically pushes pending records when network connectivity returns.

5. **Multi-Period Budgeting (Weekly / Monthly / Annual) & Alerts**
   - Switch dynamically between Weekly, Monthly, and Annual budget views.
   - Set category spending limits with animated progress indicators (`80% USED` warning and `100% EXCEEDED` badges).

6. **Multi-Timeline Dashboard & Payment Modes**
   - Period comparison table (**Today**, **This Week**, **This Month**, **Year to Date**) showing Income, Expense, and Net Balance.
   - Tracks 5 payment methods (**Cash, UPI, Debit Card, Credit Card, Net Banking**) and **Internal Transfers** vs **External Payments**.

7. **Multi-Currency, Data Export & Utility Tools**
   - Instant currency conversion across **INR (₹), USD ($), EUR (€), GBP (£), AED (د.إ), and JPY (¥)**.
   - One-tap **CSV / Excel Export** via Android FileProvider share sheet.
   - Built-in **Loan & EMI Calculator**, **Debt & IOU Tracker**, and **Category / Payee / Tag Manager**.

8. **Bank-Grade Security & Privacy Architecture**
   - **Biometric Authentication** (`AndroidX BiometricPrompt`) + **4-digit Security PIN** fallback.
   - **Brute-Force Rate Limiting**: 30-second lockout after 3 failed PIN attempts.
   - **Anti-Screenshot & Privacy Shield** (`FLAG_SECURE`) and **15-second Background Auto-Lock**.
   - **Device Integrity Verifier**: Non-rooted, Hardware Keystore & TEE enclave verification + GDPR compliance disclosures.

---

## 🏗️ Architecture & Technology Stack

```
Voice-Enabled Smart Expense Manager
│
├── android/                             # Native Android Jetpack Compose Project
│   ├── app/src/main/
│   │   ├── AndroidManifest.xml          # Audio & Network permissions
│   │   ├── java/com/smart/expense/
│   │   │   ├── SmartExpenseApp.kt       # Application class (WorkManager init)
│   │   │   ├── MainActivity.kt          # Edge-to-edge Compose root
│   │   │   ├── data/
│   │   │   │   ├── local/               # Room Entities, DAOs, AppDatabase
│   │   │   │   ├── remote/              # Retrofit 2, OkHttp, DTOs, AuthInterceptor
│   │   │   │   ├── repository/          # ExpenseRepository, AuthRepository
│   │   │   │   └── sync/                # SyncWorker, SyncManager
│   │   │   ├── voice/
│   │   │   │   ├── SpeechRecognizerHelper.kt  # Android SpeechRecognizer with RMS dB
│   │   │   │   ├── TextToSpeechHelper.kt      # Android TTS engine
│   │   │   │   ├── NlpTransactionParser.kt    # Client-side Multilingual NLP
│   │   │   │   ├── VoiceQueryEngine.kt        # Spoken question DB calculator
│   │   │   │   └── KeywordCategorizer.kt      # 9 Category classifier
│   │   │   ├── java_core/
│   │   │   │   └── FinancialMathJavaService.java # Pure Java EMI & financial math engine
│   │   │   └── ui/
│   │   │       ├── theme/               # Material 3 Color, Type, Shape, Theme
│   │   │       ├── components/          # Waveform, SummaryCard, Charts, Rows
│   │   │       ├── screens/             # Splash, Auth, Home, Voice, Budget, Analytics
│   │   │       └── navigation/          # AppNavGraph, Screen destinations
│   │   └── res/                         # Strings, colors, network config
│
├── java-backend/                        # Java 21 + Spring Boot 3 Full Stack Server
│   ├── build.gradle.kts                 # Spring Web, Spring Data JPA, H2 SQL, Thymeleaf
│   ├── run_java_backend.bat             # One-click Spring Boot server launcher
│   └── src/main/
│       ├── java/com/smart/expense/
│       │   ├── SmartExpenseJavaApplication.java
│       │   ├── model/                   # @Entity JPA models (Transaction, Budget)
│       │   ├── repository/              # JpaRepository interfaces
│       │   └── controller/              # @RestController API & Thymeleaf MVC Controller
│       └── resources/
│           ├── application.properties   # Server port 5000 & H2 console config
│           └── templates/dashboard.html # Full Stack Web Portal (Thymeleaf + HTML/CSS)
│
└── backend/                             # Node.js + Express REST API
    ├── server.js                        # Express server entry point
    ├── .env                             # Environment configuration
    ├── package.json                     # Dependencies (JWT, bcryptjs, mongoose)
    └── src/
        ├── config/db.js                 # MongoDB connection with resilient fallback
        ├── middleware/auth.js           # JWT authentication middleware
        ├── models/                      # User, Transaction, Budget, Category
        ├── controllers/                 # Auth, Transactions, Budgets, Analytics, Voice
        ├── routes/                      # REST endpoints (/api/...)
        ├── nlp/                         # Multilingual NLP parser, categorizer, query engine
        └── seed.js                      # Realistic demonstration data seeder
```

---

## 🚀 Execution & Setup Instructions

### 1. Backend Setup (Node.js & MongoDB)

1. Open a terminal in the `backend/` directory:
   ```bash
   cd backend
   npm install
   ```

2. Configure `.env` (optional, defaults provided):
   ```ini
   PORT=5000
   MONGODB_URI=mongodb://127.0.0.1:27017/voice_expense_db
   JWT_SECRET=super_secret_voice_expense_jwt_token_key_2026
   ```

3. Seed demonstration data (optional):
   ```bash
   npm run seed
   ```

4. Start the server:
   ```bash
   npm start
   ```
   > **Note:** The backend features an automatic resilient fallback store. If MongoDB is not running locally, the server continues operating in in-memory demo mode so API endpoints and college demonstrations never crash!

---

### 2. Android App Setup (Android Studio)

1. Open **Android Studio** (Hedgehog / Iguana / Jellyfish or newer).
2. Click **File** -> **Open** and select the `android/` directory (`c:\Users\DELL\OneDrive\Desktop\internship\android`).
3. Let Gradle sync project dependencies automatically:
   - **Kotlin:** `1.9.23`
   - **Compile SDK:** `34` (Android 14)
   - **Min SDK:** `26` (Android 8.0 Oreo)
4. Choose an **Android Virtual Device (AVD)** or physical Android smartphone with developer mode enabled.
5. Click **Run ▶** (`Shift + F10`).

> **Network Note:** The Android app connects to `http://10.0.2.2:5000/` which is the standard Android Emulator loopback host for your PC's `localhost:5000`. Cleartext HTTP is pre-configured in `network_security_config.xml`.

---

## 🧪 Demonstration Test Cases

### Demonstration Workflow 1: Voice Expense Recording
1. Launch the app and tap **"One-Tap Demo Login"** (or log in with `demo@expense.com` / `Demo1234`).
2. On the Home Dashboard, tap the prominent **"Add Expense by Voice"** card.
3. Speak clearly into the microphone:
   - *"I spent 250 rupees on lunch today."*
   - Or in Telugu: *"Nenu food kosam 200 rupayalu kharchu chesanu."*
   - Or in Hindi: *"Maine khane par 200 rupaye kharch kiye."*
4. The live waveform responds to your voice, and the NLP engine extracts:
   - **Amount:** `₹250.00`
   - **Category:** `Food`
   - **Description:** `Lunch`
   - **Date:** `Today`
5. The **Confirmation Dialog** appears:
   - Tap **"Confirm"** to save immediately, or tap **"Edit details"** to adjust the amount or category.
6. The transaction is instantly saved to Room DB and synced with the backend. Available balance and budget progress bars update in real time!

---

### Demonstration Workflow 2: Voice Financial Queries & TTS
1. Tap the **"Ask Financial Questions"** card on the dashboard (or navigate to Insights).
2. Tap the microphone and ask:
   - *"How much did I spend on food this month?"*
   - *"What is my total spending today?"*
   - *"What is my remaining food budget?"*
   - *"Compare my spending this month with last month."*
3. The app executes an exact database query against actual transaction records, displays the calculated answer card, and reads the response aloud:
   > *"You spent ₹300 on Food this month."*
   > *"You have ₹4,700 remaining in your Food budget."*

---

## 🔒 Security & Privacy

- **Password Protection:** Passwords are never stored in plaintext; salted and hashed with `bcryptjs`.
- **JWT Authorization:** Private REST endpoints require a valid Bearer JWT header.
- **Data Isolation:** All database queries are strictly filtered by authenticated `userId`.
- **Microphone Permissions:** Microphone access is requested only when the user taps voice interaction controls and handles permission denials gracefully.

---

## 📱 Screenshots & Visual Flow

```
[ Splash Screen ]
       │
       ▼
[ Login / One-Tap Demo ] ───► [ Register with Preferred Language (EN/TE/HI) ]
       │
       ▼
[ Home Dashboard ] ──────────┬───► [ Add Expense by Voice (Waveform + NLP Confirmation) ]
  • Available Balance Hero   ├───► [ Voice Assistant Q&A Screen (Spoken Answers) ]
  • Income vs Expense Pill   ├───► [ Activity: Date-Grouped History + Search & Filters ]
  • Monthly Budget Progress  ├───► [ Budgets: 80% & 100% Threshold Warnings ]
  • Recent Activity List     ├───► [ Insights: Donut Chart + Dual Bar Monthly Trends ]
                             └───► [ Profile: Language Toggle & Room DB Sync Manager ]
```

