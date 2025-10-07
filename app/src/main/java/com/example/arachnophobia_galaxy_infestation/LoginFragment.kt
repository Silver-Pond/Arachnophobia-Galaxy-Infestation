package com.example.arachnophobia_galaxy_infestation

import android.app.AlarmManager
import android.app.PendingIntent
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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.concurrent.TimeUnit

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailInput = view.findViewById<EditText>(R.id.emailLoginInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordLoginInput)
        val signUpText = view.findViewById<TextView>(R.id.loginText)
        val btnlogin = view.findViewById<Button>(R.id.btnLogin)
        val btnback = view.findViewById<Button>(R.id.btnBack)

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

        // Underline the sign-up text
        signUpText.paintFlags = signUpText.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Navigate to SignUpFragment (Android Developers, 2025; Firebsae, 2025)
        signUpText.setOnClickListener {
            replaceFragment(SignUpFragment())
        }

        btnlogin.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val auth = FirebaseAuth.getInstance()
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val uid = firebaseUser?.uid

                        if (uid != null) {
                            val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

                            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val username = snapshot.child("username").getValue(String::class.java) ?: "Player"

                                        // Save login details for biometric login (Android Developers, 2025; Firebsae, 2025)
                                        val sharedPref = requireActivity()
                                            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                        with(sharedPref.edit()) {
                                            putString("email", email)
                                            putString("password", password)
                                            putString("username", username) // optional
                                            apply()
                                        }
                                        Toast.makeText(requireContext(), "Welcome $username", Toast.LENGTH_SHORT).show()

                                        // Navigate to GameMenuFragment
                                        val gameMenuFragment = GameMenuFragment().apply {
                                            arguments = Bundle().apply {
                                                putString("username", username)
                                            }
                                        }
                                        replaceFragment(gameMenuFragment)
                                    } else {
                                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    } else {
                        Toast.makeText(requireContext(), "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnback.setOnClickListener{
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            // Navigate to LoginHubFragment (Android Developers, 2025; Firebsae, 2025)
            replaceFragment(LoginHubFragment())
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
            LoginFragment().apply {
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