package com.polymathdeck.engine.feed

import android.util.Xml
import com.polymathdeck.data.db.entity.FeedItemEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

data class ParsedFeedResult(
    val title: String,
    val siteUrl: String,
    val items: List<FeedItemEntity>
)

/**
 * Resilient XML Pull Parser for RSS 2.0 and Atom feeds with error recovery.
 */
object FeedParser {

    private val rfc822DateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
    )

    fun parse(inputStream: InputStream, feedId: String): ParsedFeedResult {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        var eventType = parser.eventType
        var feedTitle = ""
        var siteUrl = ""
        val items = mutableListOf<FeedItemEntity>()

        var insideItem = false
        var currentTitle = ""
        var currentLink = ""
        var currentGuid = ""
        var currentContent = ""
        var currentAuthor = ""
        var currentPubDate: Long = System.currentTimeMillis()
        var currentThumbnail = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val name = parser.name ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true) -> {
                            insideItem = true
                            currentTitle = ""
                            currentLink = ""
                            currentGuid = ""
                            currentContent = ""
                            currentAuthor = ""
                            currentPubDate = System.currentTimeMillis()
                            currentThumbnail = ""
                        }
                        insideItem -> {
                            when (name.lowercase(Locale.ROOT)) {
                                "title" -> currentTitle = readText(parser)
                                "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    currentLink = if (!href.isNullOrBlank()) href else readText(parser)
                                }
                                "guid", "id" -> currentGuid = readText(parser)
                                "description", "summary", "content" -> {
                                    val text = readText(parser)
                                    if (currentContent.isEmpty() || text.length > currentContent.length) {
                                        currentContent = text
                                    }
                                }
                                "author", "dc:creator" -> currentAuthor = readText(parser)
                                "pubdate", "published", "updated" -> {
                                    val dateStr = readText(parser)
                                    currentPubDate = parseDate(dateStr)
                                }
                                "media:thumbnail", "media:content", "enclosure" -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrBlank()) {
                                        currentThumbnail = url
                                    }
                                }
                            }
                        }
                        !insideItem -> {
                            when (name.lowercase(Locale.ROOT)) {
                                "title" -> if (feedTitle.isEmpty()) feedTitle = readText(parser)
                                "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (siteUrl.isEmpty()) {
                                        siteUrl = if (!href.isNullOrBlank()) href else readText(parser)
                                    }
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (name.equals("item", ignoreCase = true) || name.equals("entry", ignoreCase = true)) {
                        insideItem = false
                        val guid = if (currentGuid.isNotBlank()) currentGuid else (if (currentLink.isNotBlank()) currentLink else UUID.randomUUID().toString())
                        val extracted = ContentExtractor.extractPlainText(currentContent)
                        val thumb = if (currentThumbnail.isNotBlank()) currentThumbnail else (ContentExtractor.extractFirstImageUrl(currentContent) ?: "")

                        items.add(
                            FeedItemEntity(
                                itemId = UUID.randomUUID().toString(),
                                feedId = feedId,
                                guid = guid,
                                title = currentTitle.trim(),
                                author = currentAuthor.trim(),
                                contentHtml = currentContent,
                                extractedText = extracted,
                                thumbnailUrl = thumb,
                                link = currentLink.trim(),
                                publishedAt = currentPubDate
                            )
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        return ParsedFeedResult(feedTitle, siteUrl, items)
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result
    }

    private fun parseDate(dateStr: String): Long {
        for (format in rfc822DateFormats) {
            try {
                val parsed = format.parse(dateStr.trim())
                if (parsed != null) return parsed.time
            } catch (e: Exception) {
                // Try next
            }
        }
        return System.currentTimeMillis()
    }
}
