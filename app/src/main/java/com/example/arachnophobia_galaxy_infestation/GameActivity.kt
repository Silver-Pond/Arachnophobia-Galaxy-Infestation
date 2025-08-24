package com.example.arachnophobia_galaxy_infestation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class GameActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var moveRunnable: Runnable? = null
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
        setContentView(R.layout.activity_game)

        // Retrieve the username from the Intent
        username = intent.getStringExtra("username") ?: "Guest"

        // Load GameFragment into gameframe
        val gameFragment = GameFragment().apply {
            arguments = Bundle().apply {
                putString("username", username)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.gameframe, gameFragment)
            .commit()

        // Set up buttons
        leftBtn = findViewById(R.id.leftbtn)
        rightBtn = findViewById(R.id.rightbtn)
        blastBtn = findViewById(R.id.blastbtn)
        pauseBtn = findViewById(R.id.pausebtn)

        // Continuous LEFT movement
        leftBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLeftPressed = true
                    if (!isRightPressed) {
                        startMoving { gameFragment.movePlayerLeft() }
                    } else {
                        stopMoving()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isLeftPressed = false
                    if (isRightPressed) {
                        startMoving { gameFragment.movePlayerRight() }
                    } else {
                        stopMoving()
                    }
                    true
                }
                else -> false
            }
        }

// Continuous RIGHT movement
        rightBtn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isRightPressed = true
                    if (!isLeftPressed) {
                        startMoving { gameFragment.movePlayerRight() }
                    } else {
                        stopMoving()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isRightPressed = false
                    if (isLeftPressed) {
                        startMoving { gameFragment.movePlayerLeft() }
                    } else {
                        stopMoving()
                    }
                    true
                }
                else -> false
            }
        }
        // Shooting
        blastBtn.setOnClickListener {
            gameFragment.shoot()
        }
        // Pause
        pauseBtn.setOnClickListener {
            val isPaused = gameFragment.togglePause()
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
}