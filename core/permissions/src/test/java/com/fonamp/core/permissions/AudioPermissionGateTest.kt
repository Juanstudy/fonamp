package com.fonamp.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Slice I RED: `AudioPermissionGate` API-split + first-entry-Collection-only logic
 * (permissions Req 2–3) and lazy `NotificationGate` (tolerated failure).
 */
class AudioPermissionGateTest {

    @Test
    fun `api 33 plus requests READ_MEDIA_AUDIO`() {
        assertEquals(
            AudioPermission.READ_MEDIA_AUDIO,
            AudioPermissionGate.permissionForSdk(33),
        )
        assertEquals(
            AudioPermission.READ_MEDIA_AUDIO,
            AudioPermissionGate.permissionForSdk(35),
        )
    }

    @Test
    fun `api 29 to 32 requests READ_EXTERNAL_STORAGE`() {
        assertEquals(
            AudioPermission.READ_EXTERNAL_STORAGE,
            AudioPermissionGate.permissionForSdk(29),
        )
        assertEquals(
            AudioPermission.READ_EXTERNAL_STORAGE,
            AudioPermissionGate.permissionForSdk(32),
        )
    }

    @Test
    fun `first entry to Collection requests permission`() {
        assertEquals(
            AudioGateAction.RequestPermission,
            AudioPermissionGate.decide(
                AudioGateInput(
                    route = CollectionEntryRoute,
                    permissionGranted = false,
                    askedBefore = false,
                    permanentlyDenied = false,
                ),
            ),
        )
    }

    @Test
    fun `granted rebuilds list`() {
        assertEquals(
            AudioGateAction.ShowGranted,
            AudioPermissionGate.decide(
                AudioGateInput(
                    route = CollectionEntryRoute,
                    permissionGranted = true,
                    askedBefore = true,
                    permanentlyDenied = false,
                ),
            ),
        )
    }

    @Test
    fun `denied shows grant again`() {
        assertEquals(
            AudioGateAction.ShowDeniedRetry,
            AudioPermissionGate.decide(
                AudioGateInput(
                    route = CollectionEntryRoute,
                    permissionGranted = false,
                    askedBefore = true,
                    permanentlyDenied = false,
                ),
            ),
        )
    }

    @Test
    fun `permanent denial deep links to settings`() {
        assertEquals(
            AudioGateAction.ShowDeniedSettings,
            AudioPermissionGate.decide(
                AudioGateInput(
                    route = CollectionEntryRoute,
                    permissionGranted = false,
                    askedBefore = true,
                    permanentlyDenied = true,
                ),
            ),
        )
    }

    @Test
    fun `rest of app is never gated`() {
        listOf("radio/discover", "radio/stations", "favorites", "settings").forEach { route ->
            assertEquals(
                "route $route must stay usable without audio permission",
                AudioGateAction.ShowGranted,
                AudioPermissionGate.decide(
                    AudioGateInput(
                        route = route,
                        permissionGranted = false,
                        askedBefore = false,
                        permanentlyDenied = false,
                    ),
                ),
            )
        }
    }

    @Test
    fun `notification is lazy on first playback`() {
        // Never before the first playback.
        assertEquals(false, NotificationGate.shouldRequest(hasPlayedOnce = false, askedBefore = false))
        // Lazily on first playback.
        assertEquals(true, NotificationGate.shouldRequest(hasPlayedOnce = true, askedBefore = false))
    }

    @Test
    fun `notification failure is tolerated and never re-nags`() {
        // Already asked (granted or denied): playback continues, no second request.
        assertEquals(false, NotificationGate.shouldRequest(hasPlayedOnce = true, askedBefore = true))
    }
}
