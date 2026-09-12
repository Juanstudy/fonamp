package com.fonamp.core.player

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * The ONE player for every source (player Req 1, design §3).
 *
 * Owns a single `ExoPlayer`, exposes it through a `MediaSession`, and is
 * reached by the UI only via `MediaController` ([DefaultPlayerManager]).
 * Foreground-service state is managed by the Media3 library: with a session
 * and notification provider present, the service runs in the foreground only
 * while playing and drops out afterwards (player Req 2). Media-button /
 * headset intents and the play/pause/next/previous notification (kept in sync
 * by the library) come free with the session; `setHandleAudioBecomingNoisy`
 * pauses on headset unplug.
 *
 * - Stream config: [CONNECT_TIMEOUT_MS]/[READ_TIMEOUT_MS] 10 s on the HTTP
 *   data source (player Req 4); stalled streams surface as
 *   `PlaybackException` → [PlayerError] banner + manual retry, never a stall.
 * - ICY: the `Icy-MetaData: 1` request header asks radio streams for
 *   in-band metadata, surfaced through the session's `MediaMetadata` and
 *   read by [DefaultPlayerManager] into [PlayerUiState.icyTitle] (Req 3).
 * - Process death (Req 6): queue context (mediaIds + position) is saved
 *   best-effort to [PrefsQueueStore] on transitions and loaded on create.
 *   MediaItems themselves cannot survive death without their sources, so a
 *   fresh process always starts idle — coherent, never phantom-playing —
 *   and keeps the last snapshot for future source-assisted restore.
 */
class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null
    private var queueStore: QueueStore? = null

    /** Last persisted queue context, if any (null on clean installs). */
    var lastSnapshot: SavedQueue? = null
        private set

    override fun onCreate() {
        super.onCreate()
        val store = PrefsQueueStore(this)
        queueStore = store
        lastSnapshot = runCatching { store.load() }.getOrNull()

        val httpDataSource = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
            .setReadTimeoutMs(READ_TIMEOUT_MS)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(ICY_REQUEST_HEADER to ICY_REQUEST_VALUE))

        // handleAudioFocus=true is passed to setAudioAttributes below (the
        // Media3 Builder has no such setter; focus handling lives there).
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSource))
            .build()
        player = exoPlayer
        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                saveQueue(exoPlayer)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                saveQueue(exoPlayer)
            }
        })
        session = MediaSession.Builder(this, exoPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        // Leaving via recents while paused: do not linger.
        if (player?.isPlaying != true) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            release()
            session = null
        }
        player?.run {
            release()
            player = null
        }
        super.onDestroy()
    }

    private fun saveQueue(exoPlayer: ExoPlayer) {
        runCatching {
            val count = exoPlayer.mediaItemCount
            if (count == 0) {
                queueStore?.clear()
                lastSnapshot = null
            } else {
                val ids = (0 until count).map { exoPlayer.getMediaItemAt(it).mediaId }
                queueStore?.save(ids, exoPlayer.currentMediaItemIndex, exoPlayer.currentPosition)
                lastSnapshot = queueStore?.load()
            }
        }
    }

    companion object {
        /** Stream connect timeout: stalled connects fail fast (Req 4). */
        const val CONNECT_TIMEOUT_MS = 10_000

        /** Stream read timeout: stalled mid-stream reads fail fast (Req 4). */
        const val READ_TIMEOUT_MS = 10_000

        private const val ICY_REQUEST_HEADER = "Icy-MetaData"
        private const val ICY_REQUEST_VALUE = "1"
    }
}
