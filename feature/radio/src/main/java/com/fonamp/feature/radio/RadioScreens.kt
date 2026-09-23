package com.fonamp.feature.radio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import com.fonamp.core.ui.AppIcons
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
 * Generic M3 icons only (artwork deferred to v2); no tops or random controls
 * exist on any surface (radio Req 6) — the text field filters the loaded
 * index locally (RAD-1) and drives the debounced server-side name search
 * rendered as the "Search results" section below the index.
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
    onPlay: (AudioItem) -> Unit = {},
    onToggleFavorite: (AudioItem) -> Unit = {},
    onRetrySearch: () -> Unit = {},
) {
    // Single scrolling container (Req 9): the Curadas section sits on top of
    // the filter/tabs/index rows, so small screens scroll instead of squeezing
    // the station list to zero height. Order: Curadas, filter, tabs, list.
    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().testTag("discover-list")) {
            item(key = "curated") {
                CuratedSection(
                    curated = state.curated,
                    favorites = state.favorites,
                    onOpenTag = { tag -> onOpenSelection(BrowseQuery(tag = tag)) },
                    onPlay = onPlay,
                    onToggleFavorite = onToggleFavorite,
                )
            }
            item(key = "filter") {
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
            }
            item(key = "tabs") {
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
            }
            if (state.offline) {
                item(key = "offline") {
                    OfflineState(
                        onRetry = onRefresh,
                        cacheNote = "Showing cached stations",
                    )
                }
            }
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
                        if (isCountry) AppIcons.Public else AppIcons.Tag,
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
            if (state.query.trim().length >= RadioViewModel.SEARCH_MIN_CHARS) {
                item(key = "search-header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("search-section"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Search results",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        SourceBadge(kind = SourceBadgeKind.RADIO)
                    }
                }
                when {
                    state.remoteResults.isNotEmpty() -> items(
                        state.remoteResults,
                        key = { "search:${it.stableId}" },
                    ) { station ->
                        val uuid = station.stationUuid
                        val isFavorite = uuid != null && state.favorites.contains(uuid)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(station) }
                                .testTag("search-play-${uuid ?: station.stableId}")
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            RadioArtwork(station.artworkUri)
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
                            if (uuid != null) {
                                IconButton(
                                    onClick = { onToggleFavorite(station) },
                                    modifier = Modifier.testTag("search-fav-$uuid"),
                                ) {
                                    Icon(
                                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (isFavorite) "Unfavorite" else "Favorite",
                                    )
                                }
                            }
                        }
                    }
                    state.remoteSearching -> item(key = "search-loading") {
                        LoadingState(rowCount = 3)
                    }
                    state.remoteOffline -> item(key = "search-offline") {
                        ErrorRetryState(
                            message = "Search failed",
                            onRetry = onRetrySearch,
                        )
                    }
                    else -> item(key = "search-empty") {
                        EmptyState(
                            message = "No stations found",
                            hint = "Try another name",
                        )
                    }
                }
            }
        }
    }
}

/**
 * Change `curated-radio-stations` (Req 9, 11, D2): "Curadas" section on top of
 * Discover — pinned tag tiles (existing `browse(BrowseQuery(tag = …))` path,
 * 24h cache + mirror fallback) plus curated preset rows reusing the same row
 * play + heart testTags as [StationsScreen].
 */
@Composable
fun CuratedSection(
    curated: List<AudioItem>,
    favorites: Set<String>,
    onOpenTag: (String) -> Unit,
    onPlay: (AudioItem) -> Unit,
    onToggleFavorite: (AudioItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("curated-section"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Curadas",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            SourceBadge(kind = SourceBadgeKind.RADIO)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CuratedTags.TILES.forEach { tile ->
                Text(
                    text = tile.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .testTag("curated-tag-${tile.tag}")
                        .clickable { onOpenTag(tile.tag) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        curated.forEach { station ->
            val uuid = station.stationUuid
            val rowKey = uuid ?: station.stableId
            val isFavorite = uuid != null && favorites.contains(uuid)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(station) }
                    .testTag("station-play-$rowKey")
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RadioArtwork(station.artworkUri)
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
                // D3: presets without stationUuid are not favoritable — no heart.
                if (uuid != null) {
                    IconButton(
                        onClick = { onToggleFavorite(station) },
                        modifier = Modifier.testTag("station-fav-$uuid"),
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
                        RadioArtwork(station.artworkUri)
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
                        RadioArtwork(favorite.artworkUri, fallback = Icons.Filled.Place)
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
    onRetrySearch: () -> Unit = {},
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
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
            onRetrySearch = onRetrySearch,
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
        onRetrySearch = viewModel::retrySearch,
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

@Composable
private fun RadioArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    fallback: androidx.compose.ui.graphics.vector.ImageVector = AppIcons.Radio
) {
    if (artworkUri != null) {
        AsyncImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
        )
    } else {
        Icon(fallback, contentDescription = null, modifier = modifier)
    }
}
