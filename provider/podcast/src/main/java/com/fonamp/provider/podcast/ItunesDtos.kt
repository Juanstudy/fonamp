package com.fonamp.provider.podcast

import kotlinx.serialization.Serializable

@Serializable
data class ItunesSearchResponse(
    val resultCount: Int,
    val results: List<ItunesPodcastDto>
)

@Serializable
data class ItunesPodcastDto(
    val collectionId: Long? = null,
    val collectionName: String? = null,
    val artistName: String? = null,
    val feedUrl: String? = null,
    val artworkUrl600: String? = null
)