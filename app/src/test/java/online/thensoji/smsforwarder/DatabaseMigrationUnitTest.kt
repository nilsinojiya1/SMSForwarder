package online.thensoji.smsforwarder

import online.thensoji.smsforwarder.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseMigrationUnitTest {

    @Test
    fun testMigrationsDefined() {
        assertNotNull(AppDatabase.MIGRATION_1_2)
        assertEquals(1, AppDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, AppDatabase.MIGRATION_1_2.endVersion)

        assertNotNull(AppDatabase.MIGRATION_2_3)
        assertEquals(2, AppDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, AppDatabase.MIGRATION_2_3.endVersion)

        assertNotNull(AppDatabase.MIGRATION_3_4)
        assertEquals(3, AppDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, AppDatabase.MIGRATION_3_4.endVersion)

        assertNotNull(AppDatabase.MIGRATION_4_5)
        assertEquals(4, AppDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, AppDatabase.MIGRATION_4_5.endVersion)

        assertNotNull(AppDatabase.MIGRATION_5_6)
        assertEquals(5, AppDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, AppDatabase.MIGRATION_5_6.endVersion)

        assertEquals(5, AppDatabase.ALL_MIGRATIONS.size)
    }
}
