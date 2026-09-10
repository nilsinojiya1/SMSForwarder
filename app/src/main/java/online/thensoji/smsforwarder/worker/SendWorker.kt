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
import online.thensoji.smsforwarder.util.AppConstants
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
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return NotificationHelper.buildForegroundInfo(applicationContext)
    }

    override suspend fun doWork(): Result {
        val messageId = inputData.getLong(AppConstants.KEY_WORK_MESSAGE_ID, -1L)
        if (messageId == -1L) {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker started with INVALID messageId: -1L")
            return Result.failure()
        }

        val dao = db.forwardedMessageDao()
        val messageObj = dao.getById(messageId) ?: run {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker: Message #$messageId NOT FOUND in database.")
            return Result.failure()
        }

        val isManualResend = inputData.getBoolean(AppConstants.KEY_WORK_IS_MANUAL_RESEND, false)
        Log.d(TAG, "[SMSF-DEBUG] SendWorker.doWork started for ID #$messageId (isManualResend: $isManualResend, isSentInDb: ${messageObj.isSent})")

        if (!isManualResend) {
            // Idempotency Layer 1: Prevent sending the exact same database row more than once
            if (messageObj.isSent) {
                Log.d(TAG, "[SMSF-DEBUG] SendWorker: Message #$messageId is ALREADY MARKED SENT in DB. Skipping duplicate send.")
                return Result.success()
            }

            // Idempotency Layer 2: Prevent sending if an identical message was already sent or is preceded by an earlier pending row
            val minTime = messageObj.timestamp - AppConstants.DEDUP_TOLERANCE_MS
            val maxTime = messageObj.timestamp + AppConstants.DEDUP_TOLERANCE_MS
            val nearbyCandidates = dao.getNearbyMessagesByTime(minTime, maxTime)
                .filter { it.id != messageId }

            val normSender = MessageRepository.normalizeSender(messageObj.sender)
            val cleanCurrentRaw = MessageFormatter.extractRawBody(messageObj.body)

            // 1. Check if an identical message was already sent
            val isDuplicateAlreadySent = nearbyCandidates.any { candidate ->
                if (!candidate.isSent) return@any false
                val candidateNormSender = MessageRepository.normalizeSender(candidate.sender)
                val senderMatches = normSender.isEmpty() || candidateNormSender.isEmpty() || normSender == candidateNormSender
                if (!senderMatches) return@any false

                val candidateRaw = MessageFormatter.extractRawBody(candidate.body)
                candidateRaw == cleanCurrentRaw
            }

            // 2. Check if an identical message with an earlier/lower ID is also pending (race condition winner)
            val isDuplicatePrecededByEarlierPending = nearbyCandidates.any { candidate ->
                if (candidate.isSent || candidate.id >= messageId) return@any false
                val candidateNormSender = MessageRepository.normalizeSender(candidate.sender)
                val senderMatches = normSender.isEmpty() || candidateNormSender.isEmpty() || normSender == candidateNormSender
                if (!senderMatches) return@any false

                val candidateRaw = MessageFormatter.extractRawBody(candidate.body)
                candidateRaw == cleanCurrentRaw
            }

            if (isDuplicateAlreadySent || isDuplicatePrecededByEarlierPending) {
                Log.d(TAG, "[SMSF-DEBUG] SendWorker: Duplicate message detected for ID #$messageId (alreadySent: $isDuplicateAlreadySent, precededByEarlier: $isDuplicatePrecededByEarlierPending). Skipping duplicate Telegram dispatch.")
                dao.update(messageObj.copy(isSent = true, errorMessage = "Skipped duplicate dispatch"))
                return Result.success()
            }
        }

        val sharedPreferences = applicationContext.getSharedPreferences(AppConstants.PREFS_MAIN, Context.MODE_PRIVATE)
        val botToken = sharedPreferences.getString(AppConstants.KEY_BOT_TOKEN, null)
        val chatId = sharedPreferences.getString(AppConstants.KEY_CHAT_ID, null)

        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.e(TAG, "[SMSF-DEBUG] SendWorker: Bot token or chat ID is not set.")
            dao.update(messageObj.copy(errorMessage = "Bot token or chat ID is not configured"))
            return Result.failure()
        }

        val now = System.currentTimeMillis()
        val delayMillis = (now - messageObj.timestamp).coerceAtLeast(0)

        // If delay is >= 1 minute, inject delayed tag into the payload
        val payload = if (delayMillis >= AppConstants.ONE_MINUTE_MS) {
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
