package com.fonamp.provider.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice B RED: MediaItem mapping expectations need Robolectric
 * (MediaItem touches android.os.Bundle / android.net.Uri).
 *
 * The fake below implements [Source] with ONLY id/browse/search/streamOf —
 * it must compile without any download stub (source-contract Req 5).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaItemMappingTest {

    private class FakeLocalSource : Source {
        override val id = "local"
        override val kind = SourceKind.LOCAL
        override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())
        override suspend fun search(q: String): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())
        override fun streamOf(item: AudioItem) = localMediaItem(item)
    }

    private class FakeRadioSource : Source {
        override val id = "radio-browser"
        override val kind = SourceKind.RADIO
        override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())
        override suspend fun search(q: String): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())
        override fun streamOf(item: AudioItem) = radioMediaItem(item)
    }

    @Test
    fun `local streamOf maps to non-live MediaItem`() = runTest {
        val source: Source = FakeLocalSource()
        assertEquals("local", source.id)
        assertEquals(SourceKind.LOCAL, source.kind)

        val item = AudioItem(
            sourceId = "local",
            stableId = "42",
            title = "Song",
            subtitle = "Artist",
            album = "Album",
            durationMs = 180_000L,
            streamUri = "content://media/external/audio/media/42",
        )
        val media = source.streamOf(item)
        assertEquals("local:42", media.mediaId)
        assertEquals("content://media/external/audio/media/42", media.localConfiguration?.uri.toString())
        assertEquals("Song", media.mediaMetadata.title.toString())
        val extras = media.mediaMetadata.extras!!
        assertEquals("local", extras.getString(SourceExtras.KEY_SOURCE_ID))
        assertEquals("42", extras.getString(SourceExtras.KEY_STABLE_ID))
        assertFalse(extras.getBoolean(SourceExtras.KEY_IS_LIVE))
    }

    @Test
    fun `radio streamOf maps to live MediaItem with station extras`() = runTest {
        val source: Source = FakeRadioSource()
        assertEquals("radio-browser", source.id)
        assertEquals(SourceKind.RADIO, source.kind)

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
            tags = listOf("pop"),
        )
        val media = source.streamOf(item)
        assertEquals("radio:uuid-1", media.mediaId)
        assertEquals("http://stream.example/x", media.localConfiguration?.uri.toString())
        assertEquals("Station", media.mediaMetadata.title.toString())
        assertTrue(media.mediaMetadata.isPlayable == true)
        assertTrue(media.mediaMetadata.isBrowsable == false)
        val extras = media.mediaMetadata.extras!!
        assertEquals("radio-browser", extras.getString(SourceExtras.KEY_SOURCE_ID))
        assertEquals("uuid-1", extras.getString(SourceExtras.KEY_STABLE_ID))
        assertTrue(extras.getBoolean(SourceExtras.KEY_IS_LIVE))
        assertEquals("uuid-1", extras.getString(SourceExtras.KEY_STATION_UUID))
        assertEquals("Germany", extras.getString(SourceExtras.KEY_COUNTRY))
    }

    @Test
    fun `fakes browse and search without downloads`() = runTest {
        val local: Source = FakeLocalSource()
        assertTrue((local.browse(BrowseQuery()) as SourceResult.Ok).v.isEmpty())
        assertTrue((local.search("x") as SourceResult.Ok).v.isEmpty())
    }
}
