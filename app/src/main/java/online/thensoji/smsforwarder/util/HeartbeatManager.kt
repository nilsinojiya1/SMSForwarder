package online.thensoji.smsforwarder.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import online.thensoji.smsforwarder.BuildConfig
import online.thensoji.smsforwarder.R
import online.thensoji.smsforwarder.worker.HeartbeatWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Central controller for the opt-in Telegram heartbeat feature.
 *
 * The heartbeat is only active for users who explicitly configure a separate bot token in the
 * hidden Developer screen. Missing pings on the receiving channel are themselves the "app died"
 * signal, since WorkManager cannot run once an OEM force-stops the app.
 */
object HeartbeatManager {

    val INTERVAL_OPTIONS = longArrayOf(15, 30, 60, 120, 300)

    private fun prefs(context: Context) =
        context.getSharedPreferences(AppConstants.PREFS_MAIN, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(AppConstants.KEY_HEARTBEAT_ENABLED, false)

    fun getToken(context: Context): String = prefs(context).getString(AppConstants.KEY_HEARTBEAT_TOKEN, "")?.trim().orEmpty()

    /** Heartbeat chat id, falling back to the main forwarding chat id when unset. */
    fun getChatId(context: Context): String {
        val p = prefs(context)
        val heartbeatChat = p.getString(AppConstants.KEY_HEARTBEAT_CHAT_ID, "")?.trim().orEmpty()
        return heartbeatChat.ifEmpty { p.getString(AppConstants.KEY_CHAT_ID, "")?.trim().orEmpty() }
    }

    fun getIntervalMinutes(context: Context): Long =
        prefs(context).getLong(AppConstants.KEY_HEARTBEAT_INTERVAL, AppConstants.DEFAULT_HEARTBEAT_INTERVAL_MINUTES)

    fun getLastSent(context: Context): Long = prefs(context).getLong(AppConstants.KEY_HEARTBEAT_LAST_SENT, 0L)

    fun recordLastSent(context: Context) {
        prefs(context).edit { putLong(AppConstants.KEY_HEARTBEAT_LAST_SENT, System.currentTimeMillis()) }
    }

    fun recordAppOpen(context: Context) {
        prefs(context).edit { putLong(AppConstants.KEY_LAST_APP_OPEN, System.currentTimeMillis()) }
    }

    fun save(context: Context, enabled: Boolean, token: String, chatId: String, intervalMinutes: Long) {
        prefs(context).edit {
            putBoolean(AppConstants.KEY_HEARTBEAT_ENABLED, enabled)
            putString(AppConstants.KEY_HEARTBEAT_TOKEN, token.trim())
            putString(AppConstants.KEY_HEARTBEAT_CHAT_ID, chatId.trim())
            putLong(AppConstants.KEY_HEARTBEAT_INTERVAL, intervalMinutes)
        }
    }

    /** Enqueue or update the periodic worker when enabled; cancel it otherwise. */
    fun applySchedule(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        if (!isEnabled(context) || getToken(context).isEmpty() || getChatId(context).isEmpty()) {
            wm.cancelUniqueWork(AppConstants.WORK_NAME_HEARTBEAT)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<HeartbeatWorker>(
            getIntervalMinutes(context), TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        wm.enqueueUniquePeriodicWork(AppConstants.WORK_NAME_HEARTBEAT, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun intervalLabel(context: Context, minutes: Long): String = when (minutes) {
        15L -> context.getString(R.string.dev_interval_15m)
        30L -> context.getString(R.string.dev_interval_30m)
        60L -> context.getString(R.string.dev_interval_1h)
        120L -> context.getString(R.string.dev_interval_2h)
        300L -> context.getString(R.string.dev_interval_5h)
        else -> context.getString(R.string.dev_interval_15m)
    }

    private fun formatTime(context: Context, millis: Long?): String {
        if (millis == null || millis <= 0L) return context.getString(R.string.ping_never)
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        return fmt.format(Date(millis))
    }

    fun formatLastSent(context: Context): String {
        val last = getLastSent(context)
        return if (last <= 0L) context.getString(R.string.dev_value_never) else formatTime(context, last)
    }

    /**
     * Build the rich diagnostic heartbeat body. Timestamp stats are passed in so this stays free of
     * DB access and can be reused for the instant "test" ping from the UI.
     */
    fun buildPingMessage(
        context: Context,
        pending: Int,
        total: Int,
        lastReceived: Long?,
        lastForwarded: Long?
    ): String {
        val p = prefs(context)
        val battery = readBattery(context)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val unrestricted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        val yesNo = if (unrestricted) context.getString(R.string.ping_yes) else context.getString(R.string.ping_no)
        val chargingSuffix = if (battery.charging) context.getString(R.string.ping_charging) else ""

        return buildString {
            appendLine(context.getString(R.string.ping_title))
            appendLine()
            appendLine(context.getString(R.string.ping_device, MessageFormatter.getDeviceName(context)))
            appendLine(context.getString(R.string.ping_version, "v${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"))
            appendLine(context.getString(R.string.ping_time, formatTime(context, System.currentTimeMillis())))
            appendLine(context.getString(R.string.ping_interval, intervalLabel(context, getIntervalMinutes(context))))
            appendLine(context.getString(R.string.ping_battery, battery.level, chargingSuffix))
            appendLine(context.getString(R.string.ping_battery_opt, yesNo))
            appendLine(context.getString(R.string.ping_last_open, formatTime(context, p.getLong(AppConstants.KEY_LAST_APP_OPEN, 0L))))
            appendLine(context.getString(R.string.ping_last_sms, formatTime(context, lastReceived)))
            appendLine(context.getString(R.string.ping_last_forward, formatTime(context, lastForwarded)))
            appendLine(context.getString(R.string.ping_pending, pending))
            append(context.getString(R.string.ping_total, total))
        }
    }

    private data class BatteryInfo(val level: Int, val charging: Boolean)

    private fun readBattery(context: Context): BatteryInfo {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            BatteryInfo(level, plugged != 0)
        } catch (e: Exception) {
            BatteryInfo(-1, false)
        }
    }
}
