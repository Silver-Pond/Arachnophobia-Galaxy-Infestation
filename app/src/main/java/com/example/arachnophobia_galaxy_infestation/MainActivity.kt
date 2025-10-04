package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import java.util.Locale
import android.content.res.Configuration
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), NetworkMonitor.NetworkListener {

    private lateinit var networkMonitor: NetworkMonitor
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
            if (NetworkUtils.isOnline) {
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

    override fun onStart() {
        super.onStart()
        // Cancel scheduled notifications because user is back in the app
        NotificationReceiver.cancelAlarm(this)
    }

    override fun onStop() {
        super.onStop()
        // App went to background → schedule next notification (one exact alarm)
        NotificationReceiver.scheduleNextAlarm(this)
    }

    override fun onResume() {
        super.onResume()
        networkMonitor.register()

        // Reload saved volumes
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedMusicVolume = prefs.getFloat("music_volume", 0.5f)
        MusicPlayerManager.updateVolume(savedMusicVolume)

        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Restart music from beginning every time
        mediaPlayer?.seekTo(0)
        mediaPlayer?.start()
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
    }

    override fun onNetworkAvailable() {
        NetworkUtils.isOnline = true

        // Sync any pending highscores when internet is back
        HighScoreManager.syncPendingHighScores(this)
    }

    override fun onNetworkLost() {
        NetworkUtils.isOnline = false
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

    fun syncHighScoreToDatabase(score: Int) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Not logged in. Cannot sync high score.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use HighScoreManager to handle syncing
        if (NetworkUtils.isOnline) {
            HighScoreManager.saveHighScore(this, score)
        } else {
            // Offline → store locally
            val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            val pendingScore = prefs.getInt("pending_highscore", -1)
            if (score > pendingScore) {
                prefs.edit().putInt("pending_highscore", score).apply()
            }
            Toast.makeText(this, "No internet. High score saved locally.", Toast.LENGTH_SHORT).show()
        }
    }
}