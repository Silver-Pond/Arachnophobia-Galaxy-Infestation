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
    var isOnline = false
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
        isOnline = true

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        val pendingScore = HighScoreManager.getPendingHighScore(this, uid)
        if (pendingScore != -1) {
            syncHighScoreToDatabase(pendingScore)
        }
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

    fun syncHighScoreToDatabase(score: Int) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Not logged in. Cannot sync high score.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use UID as the unique identifier
        val uid = user.uid

        val playerRef = FirebaseDatabase.getInstance()
            .getReference("players")
            .child(uid)
            .child("highscore")

        playerRef.setValue(score)
            .addOnSuccessListener {
                HighScoreManager.clearPendingHighScore(this, uid)
                Toast.makeText(this, "High score synced!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // If sync failed, keep it locally for retry
                HighScoreManager.savePendingHighScore(this, uid, score)
            }
    }
}