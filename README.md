# 📲 SMS Forwarder for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.2.1-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/studio/releases/gradle-plugin)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Google Play](https://img.shields.io/badge/Google%20Play-SMS%20Forwarder-34A853.svg?logo=googleplay&logoColor=white)](https://play.google.com/store/apps/details?id=online.thensoji.smsforwarder)
[![Hilt](https://img.shields.io/badge/Dagger-Hilt%202.60.1-brightgreen.svg)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/AndroidX-Room%202.8.4-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.11.0-red.svg)](https://square.github.io/retrofit/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A robust, modern, and privacy-first Android application that automatically captures incoming SMS text messages and forwards them in real-time to your private **Telegram** chat or group.

Built using modern Android architecture principles (**Jetpack Compose**, **Material 3**, **Clean Architecture + MVVM**, **Dagger Hilt**, **Retrofit 2**, **Room SQLite Database**, and **WorkManager**), ensuring zero message loss, multi-part message reassembly, offline queue recovery, multi-device identification, tactile haptic feedback, fluid screen transitions, and 4-digit PIN security.

---

## 🌟 Key Features

- ⚡ **Trigger & Query Zero-Loss Ingestion:** Uses `SmsReceiver` as a real-time wake-up doorbell holding a CPU `WakeLock`, paired with automated Telephony System SMS Inbox (`content://sms/inbox`) querying and native `systemSmsId` deduplication. Even if the device was turned off, in deep Doze mode, or received multi-part carrier messages, not a single SMS is ever missed or duplicated.
- 🛡️ **Prominent Disclosure & Ethical Use Consent:** Google Play compliant upfront disclosure explaining data access (`RECEIVE_SMS`, `READ_SMS`, `READ_PHONE_STATE`), zero 3rd-party tracking, direct Telegram API transmission, and strict anti-stalkerware terms with balanced single-line action buttons.
- 🔒 **4-Digit App PIN Security:** Protects app access with a secure 4-digit PIN screen (SHA-256 hashed). Prompts for setup on first launch and unlocks seamlessly on subsequent opens.
- ✨ **Fluid Motion & Screen Transitions:** Material 3 shared-axis and slide-fade transitions across all screens (`NavHost`), modal slide-up for PIN changes, smooth scale-in on PIN unlock, and crossfading TopAppBar titles.
- 📳 **Tactile Haptic Feedback & Interactive Press Scale:** Physical vibration pulses (`CLICK`, `TICK`, `SUCCESS`, `ERROR`) paired with responsive spring-press visual depression (`Modifier.bounceClickable`, `Modifier.pressScale`) across keypad digits, buttons, filter chips, and action cards.
- ❌ **Animated Error Shake:** Lock screen dots indicator dynamically shakes horizontally on invalid PIN entries for clear visual feedback.
- 📱 **Multi-Device Identification:** Easily run the app on multiple phones forwarding to the same Telegram chat with auto-detected hardware names or custom tags (e.g., `📱 [Pixel 7 (Work)]`).
- 🧩 **Native Multi-Part Concatenated SMS Reassembly:** Reads complete, auto-concatenated long carrier SMS messages directly from Android's system telephony provider with native `_ID` assignment.
- 📑 **Large Message Auto-Chunking:** Automatically splits long SMS exceeding 3900 characters into numbered parts (`[Part 1/2]`, `[Part 2/2]`) to avoid Telegram API 4096-character payload limits.
- ⏳ **Forwarding Delay Tracking (> 1 min):** Automatically detects if a message was delayed due to airplane mode or network downtime and injects a delayed badge (e.g., `⏳ [Delayed by 15m]`).
- 📶 **Dual-SIM Slot Awareness:** Identifies and tags incoming messages by their active SIM slot (`SIM 1` vs `SIM 2`) with defensive `SecurityException` fallbacks.
- 🐕 **15-Minute Watchdog & Offline Auto-Drain:** WorkManager periodic watchdog sweeps and drains any stranded unsent messages from Room storage, ensuring zero message loss across reboots and offline periods.
- 🟢 **Always-On Keep-Alive Foreground Service:** A persistent `specialUse` foreground service (`ForwarderService`, `START_STICKY`) holds the app in the foreground bucket so aggressive OEM ROMs are far less likely to force-stop it. While alive it hosts a `SmsContentObserver` on `content://sms`, catching messages even if a broadcast is dropped. Toggle it on/off in Settings (enabled by default).
- 🚀 **Per-OEM Auto-start Deep-Links:** One-tap access to the manufacturer Auto-start / Background manager (MIUI, ColorOS, FuntouchOS, EMUI, One UI, OxygenOS, and more) via `AutoStartHelper`, with an App-Info fallback when no OEM screen exists. Backed by an Android 11+ `<queries>` block for package visibility.
- 🩺 **Opt-In Telegram Heartbeat Monitoring:** A hidden Developer screen (unlocked by tapping the version footer 7×) lets selected users configure a *separate* heartbeat bot that pings a Telegram chat every 15m / 30m / 1h / 2h / 5h. Each ping carries a rich diagnostic snapshot (device, app version, battery %, battery-optimization status, last app-open, last SMS received, last forward sent, pending & total counts). If pings stop arriving, you know that device force-stopped the app. A matching **Background Health** card surfaces the status on the Home screen.
- 🔋 **Battery Optimization Exemption:** In-app one-tap settings toggle to exempt the app from OEM battery optimizations for 100% reliable background execution.
- 📋 **All Messages Screen with Live Filters:** View all incoming messages categorized with filter chips (**All**, **Pending**, **Sent**, **Delayed**) with compact number formatting (`1k`, `1Lc`, `1cr`) and automatic top-scrolling on new incoming SMS.
- ⚙️ **In-App Bot Setup & Live Connection Test:** Configure and test your Telegram Bot Token & Chat ID directly within the app, plus re-examine Ethical Use & Privacy Disclosures anytime.
- 🌍 **Full Multi-Language Localization (16 Languages):** Comprehensive internationalization supporting English, Spanish (Español), French (Français), German (Deutsch), Portuguese (Português), Russian (Русский), Hindi (हिन्दी), Chinese Simplified (简体中文), Arabic (العربية with RTL support), Japanese (日本語), Italian (Italiano), Indonesian (Bahasa Indonesia), Turkish (Türkçe), Korean (한국어), and Vietnamese (Tiếng Việt).
- 🛍️ **Direct Google Play Store Updates:** Check and receive the latest app updates directly from the official [Google Play Store listing](https://play.google.com/store/apps/details?id=online.thensoji.smsforwarder).
- 🔄 **Boot Persistence & Hardened Restart:** Automatically resumes background listeners, restarts the keep-alive service, and enqueues watchdog workers when the device reboots. The `BootReceiver` is `directBootAware` and also listens to `USER_PRESENT` so it recovers on first unlock even when a ROM blocks `BOOT_COMPLETED`.
- 🤖 **Automated 4-Stage CI/CD & Play Store Deployment:** Visual GitHub Actions pipeline with Semantic Versioning, keystore signing, GitHub Releases, and direct AAB bundle deployment to Google Play **Closed Testing**.

---

## 📽️ Video Walkthrough
https://github.com/user-attachments/assets/76e29038-b1ce-46b8-8ee2-718703e1e0ba

---

## 🏗️ Architecture & Data Flow
```text
[Incoming Carrier SMS] 
       │
       ├────────────────────────────────────────┐
       ▼                                        ▼
[SmsReceiver (Doorbell)]             [Android Telephony Subsystem]
 (Acquires CPU WakeLock)              (Assembles & saves to content://sms/inbox)
       │                                        │
       ▼ (Waits 300ms)                          │
[SmsInboxSyncHelper] ◄──────────────────────────┘
       │  ▲ (Reads complete assembled SMS & deduplicates by native systemSmsId)
       │  └───── [ForwarderService → SmsContentObserver] (always-on foreground fallback)
       ▼
[Room SQLite (AppDatabase)] ◄─── Persists message (isSent = false)
       │
       ▼
[WorkManager (SendWorker)] ───► [SendTelegramMessageUseCase]
       │                                     │
       │                                     ▼
       │                            [TelegramApiService (Retrofit 2)]
       │                                     │
       ├────────── On Success ───────────────┘
       ▼
[Room Database] ───► Updates message (isSent = true, sentTimestamp, delayMillis)
       │
       ▼
[Jetpack Compose UI (MessageViewModel)] ───► Real-time StateFlow updates

Reliability & Monitoring (parallel):
[ForwarderService] ──► Keeps process alive (specialUse FGS, START_STICKY) + hosts SmsContentObserver
[WatchdogWorker]   ──► 15-min sweep to drain stranded unsent messages
[HeartbeatWorker]  ──► Opt-in periodic Telegram ping with device-health diagnostics
```

---

## 🧰 Tech Stack

| Layer | Technologies |
|---|---|
| **Language & Toolchain** | Kotlin 2.4.10, Java 17, KSP (Kotlin Symbol Processing) 2.3.10 |
| **Target SDKs** | Min SDK: `26` (Android 8.0) • Compile/Target SDK: `37` (Android 16+) |
| **Architecture** | Clean Architecture, MVVM, Repository Pattern, UDF (Unidirectional Data Flow) |
| **UI & Motion** | Jetpack Compose BOM 2026.08.00, Material 3, Navigation Compose with Motion System |
| **Haptics & Touch** | Hardware Vibrator API (API 26-37+), View Haptic Constants, Spring Press Modifiers |
| **Localization** | 16 Languages (`en`, `es`, `fr`, `de`, `pt`, `ru`, `hi`, `zh`, `ar`, `ja`, `it`, `in`/`id`, `tr`, `ko`, `vi`) with RTL |
| **Dependency Injection** | Dagger Hilt 2.60.1 (`hilt-android`, `hilt-work`, `hilt-navigation-compose`) |
| **Local Persistence** | AndroidX Room 2.8.4 (Coroutines KTX & KSP CodeGen) |
| **Background Scheduling** | AndroidX WorkManager 2.11.2 (`work-runtime-ktx`) |
| **Networking** | Retrofit 2.11.0, OkHttp 5.5.0, Gson Converter 2.11.0, Logging Interceptor |
| **Concurrency** | Kotlin Coroutines & Flow (`StateFlow`, `SharingStarted`) |
| **Security & Distribution** | SHA-256 PIN Hashing, Google Play Store (`online.thensoji.smsforwarder`) |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (or newer recommended).
- **JDK 17** configured as your Gradle JDK.
- Android device or emulator running **Android 8.0 (API 26)** or higher with active telephony capability.

### 1. Clone the Repository

```bash
git clone https://github.com/nilsinojiya1/SMSForwarder.git
cd SMSForwarder
```

### 2. Build the Project

```bash
# Windows (PowerShell / Command Prompt)
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

### 3. Install on Device

```bash
# Windows
.\gradlew.bat installDebug

# Linux / macOS
./gradlew installDebug
```

---

## 📱 Setup & Configuration

### 1. Create a Telegram Bot

1. Open Telegram and message [@BotFather](https://t.me/BotFather).
2. Send `/newbot` and follow the instructions to get your **Bot Token** (e.g., `123456789:ABCdefGhIJKlmNoPQRstuVWXyz`).
3. Message [@userinfobot](https://t.me/userinfobot) or [@RawDataBot](https://t.me/RawDataBot) to get your numeric **Chat ID** (e.g., `123456789` or group ID `-100123456789`).
4. **Important:** Open your newly created bot in Telegram and tap **Start** (or send `/start`) so it has permission to message you.

### 2. In-App Configuration

1. Launch **SMS Forwarder** on your device.
2. Review and accept the **Prominent Disclosure & Ethical Use Security Agreement** on first start.
3. Set your **4-digit PIN** when prompted.
4. Grant the required permissions on the Home overview:
   - `RECEIVE_SMS` & `READ_SMS` (To detect and read SMS messages)
   - `READ_PHONE_STATE` (To identify active SIM slot)
   - `POST_NOTIFICATIONS` (For Android 13+)
5. Navigate to **Settings**:
   - Set your **Device Tag** (or leave default hardware model).
   - Enter your **Telegram Bot Token** and **Chat ID**.
   - Tap **Save Settings** and test via **Test Telegram Connection**.
   - Keep the app updated by tapping **Check for Updates on Google Play**.

### 3. Battery Optimization & Background Reliability (Recommended)

To prevent OEM battery managers (Doze mode) from delaying or killing background forwarders:
- Go to Android **Settings > Apps > SMS Forwarder > Battery > Set to "Unrestricted"** (or use the in-app **Battery Optimization** toggle in Settings).
- Tap **Open Auto-start settings** in the app's Settings to allow the app to auto-start on Xiaomi/MIUI, Oppo/Realme (ColorOS), Vivo, Huawei, Samsung, OnePlus and similar ROMs.
- Keep the **Keep-alive Service** toggle enabled (default) so the persistent foreground service holds the app resident. On aggressive ROMs, avoid swiping the app away from Recents.
- Grant the **notification permission** (Android 13+) so the persistent keep-alive notification and status are visible.

### 4. Optional: Telegram Heartbeat Monitoring (Advanced)

To remotely confirm a device is still forwarding in the background:
1. In **Settings**, tap the version footer (e.g. `v1.0.0(1)`) **7 times** to unlock **Developer Options**.
2. Enter a **separate** heartbeat Bot Token (the chat defaults to your main Chat ID).
3. Enable heartbeats and pick an interval (15m / 30m / 1h / 2h / 5h).
4. Use **Send test heartbeat now** to verify. If scheduled pings stop arriving, that device force-stopped the app.

---

## 📂 Project Structure

```text
SMSforwarder/
├── .github/
│   └── workflows/
│       └── release.yml                # Automated CI/CD (Semantic Versioning & Signed APK/AAB)
├── app/
│   ├── src/main/
│   │   ├── java/online/thensoji/smsforwarder/
│   │   │   ├── data/                  # Room Entities (ForwardedMessage) & DAOs
│   │   │   │   ├── AppDatabase.kt     # Room database (Schema v6 migrations)
│   │   │   │   ├── ForwardedMessage.kt# Message entity with UNIQUE systemSmsId index
│   │   │   │   └── ForwardedMessageDao.kt
│   │   │   ├── di/                    # Dagger Hilt Dependency Injection Modules
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   ├── NetworkModule.kt
│   │   │   │   └── RepositoryModule.kt
│   │   │   ├── domain/                # Clean Architecture Domain Layer
│   │   │   │   ├── model/             # Domain Models (SendResult)
│   │   │   │   ├── repository/        # Domain Repository Interfaces (TelegramRepository)
│   │   │   │   └── usecase/           # Domain Use Cases (SendTelegramMessageUseCase)
│   │   │   ├── network/               # Retrofit 2 API Client, Data Sources & Interceptors
│   │   │   │   ├── api/               # TelegramApiService (Dynamic @Url endpoints)
│   │   │   │   ├── datasource/        # TelegramRemoteDataSource & Implementation
│   │   │   │   ├── interceptor/       # LoggingInterceptor (Debug payload logging)
│   │   │   │   └── model/             # Network DTOs & API Response Models
│   │   │   ├── repository/            # Repository Implementations
│   │   │   │   ├── MessageRepository.kt
│   │   │   │   └── TelegramRepositoryImpl.kt
│   │   │   ├── service/               # Broadcast Receivers & Services
│   │   │   │   ├── BootReceiver.kt    # Direct-boot-aware BOOT_COMPLETED / USER_PRESENT restart
│   │   │   │   ├── ForwarderService.kt# Always-on specialUse keep-alive foreground service
│   │   │   │   ├── SmsContentObserver.kt # ContentObserver on content://sms (hosted by service)
│   │   │   │   └── SmsReceiver.kt     # SMS_RECEIVED doorbell trigger & WakeLock holder
│   │   │   ├── ui/                    # Jetpack Compose Presentation Layer
│   │   │   │   ├── components/        # Atomic UI widgets & interactive modifiers
│   │   │   │   │   ├── ClickModifiers.kt       # bounceClickable & pressScale animations
│   │   │   │   │   ├── DeviceTagCard.kt        # Multi-device hardware tag badge
│   │   │   │   │   ├── EmptyMessagesView.kt    # Tab-specific empty illustration views
│   │   │   │   │   ├── HeartbeatHealthCard.kt  # Background-health status card (Home screen)
│   │   │   │   │   ├── LoadingMessagesView.kt  # Loading indicator view
│   │   │   │   │   ├── MessageCard.kt          # Message item card with delay & retry actions
│   │   │   │   │   ├── MessageFilterTabs.kt    # Interactive filter chips with haptics
│   │   │   │   │   ├── MessagesSummaryCard.kt  # Real-time message status metrics
│   │   │   │   │   ├── PendingBanner.kt        # Offline waiting banner with retry button
│   │   │   │   │   ├── PermissionsCard.kt      # SMS runtime permission status card
│   │   │   │   │   ├── PinDotsIndicator.kt     # 4-digit indicator with error shake
│   │   │   │   │   ├── PinKeypad.kt            # Tactile bounce keypad with tick haptics
│   │   │   │   │   ├── SecurityConsentDialog.kt # Symmetrical Google Play ethical disclosure
│   │   │   │   │   ├── SummaryItem.kt          # Compact metric counter item
│   │   │   │   │   ├── TelegramGuideCard.kt    # BotFather & Chat ID guide
│   │   │   │   │   └── TelegramStatusCard.kt   # Configuration status card
│   │   │   │   ├── screens/           # Full-screen Composables
│   │   │   │   │   ├── AllMessagesScreen.kt    # Message list with live filter tabs & refresh
│   │   │   │   │   ├── DeveloperScreen.kt      # Hidden heartbeat monitoring config (7-tap unlock)
│   │   │   │   │   ├── HomeScreen.kt           # Dashboard overview, health card & quick actions
│   │   │   │   │   ├── MainScreen.kt           # NavHost with Material 3 motion transitions
│   │   │   │   │   ├── PinLockScreen.kt        # 4-digit PIN setup, unlock & change
│   │   │   │   │   └── SettingsScreen.kt       # Bot token, keep-alive, auto-start, PIN & updates
│   │   │   │   ├── theme/             # Material 3 Color, Theme, Typography
│   │   │   │   ├── util/              # UI Utilities
│   │   │   │   │   └── HapticFeedbackHelper.kt # Hardware Vibrator & View haptic feedback
│   │   │   │   └── MessageViewModel.kt# MVVM StateFlow ViewModel
│   │   │   ├── util/                  # Shared Utilities
│   │   │   │   ├── AutoStartHelper.kt # Per-OEM auto-start / background manager deep-links
│   │   │   │   ├── ConsentManager.kt  # Ethical consent persistence
│   │   │   │   ├── HeartbeatManager.kt# Heartbeat prefs, scheduling & diagnostic ping builder
│   │   │   │   ├── KeepAliveManager.kt# Keep-alive service controller & enabled pref
│   │   │   │   ├── MessageFormatter.kt# Time, device tags & compact number formatting
│   │   │   │   ├── NotificationHelper.kt # Foreground & keep-alive notification builders
│   │   │   │   ├── PermissionUtils.kt # Runtime SMS permission validation
│   │   │   │   ├── PinManager.kt      # Salted SHA-256 PIN hashing & verification
│   │   │   │   └── SmsInboxSyncHelper.kt # Telephony inbox query & systemSmsId deduplication
│   │   │   ├── worker/                # Background WorkManager Workers
│   │   │   │   ├── HeartbeatWorker.kt # @HiltWorker periodic Telegram heartbeat ping
│   │   │   │   ├── SendWorker.kt      # @HiltWorker for idempotent Telegram forwarding
│   │   │   │   └── WatchdogWorker.kt  # 15-minute periodic watchdog to sweep stranded messages
│   │   │   ├── MainActivity.kt        # Single activity entry point hosting MainScreen
│   │   │   └── SMSForwarderApp.kt     # Application class with Hilt & Network Callback
│   │   ├── res/                       # Android App Resources & 16-Language Localizations
│   │   │   ├── values/strings.xml     # Base English strings
│   │   │   ├── values-es/strings.xml  # Spanish (Español)
│   │   │   ├── values-fr/strings.xml  # French (Français)
│   │   │   ├── values-de/strings.xml  # German (Deutsch)
│   │   │   ├── values-pt/strings.xml  # Portuguese (Português)
│   │   │   ├── values-ru/strings.xml  # Russian (Русский)
│   │   │   ├── values-hi/strings.xml  # Hindi (हिन्दी)
│   │   │   ├── values-zh/strings.xml  # Chinese Simplified (简体中文)
│   │   │   ├── values-ar/strings.xml  # Arabic (العربية - RTL)
│   │   │   ├── values-ja/strings.xml  # Japanese (日本語)
│   │   │   ├── values-it/strings.xml  # Italian (Italiano)
│   │   │   ├── values-in/strings.xml  # Indonesian (Legacy Locale)
│   │   │   ├── values-id/strings.xml  # Indonesian (Modern Locale)
│   │   │   ├── values-tr/strings.xml  # Turkish (Türkçe)
│   │   │   ├── values-ko/strings.xml  # Korean (한국어)
│   │   │   └── values-vi/strings.xml  # Vietnamese (Tiếng Việt)
│   │   └── AndroidManifest.xml        # Permissions (SMS, PHONE_STATE, FOREGROUND_SERVICE_SPECIAL_USE, POST_NOTIFICATIONS), keep-alive service & OEM queries
│   ├── build.gradle.kts               # Module build configuration & signing configs
│   └── proguard-rules.pro             # Optimized R8 / ProGuard minification rules
├── gradle/
│   └── libs.versions.toml             # Centralized Version Catalog
├── build.gradle.kts                   # Project-level build script
├── settings.gradle.kts                # Project settings & plugin repositories
├── agent.md                           # Operational Runbook & Architectural Blueprint
└── README.md
```

---

## 🔒 Security, Privacy & Google Play Compliance

- **Google Play Store Link:** Available on Google Play at [online.thensoji.smsforwarder](https://play.google.com/store/apps/details?id=online.thensoji.smsforwarder).
- **Prominent In-App Disclosure:** Explicit upfront disclosure of SMS permissions and data access prior to requesting permissions.
- **Strict Ethical Use & Anti-Stalkerware Policy:** Restricts app installation to devices owned by the user or operated with explicit consent.
- **Direct Communication:** Connects directly from your Android device to the official Telegram Bot API endpoint (`https://api.telegram.org`) over HTTPS.
- **No Third-Party Analytics:** Zero telemetry, external trackers, or intermediate ad networks.
- **Local PIN Storage:** App access is protected via salted SHA-256 hashed PIN stored in private SharedPreferences.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/nilsinojiya1/SMSForwarder/issues).

1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.
