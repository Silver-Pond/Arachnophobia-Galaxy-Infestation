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
import com.google.firebase.database.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SignUpFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SignUpFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Now you can find views by ID
        val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
        val emailInput = view.findViewById<EditText>(R.id.emailInput)
        val passwordInput = view.findViewById<EditText>(R.id.passwordInput)
        val loginText = view.findViewById<TextView>(R.id.loginText)
        val btnsignup = view.findViewById<Button>(R.id.btnSignUp)

        // Underline textView
        loginText.paintFlags = loginText.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        // Navigate to LoginFragment when loginText is clicked
        loginText.setOnClickListener {
            // Navigate to LoginFragment
            replaceFragment(LoginFragment())
        }

        // You can now set up click listeners, etc.
        btnsignup.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            // Validate inputs
            if (!usernameValidation(username)) {
                Toast.makeText(requireContext(), "Username Invalid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!emailValidation(email)) {
                Toast.makeText(requireContext(), "Email Invalid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!passwordValidation(password)) {
                Toast.makeText(requireContext(), "Password Invalid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Create user
            val dbRef = FirebaseDatabase.getInstance().getReference("players")

            // Check if username already exists
            dbRef.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(requireContext(), "Username already in use", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Create user object
                    val user = mapOf(
                        "username" to username,
                        "email" to email,
                        "password" to password
                    )

                    // Save new user
                    dbRef.child(username).setValue(user)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "User created successfully", Toast.LENGTH_SHORT).show()
                            replaceFragment(LoginFragment()) // Go back to login
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    // Username validation
    private fun usernameValidation(username: String): Boolean{
        return username.matches(".*[A-Z].*".toRegex())
                && username.matches(".*[a-z].*".toRegex())
                && username.matches(".*[0-9].*".toRegex())
                && username.matches(".*[a-zA-Z.? ].*".toRegex())
    }
    // Email validation
    private fun emailValidation(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    // Password validation
    private fun passwordValidation(password: String): Boolean{
        return password.length >= 8
                && password.matches(".*[A-Z].*".toRegex())
                && password.matches(".*[a-z].*".toRegex())
                && password.matches(".*[0-9].*".toRegex())
                && password.matches(".*[a-zA-Z.? ].*".toRegex())
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
            SignUpFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}