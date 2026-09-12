package com.fonamp.provider.local

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.api.localMediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Slice F: `Source` with id `local` over MediaStore (source-contract Req 2).
 *
 * Single-pass `query(EXTERNAL_CONTENT_URI, projection, IS_MUSIC != 0)`; no
 * network, no configuration. [BrowseQuery] filters are ignored — grouping
 * into Songs/Artists/Albums happens in `feature/library` from one
 * `browse(BrowseQuery())` result (design §2). [search] is a client-side
 * title/artist/album filter over the same single read.
 *
 * Metadata honesty: absent artist/album/duration/artwork stay null (never
 * fabricated); bitrate/codec are always null for local. A missing title
 * falls back to the display label `"Unknown title"` (the row is still
 * playable, so it is kept, not dropped). Artwork is the album-art content
 * URI derived from ALBUM_ID — null when the track carries no album id.
 */
class LocalSource(
    private val reader: MediaStoreReader,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : Source {

    /** Production wiring: real [ContentResolver] behind the reader seam. */
    constructor(
        resolver: ContentResolver,
        io: CoroutineDispatcher = Dispatchers.IO,
    ) : this(ContentResolverReader(resolver), io)

    override val id: String = "local"
    override val kind: SourceKind = SourceKind.LOCAL

    override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> =
        withContext(io) {
            runCatching { readAll() }.fold(
                onSuccess = { SourceResult.Ok(it) },
                onFailure = { SourceResult.Fail(SourceError.Unknown(it)) },
            )
        }

    override suspend fun search(q: String): SourceResult<List<AudioItem>> =
        withContext(io) {
            runCatching {
                val needle = q.trim()
                readAll().filter { item ->
                    needle.isEmpty() ||
                        item.title.contains(needle, ignoreCase = true) ||
                        item.subtitle?.contains(needle, ignoreCase = true) == true ||
                        item.album?.contains(needle, ignoreCase = true) == true
                }
            }.fold(
                onSuccess = { SourceResult.Ok(it) },
                onFailure = { SourceResult.Fail(SourceError.Unknown(it)) },
            )
        }

    override fun streamOf(item: AudioItem): MediaItem = localMediaItem(item)

    private fun readAll(): List<AudioItem> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
        )
        reader.query(
            uri,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            MediaStore.Audio.Media.TITLE,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            if (idCol == -1) return emptyList()
            return buildList {
                while (cursor.moveToNext()) {
                    val audioId = cursor.getLong(idCol)
                    add(
                        AudioItem(
                            sourceId = "local",
                            stableId = audioId.toString(),
                            title = cursor.optString(titleCol) ?: "Unknown title",
                            subtitle = cursor.optString(artistCol),
                            album = cursor.optString(albumCol),
                            durationMs = if (durationCol != -1 && !cursor.isNull(durationCol)) {
                                cursor.getLong(durationCol)
                            } else {
                                null
                            },
                            streamUri = ContentUris.withAppendedId(uri, audioId).toString(),
                            artworkUri = if (albumIdCol != -1 && !cursor.isNull(albumIdCol)) {
                                ContentUris.withAppendedId(
                                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                                    cursor.getLong(albumIdCol),
                                ).toString()
                            } else {
                                null
                            },
                        )
                    )
                }
            }
        }
        return emptyList()
    }

    private fun android.database.Cursor.optString(col: Int): String? =
        if (col != -1 && !isNull(col)) getString(col) else null
}
