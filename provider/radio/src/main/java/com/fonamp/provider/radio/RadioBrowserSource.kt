package com.fonamp.provider.radio

import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.DirectoryEntry
import com.fonamp.core.network.NetworkError
import com.fonamp.core.network.RadioBrowserClient
import com.fonamp.core.network.StationDto
import com.fonamp.core.network.StationQuery
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.api.radioMediaItem
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Radio-browser directory source, id `radio-browser` (design §2, source-contract
 * Req 3–4). Depends only on `provider/api` + `core/network`.
 *
 * - `browse(BrowseQuery())` → Discover index (countries + genres/tags) from the
 *   24h [DirectoryCache]; fresh cache serves with zero network, stale/missing
 *   triggers fetch, failed refresh keeps stale and returns typed `Fail`.
 * - `browse(filtered)` → station list for one country/genre/tag selection.
 * - `search(q)` → client-side filter over cached station lists only (radio
 *   Req 6: no directory name-search); zero network, empty cache → `Ok(empty)`.
 * - Click-count (`POST json/url/{uuid}`) fires from [streamOf] via
 *   `scope.launch + runCatching` — swallowed, never blocks or fails playback.
 *
 * Index convention: index rows are navigation nodes, not playable audio —
 * `streamUri` is empty and `stableId` is prefixed `country:` / `tag:` /
 * `genre:`. Feature code must browse deeper, never play them.
 */
class RadioBrowserSource(
    private val client: RadioBrowserClient,
    private val cache: DirectoryCache,
    private val clickScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : Source {

    override val id: String = SOURCE_ID
    override val kind: SourceKind = SourceKind.RADIO

    override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> {
        if (query.isIndex()) return browseIndex()
        val stationQuery = StationQuery(
            country = query.country,
            genre = query.genre,
            tag = query.tag,
        )
        val key = DirectoryCache.stationKey(stationQuery)
        val cached = cache.getStations(key)
        if (cached != null && !cache.isStale(cached.fetchedAt)) {
            return SourceResult.Ok(cached.stations.map { it.toAudioItem() })
        }
        return try {
            val fresh = client.fetchStations(stationQuery)
            cache.putStations(key, fresh)
            SourceResult.Ok(fresh.map { it.toAudioItem() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Stale cache stays in place for the UI's Offline card; error is typed.
            SourceResult.Fail(mapError(e))
        }
    }

    override suspend fun search(q: String): SourceResult<List<AudioItem>> {
        if (q.isBlank()) return SourceResult.Ok(emptyList())
        val needle = q.trim()
        return SourceResult.Ok(
            cache.allStations()
                .filter { it.matches(needle) }
                .map { it.toAudioItem() },
        )
    }

    override fun streamOf(item: AudioItem): MediaItem {
        item.stationUuid?.takeIf { it.isNotBlank() }?.let { recordClick(it) }
        return radioMediaItem(item)
    }

    /** Fire-and-forget click-count; failure is swallowed, never user-visible. */
    fun recordClick(stationUuid: String) {
        clickScope.launch { runCatching { client.click(stationUuid) } }
    }

    private suspend fun browseIndex(): SourceResult<List<AudioItem>> {
        val cached = cache.getIndex()
        if (cached != null && !cache.isStale(cached.fetchedAt)) {
            return SourceResult.Ok(cached.index.toAudioItems())
        }
        return try {
            val fresh = client.fetchIndex()
            cache.putIndex(fresh)
            SourceResult.Ok(fresh.toAudioItems())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            SourceResult.Fail(mapError(e))
        }
    }

    private fun mapError(e: Throwable): SourceError = when (val n = RadioBrowserClient.mapToNetworkError(e)) {
        NetworkError.Offline -> SourceError.Offline
        NetworkError.Timeout -> SourceError.Timeout
        is NetworkError.Server -> SourceError.Server(n.code)
        is NetworkError.Unknown -> SourceError.Unknown(n.error ?: e)
    }

    companion object {
        const val SOURCE_ID = "radio-browser"
    }
}

private fun BrowseQuery.isIndex(): Boolean =
    country == null && genre == null && tag == null

private fun StationDto.matches(needle: String): Boolean =
    name.contains(needle, ignoreCase = true) ||
        (country?.contains(needle, ignoreCase = true) == true) ||
        tagList.any { it.contains(needle, ignoreCase = true) } ||
        (codec?.contains(needle, ignoreCase = true) == true)

private fun StationDto.toAudioItem(): AudioItem {
    val subtitle = (listOfNotNull(country) + tagList)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
    return AudioItem(
        sourceId = RadioBrowserSource.SOURCE_ID,
        stableId = stationuuid,
        title = name,
        subtitle = subtitle,
        album = null,
        durationMs = null,
        streamUri = resolvedStreamUrl,
        bitrate = bitrate?.takeIf { it > 0 },
        codec = codec?.takeIf { it.isNotBlank() },
        stationUuid = stationuuid,
        country = country,
        tags = tagList,
    )
}

private fun com.fonamp.core.network.DirectoryIndex.toAudioItems(): List<AudioItem> =
    countries.map { it.toAudioItem("country") } + tags.map { it.toAudioItem("tag") }

private fun DirectoryEntry.toAudioItem(kind: String): AudioItem {
    val countSuffix = stationCount?.let { " · $it stations" }.orEmpty()
    return AudioItem(
        sourceId = RadioBrowserSource.SOURCE_ID,
        stableId = "$kind:$name",
        title = name,
        subtitle = (kind.replaceFirstChar { it.uppercase() } + countSuffix).takeIf { countSuffix.isNotEmpty() },
        album = null,
        durationMs = null,
        streamUri = "",
        stationUuid = null,
        country = if (kind == "country") name else null,
    )
}
