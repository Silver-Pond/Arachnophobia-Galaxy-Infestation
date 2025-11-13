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

        // Register network callback (Android Developers, 2025; Firebsae, 2025)
        HighScoreManager.registerNetworkCallback(applicationContext)
        CurrencyManager.registerNetworkCallback(applicationContext)

        // Initialize the NetworkMonitor (Android Developers, 2025; Firebsae, 2025)
        networkMonitor = NetworkMonitor(this, this)

        // Initialize background music (Android Developers, 2025; Firebsae, 2025)
        mediaPlayer = MediaPlayer.create(this, R.raw.nebula)
        MusicPlayerManager.mediaPlayer = mediaPlayer

        // Load saved music volume from preferences (Android Developers, 2025; Firebsae, 2025)
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
        // Sync player data when app starts
        PlayerDataSync.syncPlayerData(this)
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

        // Restart music from beginning every time (Android Developers, 2025; Firebsae, 2025)
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
    }

    override fun onNetworkLost() {
        NetworkUtils.isOnline = false
    }

    // Helper function to apply saved language (Android Developers, 2025; Firebsae, 2025)
    private fun applySavedLocale(context: Context) {
        val prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE)
        val languageCode = prefs.getString("language", "en") ?: "en"

        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    object PlayerDataSync {

        fun syncPlayerData(context: Context, onComplete: (() -> Unit)? = null) {
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return

            val uid = currentUser.uid
            val playerRef = FirebaseDatabase.getInstance()
                .getReference("players")
                .child(uid)

            // Fetch player data (Android Developers, 2025; Firebsae, 2025)
            playerRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val map = snapshot.value as? Map<*, *>

                    val owned = (map?.get("ownedSkins") as? List<*>)?.map { it.toString() }
                        ?: listOf("Moth", "Super Mario", "Space Invader")

                    val equipped = map?.get("equippedSkin")?.toString() ?: "Moth"

                    val username = currentUser.displayName ?: "Player"

                    val prefs = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putString("username", username)
                        .putStringSet("ownedSkins", owned.toSet())
                        .putString("equippedSkin", equipped)
                        .apply()
                }
                onComplete?.invoke()
            }.addOnFailureListener {
                onComplete?.invoke()
            }
        }
    }
}
/*
* Reference List
*
* Android Developers, 2025. AppCompatActivity. [online]. Available at:
* https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Fragment. [online]. Available at:
* https://developer.android.com/reference/androidx/fragment/app/Fragment
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. MediaPlayer. [online]. Available at:
* https://developer.android.com/reference/android/media/MediaPlayer
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. TextView. [online]. Available at:
* https://developer.android.com/reference/android/widget/TextView
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Toast. [online]. Available at:
* https://developer.android.com/reference/android/widget/Toast
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. SharedPreferences. [online]. Available at:
* https://developer.android.com/reference/android/content/SharedPreferences
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Configuration. [online]. Available at:
* https://developer.android.com/reference/android/content/res/Configuration
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Locale. [online]. Available at:
* https://developer.android.com/reference/java/util/Locale
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseAuth. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseDatabase. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/FirebaseDatabase
* [Accessed: 7 October 2025].
*/