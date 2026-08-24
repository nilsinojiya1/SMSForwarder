package online.thensoji.smsforwarder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.SendWorker
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import online.thensoji.smsforwarder.repository.MessageRepository

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase
) : ViewModel() {

    val messages: StateFlow<List<ForwardedMessage>> = repository.getAllMessagesFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshMessages() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.getAllMessages()
            _isRefreshing.value = false
        }
    }

    fun resendMessage(context: Context, messageId: Long) {
        viewModelScope.launch {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val input = Data.Builder()
                .putLong("messageId", messageId)
                .build()

            val work = OneTimeWorkRequestBuilder<SendWorker>()
                .setConstraints(constraints)
                .setInputData(input)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "send_sms_$messageId",
                ExistingWorkPolicy.REPLACE,
                work
            )
        }
    }

    fun resendAllPending(context: Context) {
        viewModelScope.launch {
            val unsent = repository.getUnsentMessages()
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            for (msg in unsent) {
                val input = Data.Builder()
                    .putLong("messageId", msg.id)
                    .build()
                val work = OneTimeWorkRequestBuilder<SendWorker>()
                    .setConstraints(constraints)
                    .setInputData(input)
                    .build()

                workManager.enqueueUniqueWork(
                    "send_sms_${msg.id}",
                    ExistingWorkPolicy.KEEP,
                    work
                )
            }
        }
    }

    fun markAsSent(id: Long, telegramMessageId: String? = null) {
        viewModelScope.launch {
            repository.markAsSent(id, telegramMessageId)
        }
    }

    fun deleteMessage(id: Long) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun testTelegramConnection(
        botToken: String,
        chatId: String,
        deviceName: String? = null,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val tag = if (!deviceName.isNullOrBlank()) "📱 [$deviceName]" else "📱 SMS Forwarder"
            val testMessage = """
                $tag
                🔔 Test message from SMS Forwarder app! Everything is set up correctly.
            """.trimIndent()
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
