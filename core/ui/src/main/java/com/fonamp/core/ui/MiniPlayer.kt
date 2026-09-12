package com.fonamp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Slice C: persistent mini-player (design §7).
 *
 * Docked above the bottom bar on every tab whenever audio is playing or
 * paused. Same instance across tabs; tap expands the [PlayerSheet].
 * Playback controls meet the 48dp + content-description baseline
 * (scaffold Req 7). Stateless: state hoisted from `PlayerManager` by `app`.
 */
data class MiniPlayerState(
    val title: String,
    val subtitle: String?,
    val isPlaying: Boolean,
    val source: SourceBadgeKind,
)

@Composable
fun MiniPlayer(
    state: MiniPlayerState,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onExpand: () -> Unit = {},
) {
    Surface(
        modifier = modifier.testTag("mini-player"),
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onExpand)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                onClick = onTogglePlayPause,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Play",
                )
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.subtitle != null) {
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SourceBadge(kind = state.source)
            IconButton(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                onClick = onClose,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close player",
                )
            }
        }
    }
}
