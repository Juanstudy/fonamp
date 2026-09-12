package com.fonamp.provider.api

import androidx.media3.common.MediaItem

/**
 * The single expandability seam (source-contract Req 1, 5).
 * The player consumes only [MediaItem] and knows nothing of the producing source.
 * No `downloadOf` in v1 — sources implement exactly these four members.
 */
interface Source {
    val id: String
    val kind: SourceKind
    suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>>
    suspend fun search(q: String): SourceResult<List<AudioItem>>
    fun streamOf(item: AudioItem): MediaItem
}
