package com.fonamp.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import com.fonamp.provider.api.AudioItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PR2 mirror: song rows render the real local cover when present and the
 * generic icon when absent; album rows reuse their first song's cover;
 * artists keep the Person icon (no thumbs).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CollectionArtworkTest {

    @get:Rule
    val compose = createComposeRule()

    private fun songs() = listOf(
        AudioItem(
            sourceId = "local", stableId = "1", title = "With Cover",
            subtitle = "Ada", album = "Metro", durationMs = 183_000L,
            streamUri = "content://media/external/audio/media/1",
            artworkUri = "content://media/external/audio/albumart/7",
        ),
        AudioItem(
            sourceId = "local", stableId = "2", title = "Without Cover",
            subtitle = "Ada", album = "Meadow", durationMs = 201_000L,
            streamUri = "content://media/external/audio/media/2",
            artworkUri = null,
        ),
    )

    private fun content(segment: LibrarySegment = LibrarySegment.Songs): LibraryUiState.Content {
        val items = songs()
        return LibraryUiState.Content(
            songs = items,
            artists = listOf("Ada"),
            albums = listOf("Metro", "Meadow"),
            segment = segment,
            artistFilter = null,
            albumFilter = null,
            query = "",
            visibleSongs = items,
        )
    }

    @Test
    fun `song rows show image for cover and fallback without`() {
        compose.setContent { CollectionScreen(state = content()) }

        compose.onNodeWithText("With Cover").assertIsDisplayed()
        compose.onNodeWithText("Without Cover").assertIsDisplayed()
        compose.onAllNodesWithTag("artwork-image", useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithTag("artwork-fallback", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `album rows reuse first song cover`() {
        compose.setContent { CollectionScreen(state = content(segment = LibrarySegment.Albums)) }

        compose.onNodeWithText("Metro").assertIsDisplayed()
        compose.onNodeWithText("Meadow").assertIsDisplayed()
        // Metro's first song has cover -> image; Meadow's has none -> fallback.
        compose.onAllNodesWithTag("artwork-image", useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithTag("artwork-fallback", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `artists keep person icon with no artwork thumbs`() {
        compose.setContent { CollectionScreen(state = content(segment = LibrarySegment.Artists)) }

        compose.onNodeWithText("Ada").assertIsDisplayed()
        compose.onAllNodesWithTag("artwork-image", useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithTag("artwork-fallback", useUnmergedTree = true).assertCountEquals(0)
    }
}
