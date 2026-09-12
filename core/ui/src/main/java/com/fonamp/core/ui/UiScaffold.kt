package com.fonamp.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Slice A scaffold: proves Compose + M3 compile in :core:ui.
 * FonampTheme + shared states land in Slice C.
 */
object UiScaffold {
    const val TAG = "fonamp-ui"
}

@Composable
fun ScaffoldPlaceholder() {
    Text("Fonamp UI scaffold")
}
