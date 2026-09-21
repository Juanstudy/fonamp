package com.fonamp.core.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pure mapping tests for [QueueResolver]: order preserved, missing ids
 * dropped, index clamped, live forces position 0, empty → null.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QueueResolverTest {

    private fun localItem(id: String): MediaItem {
        val extras = Bundle().apply {
            putString(PlayerExtras.KEY_SOURCE_ID, "local")
            putString(PlayerExtras.KEY_STABLE_ID, id)
            putBoolean(PlayerExtras.KEY_IS_LIVE, false)
        }
        return MediaItem.Builder()
            .setMediaId("local:$id")
            .setUri("content://media/external/audio/media/$id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Song $id")
                    .setIsPlayable(true)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    private fun radioItem(uuid: String): MediaItem {
        val extras = Bundle().apply {
            putString(PlayerExtras.KEY_SOURCE_ID, "radio-browser")
            putString(PlayerExtras.KEY_STABLE_ID, uuid)
            putBoolean(PlayerExtras.KEY_IS_LIVE, true)
        }
        return MediaItem.Builder()
            .setMediaId("radio:$uuid")
            .setUri("http://stream.example/$uuid")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Station $uuid")
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    @Test
    fun `resolve keeps snapshot order and position`() {
        val candidates = listOf(localItem("1"), localItem("2"), localItem("3"))
        val resolved = QueueResolver.resolve(
            SavedQueue(listOf("local:2", "local:3"), 1, 42_000L),
            candidates,
        )!!
        assertEquals(listOf(candidates[1], candidates[2]), resolved.items)
        assertEquals(1, resolved.index)
        assertEquals(42_000L, resolved.positionMs)
    }

    @Test
    fun `resolve drops missing ids and clamps index`() {
        val candidates = listOf(localItem("1"))
        val resolved = QueueResolver.resolve(
            SavedQueue(listOf("local:9", "local:1", "local:8"), 2, 5_000L),
            candidates,
        )!!
        assertEquals(listOf(candidates[0]), resolved.items)
        assertEquals(0, resolved.index)
        assertEquals(5_000L, resolved.positionMs)
    }

    @Test
    fun `resolve forces zero position for live`() {
        val station = radioItem("uuid-1")
        val resolved = QueueResolver.resolve(
            SavedQueue(listOf("radio:uuid-1"), 0, 60_000L),
            listOf(station),
        )!!
        assertEquals(0L, resolved.positionMs)
    }

    @Test
    fun `resolve returns null when nothing survives`() {
        assertNull(QueueResolver.resolve(null, listOf(localItem("1"))))
        assertNull(
            QueueResolver.resolve(
                SavedQueue(emptyList(), 0, 0L),
                listOf(localItem("1")),
            ),
        )
        assertNull(
            QueueResolver.resolve(
                SavedQueue(listOf("local:9"), 0, 0L),
                listOf(localItem("1")),
            ),
        )
        assertNull(
            QueueResolver.resolve(
                SavedQueue(listOf("local:1"), 0, 0L),
                emptyList(),
            ),
        )
    }
}
