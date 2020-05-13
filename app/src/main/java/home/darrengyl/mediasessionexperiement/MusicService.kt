package home.darrengyl.mediasessionexperiement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioManager
import android.media.AudioManager.*
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

class MusicService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private val noisyIntentFilter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)
    private val noisyBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_AUDIO_BECOMING_NOISY && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }
    }
    private val audioFocusChangeListener = OnAudioFocusChangeListener {
        when (it) {
            AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus
                // Pause playback immediately
                //mediaController.transportControls.pause()
                // Wait 30 seconds before stopping playback
                //handler.postDelayed(delayedStopRunnable, TimeUnit.SECONDS.toMillis(30))
                mediaPlayer.stop()
            }
            AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer.pause()
            }
            AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME)
            }
            AUDIOFOCUS_GAIN -> {
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    mediaPlayer.setVolume(FULL_VOLUME, FULL_VOLUME)
                }
            }
        }
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            if (!isAudioFocusGranted()) {
                return
            }
            registerReceiver(noisyBroadcastReceiver, noisyIntentFilter)
            mediaSession.isActive = true
            mediaPlayer.start()
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }

        override fun onStop() {
            releaseAudioFocus()
            setMediaPlaybackState(PlaybackStateCompat.STATE_STOPPED)
            unregisterReceiver(noisyBroadcastReceiver)
        }

        override fun onPause() {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED)
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            super.onPlayFromMediaId(mediaId, extras)
            //TODO
        }
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(AudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .setUsage(USAGE_MEDIA)
                .build())
            setMediaPlaybackState(PlaybackStateCompat.STATE_NONE)
            setVolume(FULL_VOLUME, FULL_VOLUME)
            val uri = Uri.parse("android.resource://$packageName/raw/warner_tautz_off_broadway")
            setDataSource(this@MusicService, uri)
            prepare()
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(applicationContext, "MediaSession").apply {
            setCallback(mediaSessionCallback)
            this@MusicService.sessionToken = sessionToken
        }

    }

    private fun setMediaPlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .run {
                val actions = if (state == PlaybackStateCompat.STATE_PLAYING) {
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PAUSE
                } else {
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_PLAY
                }
                setActions(actions)
            }
                //TODO
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
        mediaSession.setPlaybackState(playbackState.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        if (clientPackageName == packageName) {
            return BrowserRoot("MediaSessionExperiment", null)
        }
        return null
    }

    private fun isAudioFocusGranted(): Boolean {
        val requestResult = audioManager.requestAudioFocus(audioFocusChangeListener, STREAM_MUSIC, AUDIOFOCUS_GAIN)
        return requestResult == AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun releaseAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }

    override fun onDestroy() {
        mediaPlayer.stop()
        releaseAudioFocus()
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        const val FULL_VOLUME = 1f
        const val DUCK_VOLUME = 0.3f
    }
}