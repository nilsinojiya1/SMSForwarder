package online.thensoji.smsforwarder.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.SmsPartDao
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.MessageFormatter

import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase

@HiltWorker
class AssembleFallbackWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val smsPartDao: SmsPartDao,
    private val repository: MessageRepository,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AssembleFallbackWorker"
    }

    override suspend fun doWork(): Result {
        val sender = inputData.getString("sender") ?: return Result.failure()
        val refNumber = inputData.getInt("refNumber", -1)
        val simSlot = inputData.getInt("simSlot", -1)
        val timestamp = inputData.getLong("timestamp", System.currentTimeMillis())

        if (refNumber == -1) return Result.failure()

        val parts = smsPartDao.getPartsForRef(sender, refNumber)
        if (parts.isEmpty()) {
            // Already assembled and cleaned up by main receiver flow
            return Result.success()
        }

        Log.w(TAG, "Fallback assembly triggered for $sender (ref: $refNumber, parts received: ${parts.size}/${parts.firstOrNull()?.totalParts ?: "?"})")

        val fullBody = parts.sortedBy { it.partIndex }.joinToString("") { it.partBody }
        val message = MessageFormatter.format(
            applicationContext,
            sender,
            simSlot,
            timestamp,
            fullBody
        )

        val forwarded = ForwardedMessage(
            sender = sender,
            body = message,
            timestamp = timestamp,
            isSent = false,
            telegramMessageId = null
        )

        val id = repository.insertMessage(forwarded)
        smsPartDao.deletePartsForRef(sender, refNumber)

        // Attempt direct immediate send first
        val prefs = applicationContext.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val botToken = prefs.getString("bot_token", null)
        val chatId = prefs.getString("chat_id", null)

        var sentDirectly = false
        if (!botToken.isNullOrEmpty() && !chatId.isNullOrEmpty()) {
            try {
                val now = System.currentTimeMillis()
                val delayMillis = (now - timestamp).coerceAtLeast(0)
                val payload = if (delayMillis >= 60_000L) {
                    MessageFormatter.injectDelayTag(message, delayMillis)
                } else {
                    message
                }

                val sendResult = sendTelegramMessageUseCase(botToken, chatId, payload)
                if (sendResult is SendResult.Success) {
                    repository.markAsSent(
                        id = id,
                        telegramMessageId = sendResult.telegramMessageId,
                        sentTimestamp = now,
                        delayMillis = delayMillis
                    )
                    sentDirectly = true
                    Log.d(TAG, "Fallback message $id sent directly successfully.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Direct send in fallback worker failed, falling back to WorkManager: ${e.message}")
            }
        }

        if (!sentDirectly) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val input = Data.Builder()
            .putLong("messageId", id)
            .build()
        val work = OneTimeWorkRequestBuilder<SendWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "send_sms_$id",
            ExistingWorkPolicy.KEEP,
            work
        )
        }

        return Result.success()
    }
}

