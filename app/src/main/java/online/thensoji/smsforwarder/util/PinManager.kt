package online.thensoji.smsforwarder.util

import android.content.Context
import java.security.MessageDigest

object PinManager {

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_PIN, Context.MODE_PRIVATE)
        val hash = prefs.getString(AppConstants.KEY_PIN_HASH, null)
        return !hash.isNullOrBlank()
    }

    fun savePin(context: Context, pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return false
        val prefs = context.getSharedPreferences(AppConstants.PREFS_PIN, Context.MODE_PRIVATE)
        return prefs.edit().putString(AppConstants.KEY_PIN_HASH, hashPin(pin)).commit()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        if (pin.length != 4) return false
        val prefs = context.getSharedPreferences(AppConstants.PREFS_PIN, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(AppConstants.KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == savedHash
    }

    fun clearPin(context: Context) {
        val prefs = context.getSharedPreferences(AppConstants.PREFS_PIN, Context.MODE_PRIVATE)
        prefs.edit().remove(AppConstants.KEY_PIN_HASH).apply()
    }
}

