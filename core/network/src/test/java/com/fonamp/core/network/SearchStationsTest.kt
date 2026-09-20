package com.fonamp.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T1 (name-search delta): `searchStations` — blank short-circuits with zero
 * network, rows map with invalid ones dropped, 5xx rotates mirrors.
 * No real network — every seam is MockWebServer.
 */
class SearchStationsTest {

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

    private fun clientOf(vararg mirrors: MockWebServer): RadioBrowserClient =
        RadioBrowserClient(mirrors = mirrors.map { it.url("/").toString() })

    @Test
    fun `blank name returns empty with zero network`() = runTest {
        val s = server()
        val result = clientOf(s).searchStations("   ")
        assertTrue(result.isEmpty())
        assertEquals(0, s.requestCount)
    }

    @Test
    fun `byname rows map and invalid rows drop out`() = runTest {
        val s = server()
        s.enqueue(
            json(
                """[
                  |{"stationuuid":"u1","name":"Lofi Girl","url":"http://x/s","url_resolved":"https://x/r","bitrate":128,"codec":"MP3","country":"Nowhere","tags":"lofi, chill"},
                  |{"stationuuid":"","name":"No Uuid","url":"http://x/s","url_resolved":"http://x/r"},
                  |{"stationuuid":"u3","name":"No Url","url":"","url_resolved":""}
                  |]""".trimMargin(),
            ),
        )
        val result = clientOf(s).searchStations("lofi")
        assertEquals(1, result.size)
        assertEquals("u1", result[0].stationuuid)
        assertEquals("https://x/r", result[0].resolvedStreamUrl)
        assertEquals(listOf("lofi", "chill"), result[0].tagList)
        val path = s.takeRequest().path.orEmpty()
        assertTrue(path.startsWith("/json/stations/byname/"))
    }

    @Test
    fun `5xx on first mirror rotates to the next one`() = runTest {
        val failing = server()
        val healthy = server()
        failing.enqueue(MockResponse().setResponseCode(500))
        healthy.enqueue(json("""[{"stationuuid":"u9","name":"Chill FM","url_resolved":"https://y/r"}]"""))

        val result = clientOf(failing, healthy).searchStations("chill")

        assertEquals(listOf("u9"), result.map { it.stationuuid })
        assertEquals(1, healthy.requestCount)
    }

    @Test
    fun `results are capped to the limit`() = runTest {
        val s = server()
        val rows = (1..60).joinToString(",") { i ->
            """{"stationuuid":"u$i","name":"Station $i","url_resolved":"https://x/$i"}"""
        }
        s.enqueue(json("[$rows]"))
        val result = clientOf(s).searchStations("station")
        assertEquals(RadioBrowserClient.SEARCH_LIMIT, result.size)
    }
}
