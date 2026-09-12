package com.fonamp.provider.api

/** Typed failures only — network/IO exceptions are mapped inside providers, never leak to UI. */
sealed interface SourceError {
    data object Offline : SourceError
    data object Timeout : SourceError
    data class Server(val code: Int?) : SourceError
    data class Unknown(val cause: Throwable) : SourceError
}
