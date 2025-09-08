package com.example.arachnophobia_galaxy_infestation

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
 * Use the [TrophiesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TrophiesFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TrophyAdapter
    private val trophies = mutableListOf<Trophy>()
    private val userTrophies = mutableSetOf<String>() // store earned trophies
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
        return inflater.inflate(R.layout.fragment_trophies, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        recyclerView = view.findViewById(R.id.recyclerViewTrophies)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = TrophyAdapter(trophies, userTrophies) { trophy ->
            showDescriptionDialog(trophy)
        }
        recyclerView.adapter = adapter

        fetchUserTrophies()

        // Get username from arguments or fallback
        loggedInUser = arguments?.getString("username") ?: "Guest"

        // Set up back button
        btnBack.setOnClickListener {
            // Create a new instance of GameMenuFragment with the username
            val gameMenuFragment = GameMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("username", loggedInUser)
                }
            }
            // Navigate to high scores fragment
            replaceFragment(gameMenuFragment)
        }
    }

    private fun fetchUserTrophies() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid).child("trophies")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userTrophies.clear()
                snapshot.children.forEach { child ->
                    val trophyId = child.key
                    if (trophyId != null && child.getValue(Boolean::class.java) == true) {
                        userTrophies.add(trophyId)
                    }
                }
                fetchAllTrophies()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchAllTrophies() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Arachnotrophies")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trophies.clear()
                snapshot.children.forEach { child ->
                    val trophy = child.getValue(Trophy::class.java)
                    if (trophy != null) {
                        trophies.add(trophy)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showDescriptionDialog(trophy: Trophy) {
        AlertDialog.Builder(requireContext())
            .setTitle(trophy.name)
            .setMessage(trophy.description)
            .setPositiveButton("OK", null)
            .show()
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
            TrophiesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}