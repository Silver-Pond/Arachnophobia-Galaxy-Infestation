package com.example.arachnophobia_galaxy_infestation

import android.graphics.Paint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now you can find views by ID
        val emailInput = view.findViewById<EditText>(R.id.emailLoginInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordLoginInput)
        val signUpText = view.findViewById<TextView>(R.id.loginText)
        val btnlogin = view.findViewById<Button>(R.id.btnLogin)

        // Underline textView
        signUpText.paintFlags = signUpText.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // You can now set up click listeners, etc.
        signUpText.setOnClickListener {
            // Navigate to the signup fragment
            replaceFragment(SignUpFragment())
        }
        // You can now set up click listeners, etc.
        btnlogin.setOnClickListener {
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
                            // Retrieve username (or any extra info) from Realtime Database
                            val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

                            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val username = snapshot.child("username").getValue(String::class.java) ?: "Player"

                                        Toast.makeText(requireContext(), "Welcome $username", Toast.LENGTH_SHORT).show()

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
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}