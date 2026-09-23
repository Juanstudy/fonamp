package com.fonamp.core.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice G RED: `FakePlayerManager` Turbine flows.
 *
 * Covers play/toggle/stop/next/prev/seek-noop-when-live/retry,
 * `TIMEOUT|OFFLINE|STREAM_UNAVAILABLE` → banner state with the UI staying
 * interactive, and process-death restore coherence (restored-or-idle, never
 * phantom-playing). `MediaItem` needs the framework (`Bundle`), so these run
 * under Robolectric `@Config(sdk=[34])` on the cached `android-all`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerManagerTest {

    private fun localItem(id: String, title: String = "Song $id"): MediaItem {
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
                    .setTitle(title)
                    .setArtist("Artist")
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
            putString(PlayerExtras.KEY_STATION_UUID, uuid)
            putString(PlayerExtras.KEY_COUNTRY, "Germany")
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
    fun `play sets queue index playing and liveness`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"), localItem("3"))
        manager.state.test {
            assertEquals(PlayerUiState(), awaitItem())
            manager.play(items, 1)
            val playing = awaitItem()
            assertEquals(items, playing.queue)
            assertEquals(1, playing.index)
            assertTrue(playing.isPlaying)
            assertFalse(playing.isLive)
            assertNull(playing.error)
        }
    }

    @Test
    fun `playSingle wraps one item and radio play is live`() = runTest {
        val manager = FakePlayerManager()
        val station = radioItem("uuid-1")
        manager.state.test {
            awaitItem()
            manager.playSingle(station)
            val playing = awaitItem()
            assertEquals(listOf(station), playing.queue)
            assertEquals(0, playing.index)
            assertTrue(playing.isPlaying)
            assertTrue(playing.isLive)
        }
    }

    @Test
    fun `toggle pauses and resumes local`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.play(listOf(localItem("1")), 0)
            assertTrue(awaitItem().isPlaying)
            manager.togglePlayPause()
            assertFalse(awaitItem().isPlaying)
            manager.togglePlayPause()
            assertTrue(awaitItem().isPlaying)
        }
    }

    @Test
    fun `toggle on live playing stops to keep live edge honest`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.playSingle(radioItem("uuid-1"))
            assertTrue(awaitItem().isPlaying)
            manager.togglePlayPause()
            val stopped = awaitItem()
            assertFalse(stopped.isPlaying)
            assertTrue(stopped.isLive)
        }
    }

    @Test
    fun `stop halts playback but keeps queue context`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.play(items, 0)
            assertTrue(awaitItem().isPlaying)
            manager.stop()
            val stopped = awaitItem()
            assertFalse(stopped.isPlaying)
            assertEquals(items, stopped.queue)
            assertEquals(0, stopped.index)
        }
    }

    @Test
    fun `next and prev walk local queue and clamp at ends`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.play(items, 0)
            assertEquals(0, awaitItem().index)
            manager.next()
            assertEquals(1, awaitItem().index)
            // Clamped: no emission past the end.
            manager.next()
            expectNoEvents()
            manager.prev()
            assertEquals(0, awaitItem().index)
            // Clamped: no emission before the start.
            manager.prev()
            expectNoEvents()
        }
    }

    @Test
    fun `next and prev are no-ops on empty queue`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.next()
            manager.prev()
            expectNoEvents()
        }
    }

    @Test
    fun `seek moves position for local and is a no-op when live`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.play(listOf(localItem("1")), 0)
            awaitItem()
            manager.seekTo(60_000L)
            assertEquals(60_000L, awaitItem().positionMs)

            manager.playSingle(radioItem("uuid-1"))
            awaitItem()
            manager.seekTo(60_000L)
            // Live: seek is a no-op — position stays 0, stream uninterrupted.
            expectNoEvents()
        }
    }

    @Test
    fun `timeout offline and unavailable surface banner state`() = runTest {
        for (error in listOf(PlayerError.TIMEOUT, PlayerError.OFFLINE, PlayerError.STREAM_UNAVAILABLE)) {
            val manager = FakePlayerManager()
            manager.state.test {
                awaitItem()
                manager.failNextWith(error)
                manager.play(listOf(localItem("1")), 0)
                val failed = awaitItem()
                assertEquals(error, failed.error)
                assertFalse(failed.isPlaying)
            }
        }
    }

    @Test
    fun `ui stays interactive while banner is shown`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.failNextWith(PlayerError.TIMEOUT)
            manager.play(items, 0)
            val failed = awaitItem()
            assertEquals(PlayerError.TIMEOUT, failed.error)
            // Banner up, but navigation + toggle still emit — never frozen.
            manager.next()
            assertEquals(1, awaitItem().index)
            manager.togglePlayPause()
            assertTrue(awaitItem().isPlaying)
        }
    }

    @Test
    fun `retry clears banner and resumes after transient failure`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.failNextWith(PlayerError.OFFLINE)
            manager.play(listOf(localItem("1")), 0)
            assertEquals(PlayerError.OFFLINE, awaitItem().error)
            manager.retry()
            val retried = awaitItem()
            assertNull(retried.error)
            assertTrue(retried.isPlaying)
        }
    }

    @Test
    fun `retry keeps banner when failure persists`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.failNextWith(PlayerError.STREAM_UNAVAILABLE)
            manager.play(listOf(localItem("1")), 0)
            assertEquals(PlayerError.STREAM_UNAVAILABLE, awaitItem().error)
            manager.failNextWith(PlayerError.STREAM_UNAVAILABLE)
            manager.retry()
            // Same banner value → StateFlow conflates (no re-emission),
            // so assert the sticky value directly.
            expectNoEvents()
            assertEquals(PlayerError.STREAM_UNAVAILABLE, manager.state.value.error)
            assertFalse(manager.state.value.isPlaying)
        }
    }

    @Test
    fun `restore lands restored-but-paused never phantom-playing`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.restore(items, 1)
            val restored = awaitItem()
            assertEquals(items, restored.queue)
            assertEquals(1, restored.index)
            assertFalse(restored.isLive)
            // Restored queue, but never phantom-playing and never an error.
            assertFalse(restored.isPlaying)
            assertNull(restored.error)
        }
    }

    @Test
    fun `restore with empty snapshot lands clean idle`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.play(listOf(localItem("1")), 0)
            assertTrue(awaitItem().isPlaying)
            manager.restore(emptyList(), 0)
            assertEquals(PlayerUiState(), awaitItem())
        }
    }

    @Test
    fun `restore keeps saved position for local`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.restore(items, 1, 60_000L)
            val restored = awaitItem()
            assertEquals(items, restored.queue)
            assertEquals(1, restored.index)
            assertFalse(restored.isPlaying)
            assertEquals(60_000L, restored.positionMs)
        }
    }

    @Test
    fun `restore forces zero position for live radio`() = runTest {
        val manager = FakePlayerManager()
        val station = radioItem("uuid-1")
        manager.state.test {
            awaitItem()
            manager.restore(listOf(station), 0, 60_000L)
            val restored = awaitItem()
            assertTrue(restored.isLive)
            assertFalse(restored.isPlaying)
            assertEquals(0L, restored.positionMs)
        }
    }

    @Test
    fun `restore clamps out-of-range index`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.restore(items, 99, 5_000L)
            val restored = awaitItem()
            assertEquals(1, restored.index)
            assertEquals(5_000L, restored.positionMs)
        }
    }

    @Test
    fun `queue store round-trips snapshot and clear lands idle`() {
        val store: QueueStore = InMemoryQueueStore()
        assertNull(store.load())
        store.save(listOf("local:1", "local:2"), 1, 60_000L)
        val loaded = store.load()!!
        assertEquals(listOf("local:1", "local:2"), loaded.mediaIds)
        assertEquals(1, loaded.index)
        assertEquals(60_000L, loaded.positionMs)
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun `playback service pins 10s stream timeouts`() {
        assertEquals(10_000, PlaybackService.CONNECT_TIMEOUT_MS)
        assertEquals(10_000, PlaybackService.READ_TIMEOUT_MS)
    }

    @Test
    fun `repeat cycles off all one off`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            assertEquals(RepeatMode.OFF, awaitItem().repeatMode)
            manager.cycleRepeat()
            assertEquals(RepeatMode.ALL, awaitItem().repeatMode)
            manager.cycleRepeat()
            assertEquals(RepeatMode.ONE, awaitItem().repeatMode)
            manager.cycleRepeat()
            assertEquals(RepeatMode.OFF, awaitItem().repeatMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `shuffle toggles and speed walks the cycle`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            val initial = awaitItem()
            assertEquals(false, initial.shuffleEnabled)
            assertEquals(1f, initial.speed)
            manager.toggleShuffle()
            assertEquals(true, awaitItem().shuffleEnabled)
            manager.toggleShuffle()
            assertEquals(false, awaitItem().shuffleEnabled)
            manager.cycleSpeed()
            assertEquals(1.25f, awaitItem().speed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `speed cycle wraps and resets unknowns`() {
        assertEquals(1.25f, PlayerManager.nextSpeed(1f))
        assertEquals(0.5f, PlayerManager.nextSpeed(2f))
        assertEquals(1f, PlayerManager.nextSpeed(1.1f))
        var speed = 1f
        repeat(PlayerManager.SPEEDS.size) { speed = PlayerManager.nextSpeed(speed) }
        assertEquals(1f, speed)
    }

    @Test
    fun `repeat maps to media3 both ways`() {
        assertEquals(Player.REPEAT_MODE_OFF, RepeatMode.OFF.toMedia3())
        assertEquals(Player.REPEAT_MODE_ALL, RepeatMode.ALL.toMedia3())
        assertEquals(Player.REPEAT_MODE_ONE, RepeatMode.ONE.toMedia3())
        assertEquals(RepeatMode.ALL, RepeatMode.fromMedia3(Player.REPEAT_MODE_ALL))
        assertEquals(RepeatMode.OFF, RepeatMode.fromMedia3(999))
    }

    @Test
    fun `play preserves transport modes`() = runTest {
        val manager = FakePlayerManager()
        manager.toggleShuffle()
        manager.cycleRepeat()
        manager.cycleSpeed()
        manager.play(listOf(localItem("1")), 0)
        manager.state.test {
            val state = awaitItem()
            assertEquals(true, state.shuffleEnabled)
            assertEquals(RepeatMode.ALL, state.repeatMode)
            assertEquals(1.25f, state.speed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playAt jumps within queue and starts playback`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"), localItem("3"))
        manager.state.test {
            awaitItem()
            manager.play(items, 0)
            awaitItem()
            manager.playAt(2)
            val jumped = awaitItem()
            assertEquals(2, jumped.index)
            assertTrue(jumped.isPlaying)
            assertFalse(jumped.isLive)
            assertEquals(0L, jumped.positionMs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playAt preserves sleep timer unlike play`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.play(items, 0)
            awaitItem()
            manager.setSleepTimer(15 * 60_000L)
            val armed = awaitItem().sleepEndsAtMs
            manager.playAt(1)
            val jumped = awaitItem()
            assertEquals(1, jumped.index)
            assertEquals(armed, jumped.sleepEndsAtMs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playAt preserves banner unlike retry`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.failNextWith(PlayerError.TIMEOUT)
            manager.play(items, 0)
            assertEquals(PlayerError.TIMEOUT, awaitItem().error)
            manager.playAt(1)
            val jumped = awaitItem()
            assertEquals(1, jumped.index)
            assertTrue(jumped.isPlaying)
            assertEquals(PlayerError.TIMEOUT, jumped.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playAt clamps out-of-range index`() = runTest {
        val manager = FakePlayerManager()
        val items = listOf(localItem("1"), localItem("2"))
        manager.state.test {
            awaitItem()
            manager.play(items, 0)
            awaitItem()
            manager.playAt(99)
            assertEquals(1, awaitItem().index)
            manager.playAt(-5)
            assertEquals(0, awaitItem().index)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `playAt is no-op on empty queue and same index`() = runTest {
        val manager = FakePlayerManager()
        manager.state.test {
            awaitItem()
            manager.playAt(0)
            expectNoEvents()
            manager.play(listOf(localItem("1"), localItem("2")), 1)
            awaitItem()
            manager.playAt(1)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
