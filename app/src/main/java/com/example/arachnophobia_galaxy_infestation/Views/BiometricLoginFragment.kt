package com.example.arachnophobia_galaxy_infestation

import android.content.Context.MODE_PRIVATE
import android.media.SoundPool
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BiometricLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricInfo: BiometricPrompt.PromptInfo
    private var clickbuttonSoundId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        return inflater.inflate(R.layout.fragment_biometric_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBiometricPrompt()
        val biometricsAvailable = checkBiometricAvailability()

        val btnBiometricLogin = view.findViewById<Button>(R.id.btnBiometricLogin)
        val btnback = view.findViewById<Button>(R.id.btnback)

        // Disable button if biometrics unavailable
        if (!biometricsAvailable) {
            btnBiometricLogin.isEnabled = false
        }

        // Initialize Sound Effects
        val soundPool = SoundPool.Builder().setMaxStreams(5).build()
        SoundEffectsManager.soundPool = soundPool

        if (SoundEffectsManager.soundPool == null) {
            SoundEffectsManager.soundPool = SoundPool.Builder().setMaxStreams(5).build()
            clickbuttonSoundId = SoundEffectsManager.soundPool!!.load(requireContext(), R.raw.button_click, 1)
        } else {
            if (clickbuttonSoundId == 0) {
                clickbuttonSoundId = SoundEffectsManager.soundPool!!.load(requireContext(), R.raw.button_click, 1)
            }
        }

        val prefs = requireActivity().getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        btnBiometricLogin.setOnClickListener {
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)
            biometricPrompt.authenticate(biometricInfo)
        }

        btnback.setOnClickListener {
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)
            replaceFragment(LoginHubFragment())
        }
    }

    // ---------------------------------------------------------
    //  CHECK BIOMETRIC HARDWARE + ENROLLMENT  (FIXED)
    // ---------------------------------------------------------
    private fun checkBiometricAvailability(): Boolean {

        val biometricManager = BiometricManager.from(requireContext())

        // Use only BIOMETRIC_STRONG (fingerprint, face)
        // BIOMETRIC_WEAK causes false "hardware unavailable" errors on some devices
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG

        return when (biometricManager.canAuthenticate(authenticators)) {

            BiometricManager.BIOMETRIC_SUCCESS -> {
                Toast.makeText(requireContext(), "Biometrics available", Toast.LENGTH_SHORT).show()
                true
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(requireContext(), "Fingerprint hardware not available", Toast.LENGTH_LONG).show()
                false
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(), "Biometric hardware temporarily unavailable", Toast.LENGTH_LONG).show()
                false
            }

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(requireContext(), "No fingerprint enrolled on this device", Toast.LENGTH_LONG).show()
                false
            }

            else -> {
                Toast.makeText(requireContext(), "Biometrics not supported on this device", Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    // ---------------------------------------------------------
    //  SETUP BIOMETRIC PROMPT (FIXED)
    // ---------------------------------------------------------
    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "Authenticated", Toast.LENGTH_SHORT).show()
                    loginToFirebase()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Fingerprint not recognized", Toast.LENGTH_SHORT).show()
                }
            })

        biometricInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Use your fingerprint to log in")
            .setNegativeButtonText("Cancel") // valid because we did NOT enable device credentials
            .build()
    }

    // ---------------------------------------------------------
    //  LOGIN TO FIREBASE AFTER BIOMETRIC SUCCESS
    // ---------------------------------------------------------
    private fun loginToFirebase() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "No Firebase session. Login required first.", Toast.LENGTH_LONG).show()
            return
        }

        val database = FirebaseDatabase.getInstance().reference
        val uid = user.uid

        database.child("players").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Firebase login successful", Toast.LENGTH_SHORT).show()
                    replaceFragment(GameMenuFragment())
                } else {
                    Toast.makeText(requireContext(), "User not found in database", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
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