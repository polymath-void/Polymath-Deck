package com.crescentdeck.pip

import android.app.PendingIntent
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.crescentdeck.R

/**
 * Creates system RemoteAction controls for the Android Picture-in-Picture window.
 */
object PiPRemoteActions {

    const val ACTION_PLAY_PAUSE = "com.crescentdeck.ACTION_PLAY_PAUSE"
    const val ACTION_SKIP_NEXT = "com.crescentdeck.ACTION_SKIP_NEXT"
    const val ACTION_CLOSE = "com.crescentdeck.ACTION_CLOSE"

    @RequiresApi(Build.VERSION_CODES.O)
    fun buildActions(context: Context, isPlaying: Boolean): List<RemoteAction> {
        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val playPauseIntent = PendingIntent.getBroadcast(
            context, 10, Intent(ACTION_PLAY_PAUSE), PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseAction = RemoteAction(
            Icon.createWithResource(context, playPauseIcon),
            playPauseTitle,
            playPauseTitle,
            playPauseIntent
        )

        val skipIntent = PendingIntent.getBroadcast(
            context, 11, Intent(ACTION_SKIP_NEXT), PendingIntent.FLAG_IMMUTABLE
        )
        val skipAction = RemoteAction(
            Icon.createWithResource(context, R.drawable.ic_skip),
            "Next",
            "Next",
            skipIntent
        )

        val closeIntent = PendingIntent.getBroadcast(
            context, 12, Intent(ACTION_CLOSE), PendingIntent.FLAG_IMMUTABLE
        )
        val closeAction = RemoteAction(
            Icon.createWithResource(context, R.drawable.ic_close),
            "Close",
            "Close",
            closeIntent
        )

        return listOf(playPauseAction, skipAction, closeAction)
    }
}
