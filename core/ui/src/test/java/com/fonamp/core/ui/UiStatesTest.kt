package com.fonamp.core.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice C RED: behavior tests for the shared design-system states.
 *
 * Semantic matchers first; testTags only where semantics cannot express the
 * assertion (shimmer rows). Light + dark exercised via [FonampTheme] (M3
 * defaults) — no screenshot tests, visual identity is deferred to v2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UiStatesTest {

    @get:Rule
    val rule = createComposeRule()

    // Loading ----------------------------------------------------------------

    @Test
    fun `loading shows shimmer rows and is never blank`() {
        rule.setContent {
            FonampTheme(darkTheme = false) {
                LoadingState()
            }
        }

        rule.onNodeWithTag("loading-state").assertIsDisplayed()
        // At least one shimmer row; the screen is never an empty box.
        rule.onNodeWithTag("loading-row-0").assertIsDisplayed()
        rule.onNodeWithText("Loading").assertIsDisplayed()
    }

    @Test
    fun `loading renders in dark theme`() {
        rule.setContent {
            FonampTheme(darkTheme = true) {
                LoadingState()
            }
        }

        rule.onNodeWithTag("loading-state").assertIsDisplayed()
        rule.onNodeWithTag("loading-row-0").assertIsDisplayed()
    }

    // Empty ------------------------------------------------------------------

    @Test
    fun `empty shows message and hint`() {
        rule.setContent {
            FonampTheme {
                EmptyState(
                    message = "No music yet",
                    hint = "Add music hint",
                )
            }
        }

        rule.onNodeWithText("No music yet").assertIsDisplayed()
        rule.onNodeWithText("Add music hint").assertIsDisplayed()
    }

    @Test
    fun `empty renders in dark theme`() {
        rule.setContent {
            FonampTheme(darkTheme = true) {
                EmptyState(message = "No music yet")
            }
        }

        rule.onNodeWithText("No music yet").assertIsDisplayed()
    }

    // Offline ----------------------------------------------------------------

    @Test
    fun `offline shows card and retry invokes callback`() {
        var retries = 0
        rule.setContent {
            FonampTheme {
                OfflineState(onRetry = { retries++ })
            }
        }

        rule.onNodeWithTag("offline-card").assertIsDisplayed()
        rule.onNodeWithText("Retry", substring = true).assertHasClickAction()
        rule.onNodeWithText("Retry", substring = true).performClick()
        assert(retries == 1) { "expected one retry click, got $retries" }
    }

    @Test
    fun `offline renders in dark theme`() {
        rule.setContent {
            FonampTheme(darkTheme = true) {
                OfflineState(onRetry = {})
            }
        }

        rule.onNodeWithTag("offline-card").assertIsDisplayed()
    }

    // Denied -----------------------------------------------------------------

    @Test
    fun `denied shows why and grant invokes callback`() {
        var grants = 0
        rule.setContent {
            FonampTheme {
                DeniedState(
                    why = "Why we need audio access",
                    onGrant = { grants++ },
                )
            }
        }

        rule.onNodeWithText("Why we need audio access").assertIsDisplayed()
        rule.onNodeWithText("Grant again", substring = true).performClick()
        assert(grants == 1) { "expected one grant click, got $grants" }
    }

    @Test
    fun `denied with settings link opens settings`() {
        var settings = 0
        rule.setContent {
            FonampTheme {
                DeniedState(
                    why = "Why we need audio access",
                    onGrant = {},
                    onOpenSettings = { settings++ },
                )
            }
        }

        rule.onNodeWithText("Open settings", substring = true).performClick()
        assert(settings == 1) { "expected one settings click, got $settings" }
    }

    // ErrorRetry --------------------------------------------------------------

    @Test
    fun `error shows message and retry invokes callback`() {
        var retries = 0
        rule.setContent {
            FonampTheme {
                ErrorRetryState(
                    message = "Something failed",
                    onRetry = { retries++ },
                )
            }
        }

        rule.onNodeWithText("Something failed").assertIsDisplayed()
        rule.onNodeWithText("Retry", substring = true).performClick()
        assert(retries == 1) { "expected one retry click, got $retries" }
    }

    // MiniPlayer --------------------------------------------------------------

    @Test
    fun `mini player shows title with 48dp play-pause and close targets`() {
        var toggles = 0
        var closes = 0
        rule.setContent {
            FonampTheme {
                MiniPlayer(
                    state = MiniPlayerState(
                        title = "Station One",
                        subtitle = "News",
                        isPlaying = true,
                        source = SourceBadgeKind.RADIO,
                    ),
                    onTogglePlayPause = { toggles++ },
                    onClose = { closes++ },
                )
            }
        }

        rule.onNodeWithTag("mini-player").assertIsDisplayed()
        rule.onNodeWithText("Station One").assertIsDisplayed()
        // Playing → Pause affordance with content description.
        rule.onNodeWithContentDescription("Pause")
            .assertHasClickAction()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assert(toggles == 1) { "expected one toggle click, got $toggles" }
        rule.onNodeWithContentDescription("Close player")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assert(closes == 1) { "expected one close click, got $closes" }
    }

    @Test
    fun `mini player paused shows play description`() {
        rule.setContent {
            FonampTheme {
                MiniPlayer(
                    state = MiniPlayerState(
                        title = "Station One",
                        subtitle = null,
                        isPlaying = false,
                        source = SourceBadgeKind.LOCAL,
                    ),
                    onTogglePlayPause = {},
                    onClose = {},
                )
            }
        }

        rule.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    // PlayerSheet --------------------------------------------------------------

    @Test
    fun `player sheet skeleton shows title badge and close`() {
        rule.setContent {
            FonampTheme {
                PlayerSheet(
                    title = "Station One",
                    subtitle = "News",
                    source = SourceBadgeKind.RADIO,
                    isPlaying = true,
                    onTogglePlayPause = {},
                    onClose = {},
                )
            }
        }

        rule.onNodeWithTag("player-sheet").assertIsDisplayed()
        rule.onNodeWithText("Station One").assertIsDisplayed()
        rule.onNodeWithText("Radio").assertIsDisplayed()
        rule.onNodeWithContentDescription("Close player sheet").assertHasClickAction()
    }

    @Test
    fun `player sheet error banner retries`() {
        var retries = 0
        rule.setContent {
            FonampTheme {
                PlayerSheet(
                    title = "Station One",
                    subtitle = null,
                    source = SourceBadgeKind.RADIO,
                    isPlaying = false,
                    onTogglePlayPause = {},
                    onClose = {},
                    errorMessage = "Stream unavailable",
                    onRetry = { retries++ },
                )
            }
        }

        rule.onNodeWithTag("player-error").assertIsDisplayed()
        rule.onNodeWithText("Stream unavailable").assertIsDisplayed()
        rule.onNodeWithText("Retry", substring = true).performScrollTo()
        rule.onNodeWithText("Retry", substring = true).performClick()
        assert(retries == 1) { "expected one retry click, got $retries" }
    }

    // SourceBadge ---------------------------------------------------------------

    @Test
    fun `source badge distinguishes radio and local`() {
        rule.setContent {
            FonampTheme {
                Column {
                    SourceBadge(kind = SourceBadgeKind.RADIO)
                    SourceBadge(kind = SourceBadgeKind.LOCAL)
                }
            }
        }
        rule.onNodeWithText("Radio").assertIsDisplayed()
        rule.onNodeWithText("Local").assertIsDisplayed()
    }

    // Theme + restoration -------------------------------------------------------

    @Test
    fun `all shared states render under light theme`() {
        rule.setContent {
            FonampTheme(darkTheme = false) {
                Column {
                    EmptyState(message = "Empty light")
                    ErrorRetryState(message = "Err light", onRetry = {})
                    OfflineState(onRetry = {})
                }
            }
        }

        rule.onNodeWithText("Empty light").assertIsDisplayed()
        rule.onNodeWithText("Err light").assertIsDisplayed()
        rule.onNodeWithTag("offline-card").assertIsDisplayed()
    }

    @Test
    fun `all shared states render under dark theme`() {
        rule.setContent {
            FonampTheme(darkTheme = true) {
                Column {
                    EmptyState(message = "Empty dark")
                    ErrorRetryState(message = "Err dark", onRetry = {})
                    OfflineState(onRetry = {})
                }
            }
        }

        rule.onNodeWithText("Empty dark").assertIsDisplayed()
        rule.onNodeWithText("Err dark").assertIsDisplayed()
        rule.onNodeWithTag("offline-card").assertIsDisplayed()
    }

    @Test
    fun `mini player survives state restoration`() {
        val tester = StateRestorationTester(rule)
        tester.setContent {
            FonampTheme {
                MiniPlayer(
                    state = MiniPlayerState(
                        title = "Restored Station",
                        subtitle = null,
                        isPlaying = false,
                        source = SourceBadgeKind.RADIO,
                    ),
                    onTogglePlayPause = {},
                    onClose = {},
                )
            }
        }
        rule.onNodeWithText("Restored Station").assertIsDisplayed()

        tester.emulateSavedInstanceStateRestore()

        rule.onNodeWithText("Restored Station").assertIsDisplayed()
    }
}
