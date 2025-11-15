package com.example.arachnophobia_galaxy_infestation

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginHubFragment : Fragment() {
    private var clickbuttonSoundId: Int = 0
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In (Android Developers, 2025; Firebsae, 2025)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_hub, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val btngooglelogin = view.findViewById<Button>(R.id.btngooglelogin)
        val btnlogin = view.findViewById<Button>(R.id.btnlogin)
        val btnbiologin = view.findViewById<Button>(R.id.btnbiologin)
        val btnguest = view.findViewById<Button>(R.id.btnguest)

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

        // Check if user is already signed in
        val useBiometrics = prefs.getBoolean("use_biometrics", false)

        btngooglelogin.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            signInWithGoogle()
        }

        btnlogin.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            replaceFragment(LoginFragment())
        }
        // Hide or show the button based on preference (Android Developers, 2025; Firebsae, 2025)
        btnbiologin.visibility = if (useBiometrics) View.VISIBLE else View.GONE

        btnbiologin.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            replaceFragment(BiometricLoginFragment())
        }

        btnguest.setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            replaceFragment(GameMenuFragment())
        }
    }

    private fun signInWithGoogle() {
        // First, sign out any existing Google account to force the chooser to appear again (Android Developers, 2025; Firebsae, 2025)
        googleSignInClient.signOut().addOnCompleteListener {
            // After sign-out completes, show the Google Account picker again (Android Developers, 2025; Firebsae, 2025)
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val database = FirebaseDatabase.getInstance().getReference("players")
                        val playerId = it.uid

                        database.child(playerId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    val existingUsername = snapshot.child("username").getValue(String::class.java)
                                        ?: it.displayName ?: "Guest"

                                    val gameMenuFragment = GameMenuFragment().apply {
                                        arguments = Bundle().apply {
                                            putString("username", existingUsername)
                                        }
                                    }
                                    replaceFragment(gameMenuFragment)
                                } else {
                                    val player = Player(
                                        username = it.displayName ?: "Guest",
                                        email = it.email ?: "No Email",
                                        password = "N/A",
                                        highscore = 0,
                                        survivalhighscore = 0,
                                        spider_silk = 0.00,
                                        trophies = emptyList(),
                                        ownedSkins = listOf("Moth", "Mario", "Invader"),
                                        equippedSkin = "Moth"
                                    )
                                    database.child(playerId).setValue(player)
                                        .addOnSuccessListener {
                                            val gameMenuFragment = GameMenuFragment().apply {
                                                arguments = Bundle().apply {
                                                    putString("username", player.username)
                                                }
                                            }
                                            replaceFragment(gameMenuFragment)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(requireContext(), "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                } else {
                    Toast.makeText(requireContext(), "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Helper method to replace fragment (Android Developers, 2025; Firebsae, 2025)
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