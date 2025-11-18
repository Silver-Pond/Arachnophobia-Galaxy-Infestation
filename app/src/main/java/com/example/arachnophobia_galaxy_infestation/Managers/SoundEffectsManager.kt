package com.example.arachnophobia_galaxy_infestation

import android.media.SoundPool

object SoundEffectsManager {
    var soundPool: SoundPool? = null
    var effectsVolume: Float = 1.0f // default full volume

    // Update effects volume (Android Developers, 2025; ChatGPT-4, 2025)
    fun updateVolume(vol: Float) {
        effectsVolume = vol
    }

    // Play a sound effect (Android Developers, 2025; ChatGPT-4, 2025)
    fun playSound(soundId: Int) {
        soundPool?.play(soundId, effectsVolume, effectsVolume, 1, 0, 1f)
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