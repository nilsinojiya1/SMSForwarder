package online.thensoji.smsforwarder.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ForwardedMessage::class, SmsPart::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forwardedMessageDao(): ForwardedMessageDao
    abstract fun smsPartDao(): SmsPartDao
}
