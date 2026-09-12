package com.fonamp.feature.radio

import androidx.media3.common.MediaItem
import com.fonamp.core.database.FavoriteDao
import com.fonamp.core.database.FavoriteStation
import com.fonamp.core.player.PlayerManager
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceExtras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Station uuid carried by a playing [MediaItem] (player Req 3, design §2).
 *
 * Prefers the `station_uuid` extra written by `radioMediaItem`; falls back to
 * the `radio:<uuid>` mediaId prefix. Returns null for local items (which
 * carry `local:` ids and no station extra) — the sheet hides the heart then.
 */
fun MediaItem.stationUuid(): String? {
    mediaMetadata.extras?.getString(SourceExtras.KEY_STATION_UUID)
        ?.takeIf { it.isNotBlank() }?.let { return it }
    if (!mediaId.startsWith("radio:")) return null
    return mediaId.removePrefix("radio:").takeIf { it.isNotBlank() }
}

/**
 * Slice H: Favorites tab states (radio Req 4 — stations-only, local-only,
 * no sync/account/export).
 */
sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data object Empty : FavoritesUiState
    data class Content(val favorites: List<FavoriteStation>) : FavoritesUiState
}

/**
 * Slice H: Room-backed favorites holder — talks to [FavoriteDao], the radio
 * [Source] (for `streamOf` mapping), and [PlayerManager] only.
 *
 * Removal holds the deleted row in [pendingUndo] for a 10s snackbar window;
 * [undo] re-upserts the full row (Slice D contract). A plain class with an
 * injected scope (Slice F precedent); Slice I binds it into Hilt.
 */
class FavoritesViewModel(
    private val favorites: FavoriteDao,
    private val source: Source,
    private val player: PlayerManager,
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
) {
    private val _state = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    /** Raw station-uuid set behind [FavoritesUiState] — the sheet heart reads this. */
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _pendingUndo = MutableStateFlow<FavoriteStation?>(null)
    val pendingUndo: StateFlow<FavoriteStation?> = _pendingUndo.asStateFlow()

    init {
        scope.launch {
            favorites.observeAll().collect { rows ->
                _favoriteIds.value = rows.map { it.stationUuid }.toSet()
                _state.value = if (rows.isEmpty()) FavoritesUiState.Empty
                else FavoritesUiState.Content(rows)
            }
        }
    }

    fun remove(favorite: FavoriteStation) {
        scope.launch {
            favorites.delete(favorite.stationUuid)
            _pendingUndo.value = favorite
            delay(UNDO_WINDOW_MS)
            if (_pendingUndo.value?.stationUuid == favorite.stationUuid) {
                _pendingUndo.value = null
            }
        }
    }

    fun undo() {
        val row = _pendingUndo.value ?: return
        _pendingUndo.value = null
        scope.launch { favorites.upsert(row) }
    }

    /** 1-tap play from Favorites (radio Req 3); blank stream urls never play. */
    fun play(favorite: FavoriteStation) {
        if (favorite.streamUrl.isBlank()) return
        player.playSingle(source.streamOf(favorite.toAudioItem()))
    }

    /**
     * Sheet heart for the now-playing radio item (player Req 3): same toggle
     * semantics as `RadioViewModel.toggleFavorite` — delete when already a
     * favorite, else upsert — but starting from the [MediaItem] the player
     * holds instead of the directory [AudioItem].
     *
     * Non-radio items (no station uuid) are a no-op, so local playback never
     * grows a heart. Inserts without a stream uri are refused: an unplayable
     * row must never reach the DAO (`play` would refuse it anyway). Tag /
     * bitrate / codec details are unavailable on the `MediaItem` edge
     * (`radioMediaItem` carries no such extras and `provider/api` owns that
     * mapping), so sheet-created rows store nulls there — hearting a playing
     * station never fabricates metadata (design §10).
     */
    fun toggleMediaItem(item: MediaItem) {
        val uuid = item.stationUuid() ?: return
        scope.launch {
            if (favoriteIds.value.contains(uuid)) {
                favorites.delete(uuid)
            } else {
                val streamUrl = item.localConfiguration?.uri?.toString().orEmpty()
                if (streamUrl.isBlank()) return@launch
                favorites.upsert(
                    FavoriteStation(
                        stationUuid = uuid,
                        name = item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: uuid,
                        streamUrl = streamUrl,
                        country = item.mediaMetadata.extras?.getString(SourceExtras.KEY_COUNTRY),
                        tagsCsv = null,
                        bitrate = null,
                        codec = null,
                        favoritedAt = nowMs(),
                    ),
                )
            }
        }
    }

    companion object {
        const val UNDO_WINDOW_MS = 10_000L
    }
}

private fun FavoriteStation.toAudioItem() = AudioItem(
    sourceId = "radio-browser",
    stableId = stationUuid,
    title = name,
    subtitle = (listOfNotNull(country) + tagsCsv.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() })
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", "),
    streamUri = streamUrl,
    bitrate = bitrate,
    codec = codec,
    stationUuid = stationUuid,
    country = country,
    tags = tagsCsv.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() },
)
