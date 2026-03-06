package online.thensoji.smsforwarder.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ForwardedMessage::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forwardedMessageDao(): ForwardedMessageDao
}
