package com.example.arachnophobia_galaxy_infestation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        // Currency Notifications
        createNotificationChannel()

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "spider_channel",
                "Spider Silk Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when spider silk changes"
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
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

        // Start music when activity is visible
        mediaPlayer?.start()
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
