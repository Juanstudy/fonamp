package com.fonamp.core.player

import android.content.Context

/**
 * Best-effort queue persistence for process-death coherence (player Req 6).
 *
 * Only lightweight context is stored — mediaIds plus position — because full
 * `MediaItem`s (with URIs) cannot survive death without their sources. On
 * restart the app always lands coherent: restored queue context when the
 * items are re-resolvable, otherwise clean idle — never phantom-playing
 * (see [FakePlayerManager.restore]).
 *
 * [PlaybackService] saves on queue/position transitions and loads on create;
 * every call is wrapped in `runCatching` by the implementations — persistence
 * must never crash playback.
 */
data class SavedQueue(
    val mediaIds: List<String>,
    val index: Int,
    val positionMs: Long,
)

interface QueueStore {
    fun save(mediaIds: List<String>, index: Int, positionMs: Long)
    fun load(): SavedQueue?
    fun clear()
}

/** Pure-JVM store: the default for tests and the documented contract. */
class InMemoryQueueStore : QueueStore {
    private var snapshot: SavedQueue? = null

    override fun save(mediaIds: List<String>, index: Int, positionMs: Long) {
        snapshot = if (mediaIds.isEmpty()) null
        else SavedQueue(mediaIds.toList(), index.coerceIn(mediaIds.indices), positionMs.coerceAtLeast(0L))
    }

    override fun load(): SavedQueue? = snapshot

    override fun clear() {
        snapshot = null
    }
}

/** `SharedPreferences`-backed store used by [PlaybackService]. */
class PrefsQueueStore(context: Context) : QueueStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun save(mediaIds: List<String>, index: Int, positionMs: Long) {
        runCatching {
            prefs.edit()
                .putString(KEY_IDS, mediaIds.joinToString(SEPARATOR))
                .putInt(KEY_INDEX, index)
                .putLong(KEY_POSITION, positionMs)
                .apply()
        }
    }

    override fun load(): SavedQueue? = runCatching {
        val raw = prefs.getString(KEY_IDS, null) ?: return null
        val ids = raw.split(SEPARATOR).filter { it.isNotEmpty() }
        if (ids.isEmpty()) return null
        SavedQueue(
            mediaIds = ids,
            index = prefs.getInt(KEY_INDEX, 0).coerceIn(ids.indices),
            positionMs = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
        )
    }.getOrNull()

    override fun clear() {
        runCatching { prefs.edit().clear().apply() }
    }

    companion object {
        private const val PREFS_NAME = "fonamp_playback"
        private const val KEY_IDS = "queue_media_ids"
        private const val KEY_INDEX = "queue_index"
        private const val KEY_POSITION = "queue_position_ms"
        private const val SEPARATOR = "\n"
    }
}
