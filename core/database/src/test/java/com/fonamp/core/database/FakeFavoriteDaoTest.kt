package com.fonamp.core.database

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Slice D RED — hand-written `FakeFavoriteDao` honors the [FavoriteDao] contract
 * so ViewModel tests (feature/radio) can run Robolectric-free (design §8, no MockK).
 */
class FakeFavoriteDaoTest {

    @Test
    fun `fake round-trips upsert observe byId delete`() = runTest {
        val fake = FakeFavoriteDao()
        val station = FavoriteStation(
            stationUuid = "fake-1",
            name = "Fake Station",
            streamUrl = "http://example.com/fake-1",
            country = "France",
            tagsCsv = "pop",
            bitrate = 96,
            codec = "AAC",
            favoritedAt = 42L,
        )

        fake.upsert(station)
        assertEquals(station, fake.byId("fake-1"))
        fake.observeAll().test {
            assertEquals(listOf(station), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        fake.delete("fake-1")
        assertNull(fake.byId("fake-1"))
        fake.observeAll().test {
            assertEquals(emptyList<FavoriteStation>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fake undo re-upsert restores the row`() = runTest {
        val fake = FakeFavoriteDao()
        val station = FavoriteStation(
            stationUuid = "fake-2",
            name = "Undo Station",
            streamUrl = "http://example.com/fake-2",
            country = null,
            tagsCsv = null,
            bitrate = null,
            codec = null,
            favoritedAt = 7L,
        )
        fake.upsert(station)
        val heldForUndo = fake.byId("fake-2")!!
        fake.delete("fake-2")

        fake.upsert(heldForUndo)

        assertEquals(station, fake.byId("fake-2"))
    }
}
