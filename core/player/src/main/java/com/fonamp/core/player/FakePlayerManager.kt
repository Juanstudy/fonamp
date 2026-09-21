package com.fonamp.core.player

import android.os.SystemClock
import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Main-sourceset [PlayerManager] fake (Slice D `FakeFavoriteDao` precedent):
 * ViewModel tests in feature modules inject this — no MockK, no Robolectric.
 *
 * Synchronous and deterministic: every command updates [state] immediately so
 * Turbine assertions never flake. Test-only seams (all fake-local helpers):
 * - [failNextWith] makes the next [play]/[retry] land in the banner state
 *   instead of playing (stable until consumed once).
 * - [restore] installs a process-death snapshot as paused context — restored
 *   queue or clean idle, never phantom-playing.
 */
class FakePlayerManager : PlayerManager {

    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var pendingFailure: PlayerError? = null

    /** Next [play]/[retry] surfaces [error] as a banner instead of playing. */
    fun failNextWith(error: PlayerError) {
        pendingFailure = error
    }

    /**
     * Install a saved queue as paused context (process-death coherence).
     * Empty snapshot → clean idle. Never sets `isPlaying=true`. [positionMs]
     * is kept for local items and forced to 0 for live radio.
     */
    override fun restore(items: List<MediaItem>, index: Int, positionMs: Long) {
        if (items.isEmpty()) {
            _state.value = PlayerUiState()
            return
        }
        val safeIndex = index.coerceIn(items.indices)
        val isLive = items[safeIndex].isLive()
        _state.value = PlayerUiState(
            queue = items,
            index = safeIndex,
            isPlaying = false,
            isLive = isLive,
            icyTitle = null,
            error = null,
            positionMs = if (isLive) 0L else positionMs.coerceAtLeast(0L),
        )
    }

    override fun play(items: List<MediaItem>, index: Int) {
        if (items.isEmpty()) return
        val failure = pendingFailure
        pendingFailure = null
        val safeIndex = index.coerceIn(items.indices)
        _state.value = if (failure != null) {
            PlayerUiState(
                queue = items,
                index = safeIndex,
                isPlaying = false,
                isLive = items[safeIndex].isLive(),
                error = failure,
            )
        } else {
            PlayerUiState(
                queue = items,
                index = safeIndex,
                isPlaying = true,
                isLive = items[safeIndex].isLive(),
                error = null,
                positionMs = 0L,
            )
        }
    }

    override fun togglePlayPause() {
        _state.update { current ->
            if (current.queue.isEmpty()) {
                current
            } else if (current.isLive && current.isPlaying) {
                // Live edge has no pause position: toggle stops.
                current.copy(isPlaying = false, icyTitle = null)
            } else {
                current.copy(isPlaying = !current.isPlaying)
            }
        }
    }

    override fun stop() {
        _state.update { current ->
            if (current.queue.isEmpty()) current
            else current.copy(isPlaying = false, icyTitle = null, sleepEndsAtMs = null)
        }
    }

    override fun next() {
        _state.update { current ->
            val nextIndex = current.index + 1
            if (current.queue.isEmpty() || nextIndex !in current.queue.indices) {
                current
            } else {
                current.copy(
                    index = nextIndex,
                    isLive = current.queue[nextIndex].isLive(),
                    icyTitle = null,
                    positionMs = 0L,
                )
            }
        }
    }

    override fun prev() {
        _state.update { current ->
            val prevIndex = current.index - 1
            if (current.queue.isEmpty() || prevIndex !in current.queue.indices) {
                current
            } else {
                current.copy(
                    index = prevIndex,
                    isLive = current.queue[prevIndex].isLive(),
                    icyTitle = null,
                    positionMs = 0L,
                )
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        _state.update { current ->
            if (current.queue.isEmpty() || current.isLive) current
            else current.copy(positionMs = positionMs.coerceAtLeast(0L))
        }
    }

    override fun retry() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        val failure = pendingFailure
        pendingFailure = null
        _state.value = if (failure != null) {
            current.copy(isPlaying = false, error = failure)
        } else {
            current.copy(isPlaying = true, error = null)
        }
    }

    override fun setSleepTimer(durationMs: Long) {
        require(durationMs > 0L) { "Sleep timer duration must be positive, was=$durationMs" }
        _state.update { current ->
            current.copy(sleepEndsAtMs = SystemClock.elapsedRealtime() + durationMs)
        }
    }

    override fun clearSleepTimer() {
        _state.update { current -> current.copy(sleepEndsAtMs = null) }
    }
}
