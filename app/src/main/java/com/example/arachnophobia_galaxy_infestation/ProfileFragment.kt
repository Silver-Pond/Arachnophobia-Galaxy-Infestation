package com.example.arachnophobia_galaxy_infestation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val username = arguments?.getString("username") ?: "Guest"
        val profileusernameview = view.findViewById<TextView>(R.id.profileusernameview)
        // val btnprofilechange = view.findViewById<Button>(R.id.btnprofilechange)
        val btnskin = view.findViewById<Button>(R.id.btnskin)
        val btnback = view.findViewById<Button>(R.id.btnback)
        val btnlogout = view.findViewById<Button>(R.id.btnlogout)

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

        // Set username
        profileusernameview.text = if (!username.isNullOrEmpty()) {
            "${username}"
        } else {
            "Guest"
        }
        // Load spider silk (Android Developers, 2025; Firebsae, 2025)
        loadSpiderSilk()

        // Set up click listeners
        // btnprofilechange.setOnClickListener {}

        btnskin.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Navigate to SkinsFragment
            val skinsFragment = SkinsFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            replaceFragment(skinsFragment)
        }

        btnback.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Navigate to GameMenuFragment
            val gameMenuFragment = GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            replaceFragment(gameMenuFragment)
        }

        btnlogout.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Logout from Firebase (Android Developers, 2025; Firebsae, 2025)
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

            // Navigate back to login screen (Android Developers, 2025; Firebsae, 2025)
            replaceFragment(LoginHubFragment())
        }
    }

    private fun loadSpiderSilk() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.child("spider_silk").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val spiderSilk = snapshot.getValue(Int::class.java) ?: 0
                val spiderSilkTextView: TextView = requireActivity().findViewById(R.id.spiderSilkTextView)
                spiderSilkTextView.text = "SPIDER SILK: $spiderSilk"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load spider silk: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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
            ProfileFragment().apply {
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