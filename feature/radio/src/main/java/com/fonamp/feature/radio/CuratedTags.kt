package com.fonamp.feature.radio

/**
 * Pinned tag tiles for the "Curadas" section (change `curated-radio-stations`,
 * Req 11–12, D2).
 *
 * Tag strings fixed at apply time by curl evidence against
 * `de1.api.radio-browser.info` (2026-09-12): `lofi` beats `lo-fi` (top votes
 * 4877 vs 1110), `chillout` beats `chill` (4 https rows in top-5 vs 3),
 * `ambient` passes with 13 https rows in top-20. Each tile delegates to the
 * existing `browse(BrowseQuery(tag = …))` path (24h cache + mirror fallback).
 *
 * Tag strings only — zero maintained stream URLs (Req 11 scenario 2).
 */
data class CuratedTile(val label: String, val tag: String)

object CuratedTags {
    val TILES: List<CuratedTile> = listOf(
        CuratedTile(label = "Lofi", tag = "lofi"),
        CuratedTile(label = "Chill", tag = "chillout"),
        CuratedTile(label = "Ambient", tag = "ambient"),
    )
}
