package com.example.arachnophobia_galaxy_infestation

import android.media.SoundPool
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class SoundEffectsManagerTest {

    private lateinit var mockSoundPool: SoundPool

    @Before
    fun setUp() {
        // Mock SoundPool using Mockito (ChatGPT-4, 2025)
        mockSoundPool = mock(SoundPool::class.java)
        SoundEffectsManager.soundPool = mockSoundPool
        SoundEffectsManager.effectsVolume = 1.0f
    }

    @After
    fun tearDown() {
        // Reset singleton after each test
        SoundEffectsManager.soundPool = null
        SoundEffectsManager.effectsVolume = 1.0f
    }

    @Test
    fun testUpdateVolume_changesEffectsVolume() {
        SoundEffectsManager.updateVolume(0.5f)
        assertEquals(0.5f, SoundEffectsManager.effectsVolume, 0.0f)
    }

    @Test
    fun testPlaySound_callsSoundPoolWithCorrectParams() {
        val soundId = 123
        val volume = 0.7f

        SoundEffectsManager.updateVolume(volume)
        SoundEffectsManager.playSound(soundId)

        // Verify SoundPool.play() is called with correct arguments (ChatGPT-4, 2025)
        verify(mockSoundPool).play(
            soundId,
            volume,    // left volume
            volume,    // right volume
            1,         // priority
            0,         // loop
            1f         // rate
        )
    }

    @Test
    fun testPlaySound_whenSoundPoolNull_doesNotCrash() {
        // Set SoundPool to null (ChatGPT-4, 2025)
        SoundEffectsManager.soundPool = null

        // Should not throw exception
        SoundEffectsManager.playSound(123)
    }
}
/*
* Reference List
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/