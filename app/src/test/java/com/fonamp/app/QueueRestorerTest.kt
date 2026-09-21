package com.fonamp.app

import androidx.media3.common.MediaItem
import com.fonamp.core.player.FakePlayerManager
import com.fonamp.core.player.InMemoryQueueStore
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.provider.api.localMediaItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cold-start restore: snapshot → paused queue with position, empty snapshot
 * → untouched, occupied player → untouched, unresolvable ids → idle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QueueRestorerTest {

    private fun localAudio(id: String) = AudioItem(
        sourceId = "local",
        stableId = id,
        title = "Song $id",
        streamUri = "content://media/external/audio/media/$id",
    )

    private val localSource = object : Source {
        override val id = "local"
        override val kind = SourceKind.LOCAL
        override suspend fun browse(query: BrowseQuery) =
            SourceResult.Ok(listOf(localAudio("1"), localAudio("2")))

        override suspend fun search(q: String): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList<AudioItem>())
        override fun streamOf(item: AudioItem): MediaItem = localMediaItem(item)
    }

    @Test
    fun `restoreOnce installs snapshot paused with position`() = runBlocking {
        val player = FakePlayerManager()
        val store = InMemoryQueueStore()
        store.save(listOf("local:1", "local:2"), 1, 60_000L)
        val restorer = QueueRestorer(player, setOf(localSource), store)

        assertTrue(restorer.restoreOnce())

        val state = player.state.value
        assertEquals(listOf("local:1", "local:2"), state.queue.map { it.mediaId })
        assertEquals(1, state.index)
        assertEquals(60_000L, state.positionMs)
        assertFalse(state.isPlaying)
    }

    @Test
    fun `restoreOnce leaves player alone without snapshot`() = runBlocking {
        val player = FakePlayerManager()
        val restorer = QueueRestorer(player, setOf(localSource), InMemoryQueueStore())

        assertFalse(restorer.restoreOnce())
        assertTrue(player.state.value.queue.isEmpty())
    }

    @Test
    fun `restoreOnce never overwrites an occupied player`() = runBlocking {
        val player = FakePlayerManager()
        player.play(listOf(localMediaItem(localAudio("1"))), 0)
        val store = InMemoryQueueStore()
        store.save(listOf("local:2"), 0, 9_000L)
        val restorer = QueueRestorer(player, setOf(localSource), store)

        assertFalse(restorer.restoreOnce())
        assertEquals(listOf("local:1"), player.state.value.queue.map { it.mediaId })
    }

    @Test
    fun `restoreOnce lands idle when nothing resolves`() = runBlocking {
        val player = FakePlayerManager()
        val store = InMemoryQueueStore()
        store.save(listOf("local:9"), 0, 9_000L)
        val restorer = QueueRestorer(player, setOf(localSource), store)

        assertFalse(restorer.restoreOnce())
        assertTrue(player.state.value.queue.isEmpty())
    }
}
