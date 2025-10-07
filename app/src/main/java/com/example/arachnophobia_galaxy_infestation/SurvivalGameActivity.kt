package com.example.arachnophobia_galaxy_infestation

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SurvivalGameActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var moveRunnable: Runnable? = null
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var leftBtn: Button
    private lateinit var rightBtn: Button
    private lateinit var blastBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var username: String
    // State variables to track if buttons are pressed
    private var isLeftPressed = false
    private var isRightPressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_survival_game)

        // Retrieve the username from the Intent
        username = intent.getStringExtra("username") ?: "Guest"

        // Retrieve level JSON safely (Android Developers, 2025; Firebsae, 2025)
        val levelJson = intent.getStringExtra("levelData")

        // Pass data into the fragment (Android Developers, 2025; Firebsae, 2025)
        val gameFragment = SurvivalGameFragment().apply {
            arguments = Bundle().apply {
                levelJson?.let { putString("levelData", it) }
                putString("username", username)
            }
        }

        // Load fragment (Android Developers, 2025; Firebsae, 2025)
        supportFragmentManager.beginTransaction()
            .replace(R.id.gameframe, gameFragment)
            .commit()

        // Initialize background music (Android Developers, 2025; Firebsae, 2025)
        mediaPlayer = MediaPlayer.create(this, R.raw.comos).apply {
            isLooping = true
            start()
        }

        // Load saved music volume (Android Developers, 2025; Firebsae, 2025)
        val prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedMusicVolume = prefs.getFloat("music_volume", 0.5f)
        MusicPlayerManager.updateVolume(savedMusicVolume)

        // Set up buttons after fragment is attached (Android Developers, 2025; Firebsae, 2025)
        supportFragmentManager.executePendingTransactions()
        val fragment = supportFragmentManager.findFragmentById(R.id.gameframe) as? SurvivalGameFragment

        leftBtn = findViewById(R.id.leftbtn)
        rightBtn = findViewById(R.id.rightbtn)
        blastBtn = findViewById(R.id.blastbtn)
        pauseBtn = findViewById(R.id.pausebtn)

        // Continuous LEFT movement (Android Developers, 2025; Firebsae, 2025)
        leftBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLeftPressed = true

                    // Reverse controls between levels 10–15
                    if (fragment?.currentLevel in 10..15) {
                        if (!isRightPressed) startMoving { fragment?.movePlayerRight() } else stopMoving()
                    } else {
                        if (!isRightPressed) startMoving { fragment?.movePlayerLeft() } else stopMoving()
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isLeftPressed = false
                    if (fragment?.currentLevel in 10..15) {
                        if (isRightPressed) startMoving { fragment?.movePlayerLeft() } else stopMoving()
                    } else {
                        if (isRightPressed) startMoving { fragment?.movePlayerRight() } else stopMoving()
                    }
                    true
                }

                else -> false
            }
        }

        // Continuous RIGHT movement (Android Developers, 2025; Firebsae, 2025)
        rightBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isRightPressed = true

                    // Reverse controls between levels 10–15
                    if (fragment?.currentLevel in 10..15) {
                        if (!isLeftPressed) startMoving { fragment?.movePlayerLeft() } else stopMoving()
                    } else {
                        if (!isLeftPressed) startMoving { fragment?.movePlayerRight() } else stopMoving()
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isRightPressed = false
                    if (fragment?.currentLevel in 10..15) {
                        if (isLeftPressed) startMoving { fragment?.movePlayerRight() } else stopMoving()
                    } else {
                        if (isLeftPressed) startMoving { fragment?.movePlayerLeft() } else stopMoving()
                    }
                    true
                }

                else -> false
            }
        }

        // Shooting
        blastBtn.setOnClickListener { fragment?.shoot() }

        // Pause
        pauseBtn.setOnClickListener {
            val isPaused = fragment?.togglePause() ?: false
            setControlsEnabled(!isPaused)
        }
    }

    private fun startMoving(action: () -> Unit) {
        moveRunnable = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, 50) // move every 50ms
            }
        }
        handler.post(moveRunnable!!)
    }

    private fun stopMoving() {
        moveRunnable?.let { handler.removeCallbacks(it) }
        moveRunnable = null
    }

    private fun setControlsEnabled(enabled: Boolean) {
        leftBtn.isEnabled = enabled
        rightBtn.isEnabled = enabled
        blastBtn.isEnabled = enabled
    }

    override fun onResume() {
        super.onResume()

        // Reload saved volumes (Android Developers, 2025; Firebsae, 2025)
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
        mediaPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
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