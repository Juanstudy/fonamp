package com.fonamp.core.ui

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

// Slice C: source badge (design section 7: radio vs local).
//
// Own enum (not provider.api SourceKind) keeps the core-to-third-party-only
// dependency rule (design sections 1 and 9): later slices map the provider
// kind to this badge at the feature layer.
enum class SourceBadgeKind {
    RADIO,
    LOCAL,
}

@Composable
fun SourceBadge(
    kind: SourceBadgeKind,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        modifier = modifier.testTag("source-badge"),
        onClick = {},
        label = {
            Text(
                text = when (kind) {
                    SourceBadgeKind.RADIO -> "Radio"
                    SourceBadgeKind.LOCAL -> "Local"
                },
            )
        },
    )
}
