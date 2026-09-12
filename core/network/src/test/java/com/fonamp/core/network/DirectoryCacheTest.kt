package com.fonamp.core.network

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Slice E RED: 24h TTL disk cache — serve-stale, refresh-replaces,
 * failed-refresh-keeps-stale, stats + clear.
 */
class DirectoryCacheTest {

    private var now = 0L
    private fun cache(dir: File? = null) = DirectoryCache(dir = dir, nowMs = { now })

    private fun index(vararg countries: String) = DirectoryIndex(
        countries = countries.map { DirectoryEntry(it, 7) },
        tags = listOf(DirectoryEntry("pop", 3)),
    )

    @Test
    fun `fresh index is served without staleness`() {
        val c = cache()
        c.putIndex(index("Germany"))
        now = DirectoryCache.TTL_MS - 1
        val hit = c.getIndex()
        assertNotNull(hit)
        assertEquals(listOf("Germany"), hit!!.index.countries.map { it.name })
        assertFalse(c.isStale(hit.fetchedAt))
    }

    @Test
    fun `stale entry stays readable and refresh replaces it`() {
        val c = cache()
        c.putIndex(index("Germany"))
        now = DirectoryCache.TTL_MS + 1
        val stale = c.getIndex()
        assertNotNull("stale cache must stay readable for failed-refresh fallback", stale)
        assertTrue(c.isStale(stale!!.fetchedAt))

        c.putIndex(index("France"))
        val fresh = c.getIndex()
        assertNotNull(fresh)
        assertEquals(listOf("France"), fresh!!.index.countries.map { it.name })
        assertFalse(c.isStale(fresh.fetchedAt))
    }

    @Test
    fun `station lists are keyed per selection and aggregated`() {
        val c = cache()
        val key = DirectoryCache.stationKey(StationQuery(country = "Germany"))
        c.putStations(key, listOf(StationDto(stationuuid = "u1", name = "S1")))
        assertEquals(listOf("S1"), c.getStations(key)!!.stations.map { it.name })
        assertNull(c.getStations(DirectoryCache.stationKey(StationQuery(tag = "pop"))))
        assertEquals(listOf("S1"), c.allStations().map { it.name })
    }

    @Test
    fun `disk persistence survives reopen, stats and clear report bytes`() {
        val dir = Files.createTempDirectory("dircache").toFile()
        try {
            cache(dir).putIndex(index("Germany"))
            val reopened = cache(dir)
            assertEquals(
                listOf("Germany"),
                reopened.getIndex()!!.index.countries.map { it.name },
            )
            val stats = reopened.stats()
            assertTrue(stats.entryCount >= 1)
            assertTrue(stats.sizeBytes > 0)

            val freed = reopened.clear()
            assertTrue(freed > 0)
            assertNull(reopened.getIndex())
            assertEquals(0, reopened.stats().entryCount)
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `raw keys that sanitize identically do not collide on disk`() {
        val dir = Files.createTempDirectory("dircache-keys").toFile()
        try {
            // Under a lossy filename sanitize ('/' and ':' both became '_') these
            // two raw keys collapsed to one file; hashed filenames keep them apart.
            cache(dir).putStations("country=a/b", listOf(StationDto(stationuuid = "u1", name = "S1")))
            cache(dir).putStations("country=a:b", listOf(StationDto(stationuuid = "u2", name = "S2")))

            val reopened = cache(dir)
            assertEquals(
                listOf("S1"),
                reopened.getStations("country=a/b")!!.stations.map { it.name },
            )
            assertEquals(
                listOf("S2"),
                reopened.getStations("country=a:b")!!.stations.map { it.name },
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `hydrated corpus dedupes rows by station uuid`() {
        val dir = Files.createTempDirectory("dircache-corpus").toFile()
        try {
            val shared = StationDto(stationuuid = "u1", name = "S1")
            cache(dir).putStations("country=Germany", listOf(shared))
            cache(dir).putStations(
                "tag=pop",
                listOf(shared, StationDto(stationuuid = "u2", name = "S2")),
            )

            // Fresh instance: the corpus merges every stations_*.json file from
            // disk, so the row cached under two selections appears exactly once.
            val corpus = cache(dir).allStations()
            assertEquals(listOf("u1", "u2"), corpus.map { it.stationuuid }.sorted())
        } finally {
            dir.deleteRecursively()
        }
    }
}
