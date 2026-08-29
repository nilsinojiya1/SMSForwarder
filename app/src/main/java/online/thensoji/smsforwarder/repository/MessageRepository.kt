package online.thensoji.smsforwarder.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.ForwardedMessageDao
import online.thensoji.smsforwarder.util.MessageFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val dao: ForwardedMessageDao
) {

    fun getAllMessagesFlow(): Flow<List<ForwardedMessage>> = dao.getAllMessagesFlow()

    suspend fun getAllMessages(): List<ForwardedMessage> = withContext(Dispatchers.IO) {
        dao.getAllMessages()
    }

    suspend fun insertMessage(message: ForwardedMessage): Long = withContext(Dispatchers.IO) {
        dao.insert(message)
    }

    suspend fun getUnsentMessages(): List<ForwardedMessage> = withContext(Dispatchers.IO) {
        dao.getUnsentMessages()
    }

    suspend fun markAsSent(
        id: Long,
        telegramMessageId: String?,
        sentTimestamp: Long = System.currentTimeMillis(),
        delayMillis: Long? = null
    ) = withContext(Dispatchers.IO) {
        val msg = dao.getById(id) ?: return@withContext
        val calculatedDelay = delayMillis ?: (sentTimestamp - msg.timestamp)
        val updated = msg.copy(
            isSent = true,
            telegramMessageId = telegramMessageId,
            sentTimestamp = sentTimestamp,
            delayMillis = calculatedDelay,
            errorMessage = null
        )
        dao.update(updated)
    }

    suspend fun updateErrorMessage(id: Long, errorMessage: String?) = withContext(Dispatchers.IO) {
        val msg = dao.getById(id) ?: return@withContext
        dao.update(msg.copy(errorMessage = errorMessage))
    }

    suspend fun deleteMessage(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun getById(id: Long): ForwardedMessage? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun getBySystemSmsId(systemSmsId: Long): ForwardedMessage? = withContext(Dispatchers.IO) {
        dao.getBySystemSmsId(systemSmsId)
    }

    suspend fun existsBySystemSmsId(systemSmsId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.existsBySystemSmsId(systemSmsId)
    }

    suspend fun getMessageById(id: Long): ForwardedMessage? = getById(id)

    suspend fun updateMessage(message: ForwardedMessage) = withContext(Dispatchers.IO) {
        dao.update(message)
    }

    suspend fun isDuplicateOrNearby(
        sender: String?,
        rawBody: String,
        timestamp: Long,
        toleranceMillis: Long = 3000L
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanRaw = MessageFormatter.extractRawBody(rawBody)
        if (cleanRaw.isBlank()) return@withContext false

        val minTime = timestamp - toleranceMillis
        val maxTime = timestamp + toleranceMillis
        val candidates = dao.getNearbyMessagesByTime(minTime, maxTime)
        val normSender = normalizeSender(sender)

        candidates.any { candidate ->
            val candidateNormSender = normalizeSender(candidate.sender)
            val senderMatches = normSender.isEmpty() || candidateNormSender.isEmpty() || normSender == candidateNormSender
            if (!senderMatches) return@any false

            val candidateRaw = MessageFormatter.extractRawBody(candidate.body)
            candidateRaw == cleanRaw
        }
    }

    companion object {
        fun normalizeSender(sender: String?): String {
            if (sender.isNullOrBlank()) return ""
            val digits = sender.filter { it.isDigit() }
            return if (digits.length >= 7) digits.takeLast(10) else sender.trim().lowercase()
        }
    }
}
