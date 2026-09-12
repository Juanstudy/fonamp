package com.fonamp.feature.radio

import com.fonamp.core.database.FavoriteDao
import com.fonamp.core.database.FavoriteStation
import com.fonamp.core.player.PlayerManager
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
) {
    private val _state = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private val _pendingUndo = MutableStateFlow<FavoriteStation?>(null)
    val pendingUndo: StateFlow<FavoriteStation?> = _pendingUndo.asStateFlow()

    init {
        scope.launch {
            favorites.observeAll().collect { rows ->
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
