package com.fonamp.feature.library

import androidx.media3.common.MediaItem

/**
 * Slice F: minimal playback seam for the Collection tab (library Req 2).
 *
 * The shared `core/player` PlayerManager lands in Slice G with the identical
 * `play(items, index)` signature; app wiring (Slice I) binds this interface
 * to it. Tests use a hand-written recording fake — no MockK per the stack.
 */
interface LibraryPlayer {
    fun playQueue(items: List<MediaItem>, index: Int)
}
