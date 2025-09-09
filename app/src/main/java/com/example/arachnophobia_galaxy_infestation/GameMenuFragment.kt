package com.example.arachnophobia_galaxy_infestation

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

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

        // Retrieve username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        usernameview.text = if (!username.isNullOrEmpty()) {
            "${username}"
        } else {
            "Guest"
        }

        usernameview.setOnClickListener {
            if (username.isNullOrBlank() || username.equals("Guest", ignoreCase = true)) {
                Toast.makeText(requireContext(), "Guest users cannot access profiles", Toast.LENGTH_SHORT).show()
            } else {
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
            // Navigate to game activity
            val intent = Intent(requireContext(), GameActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }

        btnhighscores.setOnClickListener {
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
            // Exit the app
            requireActivity().finish()
        }
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