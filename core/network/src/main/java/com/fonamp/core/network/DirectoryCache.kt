package com.fonamp.core.network

import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Directory cache store (design §4): index + per-selection station lists with
 * `fetchedAt`. TTL is 24h. Stale entries stay readable — callers serve stale
 * on failed refresh and show the Offline card (radio Req 5).
 *
 * Memory-first with JSON write-through to [dir] when provided (null =
 * memory-only, as used by unit tests). Settings "clear cache" wipes the store
 * and reports freed bytes; it never touches the Room DB.
 */
class DirectoryCache(
    private val dir: File? = null,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val json: Json = RadioBrowserClient.DirectoryJson,
) {
    @Serializable
    data class CachedIndex(val fetchedAt: Long, val index: DirectoryIndex)

    @Serializable
    data class CachedStations(val fetchedAt: Long, val stations: List<StationDto>)

    data class CacheStats(val entryCount: Int, val sizeBytes: Long)

    private val memoryIndex = mutableMapOf<String, CachedIndex>()
    private val memoryStations = mutableMapOf<String, CachedStations>()

    fun isStale(fetchedAt: Long): Boolean = nowMs() - fetchedAt > TTL_MS

    // --- index ---

    fun getIndex(): CachedIndex? {
        memoryIndex[INDEX_KEY]?.let { return it }
        val disk = readFile(indexFile(), CachedIndex.serializer()) ?: return null
        memoryIndex[INDEX_KEY] = disk
        return disk
    }

    fun putIndex(index: DirectoryIndex): CachedIndex {
        val entry = CachedIndex(fetchedAt = nowMs(), index = index)
        memoryIndex[INDEX_KEY] = entry
        writeFile(indexFile(), CachedIndex.serializer(), entry)
        return entry
    }

    // --- station lists ---

    fun getStations(key: String): CachedStations? {
        memoryStations[key]?.let { return it }
        val disk = readFile(stationsFile(key), CachedStations.serializer()) ?: return null
        memoryStations[key] = disk
        return disk
    }

    fun putStations(key: String, stations: List<StationDto>): CachedStations {
        val entry = CachedStations(fetchedAt = nowMs(), stations = stations)
        memoryStations[key] = entry
        writeFile(stationsFile(key), CachedStations.serializer(), entry)
        return entry
    }

    /** Every cached station row, across selections — the search corpus. */
    fun allStations(): List<StationDto> {
        val rows = mutableListOf<StationDto>()
        rows += memoryStations.values.flatMap { it.stations }
        if (dir != null) {
            dir.listFiles { f -> f.name.startsWith(STATIONS_PREFIX) }
                ?.forEach { file ->
                    runCatching {
                        readFile(file, CachedStations.serializer())?.stations
                    }.getOrNull()?.let { rows += it }
                }
        }
        // Dedupe corpus rows by station uuid, preserving first-seen order.
        val seen = LinkedHashSet<String>()
        return rows.filter { seen.add(it.stationuuid) }
    }

    // --- stats / clear ---

    fun stats(): CacheStats {
        val diskBytes = dir?.listFiles()?.sumOf { it.length() } ?: 0L
        return CacheStats(
            entryCount = memoryIndex.size + memoryStations.size,
            sizeBytes = diskBytes,
        )
    }

    /** Wipes memory + disk; returns freed disk bytes. */
    fun clear(): Long {
        memoryIndex.clear()
        memoryStations.clear()
        val files = dir?.listFiles() ?: return 0L
        val freed = files.sumOf { it.length() }
        files.forEach { runCatching { it.delete() } }
        return freed
    }

    // --- files ---

    private fun indexFile(): File? = dir?.let { File(it, "$INDEX_KEY.json") }

    private fun stationsFile(key: String): File? =
        dir?.let { File(it, "$STATIONS_PREFIX${hash(key)}.json") }

    private fun <T> readFile(file: File?, serializer: kotlinx.serialization.KSerializer<T>): T? {
        if (file == null || !file.exists()) return null
        return runCatching { json.decodeFromString(serializer, file.readText()) }.getOrNull()
    }

    private fun <T> writeFile(file: File?, serializer: kotlinx.serialization.KSerializer<T>, value: T) {
        if (file == null) return
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(serializer, value))
        }
    }

    companion object {
        const val TTL_MS = 24L * 60 * 60 * 1000
        private const val INDEX_KEY = "index"
        private const val STATIONS_PREFIX = "stations_"

        /** Stable per-selection cache key for a station query. */
        fun stationKey(query: StationQuery): String =
            listOfNotNull(
                query.country?.let { "country=$it" },
                query.genre?.let { "genre=$it" },
                query.tag?.let { "tag=$it" },
            ).joinToString("&")

        /** Filename segment is a stable sha256 hex of the raw key (never reversible). */
        private fun hash(raw: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
