package com.example.arachnophobia_galaxy_infestation

import android.media.SoundPool

object SoundEffectsManager {
    var soundPool: SoundPool? = null
    var effectsVolume: Float = 1.0f // default full volume

    fun updateVolume(vol: Float) {
        effectsVolume = vol
    }

    fun playSound(soundId: Int) {
        soundPool?.play(soundId, effectsVolume, effectsVolume, 1, 0, 1f)
    }
}