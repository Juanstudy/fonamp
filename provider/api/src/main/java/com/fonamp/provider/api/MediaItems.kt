package com.fonamp.provider.api

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/**
 * MediaItem mapping helpers — the only thing the player sees (design §2).
 * `android.os.Bundle` here is part of the Media3 MediaItem edge, alongside
 * `androidx.media3.common.MediaItem`; every other file in this module is pure Kotlin.
 */

fun localMediaItem(item: AudioItem): MediaItem {
    val extras = Bundle().apply {
        putString(SourceExtras.KEY_SOURCE_ID, item.sourceId)
        putString(SourceExtras.KEY_STABLE_ID, item.stableId)
        putBoolean(SourceExtras.KEY_IS_LIVE, false)
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(item.title)
        .setArtist(item.subtitle)
        .setAlbumTitle(item.album)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(extras)
    item.durationMs?.let { metadata.setDurationMs(it) }
    item.artworkUri?.let { metadata.setArtworkUri(Uri.parse(it)) }
    return MediaItem.Builder()
        .setMediaId("local:${item.stableId}")
        .setUri(item.streamUri)
        .setMediaMetadata(metadata.build())
        .build()
}

fun radioMediaItem(item: AudioItem): MediaItem {
    val artistLine = (listOfNotNull(item.country) + item.tags)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
    val extras = Bundle().apply {
        putString(SourceExtras.KEY_SOURCE_ID, item.sourceId)
        putString(SourceExtras.KEY_STABLE_ID, item.stableId)
        putBoolean(SourceExtras.KEY_IS_LIVE, true)
        item.stationUuid?.let { putString(SourceExtras.KEY_STATION_UUID, it) }
        item.country?.let { putString(SourceExtras.KEY_COUNTRY, it) }
    }
    val metadata = MediaMetadata.Builder()
        .setTitle(item.title)
        .setArtist(artistLine)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(extras)
        .build()
    return MediaItem.Builder()
        .setMediaId("radio:${item.stationUuid ?: item.stableId}")
        .setUri(item.streamUri)
        .setMediaMetadata(metadata)
        .build()
}
