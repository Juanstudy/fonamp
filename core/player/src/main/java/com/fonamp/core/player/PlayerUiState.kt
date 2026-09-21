package com.fonamp.core.player

import androidx.media3.common.MediaItem

/**
 * Everything the mini-player and sheet render (player Req 5).
 *
 * `positionMs` is the last seek/current anchor for the local seek bar; it is
 * always 0 for live radio (no seek bar). `sleepEndsAtMs` is the monotonic
 * ([SystemClock.elapsedRealtime]) sleep-timer deadline, null when no timer is
 * armed. Transport modes (`shuffleEnabled`, [repeatMode], [speed]) persist
 * across queue replacements. v1 has no EQ state.
 */
data class PlayerUiState(
    val queue: List<MediaItem> = emptyList(),
    val index: Int = 0,
    val isPlaying: Boolean = false,
    val isLive: Boolean = false,
    val icyTitle: String? = null,
    val error: PlayerError? = null,
    val positionMs: Long = 0L,
    val sleepEndsAtMs: Long? = null,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val speed: Float = 1f,
)
