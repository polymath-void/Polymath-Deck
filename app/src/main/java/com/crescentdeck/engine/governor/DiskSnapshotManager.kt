package com.crescentdeck.engine.governor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persisting and loading card visual snapshots to and from device cache storage.
 * Compresses bitmaps to PNG on Dispatchers.IO to protect against memory bloat and Android LMK eviction.
 */
@Singleton
class DiskSnapshotManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val snapshotDir: File
        get() = File(context.cacheDir, "snapshots").apply {
            if (!exists()) {
                mkdirs()
            }
        }

    /**
     * Saves [bitmap] to disk asynchronously under cacheDir/snapshots/{cardId}.png.
     */
    suspend fun saveSnapshot(cardId: String, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        try {
            if (bitmap.isRecycled) return@withContext false
            val file = File(snapshotDir, "$cardId.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, out)
                out.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Loads a cached snapshot bitmap from disk asynchronously.
     */
    suspend fun loadSnapshot(cardId: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(snapshotDir, "$cardId.png")
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deletes a cached snapshot file for [cardId].
     */
    suspend fun deleteSnapshot(cardId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(snapshotDir, "$cardId.png")
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clears all cached snapshots from disk.
     */
    suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        try {
            snapshotDir.deleteRecursively()
        } catch (_: Exception) {}
    }
}
