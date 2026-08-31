package online.thensoji.smsforwarder.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import online.thensoji.smsforwarder.service.ForwarderService

/**
 * Controls the always-on keep-alive foreground service ([ForwarderService]) and persists whether
 * the user wants it running.
 */
object KeepAliveManager {

    private const val TAG = "SMSF KeepAliveManager"
    private const val PREFS = "sms_forwarder_prefs"
    const val KEY_ENABLED = "keep_alive_enabled"

    /** Enabled by default: the whole point of the app is reliable background delivery. */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_ENABLED, enabled)
        }
        if (enabled) start(context) else stop(context)
    }

    /** Start the foreground service if the user has it enabled. Safe to call repeatedly. */
    fun startIfEnabled(context: Context) {
        if (isEnabled(context)) start(context)
    }

    fun start(context: Context) {
        try {
            val intent = Intent(context.applicationContext, ForwarderService::class.java)
            ContextCompat.startForegroundService(context.applicationContext, intent)
        } catch (e: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException when triggered from the background on
            // Android 12+. The service will be (re)started next time the app is opened or on boot.
            Log.w(TAG, "Could not start keep-alive service now: ${e.message}")
        }
    }

    fun stop(context: Context) {
        try {
            val intent = Intent(context.applicationContext, ForwarderService::class.java).apply {
                action = ForwarderService.ACTION_STOP
            }
            context.applicationContext.startService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not stop keep-alive service: ${e.message}")
        }
    }
}
