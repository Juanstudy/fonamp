package com.fonamp.core.database

import android.content.Context
import androidx.room.Room
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Slice D RED — FavoriteDao contract (radio Req 4: stations-only local favorites
 * with undo; design §5 + §8: in-memory Room).
 *
 * Covers: upsert/observe/delete/byId round-trip, undo re-upsert restoring the
 * full row (ViewModel holds the row, not just the id).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoriteDaoTest {

    private lateinit var db: FonampDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setup() {
        val context: Context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, FonampDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `upsert then observeAll emits the row with all fields`() = runTest {
        val station = favoriteRow(uuid = "uuid-1")

        dao.upsert(station)

        dao.observeAll().test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals(station, rows.single())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `byId returns the row and null for unknown uuid`() = runTest {
        val station = favoriteRow(uuid = "uuid-2")
        dao.upsert(station)

        assertEquals(station, dao.byId("uuid-2"))
        assertNull(dao.byId("no-such-uuid"))
    }

    @Test
    fun `upsert with same uuid replaces the row`() = runTest {
        dao.upsert(favoriteRow(uuid = "uuid-3", name = "Old Name"))
        dao.upsert(favoriteRow(uuid = "uuid-3", name = "New Name"))

        dao.observeAll().test {
            val rows = awaitItem()
            assertEquals(1, rows.size)
            assertEquals("New Name", rows.single().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes the row from observeAll and byId`() = runTest {
        val station = favoriteRow(uuid = "uuid-4")
        dao.upsert(station)
        dao.delete("uuid-4")

        assertNull(dao.byId("uuid-4"))
        dao.observeAll().test {
            assertEquals(emptyList<FavoriteStation>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `undo re-upsert of the deleted row restores it fully`() = runTest {
        // ViewModel holds the deleted row for the 10s snackbar window (design §5).
        val station = favoriteRow(uuid = "uuid-5")
        dao.upsert(station)
        val heldForUndo = dao.byId("uuid-5")!!
        dao.delete("uuid-5")
        assertNull(dao.byId("uuid-5"))

        dao.upsert(heldForUndo)

        assertEquals(station, dao.byId("uuid-5"))
    }

    @Test
    fun `nullable columns round-trip as null`() = runTest {
        val station = FavoriteStation(
            stationUuid = "uuid-6",
            name = "Minimal",
            streamUrl = "http://example.com/stream",
            country = null,
            tagsCsv = null,
            bitrate = null,
            codec = null,
            favoritedAt = 123L,
        )
        dao.upsert(station)

        assertEquals(station, dao.byId("uuid-6"))
    }

    private fun favoriteRow(uuid: String, name: String = "Station $uuid") = FavoriteStation(
        stationUuid = uuid,
        name = name,
        streamUrl = "http://example.com/$uuid",
        country = "Germany",
        tagsCsv = "jazz,swing",
        bitrate = 128,
        codec = "MP3",
        favoritedAt = 1_700_000_000_000L,
    )
}
