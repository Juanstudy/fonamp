package com.fonamp.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Slice E RED: mirror fallback, timeout mapping, nullable-tolerant JSON.
 * No real network — every seam is MockWebServer.
 */
class MirrorPolicyTest {

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

    @Test
    fun `default client uses 10s connect-read and 12s call timeouts`() {
        val client = RadioBrowserClient(mirrors = listOf("https://example.invalid/"))
        assertEquals(10_000, client.okHttp.connectTimeoutMillis)
        assertEquals(10_000, client.okHttp.readTimeoutMillis)
        assertEquals(12_000, client.okHttp.callTimeoutMillis)
    }

    @Test
    fun `primary down falls back to secondary and records serving mirror`() = runTest {
        val dead = server()
        dead.start()
        val deadUrl = dead.url("/").toString()
        dead.shutdown()
        servers.remove(dead)

        val secondary = server()
        secondary.enqueue(json("""[{"name":"Germany","stationcount":1200}]"""))
        secondary.enqueue(json("""[{"name":"pop","stationcount":500}]"""))

        val client = RadioBrowserClient(
            mirrors = listOf(deadUrl, secondary.url("/").toString()),
        )
        val index = client.fetchIndex()

        assertEquals(listOf("Germany"), index.countries.map { it.name })
        assertEquals(listOf("pop"), index.tags.map { it.name })
        assertEquals(secondary.url("/").toString(), client.mirrorPolicy.servingMirror)
        assertTrue(
            "diagnostics must record the serving mirror, was=${client.mirrorPolicy.diagnosticsLog}",
            client.mirrorPolicy.diagnosticsLog.any { it.contains(secondary.hostName) },
        )
    }

    @Test
    fun `primary 500 rotates to secondary`() = runTest {
        val flaky = server()
        flaky.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        flaky.enqueue(MockResponse().setResponseCode(500).setBody("boom"))
        val healthy = server()
        healthy.enqueue(json("""[{"name":"France","stationcount":10}]"""))
        healthy.enqueue(json("""[]"""))

        val client = RadioBrowserClient(
            mirrors = listOf(flaky.url("/").toString(), healthy.url("/").toString()),
        )
        val index = client.fetchIndex()

        assertEquals(listOf("France"), index.countries.map { it.name })
        assertEquals(healthy.url("/").toString(), client.mirrorPolicy.servingMirror)
    }

    @Test
    fun `stalling server maps to typed Timeout, never raw`() = runTest {
        val stall = server()
        stall.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                Thread.sleep(3_000)
                return json("[]")
            }
        }
        val client = RadioBrowserClient(
            mirrors = listOf(stall.url("/").toString()),
            connectTimeoutMs = 500,
            readTimeoutMs = 500,
            callTimeoutMs = 1_000,
        )
        try {
            client.fetchIndex()
            fail("expected a typed NetworkError.Timeout")
        } catch (e: NetworkError) {
            assertTrue("expected Timeout, was=$e", e is NetworkError.Timeout)
        }
    }

    @Test
    fun `station json is nullable-tolerant with url_resolved fallback to url`() = runTest {
        val s = server()
        s.enqueue(
            json(
                """[
                  {"stationuuid":"u1","name":"Full","url":"http://x/plain","url_resolved":"http://x/resolved","bitrate":128,"codec":"MP3","country":"Germany","tags":"pop, rock"},
                  {"stationuuid":"u2","name":"Plain","url":"http://y/stream"},
                  {"stationuuid":"u3","name":"Sparse"}
                ]""",
            ),
        )
        val client = RadioBrowserClient(mirrors = listOf(s.url("/").toString()))

        val stations = client.fetchStations(StationQuery(country = "Germany"))

        val full = stations.single { it.stationuuid == "u1" }
        assertEquals("http://x/resolved", full.resolvedStreamUrl)
        assertEquals(128, full.bitrate)
        assertEquals("MP3", full.codec)
        assertEquals(listOf("pop", "rock"), full.tagList)

        val plain = stations.single { it.stationuuid == "u2" }
        assertEquals("http://y/stream", plain.resolvedStreamUrl)
        assertEquals(null, plain.bitrate)
        assertEquals(null, plain.codec)
        assertEquals(null, plain.country)
        assertEquals(emptyList<String>(), plain.tagList)

        assertTrue(
            "row with neither url nor url_resolved is unplayable and drops out",
            stations.none { it.stationuuid == "u3" },
        )
    }

    @Test
    fun `mirror policy is sticky then resets on total failure`() {
        val policy = MirrorPolicy(listOf("https://a.invalid/", "https://b.invalid/"))
        assertEquals(null, policy.servingMirror)
        policy.recordServing("https://b.invalid/")
        assertEquals("https://b.invalid/", policy.servingMirror)
        assertEquals(
            listOf("https://b.invalid/", "https://a.invalid/"),
            policy.orderedMirrors(),
        )
        policy.reset()
        assertEquals(null, policy.servingMirror)
        assertEquals(listOf("https://a.invalid/", "https://b.invalid/"), policy.orderedMirrors())
    }
}
