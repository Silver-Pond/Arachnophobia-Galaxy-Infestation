package com.example.arachnophobia_galaxy_infestation

import android.content.Context.MODE_PRIVATE
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class BiometricLoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var biometricInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_biometric_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBiometricPrompt()
        checkBiometricAvailability(view)

        // Initialize SoundPool once (Android Developers, 2025; Firebsae, 2025)
        val btnFaceLogin = view.findViewById<Button>(R.id.btnFaceLogin)

        btnFaceLogin.setOnClickListener {
            biometricPrompt.authenticate(biometricInfo)
        }
    }

    // -------------------------------------------------
    // BIOMETRIC PROMPT SETUP
    // -------------------------------------------------

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())

        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(requireContext(), "Face Verified", Toast.LENGTH_SHORT).show()
                    signInWithFirebase()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(requireContext(), "Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(requireContext(), "Face Not Recognized", Toast.LENGTH_SHORT).show()
                }
            }
        )

        biometricInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with Facial Recognition")
            .setSubtitle("Authenticate to access your account")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
    }

    // -------------------------------------------------
    // CHECK BIOMETRIC HARDWARE
    // -------------------------------------------------

    private fun checkBiometricAvailability(view: View) {

        val txtStatus = view.findViewById<TextView>(R.id.txtStatus)
        val biometricManager = BiometricManager.from(requireContext())

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {

            BiometricManager.BIOMETRIC_SUCCESS ->
                txtStatus.text = "Facial Recognition Available"

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                txtStatus.text = "No biometric hardware detected"

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                txtStatus.text = "Biometric features currently unavailable"

            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                txtStatus.text = "No face or biometric enrolled"

            else -> txtStatus.text = "Biometrics unavailable"
        }
    }

    // -------------------------------------------------
    // FIREBASE LOGIN + DATABASE FETCH
    // -------------------------------------------------

    private fun signInWithFirebase() {
        val user = auth.currentUser

        if (user == null) {
            Toast.makeText(requireContext(), "No Firebase session found", Toast.LENGTH_SHORT).show()
            return
        }

        loadUserData(user)
    }

    private fun loadUserData(user: FirebaseUser) {
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(user.uid)

        dbRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").value
                Toast.makeText(requireContext(), "Welcome $username!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
            }

        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Database error", Toast.LENGTH_SHORT).show()
            Log.e("BiometricLogin", "Firebase error", e)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .addToBackStack(null)
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