package online.thensoji.smsforwarder.domain.model

sealed class SendResult {
    data class Success(val telegramMessageId: String?) : SendResult()
    data class Error(val errorMessage: String, val throwable: Throwable? = null) : SendResult()
}

