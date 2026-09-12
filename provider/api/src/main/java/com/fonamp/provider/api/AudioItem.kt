package com.fonamp.provider.api

/**
 * One playable entry from any [Source]. Absent metadata stays null —
 * never fabricated (metadata-honesty rule, design §10).
 *
 * @param sourceId "local" | "radio-browser"
 * @param stableId local: MediaStore audioId; radio: stationuuid
 * @param subtitle artist OR station tags/country; null when absent
 * @param album local only; null for radio
 * @param durationMs local only; null for radio (live stream)
 * @param streamUri local: content://…; radio: resolved stream url
 * @param bitrate bitrate reported by the directory, when present
 * @param codec codec reported by the directory, when present
 */
data class AudioItem(
    val sourceId: String,
    val stableId: String,
    val title: String,
    val subtitle: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    val streamUri: String,
    val bitrate: Int? = null,
    val codec: String? = null,
    val stationUuid: String? = null,
    val country: String? = null,
    val tags: List<String> = emptyList(),
)
