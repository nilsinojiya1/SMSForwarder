package online.thensoji.smsforwarder.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.worker.SendWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsInboxSyncHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MessageRepository
) {

    companion object {
        private const val TAG = "SMSF SmsInboxSyncHelper"
        private const val PREFS_NAME = "sms_forwarder_prefs"
        private const val KEY_LAST_INBOX_SYNC_TIME = "last_inbox_sync_timestamp"
        private const val DEFAULT_LOOKBACK_MILLIS = 48 * 60 * 60 * 1000L // 48 hours fallback
    }

    /**
     * Reconciles Android's Telephony SMS Inbox with Room persistence.
     * Ingests any messages that were not received via broadcast and sends them.
     * @return Number of newly ingested messages.
     */
    suspend fun syncInboxMessages(forceFullWindow: Boolean = false): Int = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_SMS permission not granted. Cannot sync system SMS inbox.")
            return@withContext 0
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastSync = prefs.getLong(KEY_LAST_INBOX_SYNC_TIME, 0L)
        val lookbackLimit = now - DEFAULT_LOOKBACK_MILLIS
        val minTimestamp = if (forceFullWindow || lastSync == 0L) {
            lookbackLimit
        } else {
            // Check from 5 minutes before last sync to handle clock jitter / delayed carrier writes
            (lastSync - 5 * 60 * 1000L).coerceAtLeast(lookbackLimit)
        }

        Log.d(TAG, "Scanning device SMS inbox for messages since timestamp: $minTimestamp")

        var newlyIngestedCount = 0
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.SUBSCRIPTION_ID
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(minTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} ASC"

        try {
            context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)
                val subIdCol = cursor.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (cursor.moveToNext()) {
                    val smsId = if (idCol >= 0) cursor.getLong(idCol) else -1L
                    val sender = if (addressCol >= 0) cursor.getString(addressCol) else "Unknown"
                    val rawBody = if (bodyCol >= 0) cursor.getString(bodyCol) else ""
                    val timestamp = if (dateCol >= 0) cursor.getLong(dateCol) else System.currentTimeMillis()
                    val subId = if (subIdCol >= 0) cursor.getInt(subIdCol) else -1

                    if (rawBody.isBlank()) continue

                    // Deduplicate against existing Room database using native systemSmsId
                    if (smsId != -1L && repository.existsBySystemSmsId(smsId)) {
                        continue
                    }

                    // Fallback content-based deduplication
                    val isDuplicate = repository.isDuplicateOrNearby(sender, rawBody, timestamp)
                    if (isDuplicate) {
                        continue
                    }

                    // Newly discovered message!
                    val simSlot = resolveSimSlotFromSubId(subId)
                    Log.d(TAG, "[SMSF-DEBUG] Discovered new SMS in system inbox (systemId: $smsId, sender: $sender, date: $timestamp). Ingesting...")

                    val initialForwarded = ForwardedMessage(
                        systemSmsId = if (smsId != -1L) smsId else null,
                        sender = sender,
                        body = rawBody,
                        timestamp = timestamp,
                        isSent = false,
                        telegramMessageId = null
                    )
                    val insertedId = repository.insertMessage(initialForwarded)
                    val fullMessage = MessageFormatter.format(context, sender, simSlot, timestamp, rawBody, messageId = insertedId)
                    repository.updateMessage(initialForwarded.copy(id = insertedId, body = fullMessage))
                    newlyIngestedCount++

                    // Dispatch via unique WorkManager
                    dispatchIngestedMessage(insertedId)
                }
            }

            // Update watermark
            prefs.edit().putLong(KEY_LAST_INBOX_SYNC_TIME, now).apply()
            Log.d(TAG, "[SMSF-DEBUG] Inbox scan complete. Ingested and queued $newlyIngestedCount new message(s).")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning system SMS inbox", e)
        }

        newlyIngestedCount
    }

    private fun resolveSimSlotFromSubId(subId: Int): Int {
        if (subId == -1) return -1
        return try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val subInfo = subManager?.getActiveSubscriptionInfo(subId)
            subInfo?.simSlotIndex ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    private fun dispatchIngestedMessage(messageId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val input = Data.Builder()
            .putLong("messageId", messageId)
            .build()

        val work = OneTimeWorkRequestBuilder<SendWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "send_sms_$messageId",
            ExistingWorkPolicy.KEEP,
            work
        )
    }
}
