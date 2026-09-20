package com.fonamp.core.player

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * T1: [SleepTimer] on virtual time — deadline/remaining math on a fake
 * clock, expiry fires exactly once, clear/re-set cancel the pending shot.
 * Pure JVM: no Robolectric, no waiting.
 */
class SleepTimerTest {

    private var now = 1_000_000L
    private var expirations = 0

    private fun TestScope.timer() = SleepTimer(
        scope = backgroundScope,
        elapsedNow = { now },
        onExpire = { expirations++ },
    )

    @Test
    fun `set arms deadline and remaining`() = runTest {
        val timer = timer()
        timer.set(30 * 60_000L)

        assertEquals(now + 30 * 60_000L, timer.endsAtMs.value)
        assertEquals(30 * 60_000L, timer.remainingMs())
        assertEquals(0, expirations)
    }

    @Test
    fun `expiry fires once and clears`() = runTest {
        val timer = timer()
        timer.set(10 * 60_000L)

        testScheduler.advanceTimeBy(10 * 60_000L)
        runCurrent()

        assertNull(timer.endsAtMs.value)
        assertEquals(0L, timer.remainingMs())
        assertEquals(1, expirations)
    }

    @Test
    fun `clear before firing prevents expiry`() = runTest {
        val timer = timer()
        timer.set(10 * 60_000L)
        timer.clear()

        assertNull(timer.endsAtMs.value)
        testScheduler.advanceTimeBy(60 * 60_000L)
        runCurrent()

        assertEquals(0, expirations)
    }

    @Test
    fun `set twice fires only the second`() = runTest {
        val timer = timer()
        timer.set(30 * 60_000L)
        timer.set(5 * 60_000L)

        testScheduler.advanceTimeBy(30 * 60_000L)
        runCurrent()
        assertEquals(1, expirations)

        testScheduler.advanceTimeBy(60 * 60_000L)
        runCurrent()
        assertEquals(1, expirations)
    }

    @Test
    fun `remaining clamps past the deadline`() = runTest {
        val timer = timer()
        timer.set(1_000L)
        now += 5_000L

        assertEquals(0L, timer.remainingMs())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `set rejects non-positive durations`() = runTest {
        timer().set(0L)
    }
}
