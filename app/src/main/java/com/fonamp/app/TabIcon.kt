package com.fonamp.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Slice I: generic M3 icons for the four bottom tabs (scaffold Req 5 —
 * Material3 defaults, no custom tokens).
 */
object TabIcon {
    fun forTab(route: String): ImageVector = when (route) {
        FonampRoutes.COLLECTION -> Icons.Filled.MusicNote
        FonampRoutes.DISCOVER -> Icons.Filled.Radio
        FonampRoutes.FAVORITES -> Icons.Filled.Favorite
        else -> Icons.Filled.Settings
    }
}
