package com.fonamp.provider.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Slice B RED: pure-JVM contract tests (Robolectric-free).
 * MediaItem mapping expectations live in [MediaItemMappingTest].
 */
class SourceContractTest {

    @Test
    fun `SourceExtras keys are stable contract strings`() {
        assertEquals("source_id", SourceExtras.KEY_SOURCE_ID)
        assertEquals("stable_id", SourceExtras.KEY_STABLE_ID)
        assertEquals("is_live", SourceExtras.KEY_IS_LIVE)
        assertEquals("station_uuid", SourceExtras.KEY_STATION_UUID)
        assertEquals("country", SourceExtras.KEY_COUNTRY)
    }

    @Test
    fun `AudioItem carries honest nullable metadata with sane defaults`() {
        val item = AudioItem(
            sourceId = "local",
            stableId = "42",
            title = "Song",
            streamUri = "content://media/external/audio/media/42",
        )
        assertEquals("local", item.sourceId)
        assertEquals("42", item.stableId)
        assertNull(item.subtitle)
        assertNull(item.album)
        assertNull(item.durationMs)
        assertNull(item.bitrate)
        assertNull(item.codec)
        assertNull(item.stationUuid)
        assertNull(item.country)
        assertTrue(item.tags.isEmpty())
    }

    @Test
    fun `AudioItem holds radio-only fields when provided`() {
        val item = AudioItem(
            sourceId = "radio-browser",
            stableId = "uuid-1",
            title = "Station",
            subtitle = "Germany",
            streamUri = "http://stream.example/x",
            bitrate = 128,
            codec = "MP3",
            stationUuid = "uuid-1",
            country = "Germany",
            tags = listOf("pop", "news"),
        )
        assertEquals(128, item.bitrate)
        assertEquals("MP3", item.codec)
        assertEquals("uuid-1", item.stationUuid)
        assertEquals(listOf("pop", "news"), item.tags)
    }

    @Test
    fun `BrowseQuery defaults to unfiltered index request`() {
        val query = BrowseQuery()
        assertNull(query.country)
        assertNull(query.genre)
        assertNull(query.tag)
    }

    @Test
    fun `SourceResult Ok and Fail shapes hold their payloads`() {
        val ok: SourceResult<List<AudioItem>> = SourceResult.Ok(emptyList())
        assertTrue((ok as SourceResult.Ok).v.isEmpty())

        val fail: SourceResult<List<AudioItem>> = SourceResult.Fail(SourceError.Offline)
        val error = (fail as SourceResult.Fail).e
        assertTrue(error is SourceError.Offline)
        assertTrue((SourceError.Timeout as Any) !== (SourceError.Offline as Any))
        assertEquals(500, (SourceError.Server(500) as SourceError.Server).code)
        val cause = RuntimeException("boom")
        assertEquals(cause, (SourceError.Unknown(cause) as SourceError.Unknown).cause)
    }

    @Test
    fun `Source exposes no download surface in v1`() {
        val names = Source::class.java.methods.map { it.name }
        assertTrue(names.none { it.startsWith("download") })
    }
}
