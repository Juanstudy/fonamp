package com.fonamp.feature.library

import androidx.media3.common.MediaItem
import app.cash.turbine.test
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice F RED: LibraryViewModel states — Loading/Empty/Denied/ErrorRetry via
 * Turbine, FakeSource + FakeLibraryPlayer seams, cheap local text filter.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryViewModelTest {

    private class FakeSource(
        var result: SourceResult<List<AudioItem>> = SourceResult.Ok(emptyList()),
    ) : Source {
        override val id = "local"
        override val kind = SourceKind.LOCAL
        var browseCalls = 0
        override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> {
            browseCalls++
            return result
        }

        override suspend fun search(q: String): SourceResult<List<AudioItem>> = result

        override fun streamOf(item: AudioItem): MediaItem =
            MediaItem.Builder().setMediaId("local:${item.stableId}").setUri(item.streamUri).build()
    }

    private class FakeLibraryPlayer : LibraryPlayer {
        val calls = mutableListOf<Pair<List<MediaItem>, Int>>()
        override fun playQueue(items: List<MediaItem>, index: Int) {
            calls.add(items to index)
        }
    }

    private fun items() = listOf(
        AudioItem(
            sourceId = "local", stableId = "1", title = "Blue Line",
            subtitle = "Ada", album = "Metro", durationMs = 183_000L,
            streamUri = "content://media/external/audio/media/1",
        ),
        AudioItem(
            sourceId = "local", stableId = "2", title = "Night Static",
            subtitle = "Ada", album = "Metro", durationMs = 201_000L,
            streamUri = "content://media/external/audio/media/2",
        ),
        AudioItem(
            sourceId = "local", stableId = "3", title = "Field Hymn",
            subtitle = "Bo", album = "Meadow", durationMs = 150_000L,
            streamUri = "content://media/external/audio/media/3",
        ),
    )

    @Test
    fun `loads content grouped into artists and albums`() = runTest {
        val vm = LibraryViewModel(FakeSource(SourceResult.Ok(items())), FakeLibraryPlayer(), true, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Loading)
            val content = awaitItem() as LibraryUiState.Content
            assertEquals(3, content.songs.size)
            assertEquals(listOf("Ada", "Bo"), content.artists)
            assertEquals(listOf("Meadow", "Metro"), content.albums)
            assertEquals(3, content.visibleSongs.size)
        }
    }

    @Test
    fun `empty store renders Empty`() = runTest {
        val vm = LibraryViewModel(FakeSource(SourceResult.Ok(emptyList())), FakeLibraryPlayer(), true, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Loading)
            assertTrue(awaitItem() is LibraryUiState.Empty)
        }
    }

    @Test
    fun `permission denied renders Denied without touching the source`() = runTest {
        val source = FakeSource(SourceResult.Ok(items()))
        val vm = LibraryViewModel(source, FakeLibraryPlayer(), false, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Denied)
        }
        assertEquals(0, source.browseCalls)
    }

    @Test
    fun `failure renders Error and retry recovers`() = runTest {
        val source = FakeSource(SourceResult.Fail(SourceError.Offline))
        val vm = LibraryViewModel(source, FakeLibraryPlayer(), true, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Loading)
            assertTrue(awaitItem() is LibraryUiState.Error)
            source.result = SourceResult.Ok(items())
            vm.refresh()
            assertTrue(awaitItem() is LibraryUiState.Loading)
            assertTrue(awaitItem() is LibraryUiState.Content)
        }
    }

    @Test
    fun `artist and album filters narrow songs and clear restores`() = runTest {
        val vm = LibraryViewModel(FakeSource(SourceResult.Ok(items())), FakeLibraryPlayer(), true, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Loading)
            var content = awaitItem() as LibraryUiState.Content
            assertEquals(3, content.visibleSongs.size)
            vm.selectArtist("Ada")
            content = awaitItem() as LibraryUiState.Content
            assertEquals(listOf("Blue Line", "Night Static"), content.visibleSongs.map { it.title })
            vm.selectArtist(null)
            content = awaitItem() as LibraryUiState.Content
            assertEquals(3, content.visibleSongs.size)
            vm.selectAlbum("Meadow")
            content = awaitItem() as LibraryUiState.Content
            assertEquals(listOf("Field Hymn"), content.visibleSongs.map { it.title })
            vm.clearFilters()
            content = awaitItem() as LibraryUiState.Content
            assertEquals(3, content.visibleSongs.size)
        }
    }

    @Test
    fun `text filter is local with zero extra source reads`() = runTest {
        val source = FakeSource(SourceResult.Ok(items()))
        val vm = LibraryViewModel(source, FakeLibraryPlayer(), true, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Loading)
            awaitItem() as LibraryUiState.Content
            vm.setQuery("metro")
            val filtered = awaitItem() as LibraryUiState.Content
            assertEquals(2, filtered.visibleSongs.size)
            vm.setQuery("hymn")
            val narrowed = awaitItem() as LibraryUiState.Content
            assertEquals(listOf("Field Hymn"), narrowed.visibleSongs.map { it.title })
        }
        assertEquals(1, source.browseCalls)
    }

    @Test
    fun `tap to play hands the visible queue plus index to the player`() = runTest {
        val player = FakeLibraryPlayer()
        val vm = LibraryViewModel(FakeSource(SourceResult.Ok(items())), player, true, this)
        vm.state.test {
            assertTrue(awaitItem() is LibraryUiState.Loading)
            awaitItem() as LibraryUiState.Content
            vm.selectArtist("Ada")
            awaitItem() as LibraryUiState.Content
            vm.playAt(1)
        }
        assertEquals(1, player.calls.size)
        val (queue, index) = player.calls.single()
        assertEquals(listOf("local:1", "local:2"), queue.map { it.mediaId })
        assertEquals(1, index)
    }
}
