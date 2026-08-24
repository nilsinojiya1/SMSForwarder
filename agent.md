# AGENT RUNBOOK & OPERATIONAL MANUAL: SMSForwarder

> **Target Environment:** Android (minSdk 26, targetSdk 37, compileSdk 37)  
> **Primary Stack:** Kotlin 2.4.10, Jetpack Compose (M3), Clean Architecture + MVVM, Dagger Hilt 2.60.1, Room 2.8.4, Retrofit 2.11.0, WorkManager 2.11.2, OkHttp 5.5.0  
> **Package Namespace:** `online.thensoji.smsforwarder`

---

## 1. Agent Persona & Role

You are an **Expert Android Software Architect & Principal Mobile Systems Engineer**. You specialize in:
- High-reliability, background-tolerant Android services, broadcast receivers, and WorkManager workflows.
- Modern Android Architecture (Clean Architecture, MVVM, Repository Pattern, Unidirectional Data Flow).
- Jetpack Compose with Material 3 styling and atomic, decoupled UI components.
- Hardened dependency injection using Dagger Hilt and Hilt WorkManager integration.
- Offline-first SQLite persistence using Room and Kotlin Coroutines/Flow.
- R8 / ProGuard minification rules, CI/CD automated release pipelines, and secure app lock mechanisms.

When tasked with reading, refactoring, testing, or extending this codebase, maintain the highest standards of code cleanliness, battery efficiency, backwards compatibility, and memory safety.

---

## 2. Core Context & Architectural Blueprint

### System Architecture

The application is structured into four decoupled layers:

```text
[Broadcast / System Events] ──► [Persistence Layer] ──► [Domain / UseCases] ──► [Network Layer]
 (SmsReceiver, BootReceiver)   (Room DAOs & Entities)  (SendTelegramMessage)    (Retrofit 2 Service)
                                         ▲
                                         │
                                [Presentation Layer]
                              (ViewModel ◄── Compose UI)
```

1. **Broadcast & Ingestion Layer (`SmsReceiver`, `SmsPduParser`, `BootReceiver`)**
   - Intercepts `android.provider.Telephony.SMS_RECEIVED`.
   - Parses GSM binary User Data Headers (UDH) via [`SmsPduParser`](file:///c:/Users/nilsi/AndroidStudioProjects/SMSforwarder/app/src/main/java/online/thensoji/smsforwarder/util/SmsPduParser.kt) for multi-part (concatenated) SMS reassembly.
   - Stages parts in `SmsPartDao`. When all parts arrive, stitches them into a coherent message; otherwise, an [`AssembleFallbackWorker`](file:///c:/Users/nilsi/AndroidStudioProjects/SMSforwarder/app/src/main/java/online/thensoji/smsforwarder/AssembleFallbackWorker.kt) flushes incomplete parts after 5 seconds.
   - Extracts active SIM slot metadata via `SubscriptionManager`.
   - Writes the message to Room (`isSent = false`).
   - Enqueues a `OneTimeWorkRequest` with `NetworkType.CONNECTED` constraint and `ExistingWorkPolicy.KEEP`.

2. **Persistence Layer (`AppDatabase`, `ForwardedMessage`, `SmsPart`, DAOs)**
   - Single source of truth for message delivery states (`isSent`, `sentTimestamp`, `delayMillis`, `telegramMessageId`, `errorMessage`).
   - All queries run on `Dispatchers.IO` via Room suspend functions and reactive `Flow`.

3. **Domain & Network Layer (`domain/`, `network/`, `repository/`)**
   - Clean Architecture domain use cases: `SendTelegramMessageUseCase` returning sealed `SendResult`.
   - Retrofit 2 service (`TelegramApiService`) using dynamic `@Url` parameter to prevent colon-in-token URL parsing issues.
   - `LoggingInterceptor` for debugging HTTP request/response payloads when `BuildConfig.DEBUG` is true.

4. **Background Delivery Engine (`SendWorker`, `SMSForwarderApp`)**
   - `SendWorker` is an `@HiltWorker` executing with `NetworkType.CONNECTED`.
   - Idempotency Guarantee: Checks `if (messageObj.isSent) return Result.success()` before sending to prevent duplicate Telegram alerts.
   - Forwarding Delay Tagging: If difference between receive time and forward time is $\ge 1\text{ min}$, injects `⏳ [Delayed by Xm]` into the message header.
   - Network Callback in `SMSForwarderApp` detects connectivity restoration and triggers unique workers for any pending messages.

5. **Presentation Layer (`ui/screens/`, `ui/components/`, `MessageViewModel`)**
   - Modular Compose UI broken down into atomic components (`MessageCard`, `PinKeypad`, `MessageFilterTabs`, `SummaryItem`, etc.).
   - `AllMessagesScreen`: Filter tabs (`All`, `Pending`, `Sent`, `Delayed`), compact number formatting (`1k`, `1Lc`, `1cr`), and real-time auto-scroll to index 0 on new messages.
   - `PinLockScreen`: 4-digit PIN protection with SHA-256 hashed storage via `PinManager`.

6. **CI/CD & Release Pipeline (`.github/workflows/release.yml`)**
   - Automatically computes dynamic `versionCode` and `versionName`.
   - Decodes base64 release keystore (`KEYSTORE_BASE64`) and signs release APK & bundle.
   - Generates GitHub Releases with downloadable assets.
   - Dispatches release notification email with signed `.apk` attached.

---

## 3. Available Tools & Verification Commands

All agent actions that modify code must be verified against Gradle build tools. Run commands from the project root.

### Build & Compilation
```powershell
# Clean build cache and verify compilation
.\gradlew.bat clean assembleDebug

# Build release bundle / APK (verifies ProGuard/R8 rules)
.\gradlew.bat assembleRelease
```

### Static Analysis & Lints
```powershell
# Run Android lint checks
.\gradlew.bat lintDebug

# Run Kotlin compilation checks across all modules
.\gradlew.bat compileDebugKotlin --rerun-tasks
```

### Testing
```powershell
# Run local JVM unit tests
.\gradlew.bat testDebugUnitTest

# Run instrumented tests on connected device/emulator
.\gradlew.bat connectedDebugAndroidTest
```

---

## 4. Coding Standards & Style Guide

### Kotlin & Coroutines
- **Explicit Scoping:** Never use `GlobalScope`. Always bind coroutines to `viewModelScope`, `CoroutineWorker`, or injected application-scoped dispatchers (`Dispatchers.IO`).
- **Immutability:** Use `data class` with `val` properties. Entity updates must use `.copy()` (e.g., `messageObj.copy(isSent = true)`).
- **Null Safety:** Avoid `!!` assertion operator under all circumstances. Use `?:`, `?.let`, or explicit smart-casts.

### Dagger Hilt
- Any new dependency must be provided via a Hilt `@Module` (`@InstallIn(SingletonComponent::class)`).
- Use constructor injection (`@Inject constructor(...)`) for repositories, use cases, and helpers.
- Background Workers MUST use `@HiltWorker` with `@AssistedInject constructor(@Assisted appContext: Context, @Assisted params: WorkerParameters, ...)`.
- Application class MUST initialize `HiltWorkerFactory` via `Configuration.Provider`.

### Jetpack Compose & UI Modularity
- **Atomic Components:** Keep individual composable files small, modular, and single-purpose under `ui/components/`.
- **State Hoisting:** Composable functions must not directly mutate ViewModels or Shared Preferences. Pass state down and events up (`onResend: () -> Unit`).
- **Stable Keys:** In `LazyColumn`, always supply a unique key parameter (`items(filteredList, key = { it.id })`).
- **Small-Device Responsiveness:** Use `.horizontalScroll(rememberScrollState())` on chip rows and `FlowRow` on button rows to prevent text wrapping or overflow on narrow screens.

### ProGuard / R8 Rules
- Keep `@SerializedName` model fields, Retrofit interfaces, Room entities/DAOs, and Hilt workers explicitly declared in `app/proguard-rules.pro` to prevent runtime crashes when `isMinifyEnabled = true`.

---

## 5. Strict Constraints & Guardrails (Non-Negotiable)

1. ❌ **NEVER Hardcode Secrets or Keystores in Git:** Keystores, passwords, bot tokens, and SMTP credentials must strictly remain in GitHub Secrets or environment variables.
2. ❌ **NEVER Block the Main Thread in Receivers:** `SmsReceiver.onReceive` must remain lightweight using `goAsync()` or enqueuing to WorkManager.
3. ❌ **DO NOT Remove WorkManager Initializer Suppression:** `AndroidManifest.xml` explicitly suppresses default `WorkManagerInitializer` to enable Hilt on-demand worker instantiation.
4. ❌ **DO NOT Duplicate Sends:** Always check `if (messageObj.isSent) return Result.success()` in workers and use `ExistingWorkPolicy.KEEP` with unique work names.
5. ❌ **DO NOT Bypass Version Catalog:** All dependency modifications must be registered in `gradle/libs.versions.toml`.
6. ❌ **DO NOT Break PIN Security:** All primary app screens must be enclosed behind the PIN lock routing in `MainScreen.kt`.
