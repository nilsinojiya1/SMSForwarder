package online.thensoji.smsforwarder.network.api

import online.thensoji.smsforwarder.network.model.SendMessageRequest
import online.thensoji.smsforwarder.network.model.TelegramMessageDto
import online.thensoji.smsforwarder.network.model.TelegramResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface TelegramApiService {

    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Body request: SendMessageRequest
    ): Response<TelegramResponse<TelegramMessageDto>>
}
