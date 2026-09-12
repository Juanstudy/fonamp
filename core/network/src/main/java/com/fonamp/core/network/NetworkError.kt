package com.fonamp.core.network

/**
 * Typed network failures (design §4). The client throws only these — raw
 * Retrofit/OkHttp/IO exceptions are mapped inside [RadioBrowserClient] and
 * never leak to callers (`provider/radio` maps these to `SourceError`).
 */
sealed class NetworkError : Exception() {
    /** No connectivity (DNS, refused, reset, TLS, other IO). */
    data object Offline : NetworkError() {
        private fun readResolve(): Any = Offline
    }

    /** Connect/read/call budget exceeded. */
    data object Timeout : NetworkError() {
        private fun readResolve(): Any = Timeout
    }

    /** HTTP error status from a mirror. */
    data class Server(val code: Int?) : NetworkError()

    /** Anything else (e.g. malformed JSON). */
    data class Unknown(val error: Throwable? = null) : NetworkError()
}
