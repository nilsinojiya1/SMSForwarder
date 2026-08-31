package online.thensoji.smsforwarder.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.HeartbeatManager

/**
 * Periodically pings a separate Telegram bot with a rich device-health snapshot so the operator can
 * confirm from anywhere that this device is still forwarding in the background.
 */
@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "SMSF HeartbeatWorker"
    }

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!HeartbeatManager.isEnabled(context)) {
            Log.d(TAG, "Heartbeat disabled. Skipping.")
            return Result.success()
        }

        val token = HeartbeatManager.getToken(context)
        val chatId = HeartbeatManager.getChatId(context)
        if (token.isEmpty() || chatId.isEmpty()) {
            Log.w(TAG, "Heartbeat missing token or chat id. Skipping.")
            return Result.success()
        }

        return try {
            val pending = messageRepository.getUnsentCount()
            val total = messageRepository.getTotalCount()
            val lastReceived = messageRepository.getLatestReceivedTimestamp()
            val lastForwarded = messageRepository.getLastForwardedTimestamp()

            val message = HeartbeatManager.buildPingMessage(
                context = context,
                pending = pending,
                total = total,
                lastReceived = lastReceived,
                lastForwarded = lastForwarded
            )

            when (val result = sendTelegramMessageUseCase(token, chatId, message)) {
                is SendResult.Success -> {
                    HeartbeatManager.recordLastSent(context)
                    Log.d(TAG, "Heartbeat ping delivered.")
                    Result.success()
                }
                is SendResult.Error -> {
                    Log.w(TAG, "Heartbeat ping failed: ${result.errorMessage}")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in HeartbeatWorker execution", e)
            Result.retry()
        }
    }
}
