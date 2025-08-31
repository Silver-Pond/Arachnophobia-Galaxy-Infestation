package com.example.arachnophobia_galaxy_infestation

import android.media.MediaPlayer

object MusicPlayerManager {
    var mediaPlayer: MediaPlayer? = null
    var currentVolume: Float = 0.5f // default volume

    fun updateVolume(vol: Float) {
        currentVolume = vol
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }
}