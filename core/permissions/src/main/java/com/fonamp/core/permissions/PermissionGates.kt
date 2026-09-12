package com.fonamp.core.permissions

/**
 * Slice I: audio-permission gate decisions (permissions Req 2–3, design §6).
 *
 * Pure Kotlin (no Android imports) so the API-split and routing rules stay
 * JVM-testable. The Compose launcher wiring that *acts* on these decisions
 * lives in `:app` (the only module allowed to own navigation + Activity APIs);
 * `core` modules keep their third-party-only dependency rule (design §1/§9).
 *
 * Rules:
 * - `READ_MEDIA_AUDIO` on API 33+, `READ_EXTERNAL_STORAGE` on API 29–32.
 * - Requested only on first entering Collection; the rest of the app is never
 *   gated — no wall before value.
 * - Granted → rebuild the list; denied → `Denied` with why + grant-again;
 *   permanently denied → `Denied` with the system-settings deep-link.
 */
const val CollectionEntryRoute = "collection"

/** What the Collection entry should render for the current permission state. */
enum class AudioGateAction {
    /** Fire the system permission sheet once (first entry, never asked). */
    RequestPermission,
    /** Permission present (or route ungated): show the list, rebuild on grant. */
    ShowGranted,
    /** Denied but askable: `Denied` state with why + grant-again. */
    ShowDeniedRetry,
    /** Permanently denied: `Denied` state with the system-settings deep-link. */
    ShowDeniedSettings,
}

data class AudioGateInput(
    val route: String,
    val permissionGranted: Boolean,
    val askedBefore: Boolean,
    val permanentlyDenied: Boolean,
)

object AudioPermissionGate {
    fun permissionForSdk(sdkInt: Int): String =
        AudioPermission.permissionForSdk(sdkInt)

    fun decide(input: AudioGateInput): AudioGateAction {
        if (input.route != CollectionEntryRoute) return AudioGateAction.ShowGranted
        if (input.permissionGranted) return AudioGateAction.ShowGranted
        if (!input.askedBefore) return AudioGateAction.RequestPermission
        return if (input.permanentlyDenied) {
            AudioGateAction.ShowDeniedSettings
        } else {
            AudioGateAction.ShowDeniedRetry
        }
    }
}

/**
 * Slice I: notification-permission gate (permissions Req 1, design §6).
 *
 * Requested lazily on first playback (API 33+) and failure is tolerated:
 * playback continues and the system may hide the notification. Never blocks,
 * never re-nags once asked.
 */
object NotificationGate {
    fun shouldRequest(hasPlayedOnce: Boolean, askedBefore: Boolean): Boolean =
        hasPlayedOnce && !askedBefore
}
