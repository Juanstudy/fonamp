package com.fonamp.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import java.io.File

/** Slice A scaffold: Hilt entry point. Navigation + bindings land in Slice I. */
@HiltAndroidApp
class FonampApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.1) // 10% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "artwork_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // Strict 50 MB limit
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false) // Radio headers are notoriously unreliable
            .build()
    }
}
