package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// TODO: Rename parameter arguments, choose names that match
class SkinsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skinAdapter: SkinAdapter
    private lateinit var player: Player
    private lateinit var loggedInUser: String
    private val skins = mutableListOf<Skin>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_skins, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load player (replace with real player data from database or SharedPreferences)
        loadPlayer()

        // Initialize views
        val btnBack = view.findViewById<Button>(R.id.btnBack)
        recyclerView = view.findViewById(R.id.skinsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        skinAdapter = SkinAdapter(skins, player, ::onSkinAction)
        recyclerView.adapter = skinAdapter

        // Get username from arguments or fallback
        loggedInUser = arguments?.getString("username") ?: "Guest"

        // Load skins from JSON (hardcoded here, but could be from assets)
        loadSkinsFromJson()

        // Set up back button
        btnBack.setOnClickListener {
            // Create a new instance of ProfileFragment with the username
            val profileFragment = ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString("username", loggedInUser)
                }
            }
            // Navigate to profile fragment
            replaceFragment(profileFragment)
        }
    }

    private fun loadPlayer() {
        // Replace with real data fetching; using defaults for demo
        player = Player()
    }

    private fun loadSkinsFromJson() {
        // Normally you'd parse JSON from assets. We'll hardcode for now:
        skins.addAll(
            listOf(
                Skin("skin01","Moth",0.0,"moth"),
                Skin("skin02","Bee",299.99,"bee"),
                Skin("skin03","Butterfly",499.99,"butterfly"),
                Skin("skin04","Super Mario",0.0,"super_mario"),
                Skin("skin05","Space Invader",0.0,"space_invader"),
                Skin("skin06","Fly",399.99,"fly"),
                Skin("skin07","Firefly",599.99,"firefly"),
                Skin("skin08","Ladybug",799.99,"ladybug"),
            )
        )
        skinAdapter.notifyDataSetChanged()
    }

    private fun onSkinAction(skin: Skin) {
        val prefs = requireContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

        if (player.ownedSkins.contains(skin.name)) {
            // Equip skin
            player = player.copy(equippedSkin = skin.name)
            prefs.edit().putString("equippedSkin", skin.name).apply()

            Toast.makeText(requireContext(), "${skin.name} equipped!", Toast.LENGTH_SHORT).show()
        } else {
            // Attempt to buy
            if (player.spider_silk >= skin.price) {
                player = player.copy(
                    spider_silk = player.spider_silk - skin.price,
                    ownedSkins = player.ownedSkins + skin.name
                )
                Toast.makeText(requireContext(), "You bought ${skin.name}!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Not enough spider silk!", Toast.LENGTH_SHORT).show()
            }
        }
        skinAdapter.updatePlayer(player)
    }

    // Helper method to replace fragment
    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }
}