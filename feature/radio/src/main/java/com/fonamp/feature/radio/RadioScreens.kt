package com.fonamp.feature.radio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fonamp.core.database.FavoriteStation
import com.fonamp.core.ui.EmptyState
import com.fonamp.core.ui.ErrorRetryState
import com.fonamp.core.ui.LoadingState
import com.fonamp.core.ui.OfflineState
import com.fonamp.core.ui.SourceBadge
import com.fonamp.core.ui.SourceBadgeKind
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery

/**
 * Slice H: Discover/Stations/Favorites UI (radio Req 1–6).
 *
 * Stateless screens driven by [RadioUiState]/[FavoritesUiState]; stateful
 * `*Route` entries at the bottom bind the VMs for Slice I navigation.
 * Generic M3 icons only (artwork deferred to v2); no tops, random, or
 * name-search controls exist on any surface in v1 (radio Req 6) — the text
 * field filters the loaded index locally (RAD-1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    state: RadioUiState.Discover,
    onQueryChange: (String) -> Unit,
    onSelectSegment: (DiscoverSegment) -> Unit,
    onOpenSelection: (BrowseQuery) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Filter countries or tags") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("filter-field"),
                )
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh-button")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
            val tabs = listOf(DiscoverSegment.Countries to "Countries", DiscoverSegment.GenresTags to "Genres & tags")
            TabRow(selectedTabIndex = tabs.indexOfFirst { it.first == state.segment }) {
                tabs.forEach { (segment, label) ->
                    Tab(
                        selected = segment == state.segment,
                        onClick = { onSelectSegment(segment) },
                        text = { Text(label) },
                        modifier = Modifier.testTag(
                            if (segment == DiscoverSegment.Countries) "segment-countries" else "segment-tags",
                        ),
                    )
                }
            }
            if (state.offline) {
                OfflineState(
                    onRetry = onRefresh,
                    cacheNote = "Showing cached stations",
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize().testTag("discover-list")) {
                items(state.visible, key = { it.stableId }) { row ->
                    val isCountry = row.stableId.startsWith("country:")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenSelection(
                                    if (isCountry) BrowseQuery(country = row.title)
                                    else BrowseQuery(tag = row.title),
                                )
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            if (isCountry) Icons.Filled.Public else Icons.Filled.Tag,
                            contentDescription = null,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = row.title, style = MaterialTheme.typography.bodyLarge)
                            row.subtitle?.let {
                                Text(
                                    text = it,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsScreen(
    state: RadioUiState.Stations,
    onBack: () -> Unit,
    onPlay: (AudioItem) -> Unit,
    onToggleFavorite: (AudioItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                SourceBadge(kind = SourceBadgeKind.RADIO)
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh-button")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                }
            }
            if (state.offline) {
                OfflineState(
                    onRetry = onRefresh,
                    cacheNote = "Showing cached stations",
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize().testTag("stations-list")) {
                items(state.stations, key = { it.stableId }) { station ->
                    val isFavorite = state.favorites.contains(station.stationUuid)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlay(station) }
                            .testTag("station-play-${station.stationUuid}")
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Radio, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = station.title, style = MaterialTheme.typography.bodyLarge)
                            stationMeta(station)?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = { onToggleFavorite(station) },
                            modifier = Modifier.testTag("station-fav-${station.stationUuid}"),
                        ) {
                            Icon(
                                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Bitrate/codec line — shown only when the directory provides them (RAD-2). */
@Composable
private fun stationMeta(station: AudioItem): String? {
    val parts = listOfNotNull(
        station.bitrate?.let { "$it kbps" },
        station.codec,
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    pendingUndo: FavoriteStation?,
    onPlay: (FavoriteStation) -> Unit,
    onRemove: (FavoriteStation) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember { SnackbarHostState() }
    LaunchedEffect(pendingUndo?.stationUuid) {
        val row = pendingUndo ?: return@LaunchedEffect
        val result = host.showSnackbar(
            message = "Removed ${row.name}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Indefinite,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Favorites",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when (state) {
            FavoritesUiState.Loading -> LoadingState()
            FavoritesUiState.Empty -> EmptyState(
                message = "No favorites yet",
                hint = "Tap the heart on any station to keep it here",
            )
            is FavoritesUiState.Content -> LazyColumn(
                modifier = Modifier.weight(1f).testTag("favorites-list"),
            ) {
                items(state.favorites, key = { it.stationUuid }) { favorite ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.Place, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = favorite.name, style = MaterialTheme.typography.bodyLarge)
                            favorite.country?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(
                            onClick = { onPlay(favorite) },
                            modifier = Modifier.testTag("favorite-play-${favorite.stationUuid}"),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        }
                        IconButton(
                            onClick = { onRemove(favorite) },
                            modifier = Modifier.testTag("favorite-remove-${favorite.stationUuid}"),
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
        SnackbarHost(hostState = host)
    }
}

/**
 * Route-level dispatcher: every [RadioUiState] renders exactly one surface —
 * Loading / Empty / Discover / Stations / ErrorRetry (never blank).
 */
@Composable
fun RadioRouteScreen(
    state: RadioUiState,
    onQueryChange: (String) -> Unit,
    onSelectSegment: (DiscoverSegment) -> Unit,
    onOpenSelection: (BrowseQuery) -> Unit,
    onBack: () -> Unit,
    onPlay: (AudioItem) -> Unit,
    onToggleFavorite: (AudioItem) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        RadioUiState.Loading -> LoadingState(modifier = modifier)
        RadioUiState.Empty -> EmptyState(
            message = "No stations found",
            hint = "Pull to refresh when you're back online",
            modifier = modifier,
        )
        is RadioUiState.Discover -> DiscoverScreen(
            state = state,
            onQueryChange = onQueryChange,
            onSelectSegment = onSelectSegment,
            onOpenSelection = onOpenSelection,
            onRefresh = onRefresh,
            modifier = modifier,
        )
        is RadioUiState.Stations -> StationsScreen(
            state = state,
            onBack = onBack,
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
            onRefresh = onRefresh,
            modifier = modifier,
        )
        is RadioUiState.Error -> ErrorRetryState(
            message = state.message,
            onRetry = onRefresh,
            modifier = modifier,
        )
    }
}

/** Stateful entries for Slice I navigation — VMs stay injected, never built here. */
@Composable
fun DiscoverRoute(viewModel: RadioViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RadioRouteScreen(
        state = state,
        onQueryChange = viewModel::setQuery,
        onSelectSegment = viewModel::selectSegment,
        onOpenSelection = { query ->
            viewModel.openSelection(query, query.country ?: query.tag ?: query.genre.orEmpty())
        },
        onBack = viewModel::backToDiscover,
        onPlay = viewModel::playStation,
        onToggleFavorite = viewModel::toggleFavorite,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun FavoritesRoute(viewModel: FavoritesViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingUndo by viewModel.pendingUndo.collectAsStateWithLifecycle()
    FavoritesScreen(
        state = state,
        pendingUndo = pendingUndo,
        onPlay = viewModel::play,
        onRemove = viewModel::remove,
        onUndo = viewModel::undo,
        modifier = modifier,
    )
}
