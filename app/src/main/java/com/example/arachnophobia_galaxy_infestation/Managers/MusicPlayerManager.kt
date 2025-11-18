package com.example.arachnophobia_galaxy_infestation

import android.media.MediaPlayer

object MusicPlayerManager {
    var mediaPlayer: MediaPlayer? = null
    var currentVolume: Float = 0.5f // default

    // Initialize the MediaPlayer (Android Developers, 2025; ChatGPT-4, 2025)
    fun updateVolume(vol: Float) {
        currentVolume = vol
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }
}
/*
 * Reference List
 *
 * Android Developers, 2025. Developer centers. [online]. Available at:
 * https://developer.android.com/
 * [Accessed: 6 October 2025].
 *
 * ChatGPT-4, 2025. OpenAI. [online]. Available at:
 * https://chatgpt.com/?model=auto
 * [Accessed: 6 October 2025].
 */