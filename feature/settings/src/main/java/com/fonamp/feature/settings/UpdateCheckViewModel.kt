package com.fonamp.feature.settings

import com.fonamp.core.network.GithubRelease
import com.fonamp.core.network.isNewerVersion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * In-app update state machine (one instance per nav entry, Slice I holder
 * precedent — the shell root and the Settings entry each own one and both
 * converge on the same server truth on demand).
 *
 * - [Idle] nothing checked yet (or dismissed).
 * - [Checking] directory fetch in flight; concurrent calls collapse into it.
 * - [Available] a newer release with an installable apk; the UI offers
 *   Download/Later. Download itself is app-owned (installer lives in `:app`,
 *   features must not touch it).
 * - [UpToDate] manual checks announce this (snackbar); the auto-check path
 *   ignores it and stays silent.
 * - [Unavailable] silent failure: offline, timeout, server error, or a
 *   release without an apk asset — never a dialog, never a crash.
 *
 * [fetchLatest] is a suspend seam (hand fake in tests, client call in prod)
 * so this stays Robolectric-free.
 */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val tag: String, val notes: String?, val apkUrl: String) : UpdateUiState
    data object UpToDate : UpdateUiState
    data object Unavailable : UpdateUiState
}

class UpdateCheckViewModel(
    private val fetchLatest: suspend () -> GithubRelease,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    fun checkForUpdates(currentVersion: String) {
        if (_state.value is UpdateUiState.Checking) return
        _state.value = UpdateUiState.Checking
        scope.launch {
            val next = try {
                val release = withContext(io) { fetchLatest() }
                val apkUrl = release.apkUrl
                when {
                    apkUrl == null -> UpdateUiState.Unavailable
                    isNewerVersion(release.tag, currentVersion) ->
                        UpdateUiState.Available(release.tag, release.notes, apkUrl)
                    else -> UpdateUiState.UpToDate
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                UpdateUiState.Unavailable
            }
            _state.value = next
        }
    }

    /** Back to idle after the dialog/snackbar was handled. */
    fun dismiss() {
        _state.value = UpdateUiState.Idle
    }
}
