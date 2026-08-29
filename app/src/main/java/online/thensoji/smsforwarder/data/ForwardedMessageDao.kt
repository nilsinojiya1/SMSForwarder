package online.thensoji.smsforwarder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardedMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ForwardedMessage): Long

    @Update
    suspend fun update(message: ForwardedMessage)

    @Query("SELECT * FROM forwarded_messages ORDER BY timestamp DESC, id DESC")
    fun getAllMessagesFlow(): Flow<List<ForwardedMessage>>

    @Query("SELECT * FROM forwarded_messages ORDER BY timestamp DESC, id DESC")
    suspend fun getAllMessages(): List<ForwardedMessage>

    @Query("SELECT * FROM forwarded_messages WHERE isSent = 0 ORDER BY timestamp ASC, id ASC")
    suspend fun getUnsentMessages(): List<ForwardedMessage>

    @Query("SELECT * FROM forwarded_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ForwardedMessage?

    @Query("SELECT * FROM forwarded_messages WHERE systemSmsId = :systemSmsId LIMIT 1")
    suspend fun getBySystemSmsId(systemSmsId: Long): ForwardedMessage?

    @Query("SELECT EXISTS(SELECT 1 FROM forwarded_messages WHERE systemSmsId = :systemSmsId)")
    suspend fun existsBySystemSmsId(systemSmsId: Long): Boolean

    @Query("SELECT * FROM forwarded_messages WHERE timestamp BETWEEN :minTimestamp AND :maxTimestamp ORDER BY timestamp DESC")
    suspend fun getNearbyMessagesByTime(minTimestamp: Long, maxTimestamp: Long): List<ForwardedMessage>

    @Query("SELECT * FROM forwarded_messages WHERE sender = :sender AND timestamp BETWEEN :minTimestamp AND :maxTimestamp")
    suspend fun getNearbyMessages(sender: String?, minTimestamp: Long, maxTimestamp: Long): List<ForwardedMessage>

    @Delete
    suspend fun delete(message: ForwardedMessage)

    @Query("DELETE FROM forwarded_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM forwarded_messages")
    suspend fun clearAll()
}
