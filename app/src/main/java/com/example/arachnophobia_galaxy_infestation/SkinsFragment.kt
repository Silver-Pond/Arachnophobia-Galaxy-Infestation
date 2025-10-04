package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.media.SoundPool
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// TODO: Rename parameter arguments, choose names that match
class SkinsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var skinAdapter: SkinAdapter
    private lateinit var player: Player
    private lateinit var loggedInUser: String
    private var clickbuttonSoundId: Int = 0
    private val skins = mutableListOf<Skin>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_skins, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.skinsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Load skins
        loadSkinsFromJson()

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

        // Get username from arguments
        loggedInUser = arguments?.getString("username") ?: "Guest"

        // Load player from Firebase
        loadPlayer {
            // Initialize adapter only after player loads
            skinAdapter = SkinAdapter(skins, player, ::onSkinAction)
            recyclerView.adapter = skinAdapter
        }

        // Back button
        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            // Play button click sound
            if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

            replaceFragment(ProfileFragment().apply {
                arguments = Bundle().apply { putString("username", loggedInUser) }
            })
        }
    }

    private fun loadPlayer(onLoaded: () -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
            player = Player()
            onLoaded()
            return
        }

        val uid = currentUser.uid
        val playerRef = FirebaseDatabase.getInstance()
            .getReference("players")
            .child(uid)

        playerRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val map = snapshot.value as? Map<*, *>

                // Parse spider_silk safely
                val silk = when (val v = map?.get("spider_silk")) {
                    is Long -> v.toDouble()
                    is Double -> v
                    is String -> v.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }

                // Parse ownedSkins safely
                val owned = (map?.get("ownedSkins") as? List<*>)?.map { it.toString() }
                    ?: listOf("Moth", "Super Mario", "Space Invader")

                // Parse equippedSkin safely
                val equipped = map?.get("equippedSkin")?.toString() ?: "Moth"

                player = Player(
                    id = uid,
                    username = currentUser.displayName ?: "Player",
                    email = currentUser.email ?: "",
                    spider_silk = silk,
                    ownedSkins = owned,
                    equippedSkin = equipped
                )
            } else {
                player = Player(
                    id = uid,
                    username = currentUser.displayName ?: "Player",
                    email = currentUser.email ?: ""
                )
            }
            onLoaded()
        }.addOnFailureListener {
            player = Player(
                id = currentUser.uid,
                username = currentUser.displayName ?: "Player",
                email = currentUser.email ?: ""
            )
            onLoaded()
        }
    }

    private fun loadSkinsFromJson() {
        skins.addAll(
            listOf(
                Skin("skin01","Moth",0.0,"moth"),
                Skin("skin02","Bee",299.99,"bee"),
                Skin("skin03","Butterfly",499.99,"butterfly"),
                Skin("skin04","Super Mario",0.0,"super_mario"),
                Skin("skin05","Space Invader",0.0,"space_invader"),
                Skin("skin06","Fly",399.99,"fly"),
                Skin("skin07","Firefly",599.99,"firefly"),
                Skin("skin08","Ladybug",799.99,"ladybug")
            )
        )
    }

    private fun onSkinAction(skin: Skin) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val playerRef = FirebaseDatabase.getInstance()
            .getReference("players")
            .child(currentUser.uid)

        val ownedSkins = player.ownedSkins ?: emptyList()

        if (ownedSkins.contains(skin.name)) {
            // Equip skin
            player = player.copy(equippedSkin = skin.name)
            playerRef.child("equippedSkin").setValue(skin.name)
            saveEquippedSkinLocally(skin.name)
            Toast.makeText(requireContext(), "${skin.name} equipped!", Toast.LENGTH_SHORT).show()
            skinAdapter.updatePlayer(player)
        } else {
            // Buy skin
            val playerSilk = player.spider_silk.toDouble()
            val skinPrice = skin.price.toDouble()

            if (playerSilk >= skinPrice) {
                val newSilk = playerSilk - skinPrice
                val newOwned = ownedSkins + skin.name

                // Set spider_silk explicitly as Double
                playerRef.child("spider_silk").setValue(newSilk).addOnSuccessListener {
                    playerRef.child("ownedSkins").setValue(newOwned).addOnSuccessListener {
                        player = player.copy(spider_silk = newSilk, ownedSkins = newOwned)
                        Toast.makeText(requireContext(), "You bought ${skin.name}!", Toast.LENGTH_SHORT).show()
                        skinAdapter.updatePlayer(player)
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Failed to update owned skins", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update spider silk", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Not enough spider silk!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEquippedSkinLocally(skinName: String) {
        val prefs = requireContext().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("equippedSkin", skinName).apply()
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main, fragment)
            .commit()
    }
}