package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object HighScoreManager {

    /**
     * Saves the high score — locally if offline, or to Firebase if online.
     * Will NOT save if username is Guest, null, or empty.
     */
    fun saveHighScore(appContext: Context, score: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // --- Username validation --- (Android Developers, 2025; Firebase, 2025)
        val username = prefs.getString("username", "Guest") ?: "Guest"
        if (username.isBlank() || username.equals("Guest", ignoreCase = true)) {
            // Reject saving highscore entirely
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        val isOnline = networkInfo != null && networkInfo.isConnected

        // --- Offline case: save pending highscore locally --- (Android Developers, 2025; ChatGPT-4, 2025)
        if (!isOnline) {
            prefs.edit().putInt("pending_highscore", score).apply()
            Toast.makeText(appContext, "No internet. High score saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        // --- Online case: check pending and upload --- (Android Developers, 2025; ChatGPT-4, 2025)
        val pendingHighScore = prefs.getInt("pending_highscore", 0)
        val currentScore = maxOf(score, pendingHighScore)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingHighscore = snapshot.child("highscore").getValue(Int::class.java) ?: 0

                if (currentScore > existingHighscore) {
                    dbRef.child("highscore").setValue(currentScore)
                        .addOnSuccessListener {
                            prefs.edit().remove("pending_highscore").apply()
                            Toast.makeText(appContext, "New high score saved online!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(appContext, "Failed to update high score", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(appContext, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Automatically syncs pending high scores when network returns.
     */
    fun registerNetworkCallback(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

                // --- Username validation --- (Android Developers, 2025; Firebase, 2025)
                val username = prefs.getString("username", "Guest") ?: "Guest"
                if (username.isBlank() || username.equals("Guest", ignoreCase = true)) {
                    // Do not sync highscores for guest accounts
                    return
                }

                val pendingHighScore = prefs.getInt("pending_highscore", 0)

                if (pendingHighScore > 0) {
                    saveHighScore(context, pendingHighScore)
                }
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
}
/*
* Reference List
*
* Android Developers, 2025. SharedPreferences. [online]. Available at:
* https://developer.android.com/reference/android/content/SharedPreferences
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Toast. [online]. Available at:
* https://developer.android.com/reference/android/widget/Toast
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Context. [online]. Available at:
* https://developer.android.com/reference/android/content/Context
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseAuth. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseDatabase. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/FirebaseDatabase
* [Accessed: 7 October 2025].
*
* Firebase, 2025. DatabaseReference. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/DatabaseReference
* [Accessed: 7 October 2025].
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/