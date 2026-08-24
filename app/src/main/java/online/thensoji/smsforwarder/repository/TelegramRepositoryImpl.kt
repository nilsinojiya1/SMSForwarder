package online.thensoji.smsforwarder.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.repository.TelegramRepository
import online.thensoji.smsforwarder.network.datasource.TelegramRemoteDataSource
import online.thensoji.smsforwarder.network.model.SendMessageRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelegramRepositoryImpl @Inject constructor(
    private val remoteDataSource: TelegramRemoteDataSource
) : TelegramRepository {

    companion object {
        private const val TAG = "TelegramRepository"
    }

    override suspend fun sendMessage(
        botToken: String,
        chatId: String,
        message: String
    ): SendResult = withContext(Dispatchers.IO) {
        try {
            val request = SendMessageRequest(
                chatId = chatId,
                text = message
            )
            val response = remoteDataSource.sendMessage(botToken, request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.ok) {
                    val messageId = body.result?.messageId?.toString()
                    Log.d(TAG, "Message sent successfully. ID: $messageId")
                    SendResult.Success(messageId)
                } else {
                    val errorDescription = body?.description ?: "Unknown Telegram API response"
                    Log.e(TAG, "Telegram API returned error: $errorDescription")
                    SendResult.Error(errorDescription)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: response.message()
                Log.e(TAG, "HTTP error: ${response.code()} $errorBody")
                SendResult.Error("HTTP ${response.code()}: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception while forwarding message", e)
            SendResult.Error(e.localizedMessage ?: "Failed to connect to Telegram servers", e)
        }
    }
}

