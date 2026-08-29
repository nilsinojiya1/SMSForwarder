package online.thensoji.smsforwarder.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import online.thensoji.smsforwarder.data.AppDatabase
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.MessageFormatter
import online.thensoji.smsforwarder.util.NotificationHelper

@HiltWorker
class SendWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase,
    private val db: AppDatabase
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SMSF SendWorker"
        private const val ONE_MINUTE_MILLIS = 60_000L
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return NotificationHelper.buildForegroundInfo(applicationContext)
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getLong("messageId", -1L)
        if (messageId == -1L) {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker started with INVALID messageId: -1L")
            return Result.failure()
        }

        val dao = db.forwardedMessageDao()
        val messageObj = dao.getById(messageId) ?: run {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker: Message #$messageId NOT FOUND in database.")
            return Result.failure()
        }

        val isManualResend = inputData.getBoolean("isManualResend", false)
        Log.d(TAG, "[SMSF-DEBUG] SendWorker.doWork started for ID #$messageId (isManualResend: $isManualResend, isSentInDb: ${messageObj.isSent})")

        if (!isManualResend) {
            // Idempotency Layer 1: Prevent sending the exact same database row more than once
            if (messageObj.isSent) {
                Log.d(TAG, "[SMSF-DEBUG] SendWorker: Message #$messageId is ALREADY MARKED SENT in DB. Skipping duplicate send.")
                return Result.success()
            }

            // Idempotency Layer 2: Prevent sending if another row with matching content was already sent recently
            val minTime = messageObj.timestamp - 3000L
            val maxTime = messageObj.timestamp + 3000L
            val alreadySentNearby = dao.getNearbyMessagesByTime(minTime, maxTime)
                .filter { it.id != messageId && it.isSent }

            val normSender = MessageRepository.normalizeSender(messageObj.sender)
            val cleanCurrentRaw = MessageFormatter.extractRawBody(messageObj.body)
            val isDuplicateAlreadySent = alreadySentNearby.any { sentCandidate ->
                val sentNormSender = MessageRepository.normalizeSender(sentCandidate.sender)
                val senderMatches = normSender.isEmpty() || sentNormSender.isEmpty() || normSender == sentNormSender
                if (!senderMatches) return@any false

                val sentRaw = MessageFormatter.extractRawBody(sentCandidate.body)
                sentRaw == cleanCurrentRaw
            }

            if (isDuplicateAlreadySent) {
                Log.d(TAG, "[SMSF-DEBUG] SendWorker: Duplicate message detected for ID #$messageId within 3s. Skipping duplicate Telegram dispatch.")
                dao.update(messageObj.copy(isSent = true, errorMessage = "Skipped duplicate dispatch"))
                return Result.success()
            }
        }

        val sharedPreferences = applicationContext.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val botToken = sharedPreferences.getString("bot_token", null)
        val chatId = sharedPreferences.getString("chat_id", null)

        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker: Bot token or chat ID is not set.")
            dao.update(messageObj.copy(errorMessage = "Bot token or chat ID is not configured"))
            return Result.failure()
        }

        val now = System.currentTimeMillis()
        val delayMillis = (now - messageObj.timestamp).coerceAtLeast(0)

        // If delay is >= 1 minute, inject delayed tag into the payload
        val payload = if (delayMillis >= ONE_MINUTE_MILLIS) {
            MessageFormatter.injectDelayTag(messageObj.body, delayMillis)
        } else {
            messageObj.body
        }

        Log.d(TAG, "[SMSF-DEBUG] SendWorker: Calling Telegram API for ID #$messageId...")
        return try {
            when (val result = sendTelegramMessageUseCase(botToken, chatId, payload)) {
                is SendResult.Success -> {
                    dao.update(
                        messageObj.copy(
                            isSent = true,
                            sentTimestamp = now,
                            delayMillis = delayMillis,
                            telegramMessageId = result.telegramMessageId,
                            errorMessage = null
                        )
                    )
                    Log.d(TAG, "[SMSF-DEBUG] SendWorker: SUCCESS sending ID #$messageId (telegramMsgId: ${result.telegramMessageId})")
                    Result.success()
                }
                is SendResult.Error -> {
                    Log.e(TAG, "[SMSF-DEBUG] SendWorker: ERROR sending ID #$messageId: ${result.errorMessage}")
                    dao.update(messageObj.copy(errorMessage = result.errorMessage))
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker: EXCEPTION sending ID #$messageId", e)
            dao.update(messageObj.copy(errorMessage = e.message))
            Result.retry()
        }
    }
}
