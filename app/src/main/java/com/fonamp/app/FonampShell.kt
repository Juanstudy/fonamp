package com.fonamp.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fonamp.app.update.ApkInstaller
import com.fonamp.core.database.ThemeDao
import com.fonamp.core.database.ThemeMode
import com.fonamp.core.permissions.AudioGateAction
import com.fonamp.core.permissions.AudioGateInput
import com.fonamp.core.permissions.AudioPermissionGate
import com.fonamp.core.permissions.CollectionEntryRoute
import com.fonamp.core.permissions.NotificationGate
import com.fonamp.core.player.PlayerError
import com.fonamp.core.player.PlayerManager
import com.fonamp.core.player.durationMs
import com.fonamp.core.ui.FonampTheme
import com.fonamp.core.ui.MiniPlayer
import com.fonamp.core.ui.MiniPlayerState
import com.fonamp.core.ui.PlayerSheet
import com.fonamp.core.ui.SourceBadgeKind
import com.fonamp.feature.library.CollectionRoute
import com.fonamp.feature.radio.DiscoverRoute
import com.fonamp.feature.radio.FavoritesRoute
import com.fonamp.feature.radio.RadioRouteScreen
import com.fonamp.feature.radio.stationUuid
import com.fonamp.feature.settings.SettingsRoute
import com.fonamp.feature.settings.UpdateAvailableDialog
import com.fonamp.feature.settings.UpdateUiState
import com.fonamp.provider.api.BrowseQuery
import kotlinx.coroutines.flow.Flow

/**
 * Slice I: app shell (design §7, scaffold Req 4, player Req 2).
 *
 * - Theme from [ThemeDao] recomposes the whole tree without restart.
 * - `NavHost` routes `collection | radio/discover | radio/stations?filter |
 *   favorites | settings`; bottom tabs are exactly Collection/Radio/Favorites/
 *   Settings ([FonampTabs]).
 * - One persistent [MiniPlayer] above the tabs whenever the queue is
 *   non-empty (playing or paused); tap expands the modal [PlayerSheet]
 *   (title/artist-or-station, [SourceBadgeKind], ICY line for radio, error
 *   banner + retry; no shuffle/repeat/speed/sleep — none exists on the
 *   `core/ui` surfaces).
 * - `AudioPermissionGate` wiring lives on the Collection entry only
 *   (permissions Req 2–3); `NotificationGate` fires lazily on first playback
 *   with tolerated failure (design §6).
 * - The sheet heart is radio-only: `isFavorite` reads the shared DAO-backed
 *   `FavoritesViewModel.favoriteIds` and `onToggleFavorite` delegates to its
 *   `toggleMediaItem` (same delete-or-upsert semantics as the station-row
 *   hearts, Slice H). Local playback passes `onToggleFavorite = null`, so no
 *   heart renders there. The holder is resolved lazily inside the sheet, so
 *   the DAO flow (singleton) is the single source of truth across the
 *   per-entry `RadioHolderViewModel` instances.
 *
 * Wired note: [PlayerSheet]'s radio favorite toggle reads the DAO-backed
 * [FavoritesViewModel] (radio-only; local passes `onToggleFavorite = null`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FonampRoot(
    player: PlayerManager,
    themeFlow: Flow<com.fonamp.core.database.ThemePref?>,
    version: String = BuildConfig.VERSION_NAME,
) {
    val themePref by themeFlow.collectAsStateWithLifecycle(initialValue = null)
    val darkTheme = when (themePref?.mode ?: ThemeMode.SYSTEM) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    FonampTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val playerState by player.state.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        var showSheet by rememberSaveable { mutableStateOf(false) }
        var miniDismissed by rememberSaveable { mutableStateOf(false) }

        // UI-only dismiss: X hides the mini-player where it is.
        // Reset when new playback starts so the next track shows again.
        LaunchedEffect(playerState.queue, playerState.index, playerState.isPlaying) {
            if (playerState.isPlaying) miniDismissed = false
        }

        // Notification gate: lazy on first playback, failure tolerated.
        RequestNotificationOnce(playerHasQueue = playerState.queue.isNotEmpty())

        // In-app updates: one cold-start check (silent unless a newer release
        // is installable); manual re-checks live in Settings on their own
        // entry instance converging on the same server truth.
        val appContext = LocalContext.current.applicationContext
        val installer = remember(appContext) { ApkInstaller(appContext) }
        val updateHolder = hiltViewModel<UpdateHolderViewModel>()
        val updateState by updateHolder.updates.state.collectAsStateWithLifecycle()
        LaunchedEffect(Unit) {
            updateHolder.updates.checkForUpdates(version)
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                Column {
                    if (playerState.queue.isNotEmpty() && !miniDismissed) {
                        val current = playerState.queue.getOrNull(playerState.index)
                        MiniPlayer(
                            state = MiniPlayerState(
                                title = current?.mediaMetadata?.title?.toString()
                                    ?: "Playing",
                                subtitle = current?.mediaMetadata?.artist?.toString(),
                                isPlaying = playerState.isPlaying,
                                artworkUri = current?.mediaMetadata?.artworkUri?.toString(),
                                source = if (playerState.isLive) {
                                    SourceBadgeKind.RADIO
                                } else {
                                    SourceBadgeKind.LOCAL
                                },
                            ),
                            onTogglePlayPause = player::togglePlayPause,
                            onClose = {
                                player.stop()
                                miniDismissed = true
                            },
                            onExpand = { showSheet = true },
                        )
                    }
                    val backStack by navController.currentBackStackEntryAsState()
                    val currentRoute = backStack?.destination?.route
                    NavigationBar {
                        FonampTabs.tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute?.startsWith(
                                    tab.route.substringBefore("?"),
                                ) == true,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(TabIcon.forTab(tab.route), tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = FonampRoutes.COLLECTION,
                ) {
                    composable(FonampRoutes.COLLECTION) {
                        val holder = hiltViewModel<CollectionHolderViewModel>()
                        CollectionEntry(holder = holder)
                    }
                    composable(FonampRoutes.DISCOVER) {
                        val holder = hiltViewModel<RadioHolderViewModel>()
                        DiscoverRoute(viewModel = holder.radio)
                    }
                    composable(
                        route = FonampRoutes.STATIONS,
                        arguments = listOf(
                            navArgument("filter") {
                                type = NavType.StringType
                                defaultValue = ""
                            },
                        ),
                    ) { entry ->
                        val holder = hiltViewModel<RadioHolderViewModel>()
                        StationsEntry(
                            holder = holder,
                            filter = entry.arguments?.getString("filter").orEmpty(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable(FonampRoutes.FAVORITES) {
                        val holder = hiltViewModel<RadioHolderViewModel>()
                        FavoritesRoute(viewModel = holder.favoritesVm)
                    }
                    composable(FonampRoutes.SETTINGS) {
                        val holder = hiltViewModel<SettingsHolderViewModel>()
                        SettingsRoute(
                            viewModel = holder.settings,
                            snackbar = snackbar,
                            version = version,
                            updates = holder.updates,
                            onDownloadUpdate = { url, _ ->
                                installer.enqueueUpdate(url, ApkInstaller.UPDATE_FILE_NAME)
                            },
                        )
                    }
                }
            }
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                val current = playerState.queue.getOrNull(playerState.index)
                // Activity-scoped holder: the DAO flow is the shared truth,
                // so this converges with the per-entry holders' hearts.
                val favoritesVm = hiltViewModel<RadioHolderViewModel>().favoritesVm
                val favoriteIds by favoritesVm.favoriteIds.collectAsStateWithLifecycle()
                val currentStationUuid = current?.stationUuid()
                PlayerSheet(
                    title = current?.mediaMetadata?.title?.toString() ?: "Nothing playing",
                    subtitle = current?.mediaMetadata?.artist?.toString(),
                    source = if (playerState.isLive) {
                        SourceBadgeKind.RADIO
                    } else {
                        SourceBadgeKind.LOCAL
                    },
                    isPlaying = playerState.isPlaying,
                    onTogglePlayPause = player::togglePlayPause,
                    onClose = { showSheet = false },
                    artworkUri = current?.mediaMetadata?.artworkUri?.toString(),
                    icyTitle = playerState.icyTitle,
                    isFavorite = currentStationUuid != null && favoriteIds.contains(currentStationUuid),
                    onToggleFavorite = if (playerState.isLive && current != null &&
                        currentStationUuid != null
                    ) {
                        { favoritesVm.toggleMediaItem(current) }
                    } else {
                        null
                    },
                    errorMessage = playerState.error?.message,
                    onRetry = player::retry,
                    positionMs = playerState.positionMs,
                    durationMs = current?.durationMs(),
                    onSeekTo = player::seekTo,
                    onPrev = if (current != null && !playerState.isLive) player::prev else null,
                    onNext = if (current != null && !playerState.isLive) player::next else null,
                    sleepEndsAtMs = playerState.sleepEndsAtMs,
                    onSetSleepTimer = player::setSleepTimer,
                    onClearSleepTimer = player::clearSleepTimer,
                )
            }
        }

        val availableUpdate = updateState as? UpdateUiState.Available
        if (availableUpdate != null) {
            UpdateAvailableDialog(
                tag = availableUpdate.tag,
                notes = availableUpdate.notes,
                onDownload = {
                    installer.enqueueUpdate(availableUpdate.apkUrl, ApkInstaller.UPDATE_FILE_NAME)
                    updateHolder.updates.dismiss()
                },
                onLater = { updateHolder.updates.dismiss() },
            )
        }
    }
}

/**
 * Collection entry with the [AudioPermissionGate] (permissions Req 2–3).
 *
 * First entry fires the system sheet once; granted rebuilds the list (the
 * delegate is keyed by the flag); denied renders `Denied` with why +
 * grant-again; permanently denied adds the system-settings deep-link. Any
 * other route renders ungated — enforced by [AudioPermissionGate.decide].
 */
@Composable
private fun CollectionEntry(holder: CollectionHolderViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val permission = AudioPermissionGate.permissionForSdk(Build.VERSION.SDK_INT)
    var granted by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    var autoRequested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted = isGranted
        asked = true
    }

    val permanentlyDenied = asked && !granted &&
        (activity?.shouldShowRequestPermissionRationale(permission) == false)
    val action = AudioPermissionGate.decide(
        AudioGateInput(
            route = CollectionEntryRoute,
            permissionGranted = granted,
            askedBefore = asked,
            permanentlyDenied = permanentlyDenied,
        ),
    )
    if (action == AudioGateAction.RequestPermission && !autoRequested) {
        autoRequested = true
        LaunchedEffect(Unit) {
            asked = true
            launcher.launch(permission)
        }
    }

    CollectionRoute(
        viewModel = holder.forPermission(granted),
        onGrantPermission = {
            asked = true
            launcher.launch(permission)
        },
        onOpenSettings = if (permanentlyDenied) {
            {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            }
        } else {
            null
        },
    )
}

/** Deep-link entry: opens one selection, then behaves like Discover. */
@Composable
private fun StationsEntry(
    holder: RadioHolderViewModel,
    filter: String,
    onBack: () -> Unit,
) {
    val vm = holder.radio
    LaunchedEffect(filter) {
        parseFilter(filter)?.let { (query, title) -> vm.openSelection(query, title) }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    RadioRouteScreen(
        state = state,
        onQueryChange = vm::setQuery,
        onSelectSegment = vm::selectSegment,
        onOpenSelection = { query ->
            vm.openSelection(query, query.country ?: query.tag ?: query.genre.orEmpty())
        },
        onBack = {
            vm.backToDiscover()
            onBack()
        },
        onPlay = vm::playStation,
        onToggleFavorite = vm::toggleFavorite,
        onRefresh = vm::refresh,
    )
}

private fun parseFilter(filter: String): Pair<BrowseQuery, String>? {
    val value = filter.substringAfter(":", missingDelimiterValue = "")
    if (value.isBlank()) return null
    return when (filter.substringBefore(":")) {
        "country" -> BrowseQuery(country = value) to value
        "tag" -> BrowseQuery(tag = value) to value
        "genre" -> BrowseQuery(genre = value) to value
        else -> null
    }
}

/** Notification permission: lazy on first playback, tolerated failure. */
@Composable
private fun RequestNotificationOnce(playerHasQueue: Boolean) {
    if (Build.VERSION.SDK_INT < 33) return
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { asked = true }
    if (NotificationGate.shouldRequest(hasPlayedOnce = playerHasQueue, askedBefore = asked)) {
        asked = true
        LaunchedEffect(Unit) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private val PlayerError.message: String
    get() = when (this) {
        PlayerError.TIMEOUT -> "Stream timed out — retry?"
        PlayerError.OFFLINE -> "You're offline — retry?"
        PlayerError.STREAM_UNAVAILABLE -> "Stream unavailable — retry?"
    }
