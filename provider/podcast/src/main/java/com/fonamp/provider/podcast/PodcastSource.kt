package com.fonamp.provider.podcast

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PodcastSource(
    private val itunesApi: ItunesApi,
    private val rssParser: RssParser
) : Source {
    override val id: String = "podcast"
    override val kind: SourceKind = SourceKind.PODCAST

    override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> = withContext(Dispatchers.IO) {
        val url = query.feedUrl
        if (url != null) {
            try {
                val feed = rssParser.fetchAndParse(url)
                val items = feed.episodes.map { ep ->
                    AudioItem(
                        sourceId = id,
                        stableId = ep.guid,
                        title = ep.title,
                        subtitle = feed.title,
                        album = feed.title,
                        durationMs = ep.durationMs,
                        streamUri = ep.streamUrl,
                        artworkUri = feed.artworkUrl
                    )
                }
                SourceResult.Ok(items)
            } catch (e: Exception) {
                SourceResult.Fail(SourceError.Unknown(e))
            }
        } else {
            SourceResult.Ok(emptyList()) // Or return top podcasts if we implemented that
        }
    }

    override suspend fun search(q: String): SourceResult<List<AudioItem>> = withContext(Dispatchers.IO) {
        try {
            val response = itunesApi.searchPodcasts(q)
            // A search for podcasts returns shows, not episodes.
            // But AudioItem is for playable items.
            // We could return dummy AudioItems for the shows, or we need a way to present Shows.
            // But Source assumes playable items. For now, we will return a "Show" as an AudioItem where streamUri is empty,
            // and the UI will intercept it and call browse with feedUrl.
            // Or we just map the shows to AudioItem:
            val items = response.results.mapNotNull { show ->
                if (show.feedUrl == null) return@mapNotNull null
                AudioItem(
                    sourceId = id,
                    stableId = show.feedUrl,
                    title = show.collectionName ?: "Unknown",
                    subtitle = show.artistName,
                    streamUri = show.feedUrl, // Hack: store feedUrl in streamUri for navigation
                    artworkUri = show.artworkUrl600
                )
            }
            SourceResult.Ok(items)
        } catch (e: Exception) {
            SourceResult.Fail(SourceError.Unknown(e))
        }
    }

    override fun streamOf(item: AudioItem): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(item.title)
            .setArtist(item.subtitle)
            .setAlbumTitle(item.album)
        
        if (item.artworkUri != null) {
            metadata.setArtworkUri(android.net.Uri.parse(item.artworkUri))
        }

        return MediaItem.Builder()
            .setMediaId(item.stableId)
            .setUri(item.streamUri)
            .setMediaMetadata(metadata.build())
            .build()
    }
}