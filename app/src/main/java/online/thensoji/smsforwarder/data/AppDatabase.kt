package online.thensoji.smsforwarder.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ForwardedMessage::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun forwardedMessageDao(): ForwardedMessageDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sms_parts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sender` TEXT NOT NULL,
                        `simSlot` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `refNumber` INTEGER NOT NULL,
                        `totalParts` INTEGER NOT NULL,
                        `partIndex` INTEGER NOT NULL,
                        `partBody` TEXT NOT NULL,
                        `receivedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `forwarded_messages` ADD COLUMN `sentTimestamp` INTEGER")
                db.execSQL("ALTER TABLE `forwarded_messages` ADD COLUMN `delayMillis` INTEGER")
                db.execSQL("ALTER TABLE `forwarded_messages` ADD COLUMN `errorMessage` TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    DELETE FROM `sms_parts` 
                    WHERE `id` NOT IN (
                        SELECT MAX(`id`) 
                        FROM `sms_parts` 
                        GROUP BY `sender`, `refNumber`, `partIndex`
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sms_parts_sender_refNumber_partIndex` " +
                            "ON `sms_parts` (`sender`, `refNumber`, `partIndex`)"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `forwarded_messages` ADD COLUMN `systemSmsId` INTEGER")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_forwarded_messages_systemSmsId` " +
                            "ON `forwarded_messages` (`systemSmsId`)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `sms_parts`")
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6
        )
    }
}

