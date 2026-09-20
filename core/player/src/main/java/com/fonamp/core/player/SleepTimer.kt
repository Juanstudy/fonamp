package com.fonamp.core.player

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Sleep timer: stops playback once, at a monotonic deadline.
 *
 * Deliberately NOT an [android.app.AlarmManager] alarm:
 * - `setAndAllowWhileIdle` has no `OnAlarmListener` overload, so it would
 *   need a manifest receiver + `PendingIntent` for zero benefit;
 * - `setExactAndAllowWhileIdle` would need the `SCHEDULE_EXACT_ALARM`
 *   permission (spec + audit impact) for precision nobody needs here;
 * - while audio plays, the foreground service keeps this process alive, so
 *   an in-process countdown fires just as reliably at minute precision —
 *   and dying with the process is the desired behavior anyway, since
 *   playback dies with it too and there is nothing left to stop.
 *
 * [scope] hosts the countdown (production: the manager's scope; tests: a
 * test scope with virtual time). [elapsedNow] is injectable so tests drive
 * the deadline clock deterministically.
 */
class SleepTimer(
    private val scope: CoroutineScope,
    private val elapsedNow: () -> Long = SystemClock::elapsedRealtime,
    private val onExpire: () -> Unit = {},
) {
    private var fireJob: Job? = null

    private val _endsAtMs = MutableStateFlow<Long?>(null)

    /** Monotonic ([SystemClock.elapsedRealtime]) deadline, null when inactive. */
    val endsAtMs: StateFlow<Long?> = _endsAtMs.asStateFlow()

    /**
     * Arm (or re-arm) the timer [durationMs] from now.
     * @throws IllegalArgumentException when [durationMs] is not positive.
     */
    fun set(durationMs: Long) {
        require(durationMs > 0L) { "Sleep timer duration must be positive, was=$durationMs" }
        fireJob?.cancel()
        _endsAtMs.value = elapsedNow() + durationMs
        fireJob = scope.launch {
            delay(durationMs)
            _endsAtMs.value = null
            onExpire()
        }
    }

    /** Disarm; no-op when already inactive. */
    fun clear() {
        fireJob?.cancel()
        fireJob = null
        _endsAtMs.value = null
    }

    /** Milliseconds left, 0 when inactive or past the deadline. */
    fun remainingMs(): Long {
        val endsAt = _endsAtMs.value ?: return 0L
        return (endsAt - elapsedNow()).coerceAtLeast(0L)
    }
}
