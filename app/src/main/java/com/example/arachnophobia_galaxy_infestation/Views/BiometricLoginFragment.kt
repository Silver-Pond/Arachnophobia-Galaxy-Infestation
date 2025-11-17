package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.media.SoundPool
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class BiometricLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricInfo: BiometricPrompt.PromptInfo
    private var clickbuttonSoundId: Int = 0

    // Keys for encrypted prefs
    private val PREF_FILE_NAME = "secure_prefs"
    private val KEY_EMAIL = "saved_email"
    private val KEY_PASSWORD = "saved_password"

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

        // Views
        val btnBiometricLogin = view.findViewById<Button>(R.id.btnBiometricLogin)
        val btnback = view.findViewById<Button>(R.id.btnBack)
        val fingerprint = view.findViewById<ImageView>(R.id.fingerprint)

        if (!biometricsAvailable) {
            btnBiometricLogin.isEnabled = false
            fingerprint.isEnabled = false
            fingerprint.alpha = 0.5f
        }

        // Initialize SoundPool / SoundEffectsManager (your existing manager)
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

        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        btnBiometricLogin.setOnClickListener {
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)
            animateFingerprint(fingerprint)
            biometricPrompt.authenticate(biometricInfo)
        }

        fingerprint.setOnClickListener {
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)
            animateFingerprint(fingerprint)
            biometricPrompt.authenticate(biometricInfo)
        }

        btnback.setOnClickListener {
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)
            replaceFragment(LoginHubFragment())
        }
    }

    // ---------------------------------------------------------
    //  CHECK BIOMETRIC HARDWARE + ENROLLMENT
    // ---------------------------------------------------------
    private fun checkBiometricAvailability(): Boolean {
        val biometricManager = BiometricManager.from(requireContext())

        // Prefer BIOMETRIC_STRONG for reliability
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                Toast.makeText(requireContext(), "Biometrics available", Toast.LENGTH_SHORT).show(); true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(requireContext(), "Fingerprint hardware not available", Toast.LENGTH_LONG).show(); false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(requireContext(), "Biometric hardware temporarily unavailable", Toast.LENGTH_LONG).show(); false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(requireContext(), "No fingerprint enrolled on this device", Toast.LENGTH_LONG).show(); false
            }
            else -> {
                Toast.makeText(requireContext(), "Biometrics not supported on this device", Toast.LENGTH_LONG).show(); false
            }
        }
    }

    // ---------------------------------------------------------
    //  SETUP BIOMETRIC PROMPT
    // ---------------------------------------------------------
    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "Authenticated", Toast.LENGTH_SHORT).show()
                    handlePostBiometricAuth()
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
            .setSubtitle("Use fingerprint or face to log in")
            .setNegativeButtonText("Cancel")
            .build()
    }

    // ---------------------------------------------------------
    //  After biometric success: ensure a Firebase session exists
    // ---------------------------------------------------------
    private fun handlePostBiometricAuth() {
        // 1) If already signed in: proceed
        val current = auth.currentUser
        if (current != null) {
            // already signed in — continue to DB lookup
            loginToFirebase(current.uid)
            return
        }

        // 2) Try to sign in using encrypted stored credentials (email + password)
        val creds = loadEncryptedCredentials()
        if (creds != null) {
            val (email, password) = creds
            // Attempt sign-in with saved credentials
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // success — now we have a Firebase user
                        val user = auth.currentUser
                        if (user != null) {
                            Toast.makeText(requireContext(), "Signed in with saved account", Toast.LENGTH_SHORT).show()
                            loginToFirebase(user.uid)
                        } else {
                            // unexpected: treat as failure
                            Toast.makeText(requireContext(), "Signed in but no user found", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // saved credentials failed (maybe password changed). Fall back to anonymous or prompt.
                        Toast.makeText(requireContext(), "Saved credentials invalid. Please login manually.", Toast.LENGTH_LONG).show()
                        // OPTIONAL: fallback to anonymous sign-in (uncomment if desired)
                        // signInAnonymouslyAndContinue()
                    }
                }
            return
        }

        // 3) No stored credentials — fallback: anonymous login OR instruct user to login
        // I will perform anonymous sign-in by default so user can continue, then you can
        // connect anonymous account to an email later if you wish.
        signInAnonymouslyAndContinue()
    }

    // ---------------------------------------------------------
    //  Sign-in anonymously then continue (optional)
    // ---------------------------------------------------------
    private fun signInAnonymouslyAndContinue() {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        Toast.makeText(requireContext(), "Signed in anonymously", Toast.LENGTH_SHORT).show()
                        loginToFirebase(user.uid)
                    } else {
                        Toast.makeText(requireContext(), "Anonymous sign in succeeded but no user", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Anonymous sign in failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // ---------------------------------------------------------
    //  LOGIN TO FIREBASE AFTER WE HAVE UID
    // ---------------------------------------------------------
    private fun loginToFirebase(uid: String) {
        val database = FirebaseDatabase.getInstance().reference

        database.child("players").child(uid).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(requireContext(), "Firebase login successful", Toast.LENGTH_SHORT).show()
                    replaceFragment(GameMenuFragment())
                } else {
                    // If anonymous user and no players node exists, create basic data or redirect to setup
                    if (auth.currentUser?.isAnonymous == true) {
                        Toast.makeText(requireContext(), "Welcome — setting up new player profile", Toast.LENGTH_SHORT).show()
                        // create minimal player node if you want:
                        val playerData = mapOf("createdAt" to System.currentTimeMillis())
                        database.child("players").child(uid).setValue(playerData)
                            .addOnSuccessListener { replaceFragment(GameMenuFragment()) }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Error creating profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "User not found in database", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------------------------------------------------------
    //  Encrypted preferences: store/read email & password
    //  (Use this only if the user explicitly allowed saving credentials)
    // ---------------------------------------------------------
    private fun loadEncryptedCredentials(): Pair<String, String>? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREF_FILE_NAME,
                masterKeyAlias,
                requireContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val email = sharedPreferences.getString(KEY_EMAIL, null)
            val password = sharedPreferences.getString(KEY_PASSWORD, null)

            if (!email.isNullOrBlank() && !password.isNullOrBlank()) {
                Pair(email, password)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("BiometricLogin", "Error reading encrypted prefs: ${e.message}")
            null
        }
    }

    // Helper method to save credentials (call this from your regular login flow if you want)
    // Save only with explicit user consent.
    private fun saveEncryptedCredentials(email: String, password: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                PREF_FILE_NAME,
                masterKeyAlias,
                requireContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().putString(KEY_EMAIL, email).putString(KEY_PASSWORD, password).apply()
        } catch (e: Exception) {
            Log.e("BiometricLogin", "Error saving encrypted prefs: ${e.message}")
        }
    }

    // Helper method to replace fragment
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }

    // Tiny fingerprint animation
    private fun animateFingerprint(image: ImageView) {
        image.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(150)
            .withEndAction {
                image.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(150)
                    .start()
            }
            .start()
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