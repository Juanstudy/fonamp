package com.fonamp.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * T1: GitHub Releases client — latest maps tag/notes/apk asset, releases
 * without apk degrade to null url, failures are typed, semver table holds.
 * No real network — every seam is MockWebServer.
 */
class GithubReleasesClientTest {

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

    private fun clientOf(mirror: MockWebServer): GithubReleasesClient =
        GithubReleasesClient(baseUrl = mirror.url("/").toString())

    @Test
    fun `latest maps tag notes and apk asset`() = runTest {
        val s = server()
        s.enqueue(
            json(
                """{"tag_name":"v0.0.10","name":"v0.0.10","body":"## Qué trae\n- X",
                  |"assets":[
                  | {"name":"fonamp-v0.0.10.apk","browser_download_url":"https://cdn.example/f.apk","size":7633777},
                  | {"name":"checksums.txt","browser_download_url":"https://cdn.example/c.txt","size":100}
                  |]}""".trimMargin(),
            ),
        )
        val release = clientOf(s).fetchLatest("Juanstudy", "fonamp")

        assertEquals("v0.0.10", release.tag)
        assertTrue(release.notes.orEmpty().contains("Qué trae"))
        assertEquals("https://cdn.example/f.apk", release.apkUrl)
        assertEquals(7633777L, release.apkSize)
        assertTrue(s.takeRequest().path.orEmpty().endsWith("/repos/Juanstudy/fonamp/releases/latest"))
    }

    @Test
    fun `release without apk degrades to null url`() = runTest {
        val s = server()
        s.enqueue(json("""{"tag_name":"v0.0.10","assets":[]}"""))
        val release = clientOf(s).fetchLatest("Juanstudy", "fonamp")

        assertEquals("v0.0.10", release.tag)
        assertNull(release.apkUrl)
    }

    @Test
    fun `http failure surfaces typed server error`() = runTest {
        val s = server()
        s.enqueue(MockResponse().setResponseCode(404))
        try {
            clientOf(s).fetchLatest("Juanstudy", "fonamp")
            fail("expected NetworkError")
        } catch (e: NetworkError.Server) {
            assertEquals(404, e.code)
        }
    }

    @Test
    fun `dead host surfaces offline without throwing raw`() = runTest {
        val dead = server()
        dead.start()
        val deadUrl = dead.url("/").toString()
        dead.shutdown()
        servers.remove(dead)
        try {
            GithubReleasesClient(baseUrl = deadUrl).fetchLatest("Juanstudy", "fonamp")
            fail("expected NetworkError")
        } catch (e: NetworkError) {
            assertTrue(e is NetworkError.Offline)
        }
    }

    @Test
    fun `semver comparison table`() {
        assertTrue(isNewerVersion("v0.0.10", "0.0.9"))
        assertTrue(isNewerVersion("0.0.10", "v0.0.9"))
        assertTrue(isNewerVersion("v0.1.0", "0.0.9"))
        assertTrue(isNewerVersion("v1.0.0", "0.0.9"))
        assertFalse(isNewerVersion("v0.0.9", "0.0.9"))
        assertFalse(isNewerVersion("0.0.9", "v0.0.9"))
        assertFalse(isNewerVersion("v0.0.8", "0.0.9"))
        assertFalse(isNewerVersion("v0.0.9", "0.0.10"))
        // Qualifiers and junk never prompt.
        assertFalse(isNewerVersion("v0.0.10-beta", "0.0.10"))
        assertTrue(isNewerVersion("v0.0.10", "0.0.10-beta"))
        assertFalse(isNewerVersion("latest", "0.0.9"))
        assertFalse(isNewerVersion("v0.0.10", "dev"))
        assertFalse(isNewerVersion("", "0.0.9"))
    }
}
