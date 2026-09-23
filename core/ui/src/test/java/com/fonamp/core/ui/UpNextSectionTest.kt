package com.fonamp.core.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * QL-1: behavior tests for the visible queue list.
 *
 * Semantic matchers first; testTags for rows and the active marker.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpNextSectionTest {

    @get:Rule
    val rule = createComposeRule()

    private fun rows() = listOf(
        QueueRow(
            title = "Song One",
            subtitle = "Artist A",
            artworkUri = null,
            durationMs = 185_000L,
            isActive = true,
        ),
        QueueRow(
            title = "Song Two",
            subtitle = "Artist B",
            artworkUri = null,
            durationMs = 65_000L,
            isActive = false,
        ),
        QueueRow(
            title = "Song Three",
            subtitle = null,
            artworkUri = null,
            durationMs = null,
            isActive = false,
        ),
    )

    @Test
    fun `up next renders section title rows and durations`() {
        rule.setContent {
            FonampTheme {
                UpNextSection(rows = rows(), onSelect = {})
            }
        }

        rule.onNodeWithTag("up-next-section").assertIsDisplayed()
        rule.onNodeWithText("Up next").assertIsDisplayed()
        rule.onNodeWithText("Song One").assertIsDisplayed()
        rule.onNodeWithText("Song Two").assertIsDisplayed()
        rule.onNodeWithText("Song Three").assertIsDisplayed()
        rule.onNodeWithText("3:05").assertIsDisplayed()
        rule.onNodeWithText("1:05").assertIsDisplayed()
    }

    @Test
    fun `active row is marked now playing`() {
        rule.setContent {
            FonampTheme {
                UpNextSection(rows = rows(), onSelect = {})
            }
        }

        // The row is clickable, so its children merge into the row node:
        // assert through the row tag + merged text, like the sheet tests do.
        rule.onNodeWithTag("up-next-row-0").assertIsDisplayed()
        rule.onNodeWithText("Now playing").assertIsDisplayed()
        org.junit.Assert.assertEquals(
            1,
            rule.onAllNodesWithText("Now playing").fetchSemanticsNodes().size,
        )
    }

    @Test
    fun `tap row invokes onSelect with queue index`() {
        val selected = mutableListOf<Int>()
        rule.setContent {
            FonampTheme {
                UpNextSection(rows = rows(), onSelect = { selected += it })
            }
        }

        // No scrollable parent in the standalone section: click directly.
        rule.onNodeWithTag("up-next-row-2").performClick()
        org.junit.Assert.assertEquals(listOf(2), selected)
    }

    @Test
    fun `empty list renders nothing`() {
        rule.setContent {
            FonampTheme {
                UpNextSection(rows = emptyList(), onSelect = {})
            }
        }

        rule.onNodeWithTag("up-next-section").assertDoesNotExist()
    }

    @Test
    fun `player sheet shows up next when queue rows are provided`() {
        rule.setContent {
            FonampTheme {
                PlayerSheet(
                    title = "Song One",
                    subtitle = "Artist A",
                    source = SourceBadgeKind.LOCAL,
                    isPlaying = true,
                    onTogglePlayPause = {},
                    onClose = {},
                    upNext = rows(),
                    onSelectQueueItem = {},
                )
            }
        }

        rule.onNodeWithTag("up-next-section").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Song Two").assertIsDisplayed()
    }

    @Test
    fun `player sheet hides up next by default`() {
        rule.setContent {
            FonampTheme {
                PlayerSheet(
                    title = "Song One",
                    subtitle = "Artist A",
                    source = SourceBadgeKind.LOCAL,
                    isPlaying = true,
                    onTogglePlayPause = {},
                    onClose = {},
                )
            }
        }

        rule.onNodeWithTag("up-next-section").assertDoesNotExist()
    }
}
