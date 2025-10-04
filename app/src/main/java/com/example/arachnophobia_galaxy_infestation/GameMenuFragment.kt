package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Paint
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GameMenuFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GameMenuFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_game_menu, container, false)
    }

    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now you can find views by ID
        val usernameview = view.findViewById<TextView>(R.id.usernameview)
        val btnarcademode = view.findViewById<Button>(R.id.btnarcademode)
        val btnsurvivalmode = view.findViewById<Button>(R.id.btnsurvivalmode)
        val btnhighscores = view.findViewById<Button>(R.id.btnhighscores)
        val btntrophies = view.findViewById<Button>(R.id.btntrophies)
        val btnsettings = view.findViewById<Button>(R.id.btnsettings)
        val btnexit = view.findViewById<Button>(R.id.btnexit)

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

        // Restore effects volume from prefs
        val prefs = requireActivity().getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Retrieve username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        usernameview.text = if (!username.isNullOrEmpty()) {
            "${username}"
        } else {
            "Guest"
        }

        usernameview.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            if (username.isNullOrBlank() || username.equals("Guest", ignoreCase = true)) {
                Toast.makeText(requireContext(), "Guest users cannot access profiles", Toast.LENGTH_SHORT).show()
            } else {
                // Underline the TextView when clicked
                usernameview.paintFlags = usernameview.paintFlags or Paint.UNDERLINE_TEXT_FLAG

                // Create a new instance of ProfileFragment with the username
                val profileFragment = ProfileFragment().apply {
                    arguments = Bundle().apply {
                        putString("username", username)
                    }
                }
                // Navigate to profile fragment
                replaceFragment(profileFragment)
            }
        }

        btnarcademode.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Navigate to game activity
            val intent = Intent(requireContext(), GameActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        btnsurvivalmode.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            val username = arguments?.getString("username") ?: "Guest"

            // Disable button temporarily to prevent multiple clicks
            btnsurvivalmode.isEnabled = false

            // Try to connect to API first
            ApiClient.instance.getLevel(1).enqueue(object : Callback<Level> {
                override fun onResponse(call: Call<Level>, response: Response<Level>) {
                    btnsurvivalmode.isEnabled = true
                    if (response.isSuccessful) {
                        // Connected successfully → start the game
                        val intent = Intent(requireContext(), GameActivity::class.java)
                        intent.putExtra("username", username)
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Unable to start Survival Mode: Server error.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<Level>, t: Throwable) {
                    btnsurvivalmode.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        "Cannot connect to server. Please check your connection.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }

        btnhighscores.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Create a new instance of HighscoresMenuFragment with the username
            val highscoresMenuFragment = HighscoresMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            // Navigate to high scores fragment
            replaceFragment(highscoresMenuFragment)
        }

        btntrophies.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Create a new instance of TrophiesFragment with the username
            val trophiesFragment = TrophiesFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            // Navigate to trophies fragment
            replaceFragment(trophiesFragment)
        }

        btnsettings.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Create a new instance of LeaderboardFragment with the username
            val settingsFragment = SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            // Navigate to settings fragment
            replaceFragment(settingsFragment)
        }

        btnexit.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Logout from Firebase
            val auth = FirebaseAuth.getInstance()
            auth.signOut() // Firebase logout

            // Clear saved user data
            val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            with(prefs.edit()) {
                putBoolean("is_logged_in", false) // mark as logged out
                remove("email")
                remove("password")
                remove("username")
                apply()
            }
            // Exit the app
            requireActivity().finishAffinity()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        SoundEffectsManager.soundPool?.release()
        SoundEffectsManager.soundPool = null
    }

    // Helper method to replace fragment
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}