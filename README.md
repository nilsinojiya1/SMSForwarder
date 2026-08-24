# 📲 SMS Forwarder for Android

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android Gradle Plugin](https://img.shields.io/badge/AGP-9.2.1-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com/studio/releases/gradle-plugin)
[![Jetpack Compose](https://img.shields.io/badge/Compose-Material%203-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger-Hilt%202.60.1-brightgreen.svg)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/AndroidX-Room%202.8.4-orange.svg)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A robust, modern, and reliable Android application that automatically captures incoming SMS text messages and forwards them in real-time to your private **Telegram** chat or group.

Built with modern Android standards (**Jetpack Compose**, **Material 3**, **Dagger Hilt**, **Room Database**, **WorkManager**, and **Coroutines**), ensuring zero message loss even across network outages or device restarts.

---

## 🌟 Key Features

- ⚡ **Real-Time SMS Forwarding:** Intercepts incoming SMS broadcasts instantly and forwards sender details, timestamp, and message body to Telegram.
- 📶 **Dual-SIM Slot Awareness:** Automatically detects and displays which SIM slot received the message (`SIM 1` vs `SIM 2`).
- 🧩 **Multipart SMS Reassembly:** Intelligently stitches multi-part long SMS messages into a single coherent notification before sending.
- 🛡️ **Offline Queue & Guaranteed Delivery:** Messages are first persisted locally in a **Room SQLite Database**. **WorkManager** orchestrates delivery with automatic retries on network failure.
- 📋 **Queued Messages Monitor:** View unsent or failed messages in a dedicated UI screen, with single-tap manual retry or mark-as-sent controls.
- ⚙️ **In-App Bot Configuration & Live Test:** Setup your Telegram Bot Token & Chat ID directly within the app with built-in instant connection testing.
- 🔄 **Boot Persistence:** Automatically resumes background listeners when the Android device reboots (`RECEIVE_BOOT_COMPLETED`).
- 🎨 **Material 3 & Edge-to-Edge:** Designed using Jetpack Compose with dynamic theming and modern Android UI guidelines.

---

## 🏗️ Architecture & Data Flow

```text
[Incoming SMS] 
       │
       ▼
[SmsReceiver (BroadcastReceiver)]
       │  (Groups multipart SMS & extracts SIM metadata)
       ▼
[Room Database (AppDatabase)] ◄─── Persists message (isSent = false)
       │
       ▼
[WorkManager (SendWorker)] ───► [Telegram Bot API (OkHttp)]
       │                                     │
       ├────────── On Success ───────────────┘
       ▼
[Room Database] ───► Updated to (isSent = true)
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
| **UI Framework** | Jetpack Compose BOM 2026.08.00, Material 3, Navigation Compose |
| **Dependency Injection** | Dagger Hilt 2.60.1 (`hilt-android`, `hilt-work`, `hilt-navigation-compose`) |
| **Local Persistence** | AndroidX Room 2.8.4 (Coroutines KTX & KSP CodeGen) |
| **Background Scheduling** | AndroidX WorkManager 2.11.2 (`work-runtime-ktx`) |
| **Networking** | Square OkHttp 5.5.0 |
| **Concurrency** | Kotlin Coroutines & Flow (`StateFlow`) |

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug (or newer recommended) / IntelliJ IDEA with Android support.
- **JDK 17** configured as your Gradle JDK.
- Android device or emulator running **Android 8.0 (API 26)** or higher with an active SIM/telephony capability.

### 1. Clone the Repository

```bash
git clone https://github.com/nilsinojiya1/SMSForwarder.git
cd SMSForwarder
```

### 2. Open & Build in Android Studio

1. Open Android Studio and select **File > Open**, navigating to the cloned directory.
2. Allow Gradle sync to complete and download all dependencies.
3. Build the project using Gradle:

```bash
# Windows (PowerShell / Command Prompt)
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

### 3. Install on a Test Device

Connect your Android phone via USB debugging and run:

```bash
.\gradlew.bat installDebug
```

---

## 📱 Configuration & Usage

### 1. Setting up Telegram Bot Credentials

1. Open Telegram and search for [@BotFather](https://t.me/BotFather).
2. Send `/newbot` and follow the prompts to create your bot and obtain your **Bot Token** (e.g., `123456789:ABCdefGhIJKlmNoPQRstuVWXyz`).
3. Search for [@userinfobot](https://t.me/userinfobot) or [@RawDataBot](https://t.me/RawDataBot) to find your numeric **Chat ID** (e.g., `123456789` or group ID `-100123456789`).
4. **Important:** Open your newly created bot in Telegram and tap **Start** (or send `/start`) so the bot is permitted to message you.

### 2. In-App Setup

1. Launch **SMS Forwarder** on your device.
2. Grant the requested runtime permissions:
   - `RECEIVE_SMS` & `READ_SMS` (To detect and read SMS messages)
   - `READ_PHONE_STATE` (To identify active SIM slots)
   - `POST_NOTIFICATIONS` (For Android 13+)
3. Navigate to **Settings** (Gear icon at the top right).
4. Enter your **Bot Token** and **Chat ID**, then tap **Save Credentials**.
5. Tap **Test Telegram Connection** to verify that your device can reach the Telegram API and dispatch a test alert.

### 3. Battery Optimization (Recommended)

To prevent OEM battery optimizations (Doze mode) from suspending background forwarders:
- Go to Android **Settings > Apps > SMS Forwarder > Battery > Set to "Unrestricted"**.

---

## 📂 Project Structure

```text
SMSforwarder/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/online/thensoji/smsforwarder/
│   │   │   │   ├── data/                      # Room Entities, DAOs, and Database definition
│   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   ├── ForwardedMessage.kt
│   │   │   │   │   └── ForwardedMessageDao.kt
│   │   │   │   ├── di/                        # Hilt Dependency Injection Modules
│   │   │   │   │   ├── DatabaseModule.kt
│   │   │   │   │   └── NetworkModule.kt
│   │   │   │   ├── network/                   # Telegram HTTP network client
│   │   │   │   │   └── TelegramSender.kt
│   │   │   │   ├── repository/                # Clean Architecture Repository pattern
│   │   │   │   │   └── MessageRepository.kt
│   │   │   │   ├── ui/                        # Jetpack Compose UI & ViewModels
│   │   │   │   │   ├── theme/                 # Material 3 Color, Theme, & Typography
│   │   │   │   │   └── MessageViewModel.kt
│   │   │   │   ├── BootReceiver.kt            # Handles BOOT_COMPLETED intents
│   │   │   │   ├── MainActivity.kt            # Single Activity hosting Compose NavHost
│   │   │   │   ├── SendWorker.kt              # Hilt-injected Coroutine WorkManager Worker
│   │   │   │   ├── SMSForwarderApp.kt         # Custom Application class with HiltWorkerFactory
│   │   │   │   └── SmsReceiver.kt             # BroadcastReceiver for Telephony.SMS_RECEIVED
│   │   │   ├── res/                           # Android Drawables, Mipmaps, and Strings
│   │   │   └── AndroidManifest.xml            # Permissions, Services, Receivers configuration
│   │   └── androidTest/                       # Instrumented Android test suite
│   ├── build.gradle.kts                       # App module build configuration
│   └── proguard-rules.pro                     # R8 / ProGuard shrinkage rules
├── gradle/
│   └── libs.versions.toml                     # Centralized Gradle Version Catalog
├── build.gradle.kts                           # Root build configuration
└── settings.gradle.kts                        # Plugin management and repository settings
```

---

## 🔒 Security & Privacy Notice

- **Direct Communication:** This app connects **directly** from your Android device to the official Telegram Bot API endpoint (`api.telegram.org`) over HTTPS.
- **No Third-Party Intermediaries:** No telemetry, analytics, or third-party servers receive your SMS data or bot credentials.
- **Credential Storage:** Bot tokens and chat IDs are stored locally in Android private `SharedPreferences`.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

1. Fork the project.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) - see the LICENSE file for details.

