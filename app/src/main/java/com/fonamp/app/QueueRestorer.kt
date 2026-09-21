package com.fonamp.app

import androidx.media3.common.MediaItem
import com.fonamp.core.player.PlayerManager
import com.fonamp.core.player.QueueResolver
import com.fonamp.core.player.QueueStore
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.api.radioMediaItem
import com.fonamp.provider.radio.CuratedStations
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cold-start queue restore (player Req 6).
 *
 * Loads the persisted [QueueStore] snapshot and re-installs it via
 * [PlayerManager.restore] as paused context — never auto-playing. Candidates
 * come from the current catalog: local tracks re-browsed from `MediaStore`
 * (missing permission → no local candidates, never a crash) plus the curated
 * radio presets mapped purely. Cold start never hits the network and never
 * fires click-counts: uncached directory stations simply drop out of the
 * restored queue ([QueueResolver]). One shot: a non-empty player is left
 * untouched. Every step is best-effort — restore must never crash startup.
 */
@Singleton
class QueueRestorer @Inject constructor(
    private val player: PlayerManager,
    private val sources: Set<@JvmSuppressWildcards Source>,
    private val store: QueueStore,
) {

    /** Install the snapshot once. Returns true when a queue was restored. */
    suspend fun restoreOnce(): Boolean = runCatching {
        if (player.state.value.queue.isNotEmpty()) return false
        val snapshot = store.load() ?: return false
        val resolved = QueueResolver.resolve(snapshot, candidates()) ?: return false
        player.restore(resolved.items, resolved.index, resolved.positionMs)
        true
    }.getOrDefault(false)

    private suspend fun candidates(): List<MediaItem> = buildList {
        for (source in sources) {
            when (source.kind) {
                SourceKind.LOCAL -> {
                    val result = runCatching { source.browse(BrowseQuery()) }.getOrNull()
                    val audio = (result as? SourceResult.Ok)?.v ?: continue
                    audio.mapNotNullTo(this) { runCatching { source.streamOf(it) }.getOrNull() }
                }
                SourceKind.RADIO -> {
                    CuratedStations.browseCurated().mapTo(this, ::radioMediaItem)
                }
            }
        }
    }
}
