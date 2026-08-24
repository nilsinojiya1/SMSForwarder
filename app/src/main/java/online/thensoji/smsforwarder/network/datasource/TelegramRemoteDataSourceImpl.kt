package online.thensoji.smsforwarder.network.datasource

import online.thensoji.smsforwarder.network.api.TelegramApiService
import online.thensoji.smsforwarder.network.model.SendMessageRequest
import online.thensoji.smsforwarder.network.model.TelegramMessageDto
import online.thensoji.smsforwarder.network.model.TelegramResponse
import retrofit2.Response
import javax.inject.Inject

class TelegramRemoteDataSourceImpl @Inject constructor(
    private val apiService: TelegramApiService
) : TelegramRemoteDataSource {

    override suspend fun sendMessage(
        botToken: String,
        request: SendMessageRequest
    ): Response<TelegramResponse<TelegramMessageDto>> {
        val url = "https://api.telegram.org/bot$botToken/sendMessage"
        return apiService.sendMessage(url, request)
    }
}
