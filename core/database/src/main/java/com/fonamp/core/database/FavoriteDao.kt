package com.fonamp.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Slice D — favorites store (radio Req 4; design §5).
 *
 * Undo support: delete only needs the uuid; the 10s snackbar window re-upserts
 * the held row, so [upsert] must fully restore a previously deleted row.
 */
@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorite_stations ORDER BY favoritedAt DESC")
    fun observeAll(): Flow<List<FavoriteStation>>

    @Upsert
    suspend fun upsert(station: FavoriteStation)

    @Query("DELETE FROM favorite_stations WHERE stationUuid = :uuid")
    suspend fun delete(uuid: String)

    @Query("SELECT * FROM favorite_stations WHERE stationUuid = :uuid")
    suspend fun byId(uuid: String): FavoriteStation?
}
