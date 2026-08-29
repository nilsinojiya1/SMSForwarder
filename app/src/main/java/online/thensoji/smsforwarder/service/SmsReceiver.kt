package online.thensoji.smsforwarder.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.MessageFormatter
import online.thensoji.smsforwarder.util.SmsInboxSyncHelper
import online.thensoji.smsforwarder.worker.SendWorker

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SMSF SmsReceiver"
        private const val WAKELOCK_TIMEOUT_MS = 60_000L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsReceiverEntryPoint {
        fun getRepository(): MessageRepository
        fun getSmsInboxSyncHelper(): SmsInboxSyncHelper
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != action &&
            Telephony.Sms.Intents.SMS_DELIVER_ACTION != action
        ) {
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsReceiverEntryPoint::class.java
        )
        val repository = entryPoint.getRepository()
        val inboxSyncHelper = entryPoint.getSmsInboxSyncHelper()

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract messages from intent", e)
            null
        }

        if (messages.isNullOrEmpty()) return

        // Defensive SIM slot extraction (Guards against SecurityException or null subManager)
        val simSlot = extractSimSlotSafely(context, intent)

        // Acquire a CPU wake-lock to guarantee full execution during background/kill Doze states
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SMSForwarder:SmsReceiverWakeLock"
        )?.apply {
            setReferenceCounted(false)
            try {
                acquire(WAKELOCK_TIMEOUT_MS)
            } catch (e: Exception) {
                Log.w(TAG, "Could not acquire WakeLock: ${e.message}")
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "[SMSF-DEBUG] SmsReceiver doorbell triggered. Initiating Trigger & Query pipeline...")
                
                // Allow Android Telephony Provider ~300ms to finalize writing assembled SMS to content://sms/inbox
                delay(300L)
                val syncedCount = inboxSyncHelper.syncInboxMessages()
                Log.d(TAG, "[SMSF-DEBUG] Trigger & Query: Inbox query synced $syncedCount message(s).")

                // Fallback: If content://sms query found 0 messages (e.g. OEM delay), ingest from broadcast payload directly
                val nonNullMessages = messages.filterNotNull()
                if (syncedCount == 0 && nonNullMessages.isNotEmpty()) {
                    val firstMsg = nonNullMessages.first()
                    val sender = firstMsg.originatingAddress ?: "Unknown"
                    val timestamp = firstMsg.timestampMillis
                    val combinedBody = nonNullMessages.joinToString("") { it.messageBody ?: "" }

                    if (repository.isDuplicateOrNearby(sender, combinedBody, timestamp)) {
                        Log.d(TAG, "[SMSF-DEBUG] Duplicate SMS detected in fallback. Skipping.")
                    } else {
                        val initialForwarded = ForwardedMessage(
                            sender = sender,
                            body = combinedBody,
                            timestamp = timestamp,
                            isSent = false,
                            telegramMessageId = null
                        )
                        val id = repository.insertMessage(initialForwarded)
                        val fullMessage = MessageFormatter.format(context, sender, simSlot, timestamp, combinedBody, messageId = id)
                        repository.updateMessage(initialForwarded.copy(id = id, body = fullMessage))
                        Log.d(TAG, "[SMSF-DEBUG] Fallback inserted SMS ID #$id ($sender). Dispatching...")

                        dispatchMessage(context, id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[SMSF-DEBUG] Error processing incoming SMS in SmsReceiver", e)
            } finally {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing wake lock: ${e.message}")
                }
                pendingResult.finish()
            }
        }
    }

    private fun extractSimSlotSafely(context: Context, intent: Intent): Int {
        val slotFromExtras = intent.getIntExtra("slot", -1)
        if (slotFromExtras != -1) return slotFromExtras

        val simId = intent.getIntExtra("simId", -1)
        if (simId != -1) return simId

        val simSlot = intent.getIntExtra("simSlot", -1)
        if (simSlot != -1) return simSlot

        val subscriptionId = intent.getIntExtra("subscription", -1)
        if (subscriptionId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val subSlot = SubscriptionManager.getSlotIndex(subscriptionId)
            if (subSlot != -1) return subSlot
        }

        return try {
            if (subscriptionId != -1) {
                val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
                val subInfo = subManager?.getActiveSubscriptionInfo(subscriptionId)
                subInfo?.simSlotIndex ?: -1
            } else {
                -1
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted or restricted: ${e.message}")
            -1
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract SIM slot info: ${e.message}")
            -1
        }
    }

    private fun dispatchMessage(
        context: Context,
        messageId: Long
    ) {
        Log.d(TAG, "[SMSF-DEBUG] dispatchMessage: Enqueuing unique expedited SendWorker for ID #$messageId")
        enqueueSendWorker(context, messageId)
    }

    private fun enqueueSendWorker(context: Context, messageId: Long) {
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

