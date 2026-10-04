package com.crescentdeck.engine.router

import android.net.Uri
import android.webkit.MimeTypeMap
import com.crescentdeck.data.db.entity.CardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified URI routing layer that sniffs MIME types and routes incoming
 * URLs to the appropriate CardType (Web, Video, Audio, Article, Image, PDF).
 */
@Singleton
class ContentRouter @Inject constructor(
    private val httpClient: OkHttpClient
) {

    suspend fun resolve(uri: Uri): CardType = withContext(Dispatchers.IO) {
        val path = uri.toString().lowercase(Locale.ROOT)

        // 1. Fast path: check known file extensions
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        if (!extension.isNullOrEmpty()) {
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) {
                return@withContext mapMimeToCardType(mime)
            }
        }

        // Direct extension matching
        when {
            path.endsWith(".mp4") || path.endsWith(".mkv") || path.endsWith(".m3u8") ||
                    path.endsWith(".mpd") || path.endsWith(".webm") -> return@withContext CardType.VIDEO
            path.endsWith(".mp3") || path.endsWith(".aac") || path.endsWith(".flac") ||
                    path.endsWith(".ogg") || path.endsWith(".wav") -> return@withContext CardType.AUDIO
            path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                    path.endsWith(".webp") || path.endsWith(".gif") -> return@withContext CardType.IMAGE
            path.endsWith(".pdf") -> return@withContext CardType.PDF
            path.endsWith(".rss") || path.endsWith(".xml") || path.contains("feed") -> return@withContext CardType.ARTICLE
        }

        // 2. Slow path: HTTP HEAD request to read Content-Type header
        if (uri.scheme?.startsWith("http") == true) {
            try {
                val request = Request.Builder().url(uri.toString()).head().build()
                httpClient.newCall(request).execute().use { response ->
                    val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT)
                    if (contentType != null) {
                        return@withContext mapMimeToCardType(contentType)
                    }
                }
            } catch (e: Exception) {
                // If HEAD request fails, fallback to WEB
            }
        }

        return@withContext CardType.WEB
    }

    private fun mapMimeToCardType(mimeType: String): CardType {
        return when {
            mimeType.startsWith("video/") -> CardType.VIDEO
            mimeType.startsWith("audio/") -> CardType.AUDIO
            mimeType.startsWith("image/") -> CardType.IMAGE
            mimeType == "application/pdf" -> CardType.PDF
            mimeType.contains("rss") || mimeType.contains("atom") || mimeType.contains("xml") -> CardType.ARTICLE
            else -> CardType.WEB
        }
    }
}
