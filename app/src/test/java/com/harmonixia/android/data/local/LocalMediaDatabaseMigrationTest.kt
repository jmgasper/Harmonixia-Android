package com.harmonixia.android.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaDatabaseMigrationTest {

    @Test
    fun `migration 1_2 adds local album first track path column`() {
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        LocalMediaDatabase.MIGRATION_1_2.migrate(database)

        verify(exactly = 1) {
            database.execSQL("ALTER TABLE local_albums ADD COLUMN firstTrackPath TEXT")
        }
    }

    @Test
    fun `migration 2_3 creates cached albums table and indexes`() {
        val statements = mutableListOf<String>()
        val database = mockk<SupportSQLiteDatabase>()
        every { database.execSQL(capture(statements)) } returns Unit

        LocalMediaDatabase.MIGRATION_2_3.migrate(database)

        assertEquals(3, statements.size)
        assertTrue(statements[0].contains("CREATE TABLE IF NOT EXISTS cached_albums"))
        assertTrue(statements[0].contains("PRIMARY KEY(cacheKey)"))
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_cached_albums_sortIndex ON cached_albums(sortIndex)",
            statements[1]
        )
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_cached_albums_syncId ON cached_albums(syncId)",
            statements[2]
        )
    }

    @Test
    fun `migration 3_4 creates cached artists table and indexes`() {
        val statements = mutableListOf<String>()
        val database = mockk<SupportSQLiteDatabase>()
        every { database.execSQL(capture(statements)) } returns Unit

        LocalMediaDatabase.MIGRATION_3_4.migrate(database)

        assertEquals(3, statements.size)
        assertTrue(statements[0].contains("CREATE TABLE IF NOT EXISTS cached_artists"))
        assertTrue(statements[0].contains("PRIMARY KEY(cacheKey)"))
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_cached_artists_sortIndex ON cached_artists(sortIndex)",
            statements[1]
        )
        assertEquals(
            "CREATE INDEX IF NOT EXISTS index_cached_artists_syncId ON cached_artists(syncId)",
            statements[2]
        )
    }
}
