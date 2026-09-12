package com.fonamp.core.player

import androidx.media3.common.MediaItem

/**
 * Source-blind `MediaItem` extras contract (design §2, §3).
 *
 * `core/player` imports nothing from any provider module: it reads only these
 * keys plus standard `mediaMetadata`. The string values MUST stay identical
 * to `provider.api.SourceExtras` — [MediaItemMapperTest] pins every literal
 * so drift in either module fails loudly here.
 *
 * Providers write the bundle into `mediaMetadata.extras` (there is no
 * top-level extras edge on `MediaItem.Builder`); the readers below look
 * there. A bare item without extras (e.g. `MediaItem.fromUri`) reads as
 * non-live local.
 */
object PlayerExtras {
    const val KEY_SOURCE_ID = "source_id"
    const val KEY_STABLE_ID = "stable_id"
    const val KEY_IS_LIVE = "is_live"
    const val KEY_STATION_UUID = "station_uuid"
    const val KEY_COUNTRY = "country"
}

/** True for radio streams (`mediaId=radio:<uuid>`, `is_live=true`). */
fun MediaItem.isLive(): Boolean =
    mediaMetadata.extras?.getBoolean(PlayerExtras.KEY_IS_LIVE, false) ?: false

/** `"local"` or `"radio-browser"`; null when absent (never fabricated). */
fun MediaItem.sourceId(): String? =
    mediaMetadata.extras?.getString(PlayerExtras.KEY_SOURCE_ID)

/** Local MediaStore audioId or radio stationuuid; null when absent. */
fun MediaItem.stableId(): String? =
    mediaMetadata.extras?.getString(PlayerExtras.KEY_STABLE_ID)

/** Radio stationuuid; null for local items. */
fun MediaItem.stationUuid(): String? =
    mediaMetadata.extras?.getString(PlayerExtras.KEY_STATION_UUID)

/** Radio country label; null for local items or when undisclosed. */
fun MediaItem.country(): String? =
    mediaMetadata.extras?.getString(PlayerExtras.KEY_COUNTRY)
