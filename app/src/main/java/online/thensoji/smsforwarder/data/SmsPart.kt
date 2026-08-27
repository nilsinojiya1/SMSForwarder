package online.thensoji.smsforwarder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sms_parts",
    indices = [
        Index(value = ["sender", "refNumber", "partIndex"], unique = true)
    ]
)
data class SmsPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String,
    val simSlot: Int,
    val timestamp: Long,
    val refNumber: Int,
    val totalParts: Int,
    val partIndex: Int,
    val partBody: String,
    val receivedAt: Long = System.currentTimeMillis()
)


