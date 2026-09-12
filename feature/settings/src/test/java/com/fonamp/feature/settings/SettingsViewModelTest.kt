package com.fonamp.feature.settings

import app.cash.turbine.test
import com.fonamp.core.database.ThemeDao
import com.fonamp.core.database.ThemeMode
import com.fonamp.core.database.ThemePref
import com.fonamp.core.network.DirectoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Slice I RED: `SettingsViewModel` theme round-trip (System→Dark→restart→Light,
 * no-restart apply) + cache-stats/clear confirmation (settings Req 1–2).
 *
 * The ViewModel's DAO collector never terminates, so every ViewModel runs on
 * `backgroundScope` (cancelled at test teardown, never awaited — otherwise
 * `runTest` hangs 60s per test with `UncompletedCoroutinesError`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var themeDao: FakeThemeDao
    private lateinit var cache: DirectoryCache

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        themeDao = FakeThemeDao()
        cache = DirectoryCache(dir = null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `default theme is System`() = runTest {
        val vm = SettingsViewModel(themeDao, cache, backgroundScope, StandardTestDispatcher(testScheduler))
        vm.state.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().theme)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting Dark applies immediately without restart`() = runTest {
        val vm = SettingsViewModel(themeDao, cache, backgroundScope, StandardTestDispatcher(testScheduler))
        vm.state.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().theme)
            vm.setTheme(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem().theme)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `theme survives restart then Light applies`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val first = SettingsViewModel(themeDao, cache, backgroundScope, io)
        first.state.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem().theme)
            first.setTheme(ThemeMode.DARK)
            // Drive the scheduler via collection (Slice H pattern): the
            // background DAO write + collector run while awaiting.
            var cur = awaitItem().theme
            while (cur != ThemeMode.DARK) cur = awaitItem().theme
            assertEquals(ThemeMode.DARK, cur)
            cancelAndIgnoreRemainingEvents()
        }

        // Restart: a new ViewModel over the SAME fake store still sees DARK.
        // The collector replays the persisted row; allow one stale initial
        // emission before the DAO-driven update lands.
        val restarted = SettingsViewModel(themeDao, cache, backgroundScope, io)
        restarted.state.test {
            var cur = awaitItem().theme
            while (cur != ThemeMode.DARK) cur = awaitItem().theme
            assertEquals(ThemeMode.DARK, cur)
            restarted.setTheme(ThemeMode.LIGHT)
            cur = awaitItem().theme
            while (cur != ThemeMode.LIGHT) cur = awaitItem().theme
            assertEquals(ThemeMode.LIGHT, cur)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cache stats are reported`() = runTest {
        val vm = SettingsViewModel(themeDao, cache, backgroundScope, StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()
        vm.state.test {
            val initial = awaitItem()
            assertEquals(0, initial.cacheEntries)
            assertEquals(0L, initial.cacheBytes)
            assertNull(initial.clearConfirmation)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear cache frees bytes and confirms`() = runTest {
        val vm = SettingsViewModel(themeDao, cache, backgroundScope, StandardTestDispatcher(testScheduler))
        testScheduler.advanceUntilIdle()
        vm.state.test {
            awaitItem()
            vm.clearCache()
            val confirmed = awaitItem()
            assertNotNull(confirmed.clearConfirmation)
            assertTrue(confirmed.clearConfirmation!!.isNotBlank())
            assertEquals(0, confirmed.cacheEntries)
            assertEquals(0L, confirmed.cacheBytes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Hand-written fake: upsert-replaces the single id=1 row, emits on change. */
    private class FakeThemeDao : ThemeDao {
        private val backing = MutableStateFlow<ThemePref?>(null)

        override fun observe() = backing.asStateFlow()

        override suspend fun set(pref: ThemePref) {
            backing.update { pref.copy(id = 1) }
        }
    }
}
