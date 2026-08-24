package online.thensoji.smsforwarder.network.datasource

import online.thensoji.smsforwarder.network.model.SendMessageRequest
import online.thensoji.smsforwarder.network.model.TelegramMessageDto
import online.thensoji.smsforwarder.network.model.TelegramResponse
import retrofit2.Response

interface TelegramRemoteDataSource {
    suspend fun sendMessage(
        botToken: String,
        request: SendMessageRequest
    ): Response<TelegramResponse<TelegramMessageDto>>
}

