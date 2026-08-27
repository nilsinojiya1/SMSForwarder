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

        assertEquals(3, AppDatabase.ALL_MIGRATIONS.size)
    }
}
