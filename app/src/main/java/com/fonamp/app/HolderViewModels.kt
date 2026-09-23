package com.fonamp.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

@HiltViewModel
class PodcastHolderViewModel @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards Source>,
    private val dao: com.fonamp.core.database.PodcastSubscriptionDao,
    private val player: PlayerManager,
) : ViewModel() {
    val podcast: com.fonamp.feature.podcast.PodcastViewModel by lazy {
        com.fonamp.feature.podcast.PodcastViewModel(
            source = resolveSource(sources, "podcast"),
            dao = dao,
            player = player,
            scope = viewModelScope,
            io = kotlinx.coroutines.Dispatchers.IO,
        )
    }
}
