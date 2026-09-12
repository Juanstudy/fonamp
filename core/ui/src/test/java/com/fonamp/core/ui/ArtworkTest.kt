package com.fonamp.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PR2 mirror: artwork states for mini-player and sheet.
 *
 * Real cover (non-null uri) renders the image node; null renders the
 * generic-icon fallback. No crash, no fabricated placeholder either way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArtworkTest {

    @get:Rule
    val rule = createComposeRule()

    // Artwork helper (bisect) --------------------------------------------------

    @Test
    fun `artwork helper with uri shows image not fallback`() {
        rule.setContent {
            FonampTheme {
                AlbumArtwork(
                    artworkUri = "content://media/external/audio/albumart/7",
                    imageTestTag = "mini-artwork-image",
                    fallbackTestTag = "mini-artwork-fallback",
                )
            }
        }

        rule.onNodeWithTag("mini-artwork-image").assertIsDisplayed()
        rule.onNodeWithTag("mini-artwork-fallback").assertDoesNotExist()
    }

    @Test
    fun `artwork helper without uri shows generic fallback`() {
        rule.setContent {
            FonampTheme {
                AlbumArtwork(
                    artworkUri = null,
                    imageTestTag = "mini-artwork-image",
                    fallbackTestTag = "mini-artwork-fallback",
                )
            }
        }

        rule.onNodeWithTag("mini-artwork-fallback").assertIsDisplayed()
        rule.onNodeWithTag("mini-artwork-image").assertDoesNotExist()
    }

    // MiniPlayer -------------------------------------------------------------

    @Test
    fun `mini player with artwork shows image not fallback`() {
        rule.setContent {
            FonampTheme {
                MiniPlayer(
                    state = MiniPlayerState(
                        title = "Blue Line",
                        subtitle = "Ada",
                        isPlaying = true,
                        source = SourceBadgeKind.LOCAL,
                        artworkUri = "content://media/external/audio/albumart/7",
                    ),
                    onTogglePlayPause = {},
                    onClose = {},
                )
            }
        }

        rule.onNodeWithTag("mini-artwork-image", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("mini-artwork-fallback", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `mini player without artwork shows generic fallback`() {
        rule.setContent {
            FonampTheme {
                MiniPlayer(
                    state = MiniPlayerState(
                        title = "Blue Line",
                        subtitle = "Ada",
                        isPlaying = false,
                        source = SourceBadgeKind.LOCAL,
                        artworkUri = null,
                    ),
                    onTogglePlayPause = {},
                    onClose = {},
                )
            }
        }

        rule.onNodeWithTag("mini-artwork-fallback", useUnmergedTree = true).assertIsDisplayed()
        rule.onNodeWithTag("mini-artwork-image", useUnmergedTree = true).assertDoesNotExist()
    }

    // PlayerSheet ------------------------------------------------------------

    @Test
    fun `player sheet with artwork shows hero image not fallback`() {
        rule.setContent {
            FonampTheme {
                PlayerSheet(
                    title = "Blue Line",
                    subtitle = "Ada",
                    source = SourceBadgeKind.LOCAL,
                    isPlaying = true,
                    onTogglePlayPause = {},
                    onClose = {},
                    artworkUri = "content://media/external/audio/albumart/7",
                )
            }
        }

        rule.onNodeWithTag("player-artwork-image").assertIsDisplayed()
        rule.onNodeWithTag("player-artwork-fallback").assertDoesNotExist()
    }

    @Test
    fun `player sheet without artwork shows generic fallback`() {
        rule.setContent {
            FonampTheme {
                PlayerSheet(
                    title = "Blue Line",
                    subtitle = null,
                    source = SourceBadgeKind.LOCAL,
                    isPlaying = false,
                    onTogglePlayPause = {},
                    onClose = {},
                    artworkUri = null,
                )
            }
        }

        rule.onNodeWithTag("player-artwork-fallback").assertIsDisplayed()
        rule.onNodeWithTag("player-artwork-image").assertDoesNotExist()
    }
}
