package com.fonamp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Slice C: modal player sheet skeleton (design §7).
 *
 * Title, artist-or-station, [SourceBadge], ICY line for radio, queue/seek
 * slots for local (owned by later slices), favorite toggle (radio), error
 * banner + retry on failure, optional sleep timer (presets + remaining) and
 * transport controls (shuffle/repeat/speed, all optional). No EQ affordance.
 */
@Composable
fun PlayerSheet(
    title: String,
    subtitle: String?,
    source: SourceBadgeKind,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Local cover from mediaMetadata (PR1); null = no cover, show generic icon. */
    artworkUri: String? = null,
    icyTitle: String? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    positionMs: Long = 0L,
    durationMs: Long? = null,
    onSeekTo: ((Long) -> Unit)? = null,
    onPrev: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    /**
     * Monotonic sleep-timer deadline (`SystemClock.elapsedRealtime` base),
     * null when no timer is armed. Shown only when [onSetSleepTimer] is set.
     */
    sleepEndsAtMs: Long? = null,
    /** Non-null wires the sleep-timer button + dialog. */
    onSetSleepTimer: ((Long) -> Unit)? = null,
    onClearSleepTimer: (() -> Unit)? = null,
    /** Clock for the remaining-time text; injectable for deterministic tests. */
    nowMs: () -> Long = android.os.SystemClock::elapsedRealtime,
    /**
     * Transport modes as primitives (this module stays decoupled from
     * `core/player` by design). Repeat is two flags: all/one, both false
     * means off. Each control renders only when its callback is non-null.
     */
    shuffleEnabled: Boolean = false,
    repeatAll: Boolean = false,
    repeatOne: Boolean = false,
    speed: Float = 1f,
    onToggleShuffle: (() -> Unit)? = null,
    onCycleRepeat: (() -> Unit)? = null,
    onCycleSpeed: (() -> Unit)? = null,
    /**
     * Visible queue ("Up next"). Empty hides the section; rows are
     * clickable only when [onSelectQueueItem] is non-null.
     */
    upNext: List<QueueRow> = emptyList(),
    onSelectQueueItem: ((Int) -> Unit)? = null,
) {
    var showSleepDialog by remember { mutableStateOf(false) }
    // Minute-fresh remaining text while a timer is armed (radio has no
    // position ticker driving recomposition, so tick locally).
    var nowSnapshot by remember { mutableLongStateOf(nowMs()) }
    LaunchedEffect(sleepEndsAtMs) {
        if (sleepEndsAtMs == null) return@LaunchedEffect
        while (true) {
            delay(30_000L)
            nowSnapshot = nowMs()
        }
    }
    val sleepRemainingMs = sleepEndsAtMs?.let { (it - nowSnapshot).coerceAtLeast(0L) }
    Column(
        modifier = modifier
            .testTag("player-sheet")
            // PR2 hero can overflow small screens: keep error + controls reachable.
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceBadge(kind = source)
            IconButton(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                onClick = onClose,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close player sheet",
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
        )
        // PR2 hero: real local cover or the generic icon, never fabricated.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AlbumArtwork(
                artworkUri = artworkUri,
                size = 200.dp,
                fallbackIcon = AppIcons.MusicNote,
                imageTestTag = "player-artwork-image",
                fallbackTestTag = "player-artwork-fallback",
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (icyTitle != null) {
            Text(
                modifier = Modifier.testTag("player-icy"),
                text = icyTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier
                    .testTag("player-error")
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    if (onRetry != null) {
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        if (durationMs != null && durationMs > 0L) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player-progress-section"),
            ) {
                var isDragging by remember { mutableStateOf(false) }
                var dragFraction by remember { mutableFloatStateOf(0f) }

                val durationFloat = durationMs.toFloat()
                val positionFraction =
                    (positionMs.toFloat() / durationFloat).coerceIn(0f, 1f)
                val currentFraction = if (isDragging) dragFraction else positionFraction
                val displayedMs =
                    if (isDragging) (dragFraction * durationFloat).toLong() else positionMs

                Slider(
                    value = currentFraction,
                    onValueChange = {
                        isDragging = true
                        dragFraction = it.coerceIn(0f, 1f)
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        val target = (dragFraction * durationFloat).toLong()
                            .coerceIn(0L, durationMs)
                        onSeekTo?.invoke(target)
                    },
                    valueRange = 0f..1f,
                    enabled = (onSeekTo != null),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player-slider"),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDuration(displayedMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("player-current-time"),
                    )
                    Text(
                        text = formatDuration(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("player-total-duration"),
                    )
                }
            }
        }

        if (sleepEndsAtMs != null && sleepRemainingMs != null && onSetSleepTimer != null) {
            Text(
                text = formatSleepRemaining(sleepRemainingMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sleep-remaining"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onToggleShuffle != null) {
                IconButton(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-shuffle"),
                    onClick = onToggleShuffle,
                ) {
                    Icon(
                        imageVector = AppIcons.Shuffle,
                        contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                        tint = if (shuffleEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        },
                    )
                }
            }
            if (onPrev != null) {
                IconButton(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-prev"),
                    onClick = onPrev,
                ) {
                    Icon(
                        imageVector = AppIcons.SkipPrevious,
                        contentDescription = "Previous",
                    )
                }
            }
            IconButton(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("player-play-pause"),
                onClick = onTogglePlayPause,
            ) {
                Icon(
                    imageVector = if (isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            if (onNext != null) {
                IconButton(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-next"),
                    onClick = onNext,
                ) {
                    Icon(
                        imageVector = AppIcons.SkipNext,
                        contentDescription = "Next",
                    )
                }
            }
            if (onCycleRepeat != null) {
                val repeatOn = repeatAll || repeatOne
                IconButton(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-repeat"),
                    onClick = onCycleRepeat,
                ) {
                    Icon(
                        imageVector = AppIcons.Repeat,
                        contentDescription = when {
                            repeatOne -> "Repeat one"
                            repeatAll -> "Repeat all"
                            else -> "Repeat off"
                        },
                        tint = if (repeatOn) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        },
                    )
                }
            }
            if (onCycleSpeed != null) {
                TextButton(
                    onClick = onCycleSpeed,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-speed"),
                ) {
                    Text(
                        text = formatSpeed(speed),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            if (onSetSleepTimer != null) {
                IconButton(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-sleep"),
                    onClick = { showSleepDialog = true },
                ) {
                    Icon(
                        imageVector = AppIcons.Bedtime,
                        contentDescription = "Sleep timer",
                    )
                }
            }
            if (onToggleFavorite != null) {
                IconButton(
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("player-favorite"),
                    onClick = onToggleFavorite,
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) {
                            "Remove from favorites"
                        } else {
                            "Add to favorites"
                        },
                    )
                }
            }
        }
        if (upNext.isNotEmpty()) {
            UpNextSection(
                rows = upNext,
                onSelect = onSelectQueueItem,
            )
        }
        if (showSleepDialog && onSetSleepTimer != null) {
            SleepTimerDialog(
                activeEndsAtMs = sleepEndsAtMs,
                nowMs = nowMs,
                onPick = {
                    onSetSleepTimer.invoke(it)
                    showSleepDialog = false
                },
                onClear = {
                    onClearSleepTimer?.invoke()
                    showSleepDialog = false
                },
                onDismiss = { showSleepDialog = false },
            )
        }
    }
}

/** Sleep-timer presets in minutes. */
val SLEEP_PRESET_MINUTES = listOf(5, 10, 15, 30, 45, 60)

/**
 * Preset picker + off switch. Stateless: the sheet owns visibility,
 * the player owns the deadline.
 */
@Composable
private fun SleepTimerDialog(
    activeEndsAtMs: Long?,
    nowMs: () -> Long,
    onPick: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val activeMinutes = activeEndsAtMs?.let {
        ((it - nowMs()).coerceAtLeast(0L) + 59_999L) / 60_000L
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        modifier = Modifier.testTag("sleep-dialog"),
        title = { Text("Sleep timer") },
        text = {
            Column {
                SLEEP_PRESET_MINUTES.forEach { minutes ->
                    val isActive = activeMinutes == minutes.toLong()
                    TextButton(
                        onClick = { onPick(minutes * 60_000L) },
                        modifier = Modifier.testTag("sleep-preset-$minutes"),
                    ) {
                        Text(
                            text = if (isActive) "$minutes min (on)" else "$minutes min",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (activeEndsAtMs != null) {
                    TextButton(
                        onClick = onClear,
                        modifier = Modifier.testTag("sleep-off"),
                    ) {
                        Text(
                            text = "Off",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
    )
}

/** "Stops in 29 min", ceiling minutes so 29:01 reads 30, sub-minute reads less than 1. */
private fun formatSleepRemaining(remainingMs: Long): String {
    val minutes = (remainingMs + 59_999L) / 60_000L
    return if (minutes < 1L) "Stops in less than 1 min" else "Stops in $minutes min"
}

/** Speed label: whole values read "1x", fractional "1.25x". */
internal fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}x" else "${speed}x"

/** Formats milliseconds to mm:ss or hh:mm:ss. */
internal fun formatDuration(millis: Long): String {
    if (millis <= 0L) return "0:00"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
