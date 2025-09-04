package com.example.arachnophobia_galaxy_infestation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        val btnprofilechange = view.findViewById<Button>(R.id.btnprofilechange)
        val btnskin = view.findViewById<Button>(R.id.btnskin)
        val btnback = view.findViewById<Button>(R.id.btnback)
        val btnlogout = view.findViewById<Button>(R.id.btnlogout)

        // Set username
        profileusernameview.text = if (!username.isNullOrEmpty()) {
            "${username}"
        } else {
            "Guest"
        }
        // Load spider silk
        loadSpiderSilk()

        // Set up click listeners
        btnprofilechange.setOnClickListener {}

        btnskin.setOnClickListener {}

        btnback.setOnClickListener {
            // Navigate to GameMenuFragment
            val gameMenuFragment = GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", username)
                }
            }
            replaceFragment(gameMenuFragment)
        }

        btnlogout.setOnClickListener {
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
            // Schedule reminders since user is now logged out
            scheduleReminder()

            // Navigate back to login screen
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

    private fun scheduleReminder() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel existing alarms
        alarmManager.cancel(pendingIntent)

        // Schedule repeating alarm every 30 minutes
        val intervalMillis = 30 * 60 * 1000L // 30 minutes
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            pendingIntent
        )
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
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}