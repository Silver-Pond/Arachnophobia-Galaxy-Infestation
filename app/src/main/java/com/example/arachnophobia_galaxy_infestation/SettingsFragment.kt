package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.content.res.Configuration
import android.media.SoundPool
import android.widget.Switch
import java.util.*

class SettingsFragment : Fragment() {
    private var selectedLanguage: String = "en" // default
    private var clickbuttonSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val btnsavesettings = view.findViewById<Button>(R.id.btnsavesettings)
        val languagespinner = view.findViewById<Spinner>(R.id.languagespinner)
        val musicvolumeSlider = view.findViewById<com.google.android.material.slider.Slider>(R.id.musicvolumeSlider)
        val effectsvolumeSlider = view.findViewById<com.google.android.material.slider.Slider>(R.id.effectsvolumeSlider)
        val bioswitch = view.findViewById<Switch>(R.id.bioswitch)

        // Set up spinner
        val options = arrayOf("English", "Afrikaans", "Zulu")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, options)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        languagespinner.adapter = adapter

        // Initialize SoundPool once
        val soundPool = SoundPool.Builder().setMaxStreams(5).build()
        SoundEffectsManager.soundPool = soundPool

        // Initialize or reinitialize the SoundPool if needed
        if (SoundEffectsManager.soundPool == null) {
            SoundEffectsManager.soundPool = SoundPool.Builder().setMaxStreams(5).build()

            // Button click sound
            clickbuttonSoundId = SoundEffectsManager.soundPool!!.load(requireContext(), R.raw.button_click, 1)
        } else {
            // If already initialized but sound not loaded
            if (clickbuttonSoundId == 0) {
                clickbuttonSoundId = SoundEffectsManager.soundPool!!.load(requireContext(), R.raw.button_click, 1)
            }
        }

        // Load saved preferences (Android Developers, 2025; Firebsae, 2025)
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("language", "en") ?: "en"
        selectedLanguage = savedLang
        val savedVolume = prefs.getFloat("music_volume", 0.5f)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)

        // Set spinner selection based on saved language (Android Developers, 2025; Firebsae, 2025)
        languagespinner.setSelection(
            when (savedLang) {
                "zu" -> 2
                "af" -> 1
                else -> 0
            }
        )

        // Set slider to saved volume
        musicvolumeSlider.value = savedVolume * musicvolumeSlider.valueTo // scale to slider max
        MusicPlayerManager.updateVolume(savedVolume)

        languagespinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                selectedLanguage = when (position) {
                    0 -> "en"
                    1 -> "af"
                    2 -> "zu"
                    else -> "en"
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listen to slider changes
        musicvolumeSlider.addOnChangeListener { _, value, _ ->
            val volume = value / musicvolumeSlider.valueTo // normalize to 0.0–1.0
            MusicPlayerManager.updateVolume(volume)
            prefs.edit().putFloat("music_volume", volume).apply()
        }
        // Load saved effects volume
        effectsvolumeSlider.value = savedEffectsVolume * effectsvolumeSlider.valueTo
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Listen for changes
        effectsvolumeSlider.addOnChangeListener { _, value, _ ->
            val volume = value / effectsvolumeSlider.valueTo // normalize 0.0–1.0
            SoundEffectsManager.updateVolume(volume)
            prefs.edit().putFloat("effects_volume", volume).apply()
        }

        // Load saved biometric setting (default = false)
        val bioEnabled = prefs.getBoolean("use_biometrics", false)
        bioswitch.isChecked = bioEnabled

        bioswitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("use_biometrics", isChecked).apply()
        }

        val username = arguments?.getString("username") ?: "Guest"

        btnsavesettings.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Save language preference
            prefs.edit().putString("language", selectedLanguage).apply()

            // Change app language (Android Developers, 2025; Firebsae, 2025)
            setLocale(selectedLanguage)

            // Navigate to GameMenuFragment (Android Developers, 2025; Firebsae, 2025)
            val gameMenuFragment = GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            replaceFragment(gameMenuFragment)
        }
    }

    // Helper method to replace fragment (Android Developers, 2025; Firebsae, 2025)
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        requireActivity().baseContext.resources.updateConfiguration(
            config, requireActivity().baseContext.resources.displayMetrics
        )

        // Force activity to refresh so UI language updates immediately
        requireActivity().recreate()
    }
}
/*
* Reference List
*
* Android Developers, 2025. AppCompatActivity. [online]. Available at:
* https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Fragment. [online]. Available at:
* https://developer.android.com/reference/androidx/fragment/app/Fragment
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. MediaPlayer. [online]. Available at:
* https://developer.android.com/reference/android/media/MediaPlayer
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. TextView. [online]. Available at:
* https://developer.android.com/reference/android/widget/TextView
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Toast. [online]. Available at:
* https://developer.android.com/reference/android/widget/Toast
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. SharedPreferences. [online]. Available at:
* https://developer.android.com/reference/android/content/SharedPreferences
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Configuration. [online]. Available at:
* https://developer.android.com/reference/android/content/res/Configuration
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Locale. [online]. Available at:
* https://developer.android.com/reference/java/util/Locale
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseAuth. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseDatabase. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/FirebaseDatabase
* [Accessed: 7 October 2025].
*/