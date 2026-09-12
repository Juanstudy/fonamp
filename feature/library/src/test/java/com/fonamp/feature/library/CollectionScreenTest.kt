package com.fonamp.feature.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.fonamp.provider.api.AudioItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice F RED: CollectionScreen behavior — Songs/Artists/Albums segments,
 * artist-pick filters to songs, tap-to-play index, Empty + Denied states.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionScreenTest {

    @get:Rule
    val compose = createComposeRule()

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

    private fun content(
        segment: LibrarySegment = LibrarySegment.Songs,
        artistFilter: String? = null,
        albumFilter: String? = null,
    ): LibraryUiState.Content {
        val songs = items()
        val visible = songs.filter { item ->
            (artistFilter == null || item.subtitle == artistFilter) &&
                (albumFilter == null || item.album == albumFilter)
        }
        return LibraryUiState.Content(
            songs = songs,
            artists = listOf("Ada", "Bo"),
            albums = listOf("Meadow", "Metro"),
            segment = segment,
            artistFilter = artistFilter,
            albumFilter = albumFilter,
            query = "",
            visibleSongs = visible,
        )
    }

    @Test
    fun `segments switch between songs artists albums`() {
        compose.setContent {
            var segment by remember { mutableStateOf(LibrarySegment.Songs) }
            CollectionScreen(
                state = content(segment = segment),
                onSelectSegment = { segment = it },
            )
        }
        compose.onNodeWithText("Blue Line").assertIsDisplayed()
        compose.onNodeWithTag("segment-artists").performClick()
        compose.onNodeWithText("Bo").assertIsDisplayed()
        compose.onNodeWithText("Blue Line").assertDoesNotExist()
        compose.onNodeWithTag("segment-albums").performClick()
        compose.onNodeWithText("Metro").assertIsDisplayed()
    }

    @Test
    fun `tapping an artist reports the pick`() {
        var picked: String? = "unset"
        compose.setContent {
            CollectionScreen(
                state = content(segment = LibrarySegment.Artists),
                onSelectArtist = { picked = it },
            )
        }
        compose.onNodeWithText("Ada").performClick()
        assertEquals("Ada", picked)
    }

    @Test
    fun `artist filter narrows the song list`() {
        compose.setContent { CollectionScreen(state = content(artistFilter = "Ada")) }
        compose.onNodeWithText("Blue Line").assertIsDisplayed()
        compose.onNodeWithText("Night Static").assertIsDisplayed()
        compose.onNodeWithText("Field Hymn").assertDoesNotExist()
    }

    @Test
    fun `tapping a song reports its visible index`() {
        var played = -1
        compose.setContent {
            CollectionScreen(state = content(), onPlayAt = { played = it })
        }
        compose.onNodeWithText("Night Static").performClick()
        assertEquals(1, played)
    }

    @Test
    fun `empty state explains plus add-music hint`() {
        compose.setContent { CollectionScreen(state = LibraryUiState.Empty) }
        compose.onNodeWithText("No music found").assertIsDisplayed()
        compose.onNodeWithText("Add audio files to your device to build your collection.").assertIsDisplayed()
    }

    @Test
    fun `denied state offers grant again`() {
        var granted = false
        compose.setContent {
            CollectionScreen(state = LibraryUiState.Denied, onGrantPermission = { granted = true })
        }
        compose.onNodeWithText("Grant again").performClick()
        assertEquals(true, granted)
    }
}
