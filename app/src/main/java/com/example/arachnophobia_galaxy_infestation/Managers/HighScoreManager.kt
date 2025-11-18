package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object HighScoreManager {

    fun saveHighScore(appContext: Context, score: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        // ✅ Correct username source (fixes your issue)
        val username = auth.currentUser?.displayName ?: "Guest"
        if (username.isBlank() || username.equals("Guest", ignoreCase = true)) {
            return
        }

        val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        // Network state
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isOnline = cm.activeNetworkInfo?.isConnected == true

        // Offline → save pending
        if (!isOnline) {
            prefs.edit().putInt("pending_highscore", score).apply()
            Toast.makeText(appContext, "No internet. High score saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        // Online → merge pending
        val pending = prefs.getInt("pending_highscore", 0)
        val currentScore = maxOf(score, pending)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existing = snapshot.child("highscore").getValue(Int::class.java) ?: 0

                if (currentScore > existing) {
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

    fun registerNetworkCallback(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {

                val auth = FirebaseAuth.getInstance()
                val username = auth.currentUser?.displayName ?: "Guest"

                if (username.isBlank() || username.equals("Guest", ignoreCase = true)) {
                    return
                }

                val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val pending = prefs.getInt("pending_highscore", 0)

                if (pending > 0) {
                    saveHighScore(context, pending)
                }
            }
        }

        cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
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