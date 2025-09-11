package com.example.arachnophobia_galaxy_infestation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.util.Locale
import android.content.res.Configuration
class MainActivity : AppCompatActivity(), NetworkMonitor.NetworkListener {

    private lateinit var networkMonitor: NetworkMonitor
    private var isOnline = false
    private lateinit var start: TextView
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved language BEFORE inflating layout
        applySavedLocale(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize the NetworkMonitor
        networkMonitor = NetworkMonitor(this, this)

        // Initialize background music
        mediaPlayer = MediaPlayer.create(this, R.raw.nebula)
        MusicPlayerManager.mediaPlayer = mediaPlayer

        // Load saved music volume from preferences
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedMusicVolume = prefs.getFloat("music_volume", 0.5f)
        MusicPlayerManager.updateVolume(savedMusicVolume)
        mediaPlayer?.isLooping = true

        // Find the TextView by its ID
        start = findViewById(R.id.pressStart)

        start.setOnClickListener {
            if (isOnline) {
                replaceFragment(LoginHubFragment())
            } else {
                replaceFragment(GameMenuFragment())
            }
        }
    }

    private fun scheduleRepeatingNotification() {
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val interval = 30 * 60 * 1000L // 30 minutes in milliseconds

        // Start after 30 minutes, repeat every 30 minutes
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + interval,
            interval,
            pendingIntent
        )
    }

    private fun cancelRepeatingNotification() {
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
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
        val savedMusicVolume = prefs.getFloat("music_volume", 0.5f)
        MusicPlayerManager.updateVolume(savedMusicVolume)

        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        mediaPlayer?.start()

        // Cancel notifications since user is inside the app
        cancelRepeatingNotification()
    }

    override fun onPause() {
        super.onPause()
        networkMonitor.unregister()
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null

        // Schedule push notifications when app is closed
        scheduleRepeatingNotification()
    }

    override fun onNetworkAvailable() {
        isOnline = true
    }

    override fun onNetworkLost() {
        isOnline = false
    }

    // Helper function to apply saved language
    private fun applySavedLocale(context: Context) {
        val prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE)
        val languageCode = prefs.getString("language", "en") ?: "en"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}