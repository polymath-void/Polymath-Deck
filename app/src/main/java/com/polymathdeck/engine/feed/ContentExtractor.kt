package com.polymathdeck.engine.feed

import java.util.regex.Pattern

/**
 * Lightweight Readability-style HTML parser that extracts clean text
 * and thumbnail image URLs from article HTML payloads.
 */
object ContentExtractor {

    private val SCRIPT_PATTERN = Pattern.compile("<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE)
    private val STYLE_PATTERN = Pattern.compile("<style[^>]*>[\\s\\S]*?</style>", Pattern.CASE_INSENSITIVE)
    private val TAG_PATTERN = Pattern.compile("<[^>]+>")
    private val IMG_SRC_PATTERN = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>", Pattern.CASE_INSENSITIVE)

    /**
     * Strips HTML tags, scripts, and styling to produce clean readable plain text.
     */
    fun extractPlainText(html: String): String {
        var text = SCRIPT_PATTERN.matcher(html).replaceAll("")
        text = STYLE_PATTERN.matcher(text).replaceAll("")
        text = TAG_PATTERN.matcher(text).replaceAll(" ")
        // Unescape standard HTML entities
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    /**
     * Extracts the primary image/thumbnail URL from HTML content.
     */
    fun extractFirstImageUrl(html: String): String? {
        val matcher = IMG_SRC_PATTERN.matcher(html)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
}
