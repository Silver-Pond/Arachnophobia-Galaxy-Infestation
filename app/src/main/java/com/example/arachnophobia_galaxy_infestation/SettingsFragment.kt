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
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var selectedLanguage: String = "en" // default

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
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

        // Set up spinner
        val options = arrayOf("English", "Afrikaans")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner, options)
        adapter.setDropDownViewResource(R.layout.item_spinner)
        languagespinner.adapter = adapter

        // Load saved preferences
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedLang = prefs.getString("language", "en") ?: "en"
        selectedLanguage = savedLang
        val savedVolume = prefs.getFloat("music_volume", 0.5f)

        // Set spinner selection based on saved language
        languagespinner.setSelection(
            when (savedLang) {
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

        val username = arguments?.getString("username") ?: "Guest"

        btnsavesettings.setOnClickListener {
            // Save language preference
            prefs.edit().putString("language", selectedLanguage).apply()

            // Change app language
            setLocale(selectedLanguage)

            // Navigate to GameMenuFragment
            val gameMenuFragment = GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            replaceFragment(gameMenuFragment)
        }
    }

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

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}