package com.fonamp.provider.api

sealed interface SourceResult<out T> {
    data class Ok<T>(val v: T) : SourceResult<T>
    data class Fail(val e: SourceError) : SourceResult<Nothing>
}
