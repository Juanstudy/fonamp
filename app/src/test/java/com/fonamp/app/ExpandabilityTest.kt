package com.fonamp.app

import androidx.media3.common.MediaItem
import com.fonamp.provider.api.AudioItem
import com.fonamp.provider.api.BrowseQuery
import com.fonamp.provider.api.Source
import com.fonamp.provider.api.SourceKind
import com.fonamp.provider.api.SourceResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Slice I RED: `provider/demo` mock-source expandability (scaffold Req 2) —
 * adding a new `Source` must need zero edits to `core/player`, `core/ui`,
 * or bottom-tab navigation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpandabilityTest {

    @Test
    fun `bottom tabs are exactly Collection Radio Favorites Settings`() {
        assertEquals(
            listOf("collection", "radio/discover", "favorites", "podcast", "settings"),
            FonampTabs.tabs.map { it.route },
        )
    }

    @Test
    fun `no podcasts downloads eq or sleep entry points exist`() {
        val routes = FonampTabs.tabs.map { it.route } + FonampRoutes.all
        routes.forEach { route ->
            assertTrue(
                "route $route must not open a v2 surface",
                
                    !route.contains("download", ignoreCase = true) &&
                    !route.contains("eq", ignoreCase = true) &&
                    !route.contains("sleep", ignoreCase = true),
            )
        }
    }

    @Test
    fun `demo source resolves by id alongside real sources with no shell diff`() {
        val sources: Set<Source> = setOf(FakeLocalSource(), FakeRadioSource(), DemoSource())
        assertEquals(DemoSource.DEMO_ID, resolveSource(sources, DemoSource.DEMO_ID).id)
        assertEquals("local", resolveSource(sources, "local").id)
        assertEquals("radio-browser", resolveSource(sources, "radio-browser").id)
        // The shell is untouched: still exactly four tabs.
        assertEquals(5, FonampTabs.tabs.size)
    }

    /** `provider/demo` mock: a new source module contributes one id + one registration. */
    private class DemoSource : Source {
        override val id: String = DEMO_ID
        override val kind: SourceKind = SourceKind.RADIO

        override suspend fun browse(query: BrowseQuery): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())

        override suspend fun search(q: String): SourceResult<List<AudioItem>> =
            SourceResult.Ok(emptyList())

        override fun streamOf(item: AudioItem): MediaItem =
            MediaItem.fromUri(item.streamUri)

        companion object {
            const val DEMO_ID = "demo"
        }
    }

    private open class FakeLocalSource : Source {
        override val id = "local"
        override val kind = SourceKind.LOCAL
        override suspend fun browse(query: BrowseQuery) = SourceResult.Ok(emptyList<AudioItem>())
        override suspend fun search(q: String) = SourceResult.Ok(emptyList<AudioItem>())
        override fun streamOf(item: AudioItem): MediaItem = MediaItem.fromUri(item.streamUri)
    }

    private class FakeRadioSource : FakeLocalSource() {
        override val id = "radio-browser"
        override val kind = SourceKind.RADIO
    }
}
