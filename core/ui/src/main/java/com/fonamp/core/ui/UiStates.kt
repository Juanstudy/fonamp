package com.fonamp.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Slice C: shared list-screen states (scaffold Req 5).
 *
 * Every list screen renders one of these instead of a blank screen or
 * crash-as-UI. Styling is Material3 defaults only. Each state exposes an
 * actionable control where applicable (retry, grant again, settings link).
 *
 * Note: [LoadingState] rows are static skeleton placeholders. An animated
 * shimmer is deferred to the v2 visual pass with the rest of the custom
 * visual identity (docs/03-design.md §4); behavior (rows present, never
 * blank) is what v1 gates on.
 */

@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    rowCount: Int = 5,
) {
    Column(
        modifier = modifier
            .testTag("loading-state")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Loading",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        repeat(rowCount) { index ->
            Surface(
                modifier = Modifier
                    .testTag("loading-row-$index")
                    .fillMaxWidth()
                    .height(64.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                // Skeleton row: present but content-free by design.
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun OfflineState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    message: String = "You're offline",
    cacheNote: String? = null,
) {
    Card(
        modifier = modifier
            .testTag("offline-card")
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
            )
            if (cacheNote != null) {
                Text(
                    text = cacheNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun DeniedState(
    why: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = why,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(onClick = onGrant) {
            Text("Grant again")
        }
        if (onOpenSettings != null) {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings")
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun ErrorRetryState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
