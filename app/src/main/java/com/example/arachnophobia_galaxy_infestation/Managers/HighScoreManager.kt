package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object HighScoreManager {

    private var isSyncing = false
    private var networkCallbackRegistered = false

    fun saveHighScore(appContext: Context, score: Int) {

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        val prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isOnline = cm.activeNetwork != null

        if (!isOnline) {
            prefs.edit().putInt("pending_highscore", score).apply()
            Toast.makeText(appContext, "No internet. High score saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        val pending = prefs.getInt("pending_highscore", 0)
        val finalScore = maxOf(score, pending)

        val playerScoreRef = FirebaseDatabase.getInstance()
            .getReference("players/$uid/highscore")

        playerScoreRef.runTransaction(object : Transaction.Handler {

            private var scoreUpdated = false

            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val existing = currentData.getValue(Int::class.java) ?: 0
                if (finalScore > existing) {
                    currentData.value = finalScore
                    scoreUpdated = true
                }
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                snapshot: DataSnapshot?
            ) {
                if (error == null && committed && scoreUpdated) {
                    prefs.edit().remove("pending_highscore").apply()
                    Toast.makeText(
                        appContext,
                        "New high score saved online!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    fun registerNetworkCallback(context: Context) {
        if (networkCallbackRegistered) return
        networkCallbackRegistered = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isSyncing) return
                isSyncing = true

                val auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    isSyncing = false
                    return
                }

                val prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                val pending = prefs.getInt("pending_highscore", 0)

                if (pending > 0) {
                    saveHighScore(context, pending)
                }

                isSyncing = false
            }
        }

        cm.registerDefaultNetworkCallback(callback)
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