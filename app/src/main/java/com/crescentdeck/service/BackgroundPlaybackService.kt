package com.crescentdeck.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.crescentdeck.R
import com.crescentdeck.engine.media.MediaPlaybackEngine
import com.crescentdeck.notification.NotificationChannels
import com.crescentdeck.ui.screen.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground Service maintaining audio playback, MediaSession controls,
 * lock-screen interaction, and notification transport buttons during background operation.
 */
@UnstableApi
@AndroidEntryPoint
class BackgroundPlaybackService : Service() {

    @Inject
    lateinit var playbackEngine: MediaPlaybackEngine

    private var mediaSession: MediaSessionCompat? = null
    private var audioFocusManager: AudioFocusManager? = null
    private var noisyReceiver: BecomingNoisyReceiver? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BackgroundPlaybackService = this@BackgroundPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize MediaSessionCompat
        mediaSession = MediaSessionCompat(this, "CrescentPlaybackSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playbackEngine.resume()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    startForeground(NOTIFICATION_ID, buildNotification(true))
                }

                override fun onPause() {
                    playbackEngine.pause()
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    stopForeground(STOP_FOREGROUND_DETACH)
                    startNotificationUpdate(buildNotification(false))
                }

                override fun onStop() {
                    playbackEngine.pause()
                    stopSelf()
                }
            })
            isActive = true
        }

        // 2. Setup AudioFocus
        playbackEngine.getActivePlayer()?.let { player ->
            audioFocusManager = AudioFocusManager(this, player).also {
                it.requestAudioFocus()
            }
            setupNoisyReceiver(player)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val player = playbackEngine.getActivePlayer()
        if (player != null && audioFocusManager == null) {
            audioFocusManager = AudioFocusManager(this, player).also {
                it.requestAudioFocus()
            }
            setupNoisyReceiver(player)
        }

        val isPlaying = player?.isPlaying ?: false
        startForeground(NOTIFICATION_ID, buildNotification(isPlaying))
        return START_NOT_STICKY
    }

    private fun setupNoisyReceiver(player: ExoPlayer) {
        noisyReceiver = BecomingNoisyReceiver(player) {
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            startNotificationUpdate(buildNotification(false))
        }
        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, playbackEngine.getActivePlayer()?.currentPosition ?: 0L, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val sessionToken = mediaSession?.sessionToken

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause, "Pause",
                PendingIntent.getBroadcast(
                    this, 1, Intent("com.crescentdeck.ACTION_PLAY_PAUSE"), PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_play, "Play",
                PendingIntent.getBroadcast(
                    this, 1, Intent("com.crescentdeck.ACTION_PLAY_PAUSE"), PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val mediaStyle = MediaStyle()
            .setShowActionsInCompactView(0)
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken)
        }

        return NotificationCompat.Builder(this, NotificationChannels.CHANNEL_MEDIA)
            .setSmallIcon(R.drawable.ic_crescent)
            .setContentTitle("Crescent Deck Media")
            .setContentText("Playing background media")
            .setContentIntent(contentPendingIntent)
            .setStyle(mediaStyle)
            .addAction(playPauseAction)
            .setOngoing(isPlaying)
            .build()
    }

    private fun startNotificationUpdate(notification: Notification) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        audioFocusManager?.abandonAudioFocus()
        noisyReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {}
        }
        mediaSession?.release()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
