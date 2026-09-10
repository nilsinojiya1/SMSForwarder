package online.thensoji.smsforwarder.util

import android.content.Context

object ConsentManager {

    fun isConsentGiven(context: Context): Boolean {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_CONSENT, Context.MODE_PRIVATE)
        return prefs.getBoolean(AppConstants.KEY_CONSENT_GRANTED, false)
    }

    fun setConsentGiven(context: Context, granted: Boolean) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_CONSENT, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(AppConstants.KEY_CONSENT_GRANTED, granted)
            .putLong(AppConstants.KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getConsentTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_CONSENT, Context.MODE_PRIVATE)
        return prefs.getLong(AppConstants.KEY_CONSENT_TIMESTAMP, 0L)
    }
}
