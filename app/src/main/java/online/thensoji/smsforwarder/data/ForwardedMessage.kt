package online.thensoji.smsforwarder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "forwarded_messages")
data class ForwardedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String?,
    val body: String,
    val timestamp: Long,
    val isSent: Boolean = false,
    val sentTimestamp: Long? = null,
    val delayMillis: Long? = null,
    val telegramMessageId: String? = null,
    val partsGroupingId: String? = null,
    val errorMessage: String? = null
)
