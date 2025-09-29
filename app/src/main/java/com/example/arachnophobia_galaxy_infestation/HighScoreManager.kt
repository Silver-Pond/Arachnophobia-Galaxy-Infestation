package com.example.arachnophobia_galaxy_infestation

import android.content.Context

object HighScoreManager {
    private const val PREFS_NAME = "HighScorePrefs"
    private const val PENDING_HIGHSCORE_KEY = "pending_highscore"

    fun savePendingHighScore(context: Context, uid: String, score: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("$PENDING_HIGHSCORE_KEY-$uid", score)
            .apply()
    }

    fun getPendingHighScore(context: Context, uid: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("$PENDING_HIGHSCORE_KEY-$uid", -1)
    }

    fun clearPendingHighScore(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("$PENDING_HIGHSCORE_KEY-$uid")
            .apply()
    }
}