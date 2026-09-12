package com.fonamp.feature.library

import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Collection segments (library Req 2). No shuffle/repeat segment exists in v1. */
enum class LibrarySegment { Songs, Artists, Albums }

/**
 * Slice F: Collection UI states (library Req 1, 4; permissions Req 2–3).
 *
 * Every list render picks one of these — never a blank screen. `Denied`
 * renders the shared `DeniedState` and is entered without touching the
 * `Source` when audio permission is absent; Slice I's `AudioPermissionGate`
 * supplies [permissionGranted] and rebuilds on grant.
 */
sealed interface LibraryUiState {
    data object Loading : LibraryUiState
    data object Denied : LibraryUiState
    data object Empty : LibraryUiState
    data class Content(
        val songs: List<AudioItem>,
        val artists: List<String>,
        val albums: List<String>,
        val segment: LibrarySegment = LibrarySegment.Songs,
        val artistFilter: String? = null,
        val albumFilter: String? = null,
        val query: String = "",
        val visibleSongs: List<AudioItem> = songs,
    ) : LibraryUiState

    data class Error(val message: String) : LibraryUiState
}

/**
 * Slice F: Collection state holder — talks to `LocalSource` only (design §1).
 *
 * A plain class (not an Android `ViewModel`) so unit tests inject a test
 * scope; Slice I binds it into the Hilt/app graph. Grouping
 * Songs/Artists/Albums derives from one `browse(BrowseQuery())` result —
 * one MediaStore pass (design §2). The [query] text filter is the cheap
 * LIB-5 stretch: purely local, zero extra source reads.
 */
class LibraryViewModel(
    private val source: Source,
    private val player: LibraryPlayer,
    private val permissionGranted: Boolean,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _state = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (!permissionGranted) {
            _state.value = LibraryUiState.Denied
            return
        }
        scope.launch {
            _state.value = LibraryUiState.Loading
            val result = withContext(io) { source.browse(BrowseQuery()) }
            _state.value = when (result) {
                is SourceResult.Ok ->
                    if (result.v.isEmpty()) {
                        LibraryUiState.Empty
                    } else {
                        contentOf(result.v, priorSelections())
                    }
                is SourceResult.Fail -> LibraryUiState.Error(messageFor(result.e))
            }
        }
    }

    fun selectSegment(segment: LibrarySegment) =
        updateContent { it.copy(segment = segment) }

    fun selectArtist(name: String?) =
        updateContent {
            it.copy(
                segment = LibrarySegment.Songs,
                artistFilter = name,
                albumFilter = null,
                visibleSongs = visible(it.songs, name, null, it.query),
            )
        }

    fun selectAlbum(name: String?) =
        updateContent {
            it.copy(
                segment = LibrarySegment.Songs,
                artistFilter = null,
                albumFilter = name,
                visibleSongs = visible(it.songs, null, name, it.query),
            )
        }

    fun setQuery(query: String) =
        updateContent { it.copy(query = query, visibleSongs = visible(it.songs, it.artistFilter, it.albumFilter, query)) }

    fun clearFilters() =
        updateContent { it.copy(artistFilter = null, albumFilter = null, query = "", visibleSongs = it.songs) }

    /** Tap-to-play: hands the visible queue plus index to the player (library Req 2). */
    fun playAt(index: Int) {
        val content = _state.value as? LibraryUiState.Content ?: return
        if (index !in content.visibleSongs.indices) return
        player.playQueue(content.visibleSongs.map { source.streamOf(it) }, index)
    }

    private data class Selections(
        val segment: LibrarySegment,
        val artist: String?,
        val album: String?,
        val query: String,
    )

    private fun priorSelections(): Selections {
        val prior = _state.value as? LibraryUiState.Content
        return Selections(
            segment = prior?.segment ?: LibrarySegment.Songs,
            artist = prior?.artistFilter,
            album = prior?.albumFilter,
            query = prior?.query ?: "",
        )
    }

    private fun contentOf(songs: List<AudioItem>, selections: Selections): LibraryUiState.Content {
        val artists = songs.mapNotNull { it.subtitle }.distinct().sorted()
        val albums = songs.mapNotNull { it.album }.distinct().sorted()
        val artist = selections.artist?.takeIf { it in artists }
        val album = selections.album?.takeIf { it in albums }
        return LibraryUiState.Content(
            songs = songs,
            artists = artists,
            albums = albums,
            segment = selections.segment,
            artistFilter = artist,
            albumFilter = album,
            query = selections.query,
            visibleSongs = visible(songs, artist, album, selections.query),
        )
    }

    private fun visible(
        songs: List<AudioItem>,
        artist: String?,
        album: String?,
        query: String,
    ): List<AudioItem> {
        val needle = query.trim()
        return songs.filter { item ->
            (artist == null || item.subtitle == artist) &&
                (album == null || item.album == album) &&
                (needle.isEmpty() ||
                    item.title.contains(needle, ignoreCase = true) ||
                    item.subtitle?.contains(needle, ignoreCase = true) == true ||
                    item.album?.contains(needle, ignoreCase = true) == true)
        }
    }

    private inline fun updateContent(transform: (LibraryUiState.Content) -> LibraryUiState.Content) {
        val current = _state.value as? LibraryUiState.Content ?: return
        _state.value = transform(current)
    }

    private fun messageFor(error: SourceError): String = when (error) {
        is SourceError.Offline -> "You're offline"
        is SourceError.Timeout -> "Request timed out"
        is SourceError.Server -> "Server error"
        is SourceError.Unknown -> "Couldn't load your music"
    }
}
