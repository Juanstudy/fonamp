package com.fonamp.feature.library

import androidx.media3.common.MediaItem

/**
 * Slice F: minimal playback seam for the Collection tab (library Req 2).
 *
 * "Fav + seam" decision: this interface is deliberately kept as the single
 * narrow bridge to the shared `core/player` PlayerManager (which lands in
 * Slice G with the identical `play(items, index)` signature). The app graph
 * binds it once via `LibraryBridgeModule` → `LibraryPlayerAdapter`, so at
 * runtime there is exactly one player — no double seam, only a compile-time
 * contract that keeps `feature/library` decoupled and its tests on a
 * hand-written recording fake (no MockK per the stack). Tests use that
 * recording fake; production goes through the adapter. Do not widen this
 * interface: pause/next/seek live on PlayerManager.
 */
interface LibraryPlayer {
    fun playQueue(items: List<MediaItem>, index: Int)
}
