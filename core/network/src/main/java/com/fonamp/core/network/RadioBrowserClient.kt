package com.fonamp.core.network

import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/** Station-list selection; nulls = unset. Genre resolves via the tag endpoint. */
data class StationQuery(
    val country: String? = null,
    val genre: String? = null,
    val tag: String? = null,
)

@Serializable
data class DirectoryEntry(val name: String, val stationCount: Int? = null)

@Serializable
data class DirectoryIndex(
    val countries: List<DirectoryEntry> = emptyList(),
    val tags: List<DirectoryEntry> = emptyList(),
)

/**
 * Radio-browser client (design §4): Retrofit + kotlinx.serialization over an
 * OkHttp stack with 10s connect/read and ~12s call timeouts, optional disk
 * cache, ordered mirror fallback, and typed errors only.
 *
 * Timeouts are constructor params (defaults are the spec values) so tests can
 * use short budgets without waiting out the real 10s.
 */
class RadioBrowserClient(
    mirrors: List<String> = MirrorPolicy.defaults(),
    cacheDir: File? = null,
    httpCacheBytes: Long = 10L * 1024 * 1024,
    connectTimeoutMs: Long = CONNECT_TIMEOUT_MS,
    readTimeoutMs: Long = READ_TIMEOUT_MS,
    callTimeoutMs: Long = CALL_TIMEOUT_MS,
    private val json: Json = DirectoryJson,
) {
    val mirrorPolicy = MirrorPolicy(mirrors)

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(callTimeoutMs, TimeUnit.MILLISECONDS)
        .apply { if (cacheDir != null) cache(Cache(cacheDir, httpCacheBytes)) }
        .build()

    private val services = mutableMapOf<String, RadioBrowserApi>()

    @Synchronized
    private fun serviceFor(mirror: String): RadioBrowserApi =
        services.getOrPut(mirror) {
            val base = if (mirror.endsWith("/")) mirror else "$mirror/"
            Retrofit.Builder()
                .baseUrl(base)
                .client(okHttp)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(RadioBrowserApi::class.java)
        }

    /** Countries + tags index from the first healthy mirror. */
    suspend fun fetchIndex(): DirectoryIndex = withMirrorFallback { api ->
        DirectoryIndex(
            countries = api.getCountries()
                .filter { it.name.isNotBlank() }
                .map { DirectoryEntry(it.name, it.stationcount) },
            tags = api.getTags()
                .filter { it.name.isNotBlank() }
                .map { DirectoryEntry(it.name, it.stationcount) },
        )
    }

    /** Station list for one selection; unplayable rows (blank uuid/url) drop out. */
    suspend fun fetchStations(query: StationQuery): List<StationDto> = withMirrorFallback { api ->
        val rows = when {
            query.country != null -> api.stationsByCountry(query.country)
            query.genre != null -> api.stationsByTag(query.genre)
            query.tag != null -> api.stationsByTag(query.tag)
            else -> emptyList()
        }
        rows.filter { it.stationuuid.isNotBlank() && it.resolvedStreamUrl.isNotBlank() }
    }

    /** Click-count report; result ignored — callers fire-and-forget this. */
    suspend fun click(stationUuid: String) {
        withMirrorFallback { api -> api.click(stationUuid) }
    }

    private suspend fun <T> withMirrorFallback(call: suspend (RadioBrowserApi) -> T): T {
        var last: NetworkError = NetworkError.Offline
        for (mirror in mirrorPolicy.orderedMirrors()) {
            try {
                val result = call(serviceFor(mirror))
                mirrorPolicy.recordServing(mirror)
                return result
            } catch (e: CancellationException) {
                // Structured-concurrency contract: never swallow caller
                // cancellation — rethrow immediately, never record or rotate.
                // Retryable Timeout comes only from transport-mapped errors
                // via mapToNetworkError in the Throwable branch below.
                throw e
            } catch (e: Throwable) {
                val mapped = mapToNetworkError(e)
                last = mapped
                mirrorPolicy.recordFailure(mirror, mapped)
                if (!shouldRotate(mapped)) throw mapped
            }
        }
        mirrorPolicy.reset()
        throw last
    }

    private fun shouldRotate(error: NetworkError): Boolean = when (error) {
        // Offline/Timeout/5xx rotate to the next mirror (design §4).
        NetworkError.Offline, NetworkError.Timeout -> true
        is NetworkError.Server -> (error.code ?: 500) >= 500
        // 4xx and unparseable bodies fail fast — every mirror would agree.
        is NetworkError.Unknown -> false
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val READ_TIMEOUT_MS = 10_000L
        const val CALL_TIMEOUT_MS = 12_000L

        val DirectoryJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }

        fun mapToNetworkError(e: Throwable): NetworkError = when (e) {
            is NetworkError -> e
            is HttpException -> NetworkError.Server(e.code())
            is SocketTimeoutException,
            is TimeoutCancellationException,
            is InterruptedIOException,
            -> NetworkError.Timeout
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException,
            is SSLException,
            -> NetworkError.Offline
            is java.io.IOException -> NetworkError.Offline
            else -> NetworkError.Unknown(e)
        }
    }
}
