package com.fonamp.core.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Production [PlayerManager]: holds the `MediaController` connected to
 * [PlaybackService]'s session and maps controller callbacks to [PlayerUiState]
 * (design §3). Hilt-bound via `@Inject`/`@Singleton`; `app` (Slice I) exposes
 * it as the [PlayerManager] binding. Commands issued before the async connect
 * completes update [state] optimistically and are replayed to the controller
 * once connected, so the UI never blocks on the bind.
 *
 * Platform-bound behavior (headset buttons, notification sync, audio focus)
 * is owned by the Media3 session/service pair and covered via
 * [FakePlayerManager] flows, not JVM tests — there is no unit-testable
 * `MediaController` on the JVM.
 */
@Singleton
class DefaultPlayerManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : PlayerManager {

    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private var positionJob: Job? = null

    /**
     * Sleep timer countdown on the manager's own scope: the foreground
     * service keeps this process alive while audio plays, so no alarm,
     * permission, or receiver is needed. Expiry funnels through [stop],
     * which also clears the timer state.
     */
    private val sleepTimer = SleepTimer(scope, onExpire = { stop() })

    @Volatile
    private var controller: MediaController? = null

    private val pendingConnect: Boolean
        get() = controller == null

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                delay(500L)
                val c = controller ?: break
                if (!_state.value.isPlaying || _state.value.isLive) break
                val pos = c.currentPosition.coerceAtLeast(0L)
                _state.update { it.copy(positionMs = pos) }
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val currentPos = controller?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs
            _state.update { it.copy(isPlaying = isPlaying, positionMs = currentPos) }
            if (isPlaying && !_state.value.isLive) {
                startPositionTicker()
            } else {
                stopPositionTicker()
            }
        }

        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            val c = controller ?: return
            val isLive = item?.isLive() ?: false
            _state.update {
                it.copy(
                    index = c.currentMediaItemIndex.coerceAtLeast(0),
                    isLive = isLive,
                    icyTitle = null,
                    positionMs = 0L,
                )
            }
            if (_state.value.isPlaying && !isLive) {
                startPositionTicker()
            } else {
                stopPositionTicker()
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val current = _state.value
            if (!current.isLive) return
            val title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: return
            _state.update { it.copy(icyTitle = title) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(isPlaying = false, error = mapError(error)) }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) {
                _state.update { it.copy(positionMs = newPosition.positionMs.coerceAtLeast(0L)) }
            }
        }
    }

    init {
        connect()
    }

    override fun play(items: List<MediaItem>, index: Int) {
        if (items.isEmpty()) return
        // Fresh playback context: a timer armed for something else must not
        // fire into the new queue.
        sleepTimer.clear()
        val safeIndex = index.coerceIn(items.indices)
        _state.value = PlayerUiState(
            queue = items,
            index = safeIndex,
            isPlaying = true,
            isLive = items[safeIndex].isLive(),
            error = null,
            positionMs = 0L,
        )
        controller?.let {
            it.setMediaItems(items, safeIndex, 0L)
            it.prepare()
            it.play()
        } ?: connect()
    }

    override fun togglePlayPause() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        val c = controller
        if (c == null) {
            // Optimistic while connecting; replayed on connect.
            _state.update {
                if (it.isLive && it.isPlaying) it.copy(isPlaying = false)
                else it.copy(isPlaying = !it.isPlaying)
            }
            connect()
            return
        }
        if (current.isLive && current.isPlaying) {
            c.stop()
            _state.update { it.copy(isPlaying = false, icyTitle = null) }
        } else if (c.isPlaying) {
            c.pause()
        } else {
            c.play()
        }
    }

    override fun stop() {
        stopPositionTicker()
        sleepTimer.clear()
        controller?.stop()
        _state.update {
            if (it.queue.isEmpty()) it
            else it.copy(isPlaying = false, icyTitle = null, sleepEndsAtMs = null)
        }
    }

    override fun next() {
        val c = controller
        if (c != null) {
            if (c.hasNextMediaItem()) c.seekToNextMediaItem()
            return
        }
        _state.update { current ->
            val nextIndex = current.index + 1
            if (current.queue.isEmpty() || nextIndex !in current.queue.indices) current
            else current.copy(
                index = nextIndex,
                isLive = current.queue[nextIndex].isLive(),
                icyTitle = null,
                positionMs = 0L,
            )
        }
    }

    override fun prev() {
        val c = controller
        if (c != null) {
            if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
            return
        }
        _state.update { current ->
            val prevIndex = current.index - 1
            if (current.queue.isEmpty() || prevIndex !in current.queue.indices) current
            else current.copy(
                index = prevIndex,
                isLive = current.queue[prevIndex].isLive(),
                icyTitle = null,
                positionMs = 0L,
            )
        }
    }

    override fun seekTo(positionMs: Long) {
        val current = _state.value
        if (current.queue.isEmpty() || current.isLive) return
        val target = positionMs.coerceAtLeast(0L)
        controller?.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    override fun retry() {
        val current = _state.value
        if (current.queue.isEmpty()) return
        _state.update { it.copy(isPlaying = true, error = null, positionMs = 0L) }
        controller?.let {
            it.setMediaItems(current.queue, current.index.coerceIn(current.queue.indices), 0L)
            it.prepare()
            it.play()
        } ?: connect()
    }

    override fun restore(items: List<MediaItem>, index: Int, positionMs: Long) {
        if (items.isEmpty()) return
        // Restored context must never auto-play: a timer armed for something
        // else must not fire into it either.
        sleepTimer.clear()
        stopPositionTicker()
        val safeIndex = index.coerceIn(items.indices)
        val isLive = items[safeIndex].isLive()
        val safePosition = if (isLive) 0L else positionMs.coerceAtLeast(0L)
        _state.value = PlayerUiState(
            queue = items,
            index = safeIndex,
            isPlaying = false,
            isLive = isLive,
            error = null,
            positionMs = safePosition,
        )
        controller?.let {
            runCatching {
                it.setMediaItems(items, safeIndex, safePosition)
                it.prepare()
                it.pause()
            }
        } ?: connect()
    }

    override fun setSleepTimer(durationMs: Long) {
        sleepTimer.set(durationMs)
        _state.update { it.copy(sleepEndsAtMs = sleepTimer.endsAtMs.value) }
    }

    override fun clearSleepTimer() {
        sleepTimer.clear()
        _state.update { it.copy(sleepEndsAtMs = null) }
    }

    /** Best-effort release; the service owns the real player lifetime. */
    fun release() {
        stopPositionTicker()
        sleepTimer.clear()
        scope.cancel()
        runCatching { controller?.removeListener(listener) }
        runCatching { controller?.release() }
        controller = null
    }

    private fun connect() {
        if (!pendingConnect) return
        runCatching {
            val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
            val future = MediaController.Builder(appContext, token).buildAsync()
            future.addListener(
                {
                    runCatching {
                        val c = future.get()
                        c.addListener(listener)
                        controller = c
                        syncPendingToController(c)
                    }
                },
                MoreExecutors.directExecutor(),
            )
        }
    }

    /**
     * Replays optimistic pre-connect state (play/restore/retry/toggle issued
     * while unbound) so the controller converges with what the UI already
     * shows. Restored queues replay paused at their saved position — never
     * auto-playing.
     */
    private fun syncPendingToController(c: MediaController) {
        val current = _state.value
        if (current.queue.isEmpty()) return
        runCatching {
            val startMs = if (current.isLive) 0L else current.positionMs.coerceAtLeast(0L)
            c.setMediaItems(current.queue, current.index.coerceIn(current.queue.indices), startMs)
            c.prepare()
            if (current.isPlaying) c.play() else c.pause()
        }
    }

    companion object {
        /**
         * Maps a player failure to the visible banner kind (player Req 4).
         * Pure function, kept beside the listener for review clarity.
         */
        fun mapError(error: PlaybackException): PlayerError {
            var cause: Throwable? = error.cause
            while (cause != null) {
                when (cause) {
                    is SocketTimeoutException -> return PlayerError.TIMEOUT
                    is UnknownHostException, is ConnectException -> return PlayerError.OFFLINE
                }
                cause = cause.cause
            }
            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlayerError.TIMEOUT
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> PlayerError.OFFLINE
                else -> PlayerError.STREAM_UNAVAILABLE
            }
        }
    }
}
