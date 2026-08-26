# AGENT RUNBOOK & OPERATIONAL MANUAL: SMSForwarder

> **Target Environment:** Android (minSdk 26, targetSdk 37, compileSdk 37)  
> **Primary Stack:** Kotlin 2.4.10, Jetpack Compose (M3), Clean Architecture + MVVM, Dagger Hilt 2.60.1, Room 2.8.4, Retrofit 2.11.0, WorkManager 2.11.2, OkHttp 5.5.0  
> **Package Namespace:** `online.thensoji.smsforwarder`  
> **Play Store ID:** `online.thensoji.smsforwarder`

---

## 1. Agent Persona & Role

You are an **Expert Android Software Architect & Principal Mobile Systems Engineer**. You specialize in:
- High-reliability, background-tolerant Android services, broadcast receivers, and WorkManager workflows.
- Modern Android Architecture (Clean Architecture, MVVM, Repository Pattern, Unidirectional Data Flow).
- Jetpack Compose with Material 3 styling, Motion Design System, and atomic, decoupled UI components.
- Hardware & View tactile haptics, spring physics animations, and interactive press states.
- Hardened dependency injection using Dagger Hilt and Hilt WorkManager integration.
- Offline-first SQLite persistence using Room and Kotlin Coroutines/Flow.
- R8 / ProGuard minification rules, Google Play Store compliance, and secure app lock mechanisms.

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
   - Parses GSM binary User Data Headers (UDH) via `SmsPduParser` for multi-part (concatenated) SMS reassembly.
   - Stages parts in `SmsPartDao`. When all parts arrive, stitches them into a coherent message; otherwise, an `AssembleFallbackWorker` flushes incomplete parts after 5 seconds.
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

5. **Presentation, Motion & Security Layer (`ui/screens/`, `ui/components/`, `ui/util/`, `util/`, `MessageViewModel`)**
   - **Startup Flow:** `Launch` ──► `SecurityConsentDialog (ConsentManager)` ──► `PinLockScreen (PinManager)` ──► `HomeScreen / Navigation`.
   - **Material 3 Motion & Transitions (`MainScreen.kt`):**
     - Global `NavHost` enter/exit/pop transitions: `slideIntoContainer(SlideDirection.Start/End)` + `fadeIn()` / `fadeOut()` with `FastOutSlowInEasing`.
     - PIN Unlock -> Home: Smooth `fadeIn` + `scaleIn(0.95f)`.
     - Change PIN Flow: Slide-up modal transition (`SlideDirection.Up` / `SlideDirection.Down`).
     - TopAppBar: `AnimatedContent` for seamless crossfading between screen titles.
   - **Tactile Haptics & Physical Spring Feedback (`HapticFeedbackHelper.kt`, `ClickModifiers.kt`):**
     - Multi-tiered haptics (`CLICK`, `TICK`, `SUCCESS`, `ERROR`) using hardware `Vibrator` (API 26+) and `View` fallbacks.
     - `Modifier.bounceClickable(...)` & `Modifier.pressScale(...)`: Spring-damped scale depression (0.86f-0.96f) on touch with ripple and haptic sensation.
     - `PinDotsIndicator.kt`: Animated horizontal shake effect on incorrect PIN verification.
   - **Google Play Prominent Disclosure (`SecurityConsentDialog`):** Non-dismissible upfront dialog explaining SMS data access, direct Telegram HTTPS transmission (no 3rd-party trackers), and strict ethical use terms with symmetrical, single-line action buttons. Exits via `finishAffinity()` if declined.
   - **PinLockScreen:** 4-digit PIN protection with salted SHA-256 hashed storage via `PinManager`.
   - **AllMessagesScreen:** Filter tabs (`All`, `Pending`, `Sent`, `Delayed`), compact number formatting (`1k`, `1Lc`, `1cr`), and real-time auto-scroll to index 0 on new incoming SMS.
   - **SettingsScreen:** Configuration for Bot Token, Chat ID, custom device tag, PIN management, live Telegram test, on-demand Privacy Disclosure review, and direct Google Play Store updates link (`https://play.google.com/store/apps/details?id=online.thensoji.smsforwarder`).

6. **Automated CI/CD & Semantic Versioning (`.github/workflows/release.yml`)**
   - **Version Name (`MAJOR.MINOR.PATCH`)**: Automated via `PaulHatch/semantic-version@v5.4.0` with `bump_each_commit: false` for batch merge releases:
     - `BREAKING CHANGE:` / `feat!:` / `#major` ➔ **MAJOR** (`1.0.0` ➔ `2.0.0`)
     - `feat:` / `#minor` ➔ **MINOR** (`1.0.0` ➔ `1.1.0`)
     - `fix:` / `#patch` / regular commits ➔ **PATCH** (`1.0.0` ➔ `1.0.1`)
   - **Version Code**: Monotonically increasing build integer via `${{ github.run_number }}`.
   - **Artifacts**: Signed release APK and optional Play Store Bundle (`.aab`) published to GitHub Releases and dispatched via email.

---

## 3. Available Verification Commands

All actions that modify code must be verified against Gradle build tools from the project root.

### Build & Compilation
```powershell
# Clean build cache and verify compilation
.\gradlew.bat clean assembleDebug

# Compile Kotlin debug sources rapidly
.\gradlew.bat compileDebugKotlin

# Build release bundle / APK (verifies ProGuard/R8 rules)
.\gradlew.bat assembleRelease
```

### Static Analysis & Testing
```powershell
# Run Android lint checks
.\gradlew.bat lintDebug

# Run local JVM unit tests
.\gradlew.bat testDebugUnitTest
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

### Jetpack Compose, Motion & Haptics
- **Atomic Components:** Keep individual composable files small, modular, and single-purpose under `ui/components/`.
- **Tactile Feedback:** Apply `Modifier.pressScale(interactionSource)` or `Modifier.bounceClickable(...)` and `HapticFeedbackHelper.performHaptic(...)` to clickable interactive components so the UI feels responsive and physical.
- **Motion System:** Ensure all screen transitions are defined with smooth slide/fade easing (`FastOutSlowInEasing`) on `NavHost` rather than abrupt cut popups.
- **State Hoisting:** Composable functions must not directly mutate ViewModels or Shared Preferences. Pass state down and events up.
- **Stable Keys:** In `LazyColumn`, always supply a unique key parameter (`items(filteredList, key = { it.id })`).
- **Small-Device Responsiveness:** Use `.horizontalScroll(rememberScrollState())` on chip rows and `FlowRow` on button rows. Ensure dialog action buttons fit single-line text without awkward wrapping.

---

## 5. Strict Constraints & Security Guardrails

1. ❌ **NEVER Hardcode Secrets or Keystores in Git:** Keystores, passwords, bot tokens, and API credentials must strictly remain in environment variables or private storage.
2. ❌ **NEVER Block the Main Thread in Receivers:** `SmsReceiver.onReceive` must remain lightweight using `goAsync()` or enqueuing to WorkManager.
3. ❌ **DO NOT Remove WorkManager Initializer Suppression:** `AndroidManifest.xml` explicitly suppresses default `WorkManagerInitializer` to enable Hilt on-demand worker instantiation.
4. ❌ **DO NOT Duplicate Sends:** Always check `if (messageObj.isSent) return Result.success()` in workers and use `ExistingWorkPolicy.KEEP` with unique work names.
5. ❌ **DO NOT Break PIN Security:** All primary app screens must be enclosed behind the PIN lock routing in `MainScreen.kt`.
6. ❌ **DO NOT Remove or Bypass Prominent Disclosure:** Upfront `SecurityConsentDialog` via `ConsentManager` must precede all runtime permission requests to strictly satisfy Google Play SMS/Telephony and Anti-Stalkerware policies.
7. ❌ **PRESERVE Smooth Motion & Touch Haptics:** Do not revert to default jump-cut navigation transitions or static unresponsive click handlers.
