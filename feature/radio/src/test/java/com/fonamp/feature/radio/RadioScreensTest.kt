package com.fonamp.feature.radio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        compose.onNodeWithText("Germany").assertIsDisplayed()
        compose.onNodeWithTag("offline-card").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, refreshed)
        compose.onNodeWithTag("refresh-button").performClick()
        assertEquals(2, refreshed)
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
}
