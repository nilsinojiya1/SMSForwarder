package online.thensoji.smsforwarder.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import online.thensoji.smsforwarder.data.ForwardedMessage
import online.thensoji.smsforwarder.data.ForwardedMessageDao

@Singleton
class MessageRepository @Inject constructor(
    private val dao: ForwardedMessageDao
) {

    suspend fun insertMessage(message: ForwardedMessage): Long = withContext(Dispatchers.IO) {
        dao.insert(message)
    }

    suspend fun getUnsentMessages(): List<ForwardedMessage> = withContext(Dispatchers.IO) {
        dao.getUnsentMessages()
    }

    suspend fun markAsSent(id: Long, telegramMessageId: String?) = withContext(Dispatchers.IO) {
        val msg = dao.getById(id) ?: return@withContext
        val updated = msg.copy(isSent = true, telegramMessageId = telegramMessageId)
        dao.update(updated)
    }

    suspend fun getById(id: Long): ForwardedMessage? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }
}
