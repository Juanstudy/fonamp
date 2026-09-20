package com.fonamp.provider.radio

import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.RadioBrowserClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

/**
 * Slice E RED: RadioBrowserSource — cached index browse, station-list browse,
 * client-side search with zero network, click-count never fails play, typed errors.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RadioBrowserSourceTest {

    private val servers = mutableListOf<MockWebServer>()

    @After
    fun tearDown() {
        servers.forEach { runCatching { it.shutdown() } }
    }

    private fun server(): MockWebServer = MockWebServer().also { servers.add(it) }

    private fun json(body: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .addHeader("Content-Type", "application/json")
        .setBody(body)

    private fun CoroutineScope.sourceOf(
        mirror: MockWebServer,
        nowMs: () -> Long = { 0L },
    ): RadioBrowserSource {
        val client = RadioBrowserClient(mirrors = listOf(mirror.url("/").toString()))
        return RadioBrowserSource(
            client = client,
            cache = DirectoryCache(dir = null, nowMs = nowMs),
            clickScope = this,
        )
    }

    @Test
    fun `source identity is radio-browser and RADIO kind`() = runTest {
        val s = server()
        val source = sourceOf(s)
        assertEquals("radio-browser", source.id)
        assertEquals(SourceKind.RADIO, source.kind)
    }

    @Test
    fun `empty browse returns cached index, filtered browse returns stations`() = runTest {
        val s = server()
        s.enqueue(json("""[{"name":"Germany","stationcount":1200}]"""))
        s.enqueue(json("""[{"name":"pop","stationcount":500}]"""))
        s.enqueue(
            json(
                """[{"stationuuid":"u1","name":"Pop FM","url":"http://x/s","url_resolved":"http://x/r","bitrate":128,"codec":"MP3","country":"Germany","tags":"pop"}]""",
            ),
        )
        val source = sourceOf(s)

        val index = source.browse(BrowseQuery()) as SourceResult.Ok
        assertTrue(index.v.any { it.title == "Germany" })
        assertTrue(index.v.any { it.title == "pop" })

        val stations = source.browse(BrowseQuery(country = "Germany")) as SourceResult.Ok
        val station = stations.v.single()
        assertEquals("Pop FM", station.title)
        assertEquals("http://x/r", station.streamUri)
        assertEquals(128, station.bitrate)
        assertEquals("MP3", station.codec)
        assertEquals("u1", station.stationUuid)
    }

    @Test
    fun `search queries the directory and maps rows`() = runTest {
        val s = server()
        s.enqueue(
            json(
                """[
                  {"stationuuid":"u1","name":"Lofi Girl","url":"http://x/1","url_resolved":"https://x/r","bitrate":128,"codec":"MP3","country":"Nowhere","tags":"lofi"},
                  {"stationuuid":"","name":"No Uuid","url":"http://x/2"},
                  {"stationuuid":"u3","name":"No Url","url":""}
                ]""",
            ),
        )
        val source = sourceOf(s)

        val found = source.search("lofi") as SourceResult.Ok
        assertEquals(listOf("Lofi Girl"), found.v.map { it.title })
        assertEquals("u1", found.v.single().stationUuid)
        assertTrue(s.takeRequest().path.orEmpty().startsWith("/json/stations/byname/"))
    }

    @Test
    fun `blank search returns empty with zero network`() = runTest {
        val s = server()
        val source = sourceOf(s)
        val found = source.search("   ") as SourceResult.Ok
        assertTrue(found.v.isEmpty())
        assertEquals(0, s.requestCount)
    }

    @Test
    fun `search failure returns typed Offline`() = runTest {
        val dead = server()
        dead.start()
        val deadUrl = dead.url("/").toString()
        dead.shutdown()
        servers.remove(dead)

        // Search must surface Fail, never throw.
        val deadSource = RadioBrowserSource(
            client = RadioBrowserClient(mirrors = listOf(deadUrl)),
            cache = DirectoryCache(dir = null, nowMs = { 0L }),
            clickScope = this,
        )
        val result = deadSource.search("lofi")
        assertTrue("expected Fail(Offline), was=$result", result is SourceResult.Fail)
        assertTrue((result as SourceResult.Fail).e is SourceError.Offline)
    }

    @Test
    fun `failed refresh keeps stale cache and returns Offline`() = runTest {
        var now = 0L
        val s = server()
        s.enqueue(
            json("""[{"stationuuid":"u1","name":"Pop FM","url":"http://x/1"}]"""),
        )
        val source = sourceOf(s, nowMs = { now })
        assertTrue(source.browse(BrowseQuery(tag = "pop")) is SourceResult.Ok)

        now = DirectoryCache.TTL_MS + 1
        s.shutdown()
        servers.remove(s)

        val result = source.browse(BrowseQuery(tag = "pop"))
        assertTrue("expected Fail(Offline), was=$result", result is SourceResult.Fail)
        assertTrue((result as SourceResult.Fail).e is SourceError.Offline)
    }

    @Test
    fun `click-count failure never fails play`() = runTest {
        val s = server()
        s.enqueue(
            json("""[{"stationuuid":"u1","name":"Pop FM","url":"http://x/1"}]"""),
        )
        s.enqueue(MockResponse().setResponseCode(500).setBody("click boom"))
        val source = RadioBrowserSource(
            client = RadioBrowserClient(mirrors = listOf(s.url("/").toString())),
            cache = DirectoryCache(dir = null, nowMs = { 0L }),
            clickScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )

        val stations = source.browse(BrowseQuery(tag = "pop")) as SourceResult.Ok
        val item = source.streamOf(stations.v.single())

        assertEquals("radio:u1", item.mediaId)
        assertEquals("http://x/1", item.localConfiguration?.uri.toString())
        // The fire-and-forget click ran (and its 500 was swallowed, not thrown).
        assertNotNull(s.takeRequest(2, TimeUnit.SECONDS))
    }

    @Test
    fun `all mirrors down returns typed Offline without throwing`() = runTest {
        val dead = server()
        dead.start()
        val deadUrl = dead.url("/").toString()
        dead.shutdown()
        servers.remove(dead)

        val source = RadioBrowserSource(
            client = RadioBrowserClient(mirrors = listOf(deadUrl)),
            cache = DirectoryCache(dir = null, nowMs = { 0L }),
            clickScope = this,
        )
        val result = source.browse(BrowseQuery())
        assertTrue("expected Fail(Offline), was=$result", result is SourceResult.Fail)
        assertTrue((result as SourceResult.Fail).e is SourceError.Offline)
    }

    private fun assertNotNull(value: Any?) {
        assertTrue("expected non-null", value != null)
    }
}
