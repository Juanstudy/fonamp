package com.fonamp.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T3: Settings update UI — check button fires, dialog offers Download/Later,
 * no dialog without an available release.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun settings(
        updateState: UpdateUiState = UpdateUiState.Idle,
        onCheckUpdates: () -> Unit = {},
        onDownloadUpdate: (String, String) -> Unit = { _, _ -> },
        onDismissUpdate: () -> Unit = {},
    ) {
        compose.setContent {
            SettingsScreen(
                state = SettingsUiState(),
                onSelectTheme = {},
                onClearCache = {},
                onDismissConfirmation = {},
                version = "0.0.9",
                updateState = updateState,
                onCheckUpdates = onCheckUpdates,
                onDownloadUpdate = onDownloadUpdate,
                onDismissUpdate = onDismissUpdate,
            )
        }
    }

    @Test
    fun `check button fires and disables while checking`() {
        var checks = 0
        settings(onCheckUpdates = { checks++ })

        compose.onNodeWithTag("check-updates").performScrollTo()
        compose.onNodeWithTag("check-updates").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("check-updates").performClick()
        assertEquals(1, checks)
    }

    @Test
    fun `checking disables the button`() {
        settings(updateState = UpdateUiState.Checking)
        compose.onNodeWithTag("check-updates").performScrollTo()
        compose.onNodeWithText("Checking…").assertIsDisplayed()
        compose.onNodeWithTag("check-updates").assertIsNotEnabled()
    }

    @Test
    fun `available release shows dialog with download`() {
        var downloaded: Pair<String, String>? = null
        settings(
            updateState = UpdateUiState.Available(
                tag = "v0.0.10",
                notes = "Big fixes",
                apkUrl = "https://cdn.example/f.apk",
            ),
            onDownloadUpdate = { url, tag -> downloaded = url to tag },
        )

        compose.onNodeWithTag("update-dialog").assertIsDisplayed()
        compose.onNodeWithText("Big fixes").assertIsDisplayed()
        compose.onNodeWithTag("update-download").performClick()
        assertEquals("https://cdn.example/f.apk" to "v0.0.10", downloaded)
    }

    @Test
    fun `later dismisses the dialog`() {
        var dismissed = 0
        settings(
            updateState = UpdateUiState.Available(tag = "v0.0.10", notes = null, apkUrl = "u"),
            onDismissUpdate = { dismissed++ },
        )

        compose.onNodeWithTag("update-later").performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun `no dialog when up to date`() {
        settings(updateState = UpdateUiState.UpToDate)
        compose.onNodeWithTag("update-dialog").assertDoesNotExist()
    }

    @Test
    fun `no dialog when idle`() {
        settings(updateState = UpdateUiState.Idle)
        compose.onNodeWithTag("update-dialog").assertDoesNotExist()
    }
}
