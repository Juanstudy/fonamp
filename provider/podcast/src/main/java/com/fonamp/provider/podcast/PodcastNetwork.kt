package com.fonamp.provider.podcast

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object PodcastNetwork {
    private val client by lazy { OkHttpClient.Builder().build() }

    fun createItunesApi(): ItunesApi {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(ItunesApi::class.java)
    }

    fun createRssParser(): RssParser {
        return RssParser(client)
    }
}