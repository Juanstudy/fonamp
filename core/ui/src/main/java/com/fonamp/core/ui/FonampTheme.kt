package com.fonamp.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Slice C: v1 theme uses Material3 DEFAULT roles, typography, and shapes.
 *
 * No custom color tokens, type scale, or iconography in v1 — full visual
 * identity is deferred to v2 (docs/03-design.md §4). Every shared-state and
 * player component below must render inside this theme in both light and dark.
 */
@Composable
fun FonampTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
