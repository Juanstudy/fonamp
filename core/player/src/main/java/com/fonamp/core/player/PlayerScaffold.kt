package com.fonamp.core.player

import androidx.media3.common.MediaItem

/**
 * Slice A scaffold: proves the identical Media3 pin compiles in :core:player.
 * PlaybackService + PlayerManager land in Slice G.
 */
object PlayerScaffold {
    const val CONNECT_TIMEOUT_MS = 10_000
    const val READ_TIMEOUT_MS = 10_000

    fun emptyItem(): MediaItem = MediaItem.EMPTY
}
