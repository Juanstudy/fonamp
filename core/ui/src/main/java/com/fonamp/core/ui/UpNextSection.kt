package com.fonamp.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Queue row model for the "Up next" section (player Req 5 follow-up).
 *
 * Plain primitives only: this module stays decoupled from `core/player` by
 * design, so callers (e.g. `FonampShell`) map `MediaItem`s to [QueueRow]s.
 * `durationMs` is the raw item duration; null or <= 0 hides the label.
 */
data class QueueRow(
    val title: String,
    val subtitle: String?,
    val artworkUri: String?,
    val durationMs: Long?,
    val isActive: Boolean,
)

/**
 * Visible queue list for the player sheet.
 *
 * Hidden when [rows] is empty. Rows are clickable only when [onSelect] is
 * non-null; the tap index is the queue index the caller mapped from.
 */
@Composable
fun UpNextSection(
    rows: List<QueueRow>,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("up-next-section"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Up next",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.testTag("up-next-title"),
        )
        rows.forEachIndexed { index, row ->
            UpNextRow(
                row = row,
                index = index,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun UpNextRow(
    row: QueueRow,
    index: Int,
    onSelect: ((Int) -> Unit)?,
) {
    val click = if (onSelect != null) {
        Modifier.clickable { onSelect(index) }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .then(click)
            .padding(vertical = 4.dp)
            .testTag("up-next-row-$index"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(
            artworkUri = row.artworkUri,
            size = 44.dp,
            imageTestTag = "up-next-art-$index",
            fallbackTestTag = "up-next-art-fallback-$index",
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (row.isActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.subtitle != null) {
                Text(
                    text = row.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (row.isActive) {
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("up-next-active"),
                )
            }
        }
        val duration = row.durationMs
        if (duration != null && duration > 0L) {
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
