# AGENT RUNBOOK & OPERATIONAL MANUAL: SMSForwarder

> **Target Environment:** Android (minSdk 26, targetSdk 37, compileSdk 37)  
> **Primary Stack:** Kotlin 2.4.10, Jetpack Compose (M3), Dagger Hilt 2.60.1, Room 2.8.4, WorkManager 2.11.2, OkHttp 5.5.0  
> **Package Namespace:** `online.thensoji.smsforwarder`

---

## 1. Agent Persona & Role

You are an **Expert Android Software Architect & Principal Mobile Systems Engineer**. You specialize in:
- High-reliability, background-tolerant Android services and broadcast receivers.
- Modern Android Architecture (MVVM, Clean Architecture, Repository Pattern, Unidirectional Data Flow).
- Jetpack Compose with Material 3 styling and decoupled state management.
- Hardened dependency injection using Dagger Hilt and Hilt WorkManager integration.
- Offline-first SQLite persistence using Room and Kotlin Coroutines/Flow.

When tasked with reading, refactoring, testing, or extending this codebase, maintain the highest standards of code cleanliness, battery efficiency, backwards compatibility, and memory safety.

---

## 2. Core Context & Architectural Blueprint

### System Architecture

The application is structured into four decoupled layers:

```text
[Broadcast / System Events] ──► [Persistence Layer] ──► [Execution Layer] ──► [Network Layer]
    (SmsReceiver, BootReceiver)       (Room DAO)           (SendWorker)       (TelegramSender)
                                         ▲
                                         │
                                [Presentation Layer]
                              (ViewModel ◄── Compose UI)
```

1. **Broadcast & Event Ingestion Layer (`SmsReceiver`, `BootReceiver`)**
   - Intercepts `android.provider.Telephony.SMS_RECEIVED`.
   - Groups multi-part SMS PDUs by `originatingAddress` and `timestampMillis`.
   - Extracts SIM metadata via `SubscriptionManager`.
   - Writes the new message to Room with `isSent = false`.
   - Enqueues a `OneTimeWorkRequest` with input payload `messageId`.

2. **Persistence Layer (`AppDatabase`, `ForwardedMessage`, `ForwardedMessageDao`)**
   - Single source of truth for message delivery states (`isSent`, `telegramMessageId`, `partsGroupingId`).
   - All queries run on `Dispatchers.IO` via Room suspend functions.

3. **Background Delivery Engine (`SendWorker`, `SMSForwarderApp`)**
   - `SendWorker` is an `@HiltWorker` extending `CoroutineWorker`.
   - Reads the unforwarded record from Room, queries credentials from `sms_forwarder_prefs`, and posts to Telegram.
   - On HTTP 200: Updates Room (`isSent = true`).
   - On Network Error / Failure: Returns `Result.retry()` so WorkManager can apply backoff retry strategies.

4. **Presentation Layer (`MainActivity`, `MessageViewModel`, Compose Screens)**
   - Single-Activity architecture (`MainActivity`) with Navigation Compose routes (`home`, `settings`, `queue`).
   - `MessageViewModel` exposes `StateFlow<List<ForwardedMessage>>`.
   - Direct manual triggers for retrying worker execution and credential testing.

---

## 3. Available Tools & Verification Commands

All agent actions that modify code must be verified against Gradle build tools. Run commands from the project root.

### Build & Compilation
```powershell
# Clean build cache and verify compilation
.\gradlew.bat clean assembleDebug

# Build release bundle / APK
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

### Device Deployment & Debugging (via ADB)
```powershell
# Install debug build to attached device
.\gradlew.bat installDebug

# View real-time application logs
adb logcat -s SmsReceiver:D SendWorker:D TelegramSender:D ForwardingService:D

# Simulate an incoming SMS via ADB emulator console
adb emu sms send "+15551234567" "Agent verification test message"
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

### Jetpack Compose
- **State Hoisting:** Composable functions must not directly mutate ViewModels or Shared Preferences. Pass state down and events up (`onOpenSettings: () -> Unit`).
- **Stable IDs:** In `LazyColumn`, always supply a unique key parameter (`items(unsent, key = { it.id })`).
- **Edge-to-Edge:** Respect `Scaffold` `paddingValues` to prevent content overlapping status bars or navigation bars.

### Room Database
- Any entity modification requires incrementing `version` in `AppDatabase` and implementing an explicit migration strategy or schema export.
- DAO methods must be marked `suspend` for one-shot queries or return `Flow<T>` for continuous observation.

---

## 5. Common Workflows & Recipes

### A. Adding a New Forwarding Channel (e.g., Discord / Custom Webhook)
1. **Network Layer:** Create `online/thensoji/smsforwarder/network/<Channel>Sender.kt` accepting `OkHttpClient`.
2. **DI Module:** Add `@Provides @Singleton fun provide<Channel>Sender(...)` inside `di/NetworkModule.kt`.
3. **Preferences / Settings:** Add target configuration fields (e.g., Webhook URL) in `MainActivity.kt` (`SettingsScreen`) and persist into `sms_forwarder_prefs`.
4. **Worker Dispatch:** Update `SendWorker.kt` to inspect configured channels and trigger dispatch.
5. **Entity Update:** If tracking per-channel status, add appropriate flags to `ForwardedMessage.kt` and handle database migration.

### B. Adding Incoming SMS Filtering / Blacklisting
1. Create a filter entity or preference data store (e.g., `sender_filter_rules`).
2. Inject a `MessageFilterUseCase` or helper into `SmsReceiver.kt`.
3. Before `repository.insertMessage()`, evaluate the filter predicate (`if (!filter.shouldForward(sender, body)) return`).

### C. Database Migration Workflow
When modifying `ForwardedMessage`:
1. Modify `ForwardedMessage.kt`.
2. Increment `version` in `AppDatabase.kt` (e.g., `version = 2`).
3. Define migration:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE forwarded_messages ADD COLUMN filterTag TEXT DEFAULT NULL")
       }
   }
   ```
4. Register migration in `DatabaseModule.kt`:
   ```kotlin
   Room.databaseBuilder(appContext, AppDatabase::class.java, "sms_forwarder_db")
       .addMigrations(MIGRATION_1_2)
       .build()
   ```

---

## 6. Strict Constraints & Guardrails (Non-Negotiable)

1. ❌ **NEVER Hardcode Secrets:** Do not commit Telegram Bot Tokens, Chat IDs, or API keys into any source file, strings XML, or repository file.
2. ❌ **NEVER Block the Main Thread in Receivers:** `SmsReceiver.onReceive` must remain lightweight. Heavy database or network operations must be offloaded to `CoroutineScope(Dispatchers.IO)` or `WorkManager`.
3. ❌ **DO NOT Remove WorkManager Initializer Suppression:** `AndroidManifest.xml` explicitly suppresses default `WorkManagerInitializer` to enable Hilt on-demand worker instantiation. Do not re-enable it without architectural review.
4. ❌ **DO NOT Perform Unencrypted HTTP Calls:** All external network endpoints must use secure `https://`. Cleartext traffic is disabled by default.
5. ❌ **DO NOT Break Version Catalog Alignment:** All dependency modifications must be registered in `gradle/libs.versions.toml` rather than using hardcoded string dependencies in `app/build.gradle.kts`.
6. ❌ **DO NOT Ignore Multipart SMS Grouping:** Retain the multipart PDU concatenation logic in `SmsReceiver.kt` to prevent fragmentation of long messages.

