package com.fonamp.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.fonamp.core.database.FavoriteDao
import com.fonamp.core.database.ThemeDao
import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.GithubReleasesClient
import com.fonamp.core.player.PlayerManager
import com.fonamp.feature.library.LibraryPlayer
import com.fonamp.feature.library.LibraryViewModel
import com.fonamp.feature.radio.FavoritesViewModel
import com.fonamp.feature.radio.RadioViewModel
import com.fonamp.feature.settings.SettingsViewModel
import com.fonamp.feature.settings.UpdateCheckViewModel
import com.fonamp.provider.api.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers

/**
 * Slice I: retained-scope strategy for the plain-class VMs (known debt from
 * Slices F/H: `LibraryViewModel`/`RadioViewModel`/`FavoritesViewModel`/
 * `SettingsViewModel` are plain classes so tests can inject a test scope).
 *
 * Each holder is a Hilt `ViewModel` scoped to its nav-graph entry, so it
 * survives configuration changes; the plain delegate is cached inside and
 * reused across rotations. The Collection delegate is keyed by the audio
 * permission flag, so a grant rebuilds the list (permissions Req 3) while a
 * rotation reuses the same instance.
 *
 * Boundary (documented): full post-death restore still needs source
 * cooperation — only sources can rebuild `MediaItem`s from ids (Slice G
 * contract note). The player persists queue context best-effort and always
 * lands idle-or-restored, never phantom-playing.
 */
@HiltViewModel
class CollectionHolderViewModel @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards Source>,
    private val player: LibraryPlayer,
) : ViewModel() {
    private val delegates = mutableMapOf<Boolean, LibraryViewModel>()

    fun forPermission(permissionGranted: Boolean): LibraryViewModel =
        delegates.getOrPut(permissionGranted) {
            LibraryViewModel(
                source = resolveSource(sources, "local"),
                player = player,
                permissionGranted = permissionGranted,
                scope = viewModelScope,
                io = Dispatchers.IO,
            )
        }
}

@HiltViewModel
class RadioHolderViewModel @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards Source>,
    private val favorites: FavoriteDao,
    private val player: PlayerManager,
    private val cache: DirectoryCache,
) : ViewModel() {
    // Radio + favorites share one holder so both VMs observe the same DAO flow
    // within a single nav entry; each delegate is created once per entry.
    val radio: RadioViewModel by lazy {
        RadioViewModel(
            source = resolveSource(sources, "radio-browser"),
            favorites = favorites,
            player = player,
            cache = cache,
            scope = viewModelScope,
            io = Dispatchers.IO,
        )
    }
    val favoritesVm: FavoritesViewModel by lazy {
        FavoritesViewModel(
            favorites = favorites,
            source = resolveSource(sources, "radio-browser"),
            player = player,
            scope = viewModelScope,
        )
    }
}

@HiltViewModel
class SettingsHolderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val themeDao: ThemeDao,
    private val cache: DirectoryCache,
    private val releases: GithubReleasesClient,
) : ViewModel() {
    val settings: SettingsViewModel by lazy {
        SettingsViewModel(
            themeDao = themeDao,
            cache = cache,
            scope = viewModelScope,
            io = Dispatchers.IO,
            getArtworkCacheSize = { context.imageLoader.diskCache?.size ?: 0L },
            clearArtworkCache = {
                val coilCache = context.imageLoader.diskCache
                val size = coilCache?.size ?: 0L
                coilCache?.clear()
                size
            },
            getDatabaseSize = {
                val dbFile = context.getDatabasePath("fonamp.db")
                val walFile = context.getDatabasePath("fonamp.db-wal")
                val shmFile = context.getDatabasePath("fonamp.db-shm")
                (if (dbFile.exists()) dbFile.length() else 0L) +
                (if (walFile.exists()) walFile.length() else 0L) +
                (if (shmFile.exists()) shmFile.length() else 0L)
            },
            getPreferencesSize = {
                val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
                if (prefsDir.exists()) prefsDir.listFiles()?.sumOf { it.length() } ?: 0L else 0L
            }
        )
    }
    val updates: UpdateCheckViewModel by lazy {
        UpdateCheckViewModel(
            fetchLatest = {
                releases.fetchLatest(
                    GithubReleasesClient.REPO_OWNER,
                    GithubReleasesClient.REPO_NAME,
                )
            },
            scope = viewModelScope,
            io = Dispatchers.IO,
        )
    }
}

/**
 * Shell-root holder for the cold-start update check (one instance per
 * process at the activity entry — deliberately separate from the Settings
 * entry instance; both converge on the same server truth on demand).
 */
@HiltViewModel
class UpdateHolderViewModel @Inject constructor(
    private val releases: GithubReleasesClient,
) : ViewModel() {
    val updates: UpdateCheckViewModel by lazy {
        UpdateCheckViewModel(
            fetchLatest = {
                releases.fetchLatest(
                    GithubReleasesClient.REPO_OWNER,
                    GithubReleasesClient.REPO_NAME,
                )
            },
            scope = viewModelScope,
            io = Dispatchers.IO,
        )
    }
}
