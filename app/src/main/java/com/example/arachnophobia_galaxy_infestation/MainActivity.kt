package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity(), NetworkMonitor.NetworkListener {

    private lateinit var networkMonitor: NetworkMonitor
    private var isOnline = false
    private lateinit var start: TextView
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize the NetworkMonitor
        networkMonitor = NetworkMonitor(this, this)

        // Initialize background music
        mediaPlayer = MediaPlayer.create(this, R.raw.nebula)
        MusicPlayerManager.mediaPlayer = mediaPlayer
        MusicPlayerManager.updateVolume(0.5f)
        mediaPlayer?.isLooping = true

        // Find the TextView by its ID
        start = findViewById(R.id.pressStart)

        start.setOnClickListener {
            // Check if the device is online
            if (isOnline) {
                replaceFragment(LoginHubFragment())
            } else {
                replaceFragment(GameMenuFragment())
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        networkMonitor.register()

        // Load saved volume
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)

        // Restore music volume
        val savedMusicVolume = prefs.getFloat("music_volume", 0.5f)
        MusicPlayerManager.updateVolume(savedMusicVolume)

        // Restore effects volume
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Start music when activity is visible
        MusicPlayerManager.mediaPlayer?.start()
    }

    override fun onPause() {
        super.onPause()
        networkMonitor.unregister()

        // Pause music when activity is not visible
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onNetworkAvailable() {
        isOnline = true
    }

    override fun onNetworkLost() {
        isOnline = false
    }
}
