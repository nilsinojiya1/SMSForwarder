package online.thensoji.smsforwarder.data

import androidx.room.*

@Dao
interface ForwardedMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ForwardedMessage): Long

    @Update
    suspend fun update(message: ForwardedMessage)

    @Query("SELECT * FROM forwarded_messages WHERE isSent = 0 ORDER BY timestamp ASC")
    suspend fun getUnsentMessages(): List<ForwardedMessage>

    @Query("SELECT * FROM forwarded_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ForwardedMessage?

    @Query("SELECT * FROM forwarded_messages WHERE partsGroupingId = :groupingId ORDER BY id ASC")
    suspend fun getByGroupingId(groupingId: String): List<ForwardedMessage>

    @Delete
    suspend fun delete(message: ForwardedMessage)
}
