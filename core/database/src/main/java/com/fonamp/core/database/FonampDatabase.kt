package com.fonamp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Slice D — Room database v1 (design §5).
 *
 * Entities: [FavoriteStation] (stations-only favorites) + [ThemePref]
 * (single-row theme choice). The directory cache lives in `core/network`'s
 * disk store, NOT here — cache clear never wipes favorites/theme.
 */
@Database(
    entities = [FavoriteStation::class, ThemePref::class],
    version = 1,
    exportSchema = false,
)
abstract class FonampDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    abstract fun themeDao(): ThemeDao
}
