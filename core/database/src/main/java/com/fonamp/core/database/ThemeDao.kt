package com.fonamp.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Slice D — theme preference store (settings Req 1; design §5).
 *
 * The choice persists across restarts (single `id = 1` row) and `app` applies
 * it without restart via `collectAsStateWithLifecycle` on [observe].
 */
@Dao
interface ThemeDao {

    @Query("SELECT * FROM theme_pref WHERE id = 1")
    fun observe(): Flow<ThemePref?>

    @Upsert
    suspend fun set(pref: ThemePref)
}
