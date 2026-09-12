package com.fonamp.core.player

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice G RED: source-blind `MediaItem` contract tests for `core/player`.
 *
 * `core/player` imports nothing from any provider module (design §1, §3):
 * it sees only `MediaItem`. These tests pin the shared `streamOf` shape from the
 * consumer side — local `mediaId=local:<id>` + `is_live=false`, radio
 * `mediaId=radio:<uuid>` + `is_live=true` — so drift in either module fails
 * loudly here. Items are built exactly the way `provider/api` mappers emit
 * them (extras inside `mediaMetadata.extras`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaItemMapperTest {

    private fun localShaped(id: String, artworkUri: String? = null): MediaItem {
        val extras = Bundle().apply {
            putString("source_id", "local")
            putString("stable_id", id)
            putBoolean("is_live", false)
        }
        return MediaItem.Builder()
            .setMediaId("local:$id")
            .setUri("content://media/external/audio/media/$id")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Song")
                    .setArtist("Artist")
                    .setAlbumTitle("Album")
                    .setArtworkUri(artworkUri?.let { android.net.Uri.parse(it) })
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    private fun radioShaped(uuid: String): MediaItem {
        val extras = Bundle().apply {
            putString("source_id", "radio-browser")
            putString("stable_id", uuid)
            putBoolean("is_live", true)
            putString("station_uuid", uuid)
            putString("country", "Germany")
        }
        return MediaItem.Builder()
            .setMediaId("radio:$uuid")
            .setUri("http://stream.example/$uuid")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Station")
                    .setArtist("Germany, pop")
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    @Test
    fun `extras keys match the provider contract literals`() {
        assertEquals("source_id", PlayerExtras.KEY_SOURCE_ID)
        assertEquals("stable_id", PlayerExtras.KEY_STABLE_ID)
        assertEquals("is_live", PlayerExtras.KEY_IS_LIVE)
        assertEquals("station_uuid", PlayerExtras.KEY_STATION_UUID)
        assertEquals("country", PlayerExtras.KEY_COUNTRY)
    }

    @Test
    fun `local shaped item reads non-live with stable id`() {
        val media = localShaped("42")
        assertEquals("local:42", media.mediaId)
        assertFalse(media.isLive())
        assertEquals("local", media.sourceId())
        assertEquals("42", media.stableId())
        assertNull(media.stationUuid())
    }

    @Test
    fun `radio shaped item reads live with station uuid`() {
        val media = radioShaped("uuid-1")
        assertEquals("radio:uuid-1", media.mediaId)
        assertTrue(media.isLive())
        assertEquals("radio-browser", media.sourceId())
        assertEquals("uuid-1", media.stableId())
        assertEquals("uuid-1", media.stationUuid())
        assertEquals("Germany", media.country())
    }

    @Test
    fun `bare item without extras defaults to non-live`() {
        val media = MediaItem.fromUri("content://media/external/audio/media/7")
        assertFalse(media.isLive())
        assertNull(media.sourceId())
        assertNull(media.stableId())
        assertNull(media.stationUuid())
    }

    @Test
    fun `local shaped item carries artwork uri when album art exists`() {
        val art = "content://media/external/audio/albumart/101"
        val media = localShaped("11", artworkUri = art)
        assertEquals(art, media.mediaMetadata.artworkUri.toString())
    }

    @Test
    fun `local shaped item has null artwork when album art is absent`() {
        val media = localShaped("7")
        assertNull(media.mediaMetadata.artworkUri)
    }
}
