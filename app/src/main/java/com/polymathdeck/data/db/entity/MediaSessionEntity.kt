package com.polymathdeck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted state of a media playback session (resume position, track selection, speed).
 */
@Entity(
    tableName = "media_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["cardId"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cardId"], unique = true)
    ]
)
data class MediaSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val cardId: String,
    val mediaUri: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val subtitleTrackId: String? = null,
    val isPlaying: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
