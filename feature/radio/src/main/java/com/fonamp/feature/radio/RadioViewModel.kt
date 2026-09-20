package com.fonamp.feature.radio

import com.fonamp.core.database.FavoriteDao
import com.fonamp.core.database.FavoriteStation
import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.DirectoryEntry
import com.fonamp.core.network.DirectoryIndex
import com.fonamp.core.network.StationDto
import com.fonamp.core.network.StationQuery
import com.fonamp.core.player.PlayerManager
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.radio.CuratedStations
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Discover index segments (radio Req 1). No tops/random segment exists. */
enum class DiscoverSegment { Countries, GenresTags }

/**
 * Slice H: radio Discover + station-list states (radio Req 1–3, 5–6; player Req 5).
 *
 * Every render picks one of these — never a blank screen:
 * - [Loading] initial fetch with no cache to show.
 * - [Empty] the directory index came back with zero rows.
 * - [Discover] the country/genre-tag index (RAD-1). [Discover.offline] marks
 *   the Offline card: cached/stale rows stay visible with retry (RAD-5).
 * - [Stations] the station list for one selection (RAD-2), same offline rule.
 * - [Error] fetch failed with nothing cached; carries the failed [selection]
 *   (null = index) so retry returns to the right level.
 *
 * Index rows are navigation nodes (`stableId` `country:`/`tag:`, empty
 * `streamUri` per the Slice E contract) — [playStation] refuses them, and
 * [toggleFavorite] refuses rows without a `stationUuid`. The text [query]
 * filter narrows the loaded index locally with zero source calls per
 * keystroke (RAD-1) AND drives the debounced server-side name search whose
 * hits land in [RadioUiState.Discover.remoteResults] (name-search delta).
 */
sealed interface RadioUiState {
    data object Loading : RadioUiState
    data object Empty : RadioUiState
    data class Discover(
        val countries: List<AudioItem>,
        val tags: List<AudioItem>,
        val segment: DiscoverSegment = DiscoverSegment.Countries,
        val query: String = "",
        val visible: List<AudioItem> = countries,
        val offline: Boolean = false,
        val favorites: Set<String> = emptySet(),
        val refreshing: Boolean = false,
        val curated: List<AudioItem> = emptyList(),
        /** Server-side name-search hits for [query]; empty when idle/short/failed. */
        val remoteResults: List<AudioItem> = emptyList(),
        /** A debounced directory search is in flight. */
        val remoteSearching: Boolean = false,
        /** Last directory search failed; the index above stays intact. */
        val remoteOffline: Boolean = false,
    ) : RadioUiState
    data class Stations(
        val selection: BrowseQuery,
        val title: String,
        val stations: List<AudioItem>,
        val offline: Boolean = false,
        val favorites: Set<String> = emptySet(),
        val refreshing: Boolean = false,
    ) : RadioUiState
    data class Error(val message: String, val selection: BrowseQuery? = null) : RadioUiState
}

/**
 * Slice H: Discover/stations state holder — talks to the radio [Source],
 * [DirectoryCache] (stale fallback), [FavoriteDao], and [PlayerManager] only
 * (design §1/§9: no `feature → feature`, no provider internals).
 *
 * A plain class (not an Android `ViewModel`) so unit tests inject a test
 * scope; Slice I binds it into the Hilt/app graph (Slice F precedent).
 * Directory reads served fresh from cache cost zero network; stale-or-missing
 * entries trigger one fetch, and a failed fetch keeps stale rows visible with
 * [RadioUiState.Discover.offline] set instead of clearing them.
 */
class RadioViewModel(
    private val source: Source,
    private val favorites: FavoriteDao,
    private val player: PlayerManager,
    private val cache: DirectoryCache,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val curatedProvider: () -> List<AudioItem> = CuratedStations::browseCurated,
) {
    companion object {
        /** Debounce for server-side name search: one directory call per pause, not per keystroke. */
        const val SEARCH_DEBOUNCE_MS = 400L
        /** Minimum trimmed query length that hits the directory; shorter stays local-only. */
        const val SEARCH_MIN_CHARS = 2
    }

    private val _state = MutableStateFlow<RadioUiState>(RadioUiState.Loading)
    val state: StateFlow<RadioUiState> = _state.asStateFlow()

    private var favoriteIds: Set<String> = emptySet()
    private var lastDiscover: RadioUiState.Discover? = null
    private var currentSelection: Pair<BrowseQuery, String>? = null
    private var searchJob: Job? = null

    init {
        scope.launch { favorites.observeAll().collect { rows -> onFavorites(rows) } }
        loadIndex(forceFetch = false)
    }

    /** Pull-to-refresh / retry: re-fetches the current level, never clears the cache. */
    fun refresh() {
        setRefreshing(true)
        val selection = currentSelection
        if (selection == null) loadIndex(forceFetch = true) else loadStations(selection.first, selection.second, forceFetch = true)
    }

    private fun setRefreshing(refreshing: Boolean) {
        when (val current = _state.value) {
            is RadioUiState.Discover -> _state.value = current.copy(refreshing = refreshing)
            is RadioUiState.Stations -> _state.value = current.copy(refreshing = refreshing)
            RadioUiState.Empty, RadioUiState.Loading, is RadioUiState.Error -> Unit
        }
    }

    fun setQuery(query: String) {
        val current = _state.value as? RadioUiState.Discover ?: return
        _state.value = current.copy(query = query, visible = visible(current, query))
        searchJob?.cancel()
        searchJob = null
        if (query.trim().length < SEARCH_MIN_CHARS) {
            // Short/blank query: zero network, clear any remote section.
            val cleared = _state.value as? RadioUiState.Discover
            if (cleared != null && (cleared.remoteResults.isNotEmpty() || cleared.remoteSearching || cleared.remoteOffline)) {
                _state.value = cleared.copy(remoteResults = emptyList(), remoteSearching = false, remoteOffline = false)
                lastDiscover = _state.value as? RadioUiState.Discover
            }
            return
        }
        launchSearch(query)
    }

    /** Retry the visible directory search (offline/error retry in Discover). */
    fun retrySearch() {
        val current = _state.value as? RadioUiState.Discover ?: return
        if (current.query.trim().length >= SEARCH_MIN_CHARS) launchSearch(current.query)
    }

    /** Debounced directory search for [query]; supersedes any pending one. */
    private fun launchSearch(query: String) {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val searching = _state.value as? RadioUiState.Discover ?: return@launch
            if (searching.query != query) return@launch
            _state.value = searching.copy(remoteSearching = true, remoteOffline = false)
            lastDiscover = _state.value as? RadioUiState.Discover
            val result = withContext(io) { source.search(query.trim()) }
            val target = _state.value as? RadioUiState.Discover ?: return@launch
            // Stale guard: the user kept typing while we were in flight.
            if (target.query != query) return@launch
            _state.value = when (result) {
                is SourceResult.Ok ->
                    target.copy(remoteResults = result.v, remoteSearching = false, remoteOffline = false)
                is SourceResult.Fail ->
                    target.copy(remoteSearching = false, remoteOffline = true)
            }
            lastDiscover = _state.value as? RadioUiState.Discover
        }
    }

    fun selectSegment(segment: DiscoverSegment) {
        val current = _state.value as? RadioUiState.Discover ?: return
        val pool = if (segment == DiscoverSegment.Countries) current.countries else current.tags
        _state.value = current.copy(
            segment = segment,
            visible = pool.filter { it.title.contains(current.query, ignoreCase = true) },
        )
    }

    /** Tap 2 of the 3-tap flow: open the station list for one index row. */
    fun openSelection(selection: BrowseQuery, title: String) {
        // A pending name search belongs to Discover: cancel it so its late
        // emission can never land on the Stations sequence.
        searchJob?.cancel()
        searchJob = null
        currentSelection = selection to title
        loadStations(selection, title, forceFetch = false)
    }

    fun backToDiscover() {
        searchJob?.cancel()
        searchJob = null
        currentSelection = null
        val restored = lastDiscover
        if (restored != null) {
            // Drop a stale in-flight flag from a search cancelled above.
            _state.value = restored.copy(remoteSearching = false)
            lastDiscover = _state.value as? RadioUiState.Discover
            // Re-fire the visible query when it has no results yet: navigating
            // away cancelled the pending search, the query text survived.
            if (restored.query.trim().length >= SEARCH_MIN_CHARS &&
                restored.remoteResults.isEmpty() && !restored.remoteOffline
            ) {
                launchSearch(restored.query)
            }
        } else {
            loadIndex(forceFetch = false)
        }
    }

    /**
     * Tap 3 of the 3-tap flow: 1-tap play → `PlayerManager.playSingle`
     * (radio Req 3). Index rows carry an empty `streamUri` and are never
     * played — this is a no-op for them (Slice E contract).
     */
    fun playStation(item: AudioItem) {
        if (item.streamUri.isBlank()) return
        player.playSingle(source.streamOf(item))
    }

    /** 1-tap heart (radio Req 4). Index rows have no `stationUuid`: no-op. */
    fun toggleFavorite(item: AudioItem) {
        val uuid = item.stationUuid?.takeIf { it.isNotBlank() } ?: return
        scope.launch {
            if (favoriteIds.contains(uuid)) {
                favorites.delete(uuid)
            } else {
                favorites.upsert(
                    FavoriteStation(
                        stationUuid = uuid,
                        name = item.title,
                        streamUrl = item.streamUri,
                        country = item.country,
                        tagsCsv = item.tags.joinToString(",").takeIf { it.isNotEmpty() },
                        bitrate = item.bitrate,
                        codec = item.codec,
                        favoritedAt = nowMs(),
                    ),
                )
            }
        }
    }

    private fun loadIndex(forceFetch: Boolean) {
        scope.launch {
            val cached = withContext(io) { cache.getIndex() }
            if (cached != null && !cache.isStale(cached.fetchedAt) && !forceFetch) {
                emitDiscover(cached.index, offline = false, refreshing = false)
                return@launch
            }
            // Pre-show cache only when nothing is on screen yet; a refresh keeps
            // the current rows (with the refreshing flag) instead of flickering.
            if (cached != null && _state.value !is RadioUiState.Discover) {
                emitDiscover(cached.index, offline = false, refreshing = keepRefreshing())
            } else if (cached == null && _state.value !is RadioUiState.Discover) {
                _state.value = RadioUiState.Loading
            }
            when (val result = withContext(io) { source.browse(BrowseQuery()) }) {
                is SourceResult.Ok ->
                    if (result.v.isEmpty() && cached == null) {
                        _state.value = RadioUiState.Empty
                    } else if (result.v.isNotEmpty()) {
                        emitDiscoverRows(
                            countries = result.v.filter { it.stableId.startsWith("country:") },
                            tags = result.v.filter { it.stableId.startsWith("tag:") },
                            offline = false,
                            refreshing = false,
                        )
                    } else {
                        setRefreshing(false)
                    }
                is SourceResult.Fail ->
                    if (cached != null) emitDiscover(cached.index, offline = true, refreshing = false)
                    else _state.value = RadioUiState.Error(messageFor(result.e))
            }
        }
    }

    private fun loadStations(selection: BrowseQuery, title: String, forceFetch: Boolean) {
        scope.launch {
            val key = DirectoryCache.stationKey(
                StationQuery(country = selection.country, genre = selection.genre, tag = selection.tag),
            )
            val cached = withContext(io) { cache.getStations(key) }
            if (cached != null && !cache.isStale(cached.fetchedAt) && !forceFetch) {
                _state.value = RadioUiState.Stations(selection, title, cached.stations.map { it.toAudioItem() }, favorites = favoriteIds)
                return@launch
            }
            val current = _state.value as? RadioUiState.Stations
            val showingSelection = current?.selection == selection
            if (cached != null && !showingSelection) {
                _state.value = RadioUiState.Stations(
                    selection, title, cached.stations.map { it.toAudioItem() },
                    favorites = favoriteIds, refreshing = keepRefreshing(),
                )
            } else if (cached == null && !showingSelection) {
                _state.value = RadioUiState.Loading
            }
            when (val result = withContext(io) { source.browse(selection) }) {
                is SourceResult.Ok ->
                    if (result.v.isEmpty() && cached == null) {
                        _state.value = RadioUiState.Empty
                    } else if (result.v.isNotEmpty()) {
                        _state.value = RadioUiState.Stations(selection, title, result.v, favorites = favoriteIds)
                    } else {
                        setRefreshing(false)
                    }
                is SourceResult.Fail ->
                    if (cached != null) {
                        _state.value = RadioUiState.Stations(
                            selection, title, cached.stations.map { it.toAudioItem() },
                            offline = true, favorites = favoriteIds,
                        )
                    } else {
                        _state.value = RadioUiState.Error(messageFor(result.e), selection)
                    }
            }
        }
    }

    private fun emitDiscover(index: DirectoryIndex, offline: Boolean, refreshing: Boolean = false) {
        val countries = index.countries.map { it.toAudioItem("country") }
        val tags = index.tags.map { it.toAudioItem("tag") }
        val discover = RadioUiState.Discover(
            countries = countries,
            tags = tags,
            segment = (_state.value as? RadioUiState.Discover)?.segment ?: DiscoverSegment.Countries,
            query = (_state.value as? RadioUiState.Discover)?.query.orEmpty(),
            visible = emptyList(),
            offline = offline,
            favorites = favoriteIds,
            refreshing = refreshing,
            curated = curatedProvider(),
        ).let { it.copy(visible = visible(it, it.query)) }
        lastDiscover = discover
        _state.value = discover
    }

    /** Fresh-fetch path: index rows arrive already mapped; split by stableId prefix. */
    private fun emitDiscoverRows(
        countries: List<AudioItem>,
        tags: List<AudioItem>,
        offline: Boolean,
        refreshing: Boolean,
    ) {
        val prior = _state.value as? RadioUiState.Discover
        val discover = RadioUiState.Discover(
            countries = countries,
            tags = tags,
            segment = prior?.segment ?: DiscoverSegment.Countries,
            query = prior?.query.orEmpty(),
            visible = emptyList(),
            offline = offline,
            favorites = favoriteIds,
            refreshing = refreshing,
            curated = curatedProvider(),
        ).let { it.copy(visible = visible(it, it.query)) }
        lastDiscover = discover
        _state.value = discover
    }

    private fun visible(discover: RadioUiState.Discover, query: String): List<AudioItem> {
        val pool = if (discover.segment == DiscoverSegment.Countries) discover.countries else discover.tags
        return if (query.isBlank()) pool else pool.filter { it.title.contains(query, ignoreCase = true) }
    }

    private fun keepRefreshing(): Boolean = when (val current = _state.value) {
        is RadioUiState.Discover -> current.refreshing
        is RadioUiState.Stations -> current.refreshing
        RadioUiState.Empty, RadioUiState.Loading, is RadioUiState.Error -> false
    }

    private fun onFavorites(rows: List<FavoriteStation>) {
        favoriteIds = rows.map { it.stationUuid }.toSet()
        when (val current = _state.value) {
            is RadioUiState.Discover -> {
                val updated = current.copy(favorites = favoriteIds)
                lastDiscover = updated
                _state.value = updated
            }
            is RadioUiState.Stations -> _state.value = current.copy(favorites = favoriteIds)
            RadioUiState.Empty, RadioUiState.Loading, is RadioUiState.Error -> Unit
        }
    }
}

private fun DirectoryEntry.toAudioItem(kind: String): AudioItem {
    val countSuffix = stationCount?.let { " · $it stations" }.orEmpty()
    return AudioItem(
        sourceId = "radio-browser",
        stableId = "$kind:$name",
        title = name,
        subtitle = (kind.replaceFirstChar { it.uppercase() } + countSuffix).takeIf { countSuffix.isNotEmpty() },
        streamUri = "",
    )
}

/**
 * Mirrors the `StationDto → AudioItem` mapping owned by `provider/radio`
 * (same nullability rules: bitrate only when > 0, codec only when blank-free,
 * subtitle omitted when there is nothing to say). Duplicated here because
 * `feature/radio` may not import provider internals (design §1) yet must
 * render stale cache rows the `Source` would otherwise map.
 */
private fun StationDto.toAudioItem(): AudioItem {
    val subtitle = (listOfNotNull(country) + tagList)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
    return AudioItem(
        sourceId = "radio-browser",
        stableId = stationuuid,
        title = name,
        subtitle = subtitle,
        streamUri = resolvedStreamUrl,
        bitrate = bitrate?.takeIf { it > 0 },
        codec = codec?.takeIf { it.isNotBlank() },
        stationUuid = stationuuid,
        country = country,
        tags = tagList,
    )
}

private fun messageFor(error: SourceError): String = when (error) {
    SourceError.Offline -> "You're offline"
    SourceError.Timeout -> "Directory request timed out"
    is SourceError.Server -> "Directory unavailable"
    is SourceError.Unknown -> "Something went wrong"
}
