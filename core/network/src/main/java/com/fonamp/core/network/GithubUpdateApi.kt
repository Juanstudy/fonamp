package com.fonamp.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

/**
 * GitHub Releases surface for in-app updates (only what the update check
 * needs — never the full release graph). Every field is nullable-tolerant
 * so an API shape change degrades to "no update" instead of crashing.
 */
@Serializable
data class GithubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long? = null,
)

@Serializable
data class GithubReleaseDto(
    @SerialName("tag_name") val tag: String = "",
    val name: String? = null,
    val body: String? = null,
    val assets: List<GithubAssetDto> = emptyList(),
)

interface GithubReleasesApi {

    @Headers("Accept: application/vnd.github+json")
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
    ): GithubReleaseDto
}
