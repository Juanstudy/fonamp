package com.fonamp.provider.local

import android.content.ContentUris
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.SourceError
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice F RED: LocalSource over MediaStore — songs grouping, metadata honesty,
 * BrowseQuery filters ignored, zero network, typed failures, MediaItem mapping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalSourceTest {

    private class FakeReader(
        private val rows: List<Map<String, Any?>>,
        private val fail: Throwable? = null,
    ) : MediaStoreReader {
        var queryCount = 0
        var lastUri: Uri? = null
        var lastSelection: String? = null
        private val columns = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
        )

        override fun query(
            uri: Uri,
            projection: Array<String>?,
            selection: String?,
            selectionArgs: Array<String>?,
            sortOrder: String?,
        ): Cursor? {
            queryCount++
            lastUri = uri
            lastSelection = selection
            fail?.let { throw it }
            // Fresh cursor per call, like a real ContentResolver.
            return MatrixCursor(columns, rows.size).apply {
                rows.forEach { row -> addRow(columns.map { row[it] }) }
            }
        }
    }

    private fun sourceOf(reader: FakeReader) = LocalSource(
        reader = reader,
        io = UnconfinedTestDispatcher(),
    )

    private val fullRows = listOf(
        mapOf(
            MediaStore.Audio.Media._ID to 11L,
            MediaStore.Audio.Media.TITLE to "Blue Line",
            MediaStore.Audio.Media.ARTIST to "Ada",
            MediaStore.Audio.Media.ALBUM to "Metro",
            MediaStore.Audio.Media.DURATION to 183_000L,
        ),
        mapOf(
            MediaStore.Audio.Media._ID to 12L,
            MediaStore.Audio.Media.TITLE to "Night Static",
            MediaStore.Audio.Media.ARTIST to "Ada",
            MediaStore.Audio.Media.ALBUM to "Metro",
            MediaStore.Audio.Media.DURATION to 201_000L,
        ),
    )

    @Test
    fun `identity is local plus LOCAL kind`() {
        val source = sourceOf(FakeReader(emptyList()))
        assertEquals("local", source.id)
        assertEquals(SourceKind.LOCAL, source.kind)
    }

    @Test
    fun `browse returns songs with honest metadata and playable content URIs`() = runTest {
        val source = sourceOf(FakeReader(fullRows))
        val result = source.browse(BrowseQuery())
        assertTrue(result is SourceResult.Ok)
        val items = (result as SourceResult.Ok).v
        assertEquals(2, items.size)
        val first = items[0]
        assertEquals("local", first.sourceId)
        assertEquals("11", first.stableId)
        assertEquals("Blue Line", first.title)
        assertEquals("Ada", first.subtitle)
        assertEquals("Metro", first.album)
        assertEquals(183_000L, first.durationMs)
        assertEquals(
            ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 11L
            ).toString(),
            first.streamUri,
        )
        // Radio-only fields stay absent (never fabricated).
        assertNull(first.bitrate)
        assertNull(first.codec)
    }

    @Test
    fun `absent metadata is omitted not fabricated`() = runTest {
        val rows = listOf(
            mapOf(
                MediaStore.Audio.Media._ID to 7L,
                MediaStore.Audio.Media.TITLE to "Untagged",
                MediaStore.Audio.Media.ARTIST to null,
                MediaStore.Audio.Media.ALBUM to null,
                MediaStore.Audio.Media.DURATION to null,
            )
        )
        val source = sourceOf(FakeReader(rows))
        val items = (source.browse(BrowseQuery()) as SourceResult.Ok).v
        assertEquals(1, items.size)
        assertNull(items[0].subtitle)
        assertNull(items[0].album)
        assertNull(items[0].durationMs)
        assertTrue(items[0].streamUri.startsWith("content://"))
    }

    @Test
    fun `browse ignores BrowseQuery filters and reads in one pass`() = runTest {
        val reader = FakeReader(fullRows)
        val source = sourceOf(reader)
        val result = source.browse(BrowseQuery(country = "X", genre = "Y", tag = "Z"))
        val items = (result as SourceResult.Ok).v
        assertEquals(2, items.size)
        assertEquals(1, reader.queryCount)
        assertEquals(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, reader.lastUri)
        assertTrue(reader.lastSelection!!.contains(MediaStore.Audio.Media.IS_MUSIC))
    }

    @Test
    fun `browse over empty store is Ok empty`() = runTest {
        val source = sourceOf(FakeReader(emptyList()))
        val result = source.browse(BrowseQuery())
        assertTrue(result is SourceResult.Ok)
        assertTrue((result as SourceResult.Ok).v.isEmpty())
    }

    @Test
    fun `search filters title artist album locally with zero extra passes beyond one read`() =
        runTest {
            val reader = FakeReader(fullRows)
            val source = sourceOf(reader)
            val hits = (source.search("static") as SourceResult.Ok).v
            assertEquals(listOf("Night Static"), hits.map { it.title })
            val artistHits = (source.search("ADA") as SourceResult.Ok).v
            assertEquals(2, artistHits.size)
            val misses = (source.search("zzz-no-match") as SourceResult.Ok).v
            assertTrue(misses.isEmpty())
        }

    @Test
    fun `security failure surfaces as typed Fail never a throw`() = runTest {
        val source = sourceOf(FakeReader(emptyList(), fail = SecurityException("denied")))
        val result = source.browse(BrowseQuery())
        assertTrue(result is SourceResult.Fail)
        assertTrue((result as SourceResult.Fail).e is SourceError.Unknown)
    }

    @Test
    fun `streamOf maps local mediaId and non-live extras`() = runTest {
        val source = sourceOf(FakeReader(fullRows))
        val item = (source.browse(BrowseQuery()) as SourceResult.Ok).v.first()
        val media = source.streamOf(item)
        assertEquals("local:11", media.mediaId)
        assertEquals(item.streamUri, media.localConfiguration?.uri.toString())
        assertEquals(false, media.mediaMetadata.extras?.getBoolean("is_live"))
    }
}
