package com.fonamp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fonamp.core.database.ThemeDao
import com.fonamp.core.player.PlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Slice I: Hilt entry point hosting the app shell (design §7).
 *
 * The [PlayerManager] singleton and [ThemeDao] flow survive configuration
 * changes (process-scoped bindings); per-tab state lives in the
 * nav-graph-scoped holders (see `HolderViewModels`).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var player: PlayerManager

    @Inject
    lateinit var themeDao: ThemeDao

    @Inject
    lateinit var queueRestorer: QueueRestorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FonampRoot(
                player = player,
                themeFlow = themeDao.observe(),
                queueRestorer = queueRestorer,
            )
        }
    }
}
