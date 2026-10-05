package com.polymathdeck.engine.feed

import android.util.Xml
import com.polymathdeck.data.db.entity.FeedEntity
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.UUID

/**
 * Handles importing and exporting RSS subscription lists in standard OPML format.
 */
object OPMLManager {

    fun importOPML(inputStream: InputStream): List<FeedEntity> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, null)

        val feeds = mutableListOf<FeedEntity>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name.equals("outline", ignoreCase = true)) {
                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                val text = parser.getAttributeValue(null, "text") ?: parser.getAttributeValue(null, "title") ?: ""
                val htmlUrl = parser.getAttributeValue(null, "htmlUrl") ?: ""

                if (!xmlUrl.isNullOrBlank()) {
                    feeds.add(
                        FeedEntity(
                            feedId = UUID.randomUUID().toString(),
                            url = xmlUrl.trim(),
                            title = text.trim(),
                            siteUrl = htmlUrl.trim()
                        )
                    )
                }
            }
            eventType = parser.next()
        }
        return feeds
    }

    fun exportOPML(feeds: List<FeedEntity>, outputStream: OutputStream) {
        val writer = PrintWriter(outputStream)
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        writer.println("<opml version=\"2.0\">")
        writer.println("  <head>")
        writer.println("    <title>Polymath Deck Feeds</title>")
        writer.println("  </head>")
        writer.println("  <body>")

        for (feed in feeds) {
            val titleEsc = escapeXml(feed.title)
            val xmlEsc = escapeXml(feed.url)
            val htmlEsc = escapeXml(feed.siteUrl)
            writer.println("    <outline type=\"rss\" text=\"$titleEsc\" title=\"$titleEsc\" xmlUrl=\"$xmlEsc\" htmlUrl=\"$htmlEsc\" />")
        }

        writer.println("  </body>")
        writer.println("</opml>")
        writer.flush()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
