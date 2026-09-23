package com.fonamp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Radio-browser directory DTOs (design §4). Every field is nullable-tolerant:
 * absent or explicit-null JSON falls back to the Kotlin default, so a sparse
 * directory row never breaks parsing.
 */
@Serializable
data class CountryDto(
    val name: String = "",
    val stationcount: Int? = null,
)

@Serializable
data class TagDto(
    val name: String = "",
    val stationcount: Int? = null,
)

@Serializable
data class StationDto(
    val stationuuid: String = "",
    val name: String = "",
    val url: String? = null,
    @SerialName("url_resolved") val urlResolved: String? = null,
    val bitrate: Int? = null,
    val codec: String? = null,
    val country: String? = null,
    val favicon: String? = null,
    /** Real API sends tags as one comma-separated string. */
    val tags: String? = null,
) {
    /** `url_resolved` wins; falls back to `url`; empty when the row has neither. */
    val resolvedStreamUrl: String
        get() = urlResolved?.takeIf { it.isNotBlank() } ?: url.orEmpty()

    val tagList: List<String>
        get() = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()
}

@Serializable
data class ClickResultDto(val ok: String? = null)
