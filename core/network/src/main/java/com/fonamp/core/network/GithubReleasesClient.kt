package com.fonamp.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Latest release with its installable `.apk` asset, if any. */
data class GithubRelease(
    val tag: String,
    val name: String?,
    val notes: String?,
    val apkUrl: String?,
    val apkSize: Long?,
)

/**
 * Minimal GitHub Releases client for the update check (single base URL, no
 * mirrors — api.github.com already balances). Transport failures surface as
 * typed [NetworkError] via the shared mapper; callers decide the UI.
 *
 * A release without an `.apk` asset still returns, with null [GithubRelease.apkUrl]:
 * the update UI treats that as "nothing installable", never as an error.
 */
class GithubReleasesClient(
    baseUrl: String = API_BASE_URL,
    okHttp: OkHttpClient? = null,
    connectTimeoutMs: Long = RadioBrowserClient.CONNECT_TIMEOUT_MS,
    readTimeoutMs: Long = RadioBrowserClient.READ_TIMEOUT_MS,
    callTimeoutMs: Long = RadioBrowserClient.CALL_TIMEOUT_MS,
    private val json: Json = RadioBrowserClient.DirectoryJson,
) {
    private val service: GithubReleasesApi = Retrofit.Builder()
        .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
        .client(
            okHttp ?: OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
                .build(),
        )
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GithubReleasesApi::class.java)

    suspend fun fetchLatest(owner: String, repo: String): GithubRelease {
        try {
            return service.latestRelease(owner, repo).toDomain()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            throw RadioBrowserClient.mapToNetworkError(e)
        }
    }

    companion object {
        const val API_BASE_URL = "https://api.github.com/"
        const val REPO_OWNER = "Juanstudy"
        const val REPO_NAME = "fonamp"

        private fun GithubReleaseDto.toDomain(): GithubRelease {
            val apk = assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true) &&
                    it.downloadUrl.isNotBlank()
            }
            return GithubRelease(
                tag = tag,
                name = name?.takeIf { it.isNotBlank() },
                notes = body?.takeIf { it.isNotBlank() },
                apkUrl = apk?.downloadUrl,
                apkSize = apk?.size,
            )
        }
    }
}

/**
 * True when [latestTag] is a newer version than [current]. Both tolerate a
 * leading `v` and trailing qualifiers (`1.0-beta` < `1.0`). Anything
 * unparseable on either side returns false — an update prompt must never
 * fire on a version string we do not understand.
 */
fun isNewerVersion(latestTag: String, current: String): Boolean {
    val latest = parseVersion(latestTag) ?: return false
    val base = parseVersion(current) ?: return false
    for (i in 0 until maxOf(latest.numbers.size, base.numbers.size)) {
        val l = latest.numbers.getOrElse(i) { 0 }
        val b = base.numbers.getOrElse(i) { 0 }
        if (l != b) return l > b
    }
    // Same numbers: a qualified pre-release is older than the clean release.
    return base.qualifier != null && latest.qualifier == null
}

private data class ParsedVersion(val numbers: List<Int>, val qualifier: String?)

private fun parseVersion(raw: String): ParsedVersion? {
    val cleaned = raw.trim().removePrefix("v").removePrefix("V")
    if (cleaned.isEmpty()) return null
    val (numeric, qualifier) = cleaned.split('-', limit = 2).let {
        it[0] to it.getOrNull(1)?.takeIf { q -> q.isNotBlank() }
    }
    val numbers = numeric.split('.')
    if (numbers.isEmpty() || numbers.any { it.isBlank() || it.any { c -> !c.isDigit() } }) return null
    return ParsedVersion(numbers.map { it.toIntOrNull() ?: return null }, qualifier)
}
