package com.fonamp.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonamp.core.ui.AlbumArtwork
import com.fonamp.core.ui.AppIcons
import com.fonamp.core.ui.DeniedState
import com.fonamp.core.ui.EmptyState
import com.fonamp.core.ui.ErrorRetryState
import com.fonamp.core.ui.LoadingState

/**
 * Slice F: Collection tab (library Req 1–6).
 *
 * Segmented Songs/Artists/Albums; artist/album pick filters to songs;
 * song tap plays with the visible list as the local queue (next/previous
 * and seek execute in the Slice G player). Generic M3 icons only, absent
 * metadata omitted (Req 3), no shuffle/repeat affordance (Req 6).
 * The search field is the cheap LIB-5 stretch: a local text filter.
 *
 * Permission entry: [permissionGranted] is owned by Slice I's
 * `AudioPermissionGate`; when false the caller renders [LibraryUiState.Denied]
 * (or passes it here) with [onGrantPermission]/[onOpenSettings] actions.
 */
@Composable
fun CollectionScreen(
    state: LibraryUiState,
    modifier: Modifier = Modifier,
    onSelectSegment: (LibrarySegment) -> Unit = {},
    onSelectArtist: (String?) -> Unit = {},
    onSelectAlbum: (String?) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onClearFilters: () -> Unit = {},
    onPlayAt: (Int) -> Unit = {},
    onRetry: () -> Unit = {},
    onGrantPermission: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    when (state) {
        is LibraryUiState.Loading -> LoadingState(modifier = modifier.fillMaxSize())
        is LibraryUiState.Denied -> DeniedState(
            why = "Fonamp needs audio access to show the music on this device.",
            onGrant = onGrantPermission,
            onOpenSettings = onOpenSettings,
            modifier = modifier.fillMaxSize(),
        )
        is LibraryUiState.Empty -> EmptyState(
            message = "No music found",
            hint = "Add audio files to your device to build your collection.",
            modifier = modifier.fillMaxSize(),
        )
        is LibraryUiState.Error -> ErrorRetryState(
            message = state.message,
            onRetry = onRetry,
            modifier = modifier.fillMaxSize(),
        )
        is LibraryUiState.Content -> CollectionContent(
            state = state,
            onSelectSegment = onSelectSegment,
            onSelectArtist = onSelectArtist,
            onSelectAlbum = onSelectAlbum,
            onQueryChange = onQueryChange,
            onClearFilters = onClearFilters,
            onPlayAt = onPlayAt,
            modifier = modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CollectionContent(
    state: LibraryUiState.Content,
    onSelectSegment: (LibrarySegment) -> Unit,
    onSelectArtist: (String?) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onPlayAt: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Filter songs") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("library-search"),
        )
        val activeFilter = state.artistFilter ?: state.albumFilter
        if (activeFilter != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = activeFilter,
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onClearFilters) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear filter")
                }
            }
        }
        TabRow(selectedTabIndex = state.segment.ordinal) {
            LibrarySegment.entries.forEach { segment ->
                Tab(
                    selected = state.segment == segment,
                    onClick = { onSelectSegment(segment) },
                    text = { Text(segment.label) },
                    modifier = Modifier.testTag("segment-${segment.name.lowercase()}"),
                )
            }
        }
        when (state.segment) {
            LibrarySegment.Songs -> LazyColumn {
                itemsIndexed(state.visibleSongs, key = { _, item -> item.stableId }) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayAt(index) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AlbumArtwork(
                            artworkUri = item.artworkUri,
                            size = 48.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
                            val detail = listOfNotNull(item.subtitle, item.album).joinToString(" · ")
                            if (detail.isNotEmpty()) {
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item.durationMs?.let { duration ->
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            LibrarySegment.Artists -> LazyColumn {
                itemsIndexed(state.artists, key = { _, name -> "artist:$name" }) { _, name ->
                    val count = state.songs.count { it.subtitle == name }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectArtist(name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "$count songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            LibrarySegment.Albums -> LazyColumn {
                itemsIndexed(state.albums, key = { _, name -> "album:$name" }) { _, name ->
                    val count = state.songs.count { it.album == name }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAlbum(name) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AlbumArtwork(
                            // Albums stay List<String> (no model change):
                            // cover = first song of the album, null = generic icon.
                            artworkUri = state.songs.firstOrNull { it.album == name }?.artworkUri,
                            size = 48.dp,
                            fallbackIcon = AppIcons.Album,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "$count songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stateful entry for app wiring (Slice I): collects the view-model state.
 * Stateless [CollectionScreen] above stays directly testable.
 */
@Composable
fun CollectionRoute(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
    onGrantPermission: () -> Unit = {},
    onOpenSettings: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CollectionScreen(
        state = state,
        onSelectSegment = viewModel::selectSegment,
        onSelectArtist = viewModel::selectArtist,
        onSelectAlbum = viewModel::selectAlbum,
        onQueryChange = viewModel::setQuery,
        onClearFilters = viewModel::clearFilters,
        onPlayAt = viewModel::playAt,
        onRetry = viewModel::refresh,
        onGrantPermission = onGrantPermission,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

private val LibrarySegment.label: String
    get() = when (this) {
        LibrarySegment.Songs -> "Songs"
        LibrarySegment.Artists -> "Artists"
        LibrarySegment.Albums -> "Albums"
    }

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
