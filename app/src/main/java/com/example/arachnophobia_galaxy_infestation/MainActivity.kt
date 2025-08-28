package com.example.arachnophobia_galaxy_infestation

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize the NetworkMonitor
        networkMonitor = NetworkMonitor(this, this)

        // Find the TextView by its ID
        start = findViewById(R.id.pressStart)

        start.setOnClickListener {
            // Check if the device is online
            if (isOnline) {
                // Navigate to the login fragment
                replaceFragment(LoginHubFragment())
            } else {
                // Navigate to the game menu fragment
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
    }

    override fun onPause() {
        super.onPause()
        networkMonitor.unregister()
    }

    override fun onNetworkAvailable() {
        isOnline = true
    }

    override fun onNetworkLost() {
        isOnline = false
    }
}
