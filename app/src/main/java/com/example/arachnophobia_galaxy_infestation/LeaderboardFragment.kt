package com.example.arachnophobia_galaxy_infestation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
 * Use the [LeaderboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LeaderboardFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    private val players = mutableListOf<HighScore>()

    private lateinit var loggedInUser: String

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
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }
    // Use this method to safely access views
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        recyclerView = view.findViewById(R.id.recyclerView)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Get username from arguments or fallback
        loggedInUser = arguments?.getString("username") ?: "Guest"
        // Load leaderboard data
        loadLeaderboard()

        // Set up back button
        btnBack.setOnClickListener {
            // Create a new instance of HighscoresMenuFragment with the username
            val highscoresMenuFragment = HighscoresMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", loggedInUser)
                }
            }
            // Navigate to high scores fragment
            replaceFragment(highscoresMenuFragment)
        }
    }

    private fun loadLeaderboard() {
        val dbRef = FirebaseDatabase.getInstance().getReference("highscores")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                players.clear()
                for (playerSnap in snapshot.children) {
                    val username = playerSnap.child("username").getValue(String::class.java) ?: ""
                    val highscore = playerSnap.child("highscore").getValue(Int::class.java) ?: 0

                    // Make sure you ADD instead of filtering only logged-in user
                    players.add(HighScore(username, highscore))
                }
                // Sort by score descending
                players.sortByDescending { it.score }

                adapter = LeaderboardAdapter(players, loggedInUser)
                recyclerView.adapter = adapter

                // Find logged-in user's position
                val position = players.indexOfFirst { it.username == loggedInUser }
                if (position != -1) {
                    recyclerView.scrollToPosition(position)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading leaderboard", Toast.LENGTH_SHORT).show()
            }
        })
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
            LeaderboardFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}