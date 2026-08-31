package online.thensoji.smsforwarder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import online.thensoji.smsforwarder.worker.SendWorker
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.domain.model.SendResult
import online.thensoji.smsforwarder.domain.usecase.SendTelegramMessageUseCase
import online.thensoji.smsforwarder.repository.MessageRepository
import online.thensoji.smsforwarder.util.HeartbeatManager
import online.thensoji.smsforwarder.util.SmsInboxSyncHelper
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository,
    private val sendTelegramMessageUseCase: SendTelegramMessageUseCase,
    private val inboxSyncHelper: SmsInboxSyncHelper
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _resendingMessageIds = MutableStateFlow<Set<Long>>(emptySet())
    val resendingMessageIds: StateFlow<Set<Long>> = _resendingMessageIds.asStateFlow()

    private val _isResendingAll = MutableStateFlow(false)
    val isResendingAll: StateFlow<Boolean> = _isResendingAll.asStateFlow()

    val messages: StateFlow<List<ForwardedMessage>> = repository.getAllMessagesFlow()
        .onEach {
            _isLoading.value = false
            _isRefreshing.value = false
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshMessages() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                inboxSyncHelper.syncInboxMessages(forceFullWindow = true)
            } catch (_: Exception) {}
            repository.getAllMessages()
            delay(300)
            _isRefreshing.value = false
        }
    }

    fun resendMessage(context: Context, messageId: Long) {
        viewModelScope.launch {
            _resendingMessageIds.update { it + messageId }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val input = Data.Builder()
                .putLong("messageId", messageId)
                .putBoolean("isManualResend", true)
                .build()

            val work = OneTimeWorkRequestBuilder<SendWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(constraints)
                .setInputData(input)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "send_sms_$messageId",
                ExistingWorkPolicy.REPLACE,
                work
            )

            delay(800)
            _resendingMessageIds.update { it - messageId }
        }
    }

    fun resendAllPending(context: Context) {
        viewModelScope.launch {
            _isResendingAll.value = true
            val unsent = repository.getUnsentMessages()
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            for (msg in unsent) {
                val input = Data.Builder()
                    .putLong("messageId", msg.id)
                    .putBoolean("isManualResend", true)
                    .build()
                val work = OneTimeWorkRequestBuilder<SendWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .setConstraints(constraints)
                    .setInputData(input)
                    .build()

                workManager.enqueueUniqueWork(
                    "send_sms_${msg.id}",
                    ExistingWorkPolicy.REPLACE,
                    work
                )
            }

            delay(800)
            _isResendingAll.value = false
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

    fun sendHeartbeatTest(
        context: Context,
        botToken: String,
        chatId: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val pending = repository.getUnsentCount()
            val total = repository.getTotalCount()
            val lastReceived = repository.getLatestReceivedTimestamp()
            val lastForwarded = repository.getLastForwardedTimestamp()
            val message = HeartbeatManager.buildPingMessage(
                context, pending, total, lastReceived, lastForwarded
            )
            when (val result = sendTelegramMessageUseCase(botToken, chatId, message)) {
                is SendResult.Success -> {
                    HeartbeatManager.recordLastSent(context)
                    onComplete(true, null)
                }
                is SendResult.Error -> onComplete(false, result.errorMessage)
            }
        }
    }
}
