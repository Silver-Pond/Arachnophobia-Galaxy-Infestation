package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object HighScoreManager {

    private const val PREFS_NAME = "AppSettings"
    private const val KEY_PENDING_HIGHSCORE = "pending_highscore"
    private const val KEY_PENDING_SURVIVAL_HIGHSCORE = "pending_survival_highscore"

    /** Save normal highscore, offline or online */
    fun saveHighScore(context: Context, score: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        if (!NetworkUtils.isOnline) {
            // Offline → store locally
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pendingScore = prefs.getInt(KEY_PENDING_HIGHSCORE, -1)
            if (score > pendingScore) {
                prefs.edit().putInt(KEY_PENDING_HIGHSCORE, score).apply()
            }
            Toast.makeText(context, "No internet. High score saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        // Online → update if higher (Android Developers, 2025; Firebase, 2025)
        dbRef.child("highscore").get().addOnSuccessListener { snapshot ->
            val existingHighscore = snapshot.getValue(Int::class.java) ?: 0
            if (score > existingHighscore) {
                dbRef.child("highscore").setValue(score)
                    .addOnSuccessListener {
                        Toast.makeText(context, "New Highscore!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update highscore", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error fetching highscore from server", Toast.LENGTH_SHORT).show()
        }
    }

    /** Save survival highscore, offline or online */
    fun saveSurvivalHighScore(context: Context, survivalScore: Int) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        if (!NetworkUtils.isOnline) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val pendingScore = prefs.getInt(KEY_PENDING_SURVIVAL_HIGHSCORE, -1)
            if (survivalScore > pendingScore) {
                prefs.edit().putInt(KEY_PENDING_SURVIVAL_HIGHSCORE, survivalScore).apply()
            }
            Toast.makeText(context, "No internet. Survival high score saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.child("survivalHighscore").get().addOnSuccessListener { snapshot ->
            val existingHighscore = snapshot.getValue(Int::class.java) ?: 0
            if (survivalScore > existingHighscore) {
                dbRef.child("survivalHighscore").setValue(survivalScore)
                    .addOnSuccessListener {
                        Toast.makeText(context, "New Survival Highscore!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update survival highscore", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error fetching survival highscore from server", Toast.LENGTH_SHORT).show()
        }
    }

    /** Sync pending highscores when internet is back */
    fun syncPendingHighScores(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Normal highscore (Android Developers, 2025; Firebase, 2025)
        val pendingScore = prefs.getInt(KEY_PENDING_HIGHSCORE, -1)
        if (pendingScore > -1) {
            dbRef.child("highscore").get().addOnSuccessListener { snapshot ->
                val existingHighscore = snapshot.getValue(Int::class.java) ?: 0
                if (pendingScore > existingHighscore) {
                    dbRef.child("highscore").setValue(pendingScore)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pending highscore synced: $pendingScore", Toast.LENGTH_SHORT).show()
                        }
                }
                prefs.edit().remove(KEY_PENDING_HIGHSCORE).apply()
            }
        }

        // Survival highscore (Android Developers, 2025; Firebase, 2025)
        val pendingSurvivalScore = prefs.getInt(KEY_PENDING_SURVIVAL_HIGHSCORE, -1)
        if (pendingSurvivalScore > -1) {
            dbRef.child("survivalHighscore").get().addOnSuccessListener { snapshot ->
                val existingHighscore = snapshot.getValue(Int::class.java) ?: 0
                if (pendingSurvivalScore > existingHighscore) {
                    dbRef.child("survivalHighscore").setValue(pendingSurvivalScore)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Pending survival highscore synced: $pendingSurvivalScore", Toast.LENGTH_SHORT).show()
                        }
                }
                prefs.edit().remove(KEY_PENDING_SURVIVAL_HIGHSCORE).apply()
            }
        }
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
*/