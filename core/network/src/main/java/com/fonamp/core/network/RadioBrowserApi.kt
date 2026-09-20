package com.fonamp.core.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Directory surface (radio Req 6 as amended by the name-search delta):
 * country/tag index, stations by country or tag/genre, stations by name,
 * click-count. No tops/random.
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

    @GET("json/stations/byname/{name}")
    suspend fun stationsByName(@Path("name") name: String): List<StationDto>

    @POST("json/url/{uuid}")
    suspend fun click(@Path("uuid") stationUuid: String): ClickResultDto
}
