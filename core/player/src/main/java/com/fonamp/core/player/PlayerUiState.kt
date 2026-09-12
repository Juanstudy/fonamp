package com.fonamp.core.player

import androidx.media3.common.MediaItem

/**
 * Everything the mini-player and sheet render (player Req 5).
 *
 * `positionMs` is the last seek/current anchor for the local seek bar; it is
 * always 0 for live radio (no seek bar). v1 explicitly has no
 * shuffle/repeat/speed/sleep state.
 */
data class PlayerUiState(
    val queue: List<MediaItem> = emptyList(),
    val index: Int = 0,
    val isPlaying: Boolean = false,
    val isLive: Boolean = false,
    val icyTitle: String? = null,
    val error: PlayerError? = null,
    val positionMs: Long = 0L,
)
