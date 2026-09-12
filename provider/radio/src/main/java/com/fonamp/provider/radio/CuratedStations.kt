package com.fonamp.provider.radio

import com.fonamp.provider.api.AudioItem

/**
 * Curated presets (change `curated-radio-stations`, Req 7–8, D1).
 *
 * Fixed station list travelling as `List<AudioItem>` with
 * `sourceId = "radio-browser"`, reusing the same mapping shape as the
 * directory stations. Pure function: no network, no cache, never throws.
 *
 * Every entry below is apply-time curl evidence (`de1.api.radio-browser.info`,
 * 2026-09-12): `bytag/lofi|chillout|ambient` winners (variant with most
 * votes/streams) with https `url_resolved` + MP3 codec + real `stationuuid`.
 * `byname/omarchy` + `bytag/omarchy` returned `[]` — omarchy is out of scope,
 * no preset invented for it (Req 12).
 */
data class CuratedStation(
    val name: String,
    val streamUrl: String,
    val country: String? = null,
    val tags: List<String> = emptyList(),
    val stationUuid: String? = null,
)

object CuratedStations {
    val DEFAULT: List<CuratedStation> = listOf(
        CuratedStation(
            name = "SomaFM Groove Salad (128k MP3)",
            streamUrl = "https://ice6.somafm.com/groovesalad-128-mp3",
            country = "The United States Of America",
            tags = listOf("chillout", "ambient"),
            stationUuid = "960cf833-0601-11e8-ae97-52543be04c81",
        ),
        CuratedStation(
            name = "REYFM -#LOFI",
            streamUrl = "https://listen.reyfm.de/lofi_320kbps.mp3",
            country = "Germany",
            tags = listOf("chill", "lofi"),
            stationUuid = "4e681355-3ebb-4b6c-a79a-d7cc81a0afd5",
        ),
        CuratedStation(
            name = "Smooth Chill",
            streamUrl = "https://media-ssl.musicradio.com/ChillMP3",
            country = "The United Kingdom Of Great Britain And Northern Ireland",
            tags = listOf("chillout"),
            stationUuid = "478fd7f4-dc36-11e9-a8ba-52543be04c81",
        ),
    )

    /**
     * Maps presets to `AudioItem`s (Req 7). Entries with a blank or
     * non-`http(s)`-parseable `streamUrl` are filtered (Req 8); never throws.
     */
    fun browseCurated(presets: List<CuratedStation> = DEFAULT): List<AudioItem> =
        presets.mapNotNull { it.toAudioItemOrNull() }
}

private fun CuratedStation.toAudioItemOrNull(): AudioItem? {
    if (name.isBlank() || !isHttpUrl(streamUrl)) return null
    val subtitle = (listOfNotNull(country) + tags)
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ")
    return AudioItem(
        sourceId = RadioBrowserSource.SOURCE_ID,
        stableId = stationUuid ?: "curated:${slug(name)}",
        title = name,
        subtitle = subtitle,
        streamUri = streamUrl,
        stationUuid = stationUuid,
        country = country,
        tags = tags,
    )
}

private fun isHttpUrl(url: String): Boolean {
    if (url.isBlank()) return false
    return runCatching {
        val parsed = java.net.URI(url).toURL()
        parsed.protocol == "http" || parsed.protocol == "https"
    }.getOrDefault(false)
}

private fun slug(name: String): String =
    name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
