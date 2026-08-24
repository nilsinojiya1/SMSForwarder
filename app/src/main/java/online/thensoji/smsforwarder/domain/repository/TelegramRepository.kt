package online.thensoji.smsforwarder.domain.repository

import online.thensoji.smsforwarder.domain.model.SendResult

interface TelegramRepository {
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        message: String
    ): SendResult
}

