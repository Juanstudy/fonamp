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
    entities = [FavoriteStation::class, ThemePref::class, PodcastSubscription::class],
    version = 3,
    exportSchema = false,
)
abstract class FonampDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao

    abstract fun themeDao(): ThemeDao

    abstract fun podcastSubscriptionDao(): PodcastSubscriptionDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE favorite_stations ADD COLUMN artworkUri TEXT")
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS podcast_subscriptions (`feedUrl` TEXT NOT NULL, `title` TEXT NOT NULL, `artworkUri` TEXT, `author` TEXT, `subscribedAt` INTEGER NOT NULL, PRIMARY KEY(`feedUrl`))"
                )
            }
        }
    }
}