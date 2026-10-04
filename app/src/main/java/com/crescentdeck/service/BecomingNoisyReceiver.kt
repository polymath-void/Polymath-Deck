package com.crescentdeck.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import androidx.media3.exoplayer.ExoPlayer

/**
 * BroadcastReceiver that pauses playback when headphones or external audio outputs are disconnected.
 */
class BecomingNoisyReceiver(
    private val player: ExoPlayer? = null,
    private val onNoisyAction: (() -> Unit)? = null
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            player?.pause()
            onNoisyAction?.invoke()
        }
    }
}
