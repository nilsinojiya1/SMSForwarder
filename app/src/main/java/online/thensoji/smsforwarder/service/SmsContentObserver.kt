package online.thensoji.smsforwarder.service

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.util.SmsInboxSyncHelper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsContentObserver @Inject constructor(
    private val inboxSyncHelper: SmsInboxSyncHelper
) : ContentObserver(Handler(Looper.getMainLooper())) {

    companion object {
        private const val TAG = "SmsContentObserver"
        private const val DEBOUNCE_DELAY_MS = 1000L
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var debounceJob: Job? = null

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "Telephony SMS database change detected: $uri")

        // Debounce multiple rapid changes (e.g. multi-part or draft updates)
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)
            try {
                val syncedCount = inboxSyncHelper.syncInboxMessages()
                if (syncedCount > 0) {
                    Log.d(TAG, "ContentObserver sync captured $syncedCount message(s) directly from system inbox.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in SmsContentObserver sync", e)
            }
        }
    }
}
