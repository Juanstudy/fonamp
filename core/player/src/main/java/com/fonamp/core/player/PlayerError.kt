package com.fonamp.core.player

/**
 * Visible stream-failure kinds (player Req 4).
 *
 * Every failure lands in [PlayerUiState.error] as an inline banner with a
 * manual retry — never a freeze, crash, or silent stall — while the rest of
 * the UI stays interactive.
 */
enum class PlayerError {
    /** Stalled stream hit the 10 s connect/read timeout. */
    TIMEOUT,

    /** No connectivity (DNS/connection failure while offline). */
    OFFLINE,

    /** Connected but unplayable (HTTP error, unsupported format, dead URL). */
    STREAM_UNAVAILABLE,
}
