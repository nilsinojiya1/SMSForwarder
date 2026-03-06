package online.thensoji.smsforwarder

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import online.thensoji.smsforwarder.data.AppDatabase
import online.thensoji.smsforwarder.network.TelegramSender

@HiltWorker
class SendWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val telegramSender: TelegramSender,
    private val db: AppDatabase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val messageId = inputData.getLong("messageId", -1L)
        if (messageId == -1L) return Result.failure()

        val dao = db.forwardedMessageDao()
        val messageObj = dao.getById(messageId) ?: return Result.failure()
        val message = messageObj.body

        val sharedPreferences = applicationContext.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE)
        val botToken = sharedPreferences.getString("bot_token", null)
        val chatId = sharedPreferences.getString("chat_id", null)

        if (botToken.isNullOrEmpty() || chatId.isNullOrEmpty()) {
            Log.e("SendWorker", "Bot token or chat ID is not set.")
            return Result.failure()
        }

        return try {
            val (ok, telegramId) = telegramSender.sendMessage(botToken, chatId, message)
            if (ok) {
                dao.update(messageObj.copy(isSent = true, telegramMessageId = telegramId))
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SendWorker", "Error forwarding message", e)
            Result.retry()
        }
    }
}
