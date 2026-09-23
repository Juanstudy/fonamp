package com.fonamp.app

import com.fonamp.provider.api.Source

/**
 * Slice I: navigation routes + bottom tabs (design §7, scaffold Req 4).
 *
 * Routes: `collection`, `radio/discover`, `radio/stations?filter=…`,
 * `favorites`, `settings`. Bottom tabs are exactly Collection, Radio,
 * Favorites, Settings — no Podcasts/Downloads/EQ/sleep entry points.
 */
object FonampRoutes {
    const val COLLECTION = "collection"
    const val DISCOVER = "radio/discover"
    const val STATIONS = "radio/stations?filter={filter}"
    const val STATIONS_BASE = "radio/stations"
    const val FAVORITES = "favorites"
    const val SETTINGS = "settings"
    const val PODCAST = "podcast"

    val all: List<String> = listOf(COLLECTION, DISCOVER, STATIONS_BASE, FAVORITES, SETTINGS, PODCAST)

    fun stationsRoute(filter: String): String = "$STATIONS_BASE?filter=$filter"
}

data class FonampTab(val route: String, val label: String)

object FonampTabs {
    val tabs: List<FonampTab> = listOf(
        FonampTab(FonampRoutes.COLLECTION, "Collection"),
        FonampTab(FonampRoutes.DISCOVER, "Radio"),
        FonampTab(FonampRoutes.FAVORITES, "Favorites"),
        FonampTab(FonampRoutes.PODCAST, "Podcasts"),
        FonampTab(FonampRoutes.SETTINGS, "Settings"),
    )
}

/**
 * Slice I: source resolution by contract id (design §1).
 *
 * The Hilt graph contributes every `Source` via `@Provides @IntoSet`
 * multibindings (see [SourceModule]); callers pick by [id] (`local`,
 * `radio-browser`, future provider-source ids). Adding a new source is one id +
 * one registration — zero edits to `core/player`, `core/ui`, or tabs
 * (scaffold Req 2, proven by `ExpandabilityTest`).
 */
fun resolveSource(sources: Set<@JvmSuppressWildcards Source>, id: String): Source =
    sources.firstOrNull { it.id == id }
        ?: error("No Source with id=$id among ${sources.map { it.id }}")
