package com.fonamp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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
    /** Local cover from MediaStore (PR1); null = no cover, show generic icon. */
    val artworkUri: String? = null,
)

/**
 * PR2: local cover thumb with generic-icon fallback.
 *
 * Shows [AsyncImage] when [artworkUri] is non-null; on null or load error
 * shows [fallbackIcon]. Never crashes, never fabricates art.
 */
@Composable
fun AlbumArtwork(
    artworkUri: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    contentDescription: String = "Album artwork",
    fallbackIcon: ImageVector = AppIcons.MusicNote,
    fallbackContentDescription: String = "No album artwork",
    imageTestTag: String = "artwork-image",
    fallbackTestTag: String = "artwork-fallback",
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri != null) {
            // Coil keeps this node in the tree on error and draws the
            // generic icon via the error painter: stable for tests,
            // no crash and no fabricated art on device.
            AsyncImage(
                model = artworkUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                error = rememberVectorPainter(image = fallbackIcon),
                modifier = Modifier
                    .size(size)
                    .testTag(imageTestTag),
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = fallbackContentDescription,
                modifier = Modifier.testTag(fallbackTestTag),
            )
        }
    }
}

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
            AlbumArtwork(
                artworkUri = state.artworkUri,
                size = 44.dp,
                imageTestTag = "mini-artwork-image",
                fallbackTestTag = "mini-artwork-fallback",
            )
            IconButton(
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                onClick = onTogglePlayPause,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) AppIcons.Pause else Icons.Filled.PlayArrow,
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
