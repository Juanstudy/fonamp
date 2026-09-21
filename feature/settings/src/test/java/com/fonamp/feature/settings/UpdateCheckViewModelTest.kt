package com.fonamp.feature.settings

import app.cash.turbine.test
import com.fonamp.core.network.GithubRelease
import com.fonamp.core.network.NetworkError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T3: [UpdateCheckViewModel] — newer maps to Available, same to UpToDate,
 * failures and apk-less releases stay silent, dismiss returns to Idle.
 * Hand-faked fetch seam, Turbine flows, no Robolectric.
 */
class UpdateCheckViewModelTest {

    private fun release(
        tag: String = "v0.0.10",
        apkUrl: String? = "https://cdn.example/f.apk",
    ) = GithubRelease(
        tag = tag,
        name = tag,
        notes = "## Qué trae\n- X",
        apkUrl = apkUrl,
        apkSize = 7L,
    )

    @Test
    fun `newer release becomes available`() = runTest {
        val vm = UpdateCheckViewModel(fetchLatest = { release() }, scope = backgroundScope)
        vm.state.test {
            assertTrue(awaitItem() is UpdateUiState.Idle)
            vm.checkForUpdates("0.0.9")
            assertTrue(awaitItem() is UpdateUiState.Checking)
            val available = awaitItem() as UpdateUiState.Available
            assertEquals("v0.0.10", available.tag)
            assertEquals("https://cdn.example/f.apk", available.apkUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `same version is up to date`() = runTest {
        val vm = UpdateCheckViewModel(
            fetchLatest = { release(tag = "v0.0.9") },
            scope = backgroundScope,
        )
        vm.state.test {
            assertTrue(awaitItem() is UpdateUiState.Idle)
            vm.checkForUpdates("0.0.9")
            assertTrue(awaitItem() is UpdateUiState.Checking)
            assertTrue(awaitItem() is UpdateUiState.UpToDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `offline and apk-less releases stay silent`() = runTest {
        val offline = UpdateCheckViewModel(
            fetchLatest = { throw NetworkError.Offline },
            scope = backgroundScope,
        )
        offline.state.test {
            assertTrue(awaitItem() is UpdateUiState.Idle)
            offline.checkForUpdates("0.0.9")
            assertTrue(awaitItem() is UpdateUiState.Checking)
            assertTrue(awaitItem() is UpdateUiState.Unavailable)
            cancelAndIgnoreRemainingEvents()
        }

        val noApk = UpdateCheckViewModel(
            fetchLatest = { release(tag = "v0.0.10", apkUrl = null) },
            scope = backgroundScope,
        )
        noApk.state.test {
            assertTrue(awaitItem() is UpdateUiState.Idle)
            noApk.checkForUpdates("0.0.9")
            assertTrue(awaitItem() is UpdateUiState.Checking)
            assertTrue(awaitItem() is UpdateUiState.Unavailable)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `dismiss returns to idle`() = runTest {
        val vm = UpdateCheckViewModel(fetchLatest = { release() }, scope = backgroundScope)
        vm.state.test {
            assertTrue(awaitItem() is UpdateUiState.Idle)
            vm.checkForUpdates("0.0.9")
            assertTrue(awaitItem() is UpdateUiState.Checking)
            assertTrue(awaitItem() is UpdateUiState.Available)
            vm.dismiss()
            assertTrue(awaitItem() is UpdateUiState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
