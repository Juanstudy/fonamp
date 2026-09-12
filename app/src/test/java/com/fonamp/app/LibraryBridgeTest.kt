package com.fonamp.app

import androidx.media3.common.MediaItem
import com.fonamp.core.player.FakePlayerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * "Fav + seam" mirror: [LibraryPlayerAdapter] is the sole bridge from the
 * Collection [com.fonamp.feature.library.LibraryPlayer] contract to the shared
 * [com.fonamp.core.player.PlayerManager] — `playQueue` must reach `play` with
 * identical items and index, so the Collection tab shares one runtime player.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryBridgeTest {

    private fun item(id: String) = MediaItem.Builder()
        .setMediaId(id)
        .setUri("content://media/external/audio/media/$id")
        .build()

    @Test
    fun `adapter delegates playQueue to PlayerManager play untouched`() {
        val player = FakePlayerManager()
        val adapter = LibraryPlayerAdapter(player)
        val items = listOf(item("local:1"), item("local:2"), item("local:3"))

        adapter.playQueue(items, 1)

        val state = player.state.value
        assertEquals(listOf("local:1", "local:2", "local:3"), state.queue.map { it.mediaId })
        assertEquals(1, state.index)
        assertTrue(state.isPlaying)
        assertFalse(state.isLive)
    }

    @Test
    fun `adapter delegates single-item queues at index zero`() {
        val player = FakePlayerManager()
        val adapter = LibraryPlayerAdapter(player)

        adapter.playQueue(listOf(item("local:7")), 0)

        val state = player.state.value
        assertEquals(listOf("local:7"), state.queue.map { it.mediaId })
        assertEquals(0, state.index)
        assertTrue(state.isPlaying)
    }
}
