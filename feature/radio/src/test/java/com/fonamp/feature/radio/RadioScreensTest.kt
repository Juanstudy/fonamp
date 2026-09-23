package com.fonamp.feature.radio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.fonamp.core.database.FavoriteStation
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice H RED: radio screens behavior — Discover index + filter + segments,
 * station rows (bitrate/codec honesty, generic icons), 1-tap play, heart
 * toggle, Offline card + retry, Favorites play/remove + undo snackbar.
 * No screenshot tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private fun indexRow(kind: String, name: String, count: Int?) = AudioItem(
        sourceId = "radio-browser",
        stableId = "$kind:$name",
        title = name,
        subtitle = if (count != null) {
            "${kind.replaceFirstChar { it.uppercase() }} · $count stations"
        } else {
            null
        },
        streamUri = "",
    )

    private fun station(
        name: String,
        uuid: String = "uuid-$name",
        bitrate: Int? = 128,
        codec: String? = "MP3",
    ) = AudioItem(
        sourceId = "radio-browser",
        stableId = uuid,
        title = name,
        subtitle = "Germany",
        streamUri = "http://example.com/$uuid",
        bitrate = bitrate,
        codec = codec,
        stationUuid = uuid,
        country = "Germany",
    )

    private fun discover(
        query: String = "",
        segment: DiscoverSegment = DiscoverSegment.Countries,
        offline: Boolean = false,
        favorites: Set<String> = emptySet(),
        remoteResults: List<AudioItem> = emptyList(),
        remoteSearching: Boolean = false,
        remoteOffline: Boolean = false,
    ): RadioUiState.Discover {
        val countries = listOf(
            indexRow("country", "Germany", 10),
            indexRow("country", "France", 4),
        )
        val tags = listOf(indexRow("tag", "jazz", 7))
        val pool = if (segment == DiscoverSegment.Countries) countries else tags
        return RadioUiState.Discover(
            countries = countries,
            tags = tags,
            segment = segment,
            query = query,
            visible = pool.filter { it.title.contains(query, ignoreCase = true) },
            offline = offline,
            favorites = favorites,
            remoteResults = remoteResults,
            remoteSearching = remoteSearching,
            remoteOffline = remoteOffline,
        )
    }

    @Test
    fun `discover renders index rows with counts and filter narrows locally`() {
        var opened: BrowseQuery? = null
        compose.setContent {
            var query by remember { mutableStateOf("") }
            DiscoverScreen(
                state = discover(query = query),
                onQueryChange = { query = it },
                onSelectSegment = {},
                onOpenSelection = { opened = it },
                onRefresh = {},
            )
        }
        compose.onNodeWithText("Germany").assertIsDisplayed()
        compose.onNodeWithText("Country · 10 stations").assertIsDisplayed()
        compose.onNodeWithTag("filter-field").performTextInput("fran")
        compose.onNodeWithText("France").assertIsDisplayed()
        compose.onNodeWithText("Germany").assertDoesNotExist()
        // Tapping an index row opens the station list — it never plays directly.
        compose.onNodeWithText("France").performClick()
        assertEquals(BrowseQuery(country = "France"), opened)
    }

    @Test
    fun `segment switch swaps countries for tags`() {
        var selected: DiscoverSegment? = null
        compose.setContent {
            DiscoverScreen(
                state = discover(),
                onQueryChange = {},
                onSelectSegment = { selected = it },
                onOpenSelection = {},
                onRefresh = {},
            )
        }
        compose.onNodeWithText("Germany").assertIsDisplayed()
        compose.onNodeWithTag("segment-tags").performClick()
        assertEquals(DiscoverSegment.GenresTags, selected)
    }

    @Test
    fun `search results render below the index with play and favorite`() {
        var played: AudioItem? = null
        var toggled: AudioItem? = null
        val hit = station("Lofi Girl")
        compose.setContent {
            DiscoverScreen(
                state = discover(query = "lofi", remoteResults = listOf(hit)),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = {},
                onPlay = { played = it },
                onToggleFavorite = { toggled = it },
            )
        }
        compose.onNodeWithTag("discover-list").performScrollToNode(hasText("Lofi Girl"))
        compose.onNodeWithText("Search results").assertIsDisplayed()
        compose.onNodeWithTag("search-play-${hit.stationUuid}").performClick()
        assertEquals(hit, played)
        compose.onNodeWithTag("search-fav-${hit.stationUuid}").performClick()
        assertEquals(hit, toggled)
    }

    @Test
    fun `empty search shows empty state and short query shows no section`() {
        compose.setContent {
            DiscoverScreen(
                state = discover(query = "zzz-no-match"),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = {},
            )
        }
        compose.onNodeWithTag("discover-list").performScrollToNode(hasText("No stations found"))
        compose.onNodeWithText("No stations found").assertIsDisplayed()
        compose.onNodeWithText("Try another name").assertIsDisplayed()
    }

    @Test
    fun `short query shows no search section`() {
        compose.setContent {
            DiscoverScreen(
                state = discover(query = "l"),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = {},
            )
        }
        compose.onNodeWithTag("search-section").assertDoesNotExist()
    }

    @Test
    fun `searching shows loading shimmer`() {
        compose.setContent {
            DiscoverScreen(
                state = discover(query = "lofi", remoteSearching = true),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = {},
            )
        }
        compose.onNodeWithTag("discover-list").performScrollToNode(hasText("Search results"))
        compose.onNodeWithTag("loading-state").assertIsDisplayed()
    }

    @Test
    fun `failed search shows retry that retries`() {
        var retried = 0
        compose.setContent {
            DiscoverScreen(
                state = discover(query = "lofi", remoteOffline = true),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = {},
                onRetrySearch = { retried++ },
            )
        }
        compose.onNodeWithTag("discover-list").performScrollToNode(hasText("Search failed"))
        compose.onNodeWithText("Search failed").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `station rows show bitrate and codec only when present`() {
        var played: AudioItem? = null
        var toggled: AudioItem? = null
        val stations = listOf(
            station("Full Meta"),
            station("Bare", uuid = "uuid-bare", bitrate = null, codec = null),
        )
        compose.setContent {
            StationsScreen(
                state = RadioUiState.Stations(
                    selection = BrowseQuery(country = "Germany"),
                    title = "Germany",
                    stations = stations,
                    favorites = setOf("uuid-Full Meta"),
                ),
                onBack = {},
                onPlay = { played = it },
                onToggleFavorite = { toggled = it },
                onRefresh = {},
            )
        }
        compose.onNodeWithText("Full Meta").assertIsDisplayed()
        compose.onNodeWithText("128 kbps · MP3").assertIsDisplayed()
        compose.onNodeWithText("Bare").assertIsDisplayed()
        compose.onNodeWithTag("station-play-uuid-bare").performClick()
        assertEquals("Bare", played?.title)
        compose.onNodeWithTag("station-fav-uuid-bare").performClick()
        assertEquals("Bare", toggled?.title)
        // Radio badge marks the list as a radio surface.
        compose.onNodeWithTag("source-badge").assertIsDisplayed()
        compose.onNodeWithText("Radio").assertIsDisplayed()
    }

    @Test
    fun `offline card with retry shows alongside cached content`() {
        var refreshed = 0
        compose.setContent {
            DiscoverScreen(
                state = discover(offline = true),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = { refreshed++ },
            )
        }
        // The Curadas section sits above the filter: cached rows scroll into view.
        compose.onNodeWithTag("offline-card").assertIsDisplayed()
        compose.onNodeWithTag("refresh-button").performClick()
        assertEquals(1, refreshed)
        compose.onNodeWithText("Retry").performClick()
        assertEquals(2, refreshed)
        compose.onNodeWithTag("discover-list").performScrollToNode(hasText("Germany"))
        compose.onNodeWithText("Germany").assertIsDisplayed()
    }

    @Test
    fun `error state offers retry`() {
        var refreshed = 0
        compose.setContent {
            RadioRouteScreen(
                state = RadioUiState.Error("Directory unreachable"),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onBack = {},
                onPlay = {},
                onToggleFavorite = {},
                onRefresh = { refreshed++ },
            )
        }
        compose.onNodeWithText("Directory unreachable").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, refreshed)
    }

    @Test
    fun `favorites screen plays removes and offers undo`() {
        var played: FavoriteStation? = null
        var removed: FavoriteStation? = null
        var undone = false
        val fav = FavoriteStation(
            stationUuid = "uuid-1", name = "Jazz FM",
            streamUrl = "http://example.com/uuid-1", country = "Germany",
            artworkUri = null,
            tagsCsv = "jazz", bitrate = 128, codec = "MP3", favoritedAt = 1L,
        )
        compose.setContent {
            FavoritesScreen(
                state = FavoritesUiState.Content(listOf(fav)),
                pendingUndo = fav,
                onPlay = { played = it },
                onRemove = { removed = it },
                onUndo = { undone = true },
            )
        }
        compose.onNodeWithText("Jazz FM").assertIsDisplayed()
        compose.onNodeWithTag("favorite-play-uuid-1").performClick()
        assertEquals("uuid-1", played?.stationUuid)
        compose.onNodeWithTag("favorite-remove-uuid-1").performClick()
        assertEquals("uuid-1", removed?.stationUuid)
        compose.onNodeWithText("Undo").performClick()
        assertTrue(undone)
    }

    @Test
    fun `empty favorites shows message and no undo`() {
        compose.setContent {
            FavoritesScreen(
                state = FavoritesUiState.Empty,
                pendingUndo = null,
                onPlay = {},
                onRemove = {},
                onUndo = {},
            )
        }
        compose.onNodeWithText("No favorites yet").assertIsDisplayed()
        compose.onNodeWithText("Undo").assertDoesNotExist()
    }

    @Test
    fun `curated section shows tiles and preset rows with play plus heart`() {
        var opened: BrowseQuery? = null
        var played: AudioItem? = null
        var toggled: AudioItem? = null
        val curated = listOf(
            AudioItem(
                sourceId = "radio-browser",
                stableId = "uuid-groove",
                title = "Groove Salad",
                streamUri = "https://example.com/groove.mp3",
                stationUuid = "uuid-groove",
            ),
        )
        compose.setContent {
            DiscoverScreen(
                state = discover().copy(curated = curated),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = { opened = it },
                onRefresh = {},
                onPlay = { played = it },
                onToggleFavorite = { toggled = it },
            )
        }
        compose.onNodeWithTag("curated-section").assertIsDisplayed()
        compose.onNodeWithText("Curadas").assertIsDisplayed()
        // Tiles carry zero stream URLs — only tag strings (Req 11 scenario 2).
        CuratedTags.TILES.forEach { tile ->
            compose.onNodeWithTag("curated-tag-${tile.tag}").assertIsDisplayed()
        }
        // Lofi tile delegates to the existing browse(tag) path.
        compose.onNodeWithTag("curated-tag-lofi").performClick()
        assertEquals(BrowseQuery(tag = "lofi"), opened)
        // Preset row reuses the same play + heart testTags.
        compose.onNodeWithText("Groove Salad").assertIsDisplayed()
        compose.onNodeWithTag("station-play-uuid-groove").performClick()
        assertEquals("Groove Salad", played?.title)
        compose.onNodeWithTag("station-fav-uuid-groove").performClick()
        assertEquals("Groove Salad", toggled?.title)
    }

    @Test
    fun `curated preset without uuid shows no heart`() {
        compose.setContent {
            DiscoverScreen(
                state = discover().copy(
                    curated = listOf(
                        AudioItem(
                            sourceId = "radio-browser",
                            stableId = "curated:night-wave",
                            title = "Night Wave",
                            streamUri = "https://example.com/night.mp3",
                            stationUuid = null,
                        ),
                    ),
                ),
                onQueryChange = {},
                onSelectSegment = {},
                onOpenSelection = {},
                onRefresh = {},
            )
        }
        compose.onNodeWithTag("curated-section").assertIsDisplayed()
        compose.onNodeWithText("Night Wave").assertIsDisplayed()
        compose.onNodeWithTag("station-play-curated:night-wave").assertIsDisplayed()
        compose.onNodeWithTag("station-fav-null").assertDoesNotExist()
    }
}
