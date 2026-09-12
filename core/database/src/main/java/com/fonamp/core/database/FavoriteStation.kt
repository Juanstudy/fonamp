package com.fonamp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Slice D — favorited radio station (radio Req 4: stations-only, local-only,
 * no sync/account/export; design §5).
 *
 * Nullable directory fields stay nullable (metadata-honesty rule): absent
 * country/tags/bitrate/codec are stored as null, never fabricated.
 * [tagsCsv] holds the directory `tags` list as a comma-joined string.
 */
@Entity(tableName = "favorite_stations")
data class FavoriteStation(
    @PrimaryKey val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val country: String?,
    val tagsCsv: String?,
    val bitrate: Int?,
    val codec: String?,
    val favoritedAt: Long,
)
