package com.fonamp.provider.radio

import com.fonamp.core.network.DirectoryCache
import com.fonamp.core.network.RadioBrowserClient
import com.fonamp.provider.api.AudioItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Change `curated-radio-stations` (Req 7–8): preset mapping, URL filtering,
 * and click-count never blocking curated playback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CuratedStationsTest {

    @Test
    fun `valid preset maps with identity preserved`() {
        val items = CuratedStations.browseCurated()
        assertEquals(3, items.size)
        val groove = items.first { it.title.startsWith("SomaFM") }
        assertEquals("https://ice6.somafm.com/groovesalad-128-mp3", groove.streamUri)
        assertEquals("radio-browser", groove.sourceId)
        assertEquals("960cf833-0601-11e8-ae97-52543be04c81", groove.stationUuid)
        assertEquals("960cf833-0601-11e8-ae97-52543be04c81", groove.stableId)
    }

    @Test
    fun `blank streamUrl is filtered`() {
        val presets = listOf(
            CuratedStation(name = "Empty", streamUrl = "   ", stationUuid = "uuid-empty"),
            CuratedStation(
                name = "Valid",
                streamUrl = "https://example.com/stream.mp3",
                stationUuid = "uuid-valid",
            ),
        )
        val items = CuratedStations.browseCurated(presets)
        assertEquals(listOf("Valid"), items.map { it.title })
    }

    @Test
    fun `malformed streamUrl is filtered without crash`() {
        val presets = listOf(
            CuratedStation(name = "Garbage", streamUrl = "not a url", stationUuid = "uuid-1"),
            CuratedStation(name = "Ftp", streamUrl = "ftp://example.com/stream.mp3", stationUuid = "uuid-2"),
            CuratedStation(
                name = "Valid",
                streamUrl = "https://example.com/stream.mp3",
                stationUuid = "uuid-3",
            ),
        )
        val items = CuratedStations.browseCurated(presets)
        assertEquals(listOf("Valid"), items.map { it.title })
    }

    @Test
    fun `preset without uuid falls back to curated slug stableId`() {
        val items = CuratedStations.browseCurated(
            listOf(
                CuratedStation(
                    name = "Night Wave",
                    streamUrl = "https://example.com/night.mp3",
                ),
            ),
        )
        assertEquals("curated:night-wave", items.single().stableId)
    }

    @Test
    fun `click-count failure never blocks curated playback`() = runTest {
        val deadClient = RadioBrowserClient(mirrors = listOf("http://127.0.0.1:1/"))
        val source = RadioBrowserSource(
            client = deadClient,
            cache = DirectoryCache(dir = null, nowMs = { 0L }),
            clickScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        )
        val curated: AudioItem = CuratedStations.browseCurated().first()
        val media = source.streamOf(curated)
        assertEquals("radio:${curated.stationUuid}", media.mediaId)
        assertEquals(curated.streamUri, media.localConfiguration?.uri.toString())
        assertTrue(media.localConfiguration != null)
    }
}
