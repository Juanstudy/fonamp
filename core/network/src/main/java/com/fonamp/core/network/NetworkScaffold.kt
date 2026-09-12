package com.fonamp.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Slice A scaffold: proves Retrofit/OkHttp/serialization wiring compiles.
 * RadioBrowser client + mirror policy + DirectoryCache land in Slice E.
 */
object NetworkScaffold {
    const val CONNECT_TIMEOUT_S = 10L
    const val READ_TIMEOUT_S = 10L

    fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
        .build()
}
