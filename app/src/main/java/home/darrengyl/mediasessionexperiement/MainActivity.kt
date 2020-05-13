package home.darrengyl.mediasessionexperiement

import android.content.ComponentName
import android.media.session.PlaybackState
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var mediaBrowser: MediaBrowserCompat
    private lateinit var playPauseBtn: ImageButton
    private lateinit var title: TextView
    private lateinit var artist: TextView

    private val mediaBrowserCompatConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val mediaController = MediaControllerCompat(this@MainActivity, mediaBrowser.sessionToken)
            MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
            buildTransportControls()
        }

        override fun onConnectionSuspended() {
            // The Service has crashed. Disable transport controls until it automatically reconnects
        }

        override fun onConnectionFailed() {
            // The Service has refused our connection
        }
    }

    private val mediaControllerCompatCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            when (state?.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    playPauseBtn.setImageResource(R.drawable.ic_pause_black_36dp)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    playPauseBtn.setImageResource(R.drawable.ic_play_arrow_black_36dp)
                }
                PlaybackStateCompat.STATE_ERROR -> {
                    playPauseBtn.setImageResource(R.drawable.ic_play_arrow_black_36dp)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)?.let {
                title.text = it
            }
            metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)?.let {
                artist.text = it
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        playPauseBtn = findViewById(R.id.play_pause)
        title = findViewById(R.id.title)
        artist = findViewById(R.id.artist)
        mediaBrowser = MediaBrowserCompat(this,
            ComponentName(this, MusicService::class.java),
            mediaBrowserCompatConnectionCallback,
            null)

    }

    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    fun buildTransportControls() {
        val mediaControllerCompat = MediaControllerCompat.getMediaController(this)
        playPauseBtn.setOnClickListener {
            if (mediaControllerCompat.playbackState.state == PlaybackStateCompat.STATE_PLAYING) {
                mediaControllerCompat.transportControls.pause()
            } else {
                mediaControllerCompat.transportControls.play()
            }
        }
        mediaControllerCompat.registerCallback(mediaControllerCompatCallback)
    }

    override fun onStop() {
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(mediaControllerCompatCallback)
        mediaBrowser.disconnect()
        super.onStop()
    }


}
