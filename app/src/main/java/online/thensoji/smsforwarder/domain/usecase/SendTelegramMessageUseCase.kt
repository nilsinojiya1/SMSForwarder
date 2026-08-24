package online.thensoji.smsforwarder.domain.usecase

import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.repository.TelegramRepository
import javax.inject.Inject

class SendTelegramMessageUseCase @Inject constructor(
    private val telegramRepository: TelegramRepository
) {
    suspend operator fun invoke(botToken: String, chatId: String, message: String): SendResult {
        if (botToken.isBlank()) {
            return SendResult.Error("Bot token cannot be blank")
        }
        if (chatId.isBlank()) {
            return SendResult.Error("Chat ID cannot be blank")
        }
        return telegramRepository.sendMessage(botToken.trim(), chatId.trim(), message)
    }
}

