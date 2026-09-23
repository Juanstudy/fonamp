package com.fonamp.provider.podcast

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

data class RssEpisode(
    val title: String,
    val description: String?,
    val streamUrl: String,
    val durationMs: Long?,
    val pubDate: String?,
    val guid: String
)

data class RssFeed(
    val title: String,
    val description: String?,
    val artworkUrl: String?,
    val episodes: List<RssEpisode>
)

class RssParser(private val client: OkHttpClient) {

    suspend fun fetchAndParse(feedUrl: String): RssFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(feedUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch RSS: ${response.code}")
            val inputStream = response.body?.byteStream() ?: throw Exception("Empty body")
            parseXml(inputStream)
        }
    }

    private fun parseXml(inputStream: InputStream): RssFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var feedTitle = ""
        var feedDescription: String? = null
        var feedArtworkUrl: String? = null
        val episodes = mutableListOf<RssEpisode>()

        var insideItem = false
        var insideImage = false

        var currentTitle = ""
        var currentDescription: String? = null
        var currentStreamUrl = ""
        var currentDuration: Long? = null
        var currentPubDate: String? = null
        var currentGuid = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (name) {
                        "item" -> insideItem = true
                        "image" -> if (!insideItem) insideImage = true
                        "title" -> {
                            if (insideItem) currentTitle = readText(parser)
                            else if (!insideImage) feedTitle = readText(parser)
                        }
                        "description" -> {
                            if (insideItem) currentDescription = readText(parser)
                            else if (!insideImage) feedDescription = readText(parser)
                        }
                        "url" -> {
                            if (insideImage) feedArtworkUrl = readText(parser)
                        }
                        "itunes:image" -> {
                            if (!insideItem && feedArtworkUrl == null) {
                                feedArtworkUrl = parser.getAttributeValue(null, "href")
                            }
                        }
                        "enclosure" -> {
                            if (insideItem) {
                                currentStreamUrl = parser.getAttributeValue(null, "url") ?: ""
                            }
                        }
                        "pubDate" -> {
                            if (insideItem) currentPubDate = readText(parser)
                        }
                        "guid" -> {
                            if (insideItem) currentGuid = readText(parser)
                        }
                        "itunes:duration" -> {
                            if (insideItem) {
                                val durStr = readText(parser)
                                currentDuration = parseDuration(durStr)
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (name) {
                        "item" -> {
                            insideItem = false
                            if (currentStreamUrl.isNotEmpty()) {
                                episodes.add(
                                    RssEpisode(
                                        title = currentTitle,
                                        description = currentDescription,
                                        streamUrl = currentStreamUrl,
                                        durationMs = currentDuration,
                                        pubDate = currentPubDate,
                                        guid = currentGuid.ifEmpty { currentStreamUrl }
                                    )
                                )
                            }
                            currentTitle = ""
                            currentDescription = null
                            currentStreamUrl = ""
                            currentDuration = null
                            currentPubDate = null
                            currentGuid = ""
                        }
                        "image" -> insideImage = false
                    }
                }
            }
            eventType = parser.next()
        }

        return RssFeed(feedTitle, feedDescription, feedArtworkUrl, episodes)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    private fun parseDuration(duration: String): Long? {
        // e.g. "01:23:45" or "1234" (seconds)
        if (duration.isEmpty()) return null
        return try {
            if (duration.contains(":")) {
                val parts = duration.split(":").map { it.toLong() }
                when (parts.size) {
                    2 -> (parts[0] * 60 + parts[1]) * 1000
                    3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000
                    else -> null
                }
            } else {
                duration.toLong() * 1000
            }
        } catch (e: Exception) {
            null
        }
    }
}