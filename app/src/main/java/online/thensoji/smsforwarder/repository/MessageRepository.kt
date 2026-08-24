package online.thensoji.smsforwarder.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.ForwardedMessageDao
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
}
