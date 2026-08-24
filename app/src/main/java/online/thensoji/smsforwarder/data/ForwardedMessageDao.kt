package online.thensoji.smsforwarder.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ForwardedMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ForwardedMessage): Long

    @Update
    suspend fun update(message: ForwardedMessage)

    @Query("SELECT * FROM forwarded_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ForwardedMessage>>

    @Query("SELECT * FROM forwarded_messages ORDER BY timestamp DESC")
    suspend fun getAllMessages(): List<ForwardedMessage>

    @Query("SELECT * FROM forwarded_messages WHERE isSent = 0 ORDER BY timestamp ASC")
    suspend fun getUnsentMessages(): List<ForwardedMessage>

    @Query("SELECT * FROM forwarded_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ForwardedMessage?

    @Query("SELECT * FROM forwarded_messages WHERE partsGroupingId = :groupingId ORDER BY id ASC")
    suspend fun getByGroupingId(groupingId: String): List<ForwardedMessage>

    @Delete
    suspend fun delete(message: ForwardedMessage)

    @Query("DELETE FROM forwarded_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM forwarded_messages")
    suspend fun clearAll()
}
