package com.fonamp.core.player

import androidx.media3.common.MediaItem

/**
 * Pure queue-restore mapping (player Req 6).
 *
 * Turns a persisted [SavedQueue] (mediaIds + index + position) back into
 * playable context given the currently resolvable [candidates]. Every
 * snapshot id that no longer resolves (revoked permission, deleted track,
 * uncached directory station) is dropped; the index is clamped to what
 * survived. Position is forced to 0 for live radio. Returns null when
 * nothing can be restored — the caller must leave the player idle.
 */
data class ResolvedQueue(
    val items: List<MediaItem>,
    val index: Int,
    val positionMs: Long,
)

object QueueResolver {
    fun resolve(snapshot: SavedQueue?, candidates: List<MediaItem>): ResolvedQueue? {
        if (snapshot == null || snapshot.mediaIds.isEmpty() || candidates.isEmpty()) return null
        val byId = candidates.associateBy { it.mediaId }
        val items = snapshot.mediaIds.mapNotNull { byId[it] }
        if (items.isEmpty()) return null
        val safeIndex = snapshot.index.coerceIn(items.indices)
        val position = if (items[safeIndex].isLive()) 0L else snapshot.positionMs.coerceAtLeast(0L)
        return ResolvedQueue(items, safeIndex, position)
    }
}
