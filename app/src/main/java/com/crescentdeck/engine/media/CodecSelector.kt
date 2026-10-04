package com.crescentdeck.engine.media

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates hardware decoder availability for video formats (H.264, H.265/HEVC, VP9, AV1).
 */
@Singleton
class CodecSelector @Inject constructor() {

    fun hasHardwareDecoder(mimeType: String): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.equals(mimeType, ignoreCase = true)) {
                        if (info.isHardwareAccelerated) {
                            return true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}
