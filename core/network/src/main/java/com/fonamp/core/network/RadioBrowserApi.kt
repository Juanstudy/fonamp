package com.fonamp.core.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * v1 directory surface only (radio Req 6): country/tag index, stations
 * by country or tag/genre, click-count. No tops/random/name-search.
 *
 * Genres resolve through the tags endpoint — the directory exposes no
 * separate genre listing, so genre selections query `stations/bytag`.
 */
interface RadioBrowserApi {

    @GET("json/countries")
    suspend fun getCountries(): List<CountryDto>

    @GET("json/tags")
    suspend fun getTags(): List<TagDto>

    @GET("json/stations/bycountry/{country}")
    suspend fun stationsByCountry(@Path("country") country: String): List<StationDto>

    @GET("json/stations/bytag/{tag}")
    suspend fun stationsByTag(@Path("tag") tag: String): List<StationDto>

    @POST("json/url/{uuid}")
    suspend fun click(@Path("uuid") stationUuid: String): ClickResultDto
}
