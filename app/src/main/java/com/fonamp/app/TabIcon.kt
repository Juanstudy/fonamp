package com.fonamp.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.fonamp.core.ui.AppIcons

/**
 * Slice I: generic M3 icons for the four bottom tabs (scaffold Req 5 —
 * Material3 defaults, no custom tokens).
 */
object TabIcon {
    fun forTab(route: String): ImageVector = when (route) {
        FonampRoutes.COLLECTION -> AppIcons.MusicNote
        FonampRoutes.DISCOVER -> AppIcons.Radio
        FonampRoutes.FAVORITES -> Icons.Filled.Favorite
        FonampRoutes.PODCAST -> Icons.Filled.Search
        else -> Icons.Filled.Settings
    }
}
