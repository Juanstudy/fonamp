package com.fonamp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonamp.core.database.ThemeMode

/**
 * Slice I: Settings tab (settings Req 1–4).
 *
 * Theme System/Light/Dark (backed by `ThemeDao`, applies without restart),
 * directory-cache usage + clear with confirmation + freed bytes, and About
 * (version/licenses/source). v1 exposes no tab editor, EQ, sleep timer, or
 * playback-speed control anywhere on this surface.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSelectTheme: (ThemeMode) -> Unit,
    onClearCache: () -> Unit,
    onDismissConfirmation: () -> Unit,
    modifier: Modifier = Modifier,
    version: String = "0.1.0",
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = "Appearance", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = state.theme == mode,
                        onClick = { onSelectTheme(mode) },
                        role = Role.RadioButton,
                    )
                    .testTag("theme-${mode.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = state.theme == mode,
                    onClick = { onSelectTheme(mode) },
                )
                Text(
                    text = mode.label,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Text(text = "Storage", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Directory cache: ${state.cacheEntries} entries, " +
                "${SettingsViewModel.formatBytes(state.cacheBytes)}",
            modifier = Modifier.testTag("cache-stats"),
        )
        Button(
            onClick = { showClearDialog = true },
            modifier = Modifier.testTag("clear-cache"),
        ) {
            Text("Clear directory cache")
        }

        Text(text = "About", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Fonamp $version\n" +
                "Open-source licenses: see project NOTICE.\n" +
                "Sources: on-device library plus the radio-browser directory.",
            modifier = Modifier.testTag("about"),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear directory cache?") },
            text = { Text("Cached station lists will be refetched on next visit.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearCache()
                    },
                    modifier = Modifier.testTag("clear-confirm"),
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Stateful entry for app wiring (Slice I): collects the view-model state and
 * surfaces the clear confirmation. Stateless [SettingsScreen] above stays
 * directly testable. [snackbar] is owned by the app scaffold.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    version: String = "0.1.0",
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state.clearConfirmation?.let { message ->
        LaunchedEffect(message) {
            snackbar.showSnackbar(message)
            viewModel.dismissConfirmation()
        }
    }
    SettingsScreen(
        state = state,
        onSelectTheme = viewModel::setTheme,
        onClearCache = viewModel::clearCache,
        onDismissConfirmation = viewModel::dismissConfirmation,
        modifier = modifier,
        version = version,
    )
}

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "System default"
        ThemeMode.LIGHT -> "Light"
        ThemeMode.DARK -> "Dark"
    }
