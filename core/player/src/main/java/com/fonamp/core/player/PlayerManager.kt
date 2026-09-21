package com.fonamp.core.player

import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.StateFlow

/**
 * The single playback seam every screen talks to (design §3, player Req 1).
 *
 * UI connects via this interface only — never ExoPlayer directly — so
 * ViewModel tests run Robolectric-free against [FakePlayerManager]. The
 * production binding is [DefaultPlayerManager], which holds a
 * `MediaController` to [PlaybackService]; Hilt wires it in `app` (Slice I).
 *
 * Per-source model (player Req 3): radio is play/stop ([togglePlayPause] on a
 * playing live item stops, keeping the live edge honest; [seekTo] is a no-op
 * when live), local is pause/resume plus [next]/[prev]/[seekTo], plus sleep
 * timer ([setSleepTimer]) and full transport controls ([cycleSpeed],
 * [toggleShuffle], [cycleRepeat]) for both sources. No EQ in v1.
 */
interface PlayerManager {
    /** Render state for the mini-player and sheet. */
    val state: StateFlow<PlayerUiState>

    /** Replace the queue and start [index]. Clears any banner. */
    fun play(items: List<MediaItem>, index: Int)

    /** Convenience for one-tap radio/local play. */
    fun playSingle(item: MediaItem) = play(listOf(item), 0)

    /**
     * Pause/resume for local; for a playing live item this STOPS (live has no
     * pause position), otherwise it starts playback.
     */
    fun togglePlayPause()

    /** Halt playback, keeping the queue context. Never auto-resumes. */
    fun stop()

    /** Next queue item; clamped no-op at the end or on an empty queue. */
    fun next()

    /** Previous queue item; clamped no-op at the start or on an empty queue. */
    fun prev()

    /** Seek for local queues; no-op when the current item is live. */
    fun seekTo(positionMs: Long)

    /** Clear the banner and re-attempt the current queue/index. */
    fun retry()

    /**
     * Arm a sleep timer that halts playback [durationMs] from now (one-shot,
     * keeps the queue, never auto-resumes). Replaces any active timer.
     * Starting fresh playback ([play]) or a manual [stop] clears it.
     */
    fun setSleepTimer(durationMs: Long)

    /** Cancel any active sleep timer; no-op when none. */
    fun clearSleepTimer()

    /**
     * Advance playback speed through [SPEEDS] (wraps to 1x). Applies to
     * local and radio; modes persist across queue replacements.
     */
    fun cycleSpeed()

    /** Toggle shuffle mode; modes persist across queue replacements. */
    fun toggleShuffle()

    /** Cycle repeat OFF → ALL → ONE → OFF. */
    fun cycleRepeat()

    companion object {
        /** Speed cycle, starting and wrapping at 1x. */
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 1.75f, 2f, 0.5f, 0.75f)

        /** Next speed after [current]; unknown values reset to 1x. */
        fun nextSpeed(current: Float): Float {
            val index = SPEEDS.indexOf(current)
            return if (index < 0) 1f else SPEEDS[(index + 1) % SPEEDS.size]
        }
    }
}
