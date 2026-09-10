package online.thensoji.smsforwarder.util

/**
 * Central repository of application-wide constants, preference keys,
 * notification configurations, timing thresholds, and action strings.
 */
object AppConstants {

    // =========================================================================
    // SharedPreferences File Names
    // =========================================================================
    const val PREFS_MAIN = "sms_forwarder_prefs"
    const val PREFS_CONSENT = "sms_forwarder_consent_prefs"
    const val PREFS_PIN = "sms_forwarder_pin_prefs"

    // =========================================================================
    // SharedPreferences Keys - Main Configuration
    // =========================================================================
    const val KEY_BOT_TOKEN = "bot_token"
    const val KEY_CHAT_ID = "chat_id"
    const val KEY_DEVICE_NAME = "device_name"
    const val KEY_KEEP_ALIVE_ENABLED = "keep_alive_enabled"
    const val KEY_LAST_INBOX_SYNC_TIME = "last_inbox_sync_timestamp"

    // =========================================================================
    // SharedPreferences Keys - Consent & Security
    // =========================================================================
    const val KEY_CONSENT_GRANTED = "ethical_consent_granted"
    const val KEY_CONSENT_TIMESTAMP = "ethical_consent_timestamp"
    const val KEY_PIN_HASH = "app_pin_hash"

    // =========================================================================
    // SharedPreferences Keys - Heartbeat & Diagnostics
    // =========================================================================
    const val KEY_HEARTBEAT_ENABLED = "heartbeat_enabled"
    const val KEY_HEARTBEAT_TOKEN = "heartbeat_bot_token"
    const val KEY_HEARTBEAT_CHAT_ID = "heartbeat_chat_id"
    const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval_minutes"
    const val KEY_HEARTBEAT_LAST_SENT = "heartbeat_last_sent"
    const val KEY_LAST_APP_OPEN = "last_app_open"

    // =========================================================================
    // WorkManager Unique Work Identifiers
    // =========================================================================
    const val WORK_NAME_WATCHDOG = "periodic_sms_watchdog"
    const val WORK_NAME_HEARTBEAT = "periodic_heartbeat"
    const val WORK_NAME_SEND_PREFIX = "send_sms_"

    // WorkManager Input Data Keys
    const val KEY_WORK_MESSAGE_ID = "messageId"
    const val KEY_WORK_IS_MANUAL_RESEND = "isManualResend"

    // =========================================================================
    // Notification Constants
    // =========================================================================
    const val NOTIFICATION_CHANNEL_ID = "sms_forwarding_channel"
    const val NOTIFICATION_ID_SENDING = 1001
    const val NOTIFICATION_ID_KEEP_ALIVE = 1002

    // =========================================================================
    // Network & API
    // =========================================================================
    const val TELEGRAM_BASE_URL = "https://api.telegram.org/"
    const val TELEGRAM_MAX_CHUNK_SIZE = 3900
    const val HTTP_TIMEOUT_SECONDS = 30L

    // =========================================================================
    // Timings, Thresholds & Delays (Milliseconds)
    // =========================================================================
    const val DEDUP_TOLERANCE_MS = 120_000L // 2-minute tolerance window for SMS dedup
    const val WAKELOCK_TIMEOUT_MS = 60_000L // 60s max partial wake-lock
    const val ONE_MINUTE_MS = 60_000L
    const val INBOX_LOOKBACK_DEFAULT_MS = 48 * 60 * 60 * 1000L // 48 hours fallback lookback
    const val INBOX_CLOCK_JITTER_MS = 5 * 60 * 1000L // 5 minutes lookback from last sync
    const val OBSERVER_DEBOUNCE_DELAY_MS = 1000L
    const val SMS_RECEIVER_TELEPHONY_DELAY_MS = 300L
    const val DEFAULT_HEARTBEAT_INTERVAL_MINUTES = 15L

    // =========================================================================
    // Service Actions
    // =========================================================================
    const val ACTION_STOP_KEEPALIVE = "online.thensoji.smsforwarder.action.STOP_KEEPALIVE"
}
