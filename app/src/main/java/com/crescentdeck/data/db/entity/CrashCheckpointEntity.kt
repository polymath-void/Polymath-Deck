package com.crescentdeck.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Checkpoint entity storing binary/json serialized state for crash recovery.
 */
@Entity(tableName = "crash_checkpoints")
data class CrashCheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    val checkpointId: Long = 0L,
    val deckId: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val serializedState: ByteArray,
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CrashCheckpointEntity
        if (checkpointId != other.checkpointId) return false
        if (deckId != other.deckId) return false
        if (!serializedState.contentEquals(other.serializedState)) return false
        if (timestamp != other.timestamp) return false
        return isValid == other.isValid
    }

    override fun hashCode(): Int {
        var result = checkpointId.hashCode()
        result = 31 * result + deckId.hashCode()
        result = 31 * result + serializedState.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isValid.hashCode()
        return result
    }
}
