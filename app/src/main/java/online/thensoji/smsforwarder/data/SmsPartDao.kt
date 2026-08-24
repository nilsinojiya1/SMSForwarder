package online.thensoji.smsforwarder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmsPartDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: SmsPart): Long

    @Query("SELECT * FROM sms_parts WHERE sender = :sender AND refNumber = :refNumber ORDER BY partIndex ASC")
    suspend fun getPartsForRef(sender: String, refNumber: Int): List<SmsPart>

    @Query("DELETE FROM sms_parts WHERE sender = :sender AND refNumber = :refNumber")
    suspend fun deletePartsForRef(sender: String, refNumber: Int)

    @Query("DELETE FROM sms_parts WHERE id = :id")
    suspend fun deletePartById(id: Long)
}

