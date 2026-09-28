package com.fonamp.feature.settings

import com.fonamp.core.database.ThemeDao
import com.fonamp.core.database.ThemeMode
import com.fonamp.core.database.ThemePref
import com.fonamp.core.network.DirectoryCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val cacheEntries: Int = 0,
    val cacheBytes: Long = 0L,
    val artworkBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val preferencesBytes: Long = 0L,
    /** Non-null right after a clear: confirmation carrying the freed bytes. */
    val clearConfirmation: String? = null,
) {
    val totalBytes: Long
        get() = cacheBytes + artworkBytes + databaseBytes + preferencesBytes
}

/**
 * Slice I: Settings state holder (settings Req 1–2, design §5).
 *
 * A plain class (not an Android `ViewModel`) so unit tests inject a test
 * scope — Slice F/H precedent. Slice I (`:app`) binds it into the Hilt graph
 * inside a retained holder (see `SettingsHolderViewModel`), which is the
 * documented config-change survival strategy for all plain-class VMs.
 *
 * - Theme System/Light/Dark persists via [ThemeDao] (single id=1 row) and
 *   applies immediately — no restart (the app root recomposes on the flow).
 * - Cache stats come from [DirectoryCache]; clear wipes the store (never the
 *   Room DB) and reports freed bytes with a confirmation.
 */
class SettingsViewModel(
    private val themeDao: ThemeDao,
    private val cache: DirectoryCache,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val getArtworkCacheSize: () -> Long = { 0L },
    private val clearArtworkCache: () -> Long = { 0L },
    private val getDatabaseSize: () -> Long = { 0L },
    private val getPreferencesSize: () -> Long = { 0L },
) {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        scope.launch {
            themeDao.observe().collect { pref ->
                _state.update { it.copy(theme = pref?.mode ?: ThemeMode.SYSTEM) }
            }
        }
        refreshStats()
    }

    fun setTheme(mode: ThemeMode) {
        scope.launch {
            withContext(io) { themeDao.set(ThemePref(mode = mode)) }
        }
    }

    fun refreshStats() {
        scope.launch {
            val stats = withContext(io) { cache.stats() }
            val artwork = withContext(io) { getArtworkCacheSize() }
            val dbSize = withContext(io) { getDatabaseSize() }
            val prefsSize = withContext(io) { getPreferencesSize() }
            _state.update {
                it.copy(
                    cacheEntries = stats.entryCount,
                    cacheBytes = stats.sizeBytes,
                    artworkBytes = artwork,
                    databaseBytes = dbSize,
                    preferencesBytes = prefsSize,
                )
            }
        }
    }

    fun clearCache() {
        scope.launch {
            val freedDir = withContext(io) { cache.clear() }
            val freedArt = withContext(io) { clearArtworkCache() }
            val freed = freedDir + freedArt
            val stats = withContext(io) { cache.stats() }
            val artwork = withContext(io) { getArtworkCacheSize() }
            _state.update {
                it.copy(
                    cacheEntries = stats.entryCount,
                    cacheBytes = stats.sizeBytes,
                    artworkBytes = artwork,
                    clearConfirmation = "Caches cleared — ${formatBytes(freed)} freed.",
                )
            }
        }
    }

    fun dismissConfirmation() {
        _state.update { it.copy(clearConfirmation = null) }
    }

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}
