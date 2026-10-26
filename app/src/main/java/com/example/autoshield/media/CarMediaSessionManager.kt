package com.example.autoshield.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveVideoState(
    val title: String = "No Active Video",
    val isPlaying: Boolean = false,
    val currentTimeSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val currentUrl: String = ""
)

object CarMediaSessionManager {

    private const val SESSION_TAG = "AutoShieldCarMediaSession"
    private const val NOTIFICATION_CHANNEL_ID = "autoshield_car_media"
    const val NOTIFICATION_ID = 1001

    private var appContext: Context? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Callbacks to send controls back to the active WebView
    var onPlayCommand: (() -> Unit)? = null
    var onPauseCommand: (() -> Unit)? = null
    var onSeekCommand: ((seconds: Long) -> Unit)? = null
    var onSkipForwardCommand: (() -> Unit)? = null
    var onSkipBackwardCommand: (() -> Unit)? = null

    private val _videoState = MutableStateFlow(ActiveVideoState())
    val videoState: StateFlow<ActiveVideoState> = _videoState.asStateFlow()

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (mediaSession == null) {
            mediaSession = MediaSession(app, SESSION_TAG).apply {
                setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                            MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )

                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        requestAudioFocus()
                        onPlayCommand?.invoke()
                        updatePlaybackState(isPlaying = true)
                    }

                    override fun onPause() {
                        onPauseCommand?.invoke()
                        updatePlaybackState(isPlaying = false)
                    }

                    override fun onSkipToNext() {
                        onSkipForwardCommand?.invoke()
                    }

                    override fun onSkipToPrevious() {
                        onSkipBackwardCommand?.invoke()
                    }

                    override fun onFastForward() {
                        onSkipForwardCommand?.invoke()
                    }

                    override fun onRewind() {
                        onSkipBackwardCommand?.invoke()
                    }

                    override fun onSeekTo(pos: Long) {
                        onSeekCommand?.invoke(pos / 1000)
                    }
                })

                isActive = true
            }
        }

        createNotificationChannel(app)
    }

    fun getSessionToken(): MediaSession.Token? = mediaSession?.sessionToken

    fun updateVideoInfo(title: String, isPlaying: Boolean, currentTime: Long, duration: Long, url: String) {
        val cleanTitle = if (title.isBlank()) "Online Video" else title
        _videoState.value = ActiveVideoState(
            title = cleanTitle,
            isPlaying = isPlaying,
            currentTimeSeconds = currentTime,
            durationSeconds = duration,
            currentUrl = url
        )

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, cleanTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "AutoShield Car Browser")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration * 1000L)
            .build()

        mediaSession?.setMetadata(metadata)
        updatePlaybackState(isPlaying, currentTime * 1000L)

        // Request audio focus when playback begins
        if (isPlaying) {
            requestAudioFocus()
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean, positionMs: Long = 0L) {
        val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val actions = PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_REWIND or
                PlaybackState.ACTION_SEEK_TO

        val playbackState = PlaybackState.Builder()
            .setActions(actions)
            .setState(state, positionMs, 1.0f)
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange ->
                    handleAudioFocusChange(focusChange)
                }
                .build()
            audioFocusRequest = focusRequest
            am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                { focusChange -> handleAudioFocusChange(focusChange) },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                onPauseCommand?.invoke()
                updatePlaybackState(isPlaying = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                onPauseCommand?.invoke()
                updatePlaybackState(isPlaying = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower volume or keep playing quietly
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                onPlayCommand?.invoke()
                updatePlaybackState(isPlaying = true)
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Car Video & Audio Playback"
            val descriptionText = "Controls video audio playback in moving car"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildPlaybackNotification(context: Context): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val current = _videoState.value

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(current.title)
            .setContentText("AutoShield: Video Audio Active (Tata Tiago / Car Connected)")
            .setContentIntent(pendingIntent)
            .setOngoing(current.isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
}
