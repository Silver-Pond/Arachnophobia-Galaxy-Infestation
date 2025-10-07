package com.example.arachnophobia_galaxy_infestation

import android.content.Context.MODE_PRIVATE
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HighscoresMenuFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HighscoresMenuFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var clickbuttonSoundId: Int = 0

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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_highscores_menu, container, false)
    }

    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now you can find views by ID
        val btnarcadehighscore = view.findViewById<Button>(R.id.btnarcadehighscore)
        val btnsurvivalhighscore = view.findViewById<Button>(R.id.btnsurvivalhighscore)
        val btnback = view.findViewById<Button>(R.id.btnback)

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

        // Restore effects volume from prefs (Android Developers, 2025; Firebsae, 2025)
        val prefs = requireActivity().getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Retrieve username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        // Set up click listeners for buttons
        btnarcadehighscore.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Create a new instance of LeaderboardFragment with the username (Android Developers, 2025; Firebsae, 2025)
            val leaderboardFragment = LeaderboardFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            // Navigate to high scores fragment
            replaceFragment(leaderboardFragment)
        }

        btnsurvivalhighscore.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Create a new instance of SurvivalLeaderboardFragment with the username (Android Developers, 2025; Firebsae, 2025)
            val survivalLeaderboardFragment = SurvivalLeaderboardFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            // Navigate to high scores fragment (Android Developers, 2025; Firebsae, 2025)
            replaceFragment(survivalLeaderboardFragment)
        }

        btnback.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Create a new instance of GameMenuFragment with the username
            val gameMenuFragment = GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            // Navigate to profile fragment (Android Developers, 2025; Firebsae, 2025)
            replaceFragment(gameMenuFragment)
        }
    }

    // Helper method to replace fragment (Android Developers, 2025; Firebsae, 2025)
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HighscoresMenuFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
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