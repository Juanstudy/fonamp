package com.fonamp.feature.radio

import androidx.media3.common.MediaItem
import app.cash.turbine.test
import com.fonamp.core.database.FakeFavoriteDao
import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.DirectoryEntry
import com.fonamp.core.network.DirectoryIndex
import com.fonamp.core.network.StationDto
import com.fonamp.core.player.FakePlayerManager
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.api.radioMediaItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice H RED: RadioViewModel Turbine flows — Loading/Empty/Offline-with-cache/
 * ErrorRetry, local index filter with zero per-keystroke network, Discover→
 * stations→play raising the player with a radio item, favorite round-trip +
 * restart, index rows never played, no tops/random/name-search surface.
 *
 * Robolectric only because `streamOf` builds MediaItems; the VM talks to
 * `Source`/`FavoriteDao`/`PlayerManager` interfaces (hand fakes, no MockK).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioViewModelTest {

    private class FakeRadioSource(
        var indexResult: SourceResult<List<AudioItem>> = SourceResult.Ok(emptyList()),
        var stationsResult: SourceResult<List<AudioItem>> = SourceResult.Ok(emptyList()),
    ) : Source {
        override val id = "radio-browser"
        override val kind = SourceKind.RADIO
        var browseCalls = 0
        var searchCalls = 0
        val queries = mutableListOf<BrowseQuery>()
        override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> {
            browseCalls++
            queries += query
            return if (query.country == null && query.genre == null && query.tag == null) {
                indexResult
            } else {
                stationsResult
            }
        }

        override suspend fun search(q: String): SourceResult<List<AudioItem>> {
            searchCalls++
            return SourceResult.Ok(emptyList())
        }

        override fun streamOf(item: AudioItem): MediaItem = radioMediaItem(item)
    }

    private fun station(
        name: String,
        uuid: String = "uuid-$name",
        country: String? = "Germany",
        bitrate: Int? = 128,
        codec: String? = "MP3",
        tags: List<String> = listOf("jazz"),
    ) = AudioItem(
        sourceId = "radio-browser",
        stableId = uuid,
        title = name,
        subtitle = country,
        streamUri = "http://example.com/$uuid",
        bitrate = bitrate,
        codec = codec,
        stationUuid = uuid,
        country = country,
        tags = tags,
    )

    private fun cachedIndex() = DirectoryIndex(
        countries = listOf(DirectoryEntry("Germany", 10), DirectoryEntry("France", 4)),
        tags = listOf(DirectoryEntry("jazz", 7)),
    )

    private fun vm(
        source: FakeRadioSource,
        cache: DirectoryCache = DirectoryCache(),
        dao: FakeFavoriteDao = FakeFavoriteDao(),
        player: FakePlayerManager = FakePlayerManager(),
        scope: kotlinx.coroutines.CoroutineScope,
    ) = RadioViewModel(source, dao, player, cache, scope)

    @Test
    fun `fresh cached index served with zero network`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource()
        vm(source, cache, scope = backgroundScope).state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val first = awaitItem()
            assertTrue(first is RadioUiState.Discover)
            assertEquals(0, source.browseCalls)
            val discover = first as RadioUiState.Discover
            assertEquals(listOf("Germany", "France"), discover.countries.map { it.title })
            assertEquals("Country · 10 stations", discover.countries.first().subtitle)
            assertEquals(listOf("jazz"), discover.tags.map { it.title })
            assertFalse(discover.offline)
        }
    }

    @Test
    fun `empty store renders Empty`() = runTest {
        val source = FakeRadioSource(indexResult = SourceResult.Ok(emptyList()))
        vm(source, scope = backgroundScope).state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            assertTrue(awaitItem() is RadioUiState.Empty)
        }
    }

    @Test
    fun `fetch failure with no cache renders ErrorRetry`() = runTest {
        val source = FakeRadioSource(indexResult = SourceResult.Fail(SourceError.Offline))
        vm(source, scope = backgroundScope).state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val error = awaitItem() as RadioUiState.Error
            assertTrue(error.message.isNotBlank())
        }
    }

    @Test
    fun `stale cache shown immediately then fresh fetch replaces`() = runTest {
        var now = 0L
        val cache = DirectoryCache(nowMs = { now })
        cache.putIndex(cachedIndex())
        now += 25L * 60 * 60 * 1000 // 25h — stale
        val fresh = listOf(
            AudioItem("radio-browser", "country:Spain", "Spain", "Country · 3 stations", streamUri = ""),
        )
        val source = FakeRadioSource(indexResult = SourceResult.Ok(fresh))
        vm(source, cache, scope = backgroundScope).state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val stale = awaitItem() as RadioUiState.Discover
            assertEquals(listOf("Germany", "France"), stale.countries.map { it.title })
            val updated = awaitItem() as RadioUiState.Discover
            assertEquals(listOf("Spain"), updated.countries.map { it.title })
            assertFalse(updated.offline)
        }
    }

    @Test
    fun `failed refresh keeps stale cache with offline card and retry does not clear cache`() = runTest {
        var now = 0L
        val cache = DirectoryCache(nowMs = { now })
        cache.putIndex(cachedIndex())
        now += 25L * 60 * 60 * 1000
        val source = FakeRadioSource(indexResult = SourceResult.Fail(SourceError.Offline))
        val player = FakePlayerManager()
        val dao = FakeFavoriteDao()
        val viewModel = vm(source, cache, dao, player, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val first = awaitItem() as RadioUiState.Discover
            assertEquals(listOf("Germany", "France"), first.countries.map { it.title })
            val offline = awaitItem() as RadioUiState.Discover
            assertTrue(offline.offline)
            assertEquals(listOf("Germany", "France"), offline.countries.map { it.title })
            // Retry re-attempts the fetch without clearing the cache.
            viewModel.refresh()
            val refreshing = awaitItem() as RadioUiState.Discover
            assertTrue(refreshing.refreshing)
            val retried = awaitItem() as RadioUiState.Discover
            assertTrue(retried.offline)
            assertFalse(retried.refreshing)
            assertEquals(2, source.browseCalls)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(cache.getIndex() != null)
    }

    @Test
    fun `local index filter narrows countries with zero per-keystroke network`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource()
        val viewModel = vm(source, cache, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val loaded = awaitItem() as RadioUiState.Discover
            assertEquals(2, loaded.visible.size)
            viewModel.setQuery("ger")
            val filtered = awaitItem() as RadioUiState.Discover
            assertEquals(listOf("Germany"), filtered.visible.map { it.title })
            // Counts survive filtering.
            assertEquals("Country · 10 stations", filtered.visible.first().subtitle)
            viewModel.setQuery("ger!")
            val none = awaitItem() as RadioUiState.Discover
            assertTrue(none.visible.isEmpty())
            assertEquals(0, source.browseCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `segment switch shows tags without network`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource()
        val viewModel = vm(source, cache, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            awaitItem() as RadioUiState.Discover
            viewModel.selectSegment(DiscoverSegment.GenresTags)
            val tags = awaitItem() as RadioUiState.Discover
            assertEquals(listOf("jazz"), tags.visible.map { it.title })
            assertEquals(0, source.browseCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `three-tap discover to stations to play raises player with radio item`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val stations = listOf(station("Jazz FM"), station("Night Wave"))
        val source = FakeRadioSource(stationsResult = SourceResult.Ok(stations))
        val player = FakePlayerManager()
        val viewModel = vm(source, cache, player = player, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            awaitItem() as RadioUiState.Discover // tap 1: Radio tab lands here
            viewModel.openSelection(BrowseQuery(country = "Germany"), "Germany") // tap 2
                assertTrue(awaitItem() is RadioUiState.Loading)
            val list = awaitItem() as RadioUiState.Stations
            assertEquals("Germany", list.title)
            assertEquals(2, list.stations.size)
            viewModel.playStation(list.stations[0]) // tap 3
            cancelAndIgnoreRemainingEvents()
        }
        val playerState = player.state.value
        assertTrue(playerState.isPlaying)
        assertEquals(1, playerState.queue.size)
        assertTrue(playerState.isLive)
        assertEquals("radio:uuid-Jazz FM", playerState.queue.first().mediaId)
    }

    @Test
    fun `index rows with empty streamUri are never played`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource()
        val player = FakePlayerManager()
        val viewModel = vm(source, cache, player = player, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val discover = awaitItem() as RadioUiState.Discover
            viewModel.playStation(discover.countries.first())
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(player.state.value.isPlaying)
        assertTrue(player.state.value.queue.isEmpty())
    }

    @Test
    fun `favorite round-trip survives restart via dao`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource()
        val dao = FakeFavoriteDao()
        val item = station("Jazz FM")
        val first = vm(source, cache, dao, scope = backgroundScope)
        first.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            awaitItem() as RadioUiState.Discover
            first.toggleFavorite(item)
            val fav = awaitItem() as RadioUiState.Discover
            assertTrue(fav.favorites.contains(item.stationUuid))
            cancelAndIgnoreRemainingEvents()
        }
        // Restart: a new VM over the same DAO still sees the favorite.
        val second = vm(source, cache, dao, scope = backgroundScope)
        second.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val reloaded = awaitItem() as RadioUiState.Discover
            assertTrue(reloaded.favorites.contains(item.stationUuid))
            second.toggleFavorite(item)
            val removed = awaitItem() as RadioUiState.Discover
            assertFalse(removed.favorites.contains(item.stationUuid))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `index rows without stationUuid cannot be favorited`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource()
        val dao = FakeFavoriteDao()
        val viewModel = vm(source, cache, dao, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            val discover = awaitItem() as RadioUiState.Discover
            viewModel.toggleFavorite(discover.countries.first())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun `station list failure without cache renders Error with selection`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource(stationsResult = SourceResult.Fail(SourceError.Timeout))
        val viewModel = vm(source, cache, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            awaitItem() as RadioUiState.Discover
            viewModel.openSelection(BrowseQuery(country = "Germany"), "Germany")
                assertTrue(awaitItem() is RadioUiState.Loading)
            val error = awaitItem() as RadioUiState.Error
            assertEquals(BrowseQuery(country = "Germany"), error.selection)
            viewModel.backToDiscover()
            assertTrue(awaitItem() is RadioUiState.Discover)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `directory search is never used — no tops random or name-search surface`() = runTest {
        val cache = DirectoryCache()
        cache.putIndex(cachedIndex())
        val source = FakeRadioSource(
            stationsResult = SourceResult.Ok(listOf(station("Jazz FM"))),
        )
        val viewModel = vm(source, cache, scope = backgroundScope)
        viewModel.state.test {
            assertTrue(awaitItem() is RadioUiState.Loading)
            awaitItem() as RadioUiState.Discover
            viewModel.setQuery("jazz")
            awaitItem() as RadioUiState.Discover
            viewModel.selectSegment(DiscoverSegment.GenresTags)
            awaitItem() as RadioUiState.Discover
            viewModel.openSelection(BrowseQuery(tag = "jazz"), "jazz")
                assertTrue(awaitItem() is RadioUiState.Loading)
            awaitItem() as RadioUiState.Stations
            viewModel.refresh()
            val refreshing = awaitItem() as RadioUiState.Stations
            assertTrue(refreshing.refreshing)
            val reloaded = awaitItem() as RadioUiState.Stations
            assertFalse(reloaded.refreshing)
            assertEquals(1, reloaded.stations.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(0, source.searchCalls)
        // Fresh index served from cache (zero network) + stations fetch + refresh.
        assertEquals(2, source.browseCalls)
    }

}
