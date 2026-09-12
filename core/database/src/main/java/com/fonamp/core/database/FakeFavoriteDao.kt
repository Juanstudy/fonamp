package com.fonamp.core.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Slice D — hand-written [FavoriteDao] fake for ViewModel tests
 * (design §8: no MockK; VM tests in `feature/radio` stay Robolectric-free).
 *
 * Mirrors Room semantics relevant to callers: [upsert] replaces by primary key,
 * [delete] is a no-op for unknown uuids, [byId] returns null when absent.
 */
class FakeFavoriteDao : FavoriteDao {

    private val rows = MutableStateFlow<List<FavoriteStation>>(emptyList())

    override fun observeAll(): Flow<List<FavoriteStation>> = rows

    override suspend fun upsert(station: FavoriteStation) {
        rows.update { current ->
            (current.filterNot { it.stationUuid == station.stationUuid } + station)
                .sortedByDescending { it.favoritedAt }
        }
    }

    override suspend fun delete(uuid: String) {
        rows.update { current -> current.filterNot { it.stationUuid == uuid } }
    }

    override suspend fun byId(uuid: String): FavoriteStation? =
        rows.value.firstOrNull { it.stationUuid == uuid }
}
