package com.fonamp.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Theme choice (settings Req 1). Room persists enums by name. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Single-row theme preference (design §5: `id = 1` primary key, one row ever —
 * [ThemeDao.set] upserts so the row is replaced, never duplicated).
 */
@Entity(tableName = "theme_pref")
data class ThemePref(
    @PrimaryKey val id: Int = 1,
    val mode: ThemeMode,
)
