package com.fonamp.provider.podcast

import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesApi {
    @GET("search?media=podcast")
    suspend fun searchPodcasts(@Query("term") term: String): ItunesSearchResponse
}