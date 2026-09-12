package com.fonamp.provider.api

import androidx.media3.common.MediaItem

/**
 * Slice A scaffold: proves the MediaItem-only Android edge compiles.
 * Source contract (SourceKind/AudioItem/Source/...) lands in Slice B.
 */
object ApiScaffold {
    const val LOCAL_ID = "local"
    const val RADIO_ID = "radio-browser"

    fun emptyItem(): MediaItem = MediaItem.EMPTY
}
