package com.fonamp.feature.radio

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import app.cash.turbine.test
import com.fonamp.core.database.FakeFavoriteDao
import com.fonamp.core.database.FavoriteStation
import com.fonamp.core.player.FakePlayerManager
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.api.localMediaItem
import com.fonamp.provider.api.radioMediaItem
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice H RED: FavoritesViewModel — Room-backed favorites (stations-only),
 * play/remove + 10s snackbar undo, restart persistence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoritesViewModelTest {

    private class FakeRadioSource : Source {
        override val id = "radio-browser"
        override val kind = SourceKind.RADIO
        override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())

        override suspend fun search(q: String): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())

        override fun streamOf(item: AudioItem): MediaItem = radioMediaItem(item)
    }

    private fun favorite(
        uuid: String = "uuid-1",
        name: String = "Jazz FM",
    ) = FavoriteStation(
        stationUuid = uuid,
        name = name,
        streamUrl = "http://example.com/$uuid",
            artworkUri = null,
        country = "Germany",
        tagsCsv = "jazz",
        bitrate = 128,
        codec = "MP3",
        favoritedAt = 1L,
    )

    private fun vm(
        dao: FakeFavoriteDao = FakeFavoriteDao(),
        player: FakePlayerManager = FakePlayerManager(),
        scope: kotlinx.coroutines.CoroutineScope,
    ) = FavoritesViewModel(dao, FakeRadioSource(), player, scope)

    @Test
    fun `empty favorites renders Empty then Content on insert`() = runTest {
        val dao = FakeFavoriteDao()
        val viewModel = vm(dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            dao.upsert(favorite())
            val content = awaitItem() as FavoritesUiState.Content
            assertEquals(listOf("Jazz FM"), content.favorites.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `remove then undo restores the favorite`() = runTest {
        val dao = FakeFavoriteDao()
        dao.upsert(favorite())
        val viewModel = vm(dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            awaitItem() as FavoritesUiState.Content
            viewModel.remove(favorite())
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.pendingUndo.test {
            assertEquals("uuid-1", awaitItem()?.stationUuid)
            viewModel.undo()
            assertNull(awaitItem())
        }
        // The restored Content can only arrive after the undo re-upsert lands.
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            val content = awaitItem() as FavoritesUiState.Content
            assertEquals(listOf("uuid-1"), content.favorites.map { it.stationUuid })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(favorite(), dao.byId("uuid-1"))
    }

    @Test
    fun `undo window expires after ten seconds`() = runTest {
        val dao = FakeFavoriteDao()
        dao.upsert(favorite())
        val viewModel = vm(dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            awaitItem() as FavoritesUiState.Content
            viewModel.remove(favorite())
            awaitItem() as FavoritesUiState.Empty
            cancelAndIgnoreRemainingEvents()
        }
        testScheduler.advanceUntilIdle()
        assertEquals("uuid-1", viewModel.pendingUndo.value?.stationUuid)
        advanceTimeBy(10_001)
        assertNull(viewModel.pendingUndo.value)
        assertNull(dao.byId("uuid-1"))
    }

    @Test
    fun `favorites survive viewmodel restart`() = runTest {
        val dao = FakeFavoriteDao()
        dao.upsert(favorite())
        dao.upsert(favorite("uuid-2", "Night Wave"))
        val restarted = vm(dao, scope = backgroundScope)
        restarted.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            val content = awaitItem() as FavoritesUiState.Content
            assertEquals(2, content.favorites.size)
            // Stations-only: every row carries a station uuid and stream url.
            assertTrue(content.favorites.all { it.stationUuid.isNotBlank() && it.streamUrl.isNotBlank() })
        }
    }

    @Test
    fun `play favorite raises player with radio item`() = runTest {
        val player = FakePlayerManager()
        val viewModel = vm(player = player, scope = backgroundScope)
        viewModel.play(favorite())
        val playerState = player.state.value
        assertTrue(playerState.isPlaying)
        assertTrue(playerState.isLive)
        assertEquals("radio:uuid-1", playerState.queue.first().mediaId)
    }

    @Test
    fun `undo with nothing pending is a no-op`() = runTest {
        val dao = FakeFavoriteDao()
        val viewModel = vm(dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            viewModel.undo()
            expectNoEvents()
        }
        assertNull(viewModel.pendingUndo.value)
        assertNull(dao.byId("uuid-1"))
    }

    // "Fav + seam": sheet-heart path — toggleMediaItem + stationUuid + favoriteIds.

    private fun playingStation(
        uuid: String = "uuid-9",
        streamUrl: String = "http://example.com/uuid-9",
    ): MediaItem = radioMediaItem(
        AudioItem(
            sourceId = "radio-browser",
            stableId = uuid,
            title = "Night Wave",
            streamUri = streamUrl,
            stationUuid = uuid,
            country = "Germany",
            tags = listOf("jazz"),
        ),
    )

    @Test
    fun `sheet toggle adds the playing station then removes it`() = runTest {
        val dao = FakeFavoriteDao()
        val viewModel = vm(dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            viewModel.toggleMediaItem(playingStation())
            val content = awaitItem() as FavoritesUiState.Content
            assertTrue(content.favorites.any { it.stationUuid == "uuid-9" })
            assertEquals("Night Wave", dao.byId("uuid-9")?.name)
            assertEquals("http://example.com/uuid-9", dao.byId("uuid-9")?.streamUrl)
            assertTrue(viewModel.favoriteIds.value.contains("uuid-9"))
            viewModel.toggleMediaItem(playingStation())
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(dao.byId("uuid-9"))
        assertTrue(viewModel.favoriteIds.value.isEmpty())
    }

    @Test
    fun `sheet toggle on a local item is a no-op`() = runTest {
        val dao = FakeFavoriteDao()
        val viewModel = vm(dao, scope = backgroundScope)
        val local = localMediaItem(
            AudioItem(
                sourceId = "local",
                stableId = "1",
                title = "Blue Line",
                streamUri = "content://media/external/audio/media/1",
            ),
        )
        assertNull(local.stationUuid())
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            viewModel.toggleMediaItem(local)
            expectNoEvents()
        }
        assertTrue(viewModel.favoriteIds.value.isEmpty())
    }

    @Test
    fun `sheet toggle refuses an insert without a stream uri`() = runTest {
        val dao = FakeFavoriteDao()
        val viewModel = vm(dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is FavoritesUiState.Loading)
            assertTrue(awaitItem() is FavoritesUiState.Empty)
            viewModel.toggleMediaItem(playingStation(streamUrl = ""))
            expectNoEvents()
        }
        assertNull(dao.byId("uuid-9"))
        assertTrue(viewModel.favoriteIds.value.isEmpty())
    }

    @Test
    fun `stationUuid prefers extras and falls back to the radio mediaId`() {
        assertEquals("uuid-9", playingStation().stationUuid())
        val bare = MediaItem.Builder()
            .setMediaId("radio:bare-1")
            .setUri("http://example.com/bare-1")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Bare").build())
            .build()
        assertEquals("bare-1", bare.stationUuid())
    }
}
