# 📲 SMS Forwarder for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.2.1-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/studio/releases/gradle-plugin)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger-Hilt%202.60.1-brightgreen.svg)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/AndroidX-Room%202.8.4-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Retrofit](https://img.shields.io/badge/Retrofit-2.11.0-red.svg)](https://square.github.io/retrofit/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A robust, modern, and privacy-first Android application that automatically captures incoming SMS text messages and forwards them in real-time to your private **Telegram** chat or group.

Built using modern Android architecture principles (**Jetpack Compose**, **Material 3**, **Clean Architecture + MVVM**, **Dagger Hilt**, **Retrofit 2**, **Room SQLite Database**, and **WorkManager**), ensuring zero message loss, multi-part message reassembly, offline queue recovery, multi-device identification, and 4-digit PIN security.

---

## 🌟 Key Features

- ⚡ **Real-Time SMS Forwarding:** Intercepts incoming SMS broadcasts instantly and forwards sender details, timestamp, SIM slot, and body to Telegram.
- 🔒 **4-Digit App PIN Security:** Protects app access with a secure 4-digit PIN screen (SHA-256 hashed). Prompts for setup on first launch and unlocks seamlessly on subsequent opens.
- 📱 **Multi-Device Identification:** Easily run the app on multiple phones forwarding to the same Telegram chat with auto-detected hardware names or custom tags (e.g., `📱 [Pixel 7 (Work)]`).
- 🧩 **Multi-Part Concatenated SMS Reassembly:** Binary UDH (User Data Header) parser stages and reassembles fragmented long carrier SMS messages into a single complete Telegram notification.
- ⏳ **Forwarding Delay Tracking (> 1 min):** Automatically detects if a message was delayed due to airplane mode or network downtime and injects a delayed badge (e.g., `⏳ [Delayed by 15m]`).
- 📶 **Dual-SIM Slot Awareness:** Identifies and tags incoming messages by their active SIM slot (`SIM 1` vs `SIM 2`).
- 🌐 **Offline Resilience & Auto-Drain:** When offline, messages queue in Room. WorkManager and real-time network callbacks automatically send pending messages once internet returns—with strict single-send idempotency.
- 📋 **All Messages Screen with Live Filters:** View all incoming messages categorized with filter chips (**All**, **Pending**, **Sent**, **Delayed**) with compact number formatting (`1k`, `1Lc`, `1cr`) and automatic top-scrolling on new incoming SMS.
- ⚙️ **In-App Bot Setup & Live Connection Test:** Configure and test your Telegram Bot Token & Chat ID directly within the app.
- 🔄 **Boot Persistence:** Automatically resumes background listeners when the Android device reboots (`RECEIVE_BOOT_COMPLETED`).
- 🎨 **Modular Material 3 Compose UI:** Modern, responsive UI with smooth loading spinners, edge-to-edge support, and small-device responsiveness.

---

## 📽️ Video Walkthrough
https://github.com/user-attachments/assets/76e29038-b1ce-46b8-8ee2-718703e1e0ba

## 🏗️ Architecture & Data Flow
```text
[Incoming SMS Broadcast] 
       │
       ▼
[SmsReceiver (BroadcastReceiver)]
       │  (Extracts SIM metadata & stages multi-part PDUs in SmsPartDao)
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
```

---

## 🧰 Tech Stack

| Layer | Technologies |
|---|---|
| **Language & Toolchain** | Kotlin 2.4.10, Java 17, KSP (Kotlin Symbol Processing) 2.3.10 |
| **Target SDKs** | Min SDK: `26` (Android 8.0) • Compile/Target SDK: `37` (Android 16+) |
| **Architecture** | Clean Architecture, MVVM, Repository Pattern, UDF (Unidirectional Data Flow) |
| **UI Framework** | Jetpack Compose BOM 2026.08.00, Material 3, Navigation Compose |
| **Dependency Injection** | Dagger Hilt 2.60.1 (`hilt-android`, `hilt-work`, `hilt-navigation-compose`) |
| **Local Persistence** | AndroidX Room 2.8.4 (Coroutines KTX & KSP CodeGen) |
| **Background Scheduling** | AndroidX WorkManager 2.11.2 (`work-runtime-ktx`) |
| **Networking** | Retrofit 2.11.0, OkHttp 5.5.0, Gson Converter 2.11.0, Logging Interceptor |
| **Concurrency** | Kotlin Coroutines & Flow (`StateFlow`, `SharingStarted`) |
| **Security** | SHA-256 PIN Hashing, Private SharedPreferences |

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
2. Set your **4-digit PIN** when prompted.
3. Grant the required permissions on the Home overview:
   - `RECEIVE_SMS` & `READ_SMS` (To detect and read SMS messages)
   - `READ_PHONE_STATE` (To identify active SIM slot)
   - `POST_NOTIFICATIONS` (For Android 13+)
4. Navigate to **Settings**:
   - Set your **Device Tag** (or leave default hardware model).
   - Enter your **Telegram Bot Token** and **Chat ID**.
   - Tap **Save Settings** and test via **Test Telegram Connection**.

### 3. Battery Optimization (Recommended)

To prevent OEM battery managers (Doze mode) from delaying background forwarders:
- Go to Android **Settings > Apps > SMS Forwarder > Battery > Set to "Unrestricted"**.

---

## 📂 Project Structure

```text
SMSforwarder/
├── app/
│   ├── src/main/java/online/thensoji/smsforwarder/
│   │   ├── data/                      # Room Entities (ForwardedMessage, SmsPart) & DAOs
│   │   ├── di/                        # Hilt Modules (DatabaseModule, NetworkModule, RepositoryModule)
│   │   ├── domain/                    # Clean Architecture Domain Layer (UseCases, Models, Repositories)
│   │   ├── network/                   # Retrofit 2 API Service, DTO Models, Logging Interceptor
│   │   ├── repository/                # Repository Implementations (MessageRepository, TelegramRepositoryImpl)
│   │   ├── ui/                        # Jetpack Compose UI
│   │   │   ├── components/            # Reusable UI widgets (MessageCard, PinKeypad, FilterTabs, etc.)
│   │   │   ├── screens/               # MainScreen, HomeScreen, AllMessagesScreen, SettingsScreen, PinLockScreen
│   │   │   ├── theme/                 # Material 3 Color, Theme, Typography
│   │   │   └── MessageViewModel.kt    # MVVM StateFlow ViewModel
│   │   ├── util/                      # MessageFormatter, PermissionUtils, PinManager, SmsPduParser
│   │   ├── AssembleFallbackWorker.kt  # Fallback worker for incomplete multi-part SMS
│   │   ├── BootReceiver.kt            # BOOT_COMPLETED receiver
│   │   ├── MainActivity.kt            # Clean Activity Entry Point hosting MainScreen
│   │   ├── SendWorker.kt              # Hilt WorkManager worker for dispatching messages
│   │   ├── SMSForwarderApp.kt         # Application class with HiltWorkerFactory & Network Callback
│   │   └── SmsReceiver.kt             # BroadcastReceiver for Telephony.SMS_RECEIVED
│   └── proguard-rules.pro             # Optimized R8 / ProGuard rules for release builds
├── gradle/libs.versions.toml          # Centralized Version Catalog
└── README.md
```

---

## 🔒 Security & Privacy

- **Direct Communication:** Connects directly from your Android device to the official Telegram Bot API endpoint (`https://api.telegram.org`) over HTTPS.
- **No Third-Party Analytics:** Zero telemetry, external trackers, or intermediate servers.
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
