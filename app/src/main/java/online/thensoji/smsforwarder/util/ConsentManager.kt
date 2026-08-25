package online.thensoji.smsforwarder.util

import android.content.Context

object ConsentManager {
    private const val PREFS_NAME = "sms_forwarder_consent_prefs"
    private const val KEY_CONSENT_GRANTED = "ethical_consent_granted"
    private const val KEY_CONSENT_TIMESTAMP = "ethical_consent_timestamp"

    fun isConsentGiven(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_CONSENT_GRANTED, false)
    }

    fun setConsentGiven(context: Context, granted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_CONSENT_GRANTED, granted)
            .putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getConsentTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_CONSENT_TIMESTAMP, 0L)
    }
}
