package online.thensoji.smsforwarder.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import online.thensoji.smsforwarder.repository.MessageRepository

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase
) : ViewModel() {

    private val _unsent = MutableStateFlow<List<ForwardedMessage>>(emptyList())
    val unsent: StateFlow<List<ForwardedMessage>> = _unsent.asStateFlow()

    fun refreshUnsent() {
        viewModelScope.launch {
            _unsent.value = repository.getUnsentMessages()
        }
    }

    fun insertMessage(message: ForwardedMessage, onInserted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertMessage(message)
            onInserted(id)
        }
    }

    fun markAsSent(id: Long, telegramMessageId: String?) {
        viewModelScope.launch {
            repository.markAsSent(id, telegramMessageId)
            refreshUnsent()
        }
    }

    fun testTelegramConnection(
        botToken: String,
        chatId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val testMessage = "🔔 Test message from SMS Forwarder app! Everything is set up correctly."
            when (val result = sendTelegramMessageUseCase(botToken, chatId, testMessage)) {
                is SendResult.Success -> {
                    onComplete(true, null)
                }
                is SendResult.Error -> {
                    onComplete(false, result.errorMessage)
                }
            }
        }
    }
}
