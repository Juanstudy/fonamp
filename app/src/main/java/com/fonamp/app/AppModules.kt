package com.fonamp.app

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.room.Room
import com.fonamp.core.database.FavoriteDao
import com.fonamp.core.database.FonampDatabase
import com.fonamp.core.database.ThemeDao
import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.RadioBrowserClient
import com.fonamp.core.player.DefaultPlayerManager
import com.fonamp.core.player.PlayerManager
import com.fonamp.feature.library.LibraryPlayer
import com.fonamp.provider.api.Source
import com.fonamp.provider.local.LocalSource
import com.fonamp.provider.radio.RadioBrowserSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.multibindings.IntoSet
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Slice I: Hilt app graph (design §1).
 *
 * - Sources reach the graph as a `Set<Source>` via `@Provides @IntoSet`
 *   multibindings keyed by contract [Source.id] (see [resolveSource]).
 *   Deviation note: `@Provides` instead of `@Binds` because `LocalSource`
 *   and `RadioBrowserSource` have no `@Inject` constructors and those modules
 *   are read-only from this slice; the multibinding shape (one line per new
 *   provider source module, keyed by id) is the design §1 contract.
 * - [PlayerModule] binds the production [PlayerManager].
 * - [DatabaseModule] owns Room, the directory cache dir, and the radio client.
 */
@Module
@InstallIn(SingletonComponent::class)
object SourceModule {

    @Provides
    @IntoSet
    fun provideLocalSource(@ApplicationContext context: Context): Source =
        LocalSource(context.contentResolver)

    @Provides
    @IntoSet
    fun provideRadioSource(
        client: RadioBrowserClient,
        cache: DirectoryCache,
    ): Source = RadioBrowserSource(client, cache)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    abstract fun bindPlayerManager(impl: DefaultPlayerManager): PlayerManager
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FonampDatabase =
        Room.databaseBuilder(context, FonampDatabase::class.java, "fonamp.db").build()

    @Provides
    fun provideFavoriteDao(db: FonampDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideThemeDao(db: FonampDatabase): ThemeDao = db.themeDao()

    @Provides
    @Singleton
    fun provideDirectoryCache(@ApplicationContext context: Context): DirectoryCache =
        DirectoryCache(File(context.cacheDir, "directory"))

    @Provides
    @Singleton
    fun provideRadioClient(@ApplicationContext context: Context): RadioBrowserClient =
        RadioBrowserClient(cacheDir = File(context.cacheDir, "http"))
}

/**
 * Slice F contract, Slice I binding: the Collection tab's [LibraryPlayer] seam
 * has the Slice G `PlayerManager.play` signature, so this adapter is the
 * drop-in production binding — no `feature/library` change needed.
 */
class LibraryPlayerAdapter @javax.inject.Inject constructor(
    private val player: PlayerManager,
) : LibraryPlayer {
    override fun playQueue(items: List<MediaItem>, index: Int) {
        player.play(items, index)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryBridgeModule {

    @Binds
    abstract fun bindLibraryPlayer(impl: LibraryPlayerAdapter): LibraryPlayer
}
