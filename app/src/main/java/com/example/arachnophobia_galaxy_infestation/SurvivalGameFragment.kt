package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.max
import kotlin.math.min

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SurvivalGameFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SurvivalGameFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var player: ImageView
    private lateinit var gameArea: FrameLayout
    private lateinit var pauseText: TextView
    private lateinit var livesText: TextView

    private var playerX = 0f
    private var playerY = 0f
    private val moveStep = 40f
    private var isPaused = false
    private var lives = 3
    private var score = 0
    private val handler = Handler(Looper.getMainLooper())
    private val bullets = mutableListOf<ImageView>()
    private var username: String? = null
    private var level: Level? = null

    private var currentBulletDrawable: String = "moth_blast"
    private var bulletSpeed = 15f
    private var shootSoundId: Int = 0
    private var enemyfireId: Int = 0
    private var enemykilledId: Int = 0
    private var explosionId: Int = 0
    private var gameOverSoundId: Int = 0

    private val scoreText: TextView
        get() = requireActivity().findViewById(R.id.scoreText)
    private val highScoreText: TextView
        get() = requireActivity().findViewById(R.id.highscoreText)

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isAdded && view != null && !isPaused) {
                updateGame()
            }
            // Re-post ONLY if still added and the view exists
            if (isAdded && view != null) {
                handler.postDelayed(this, 16)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)

            username = it.getString("username")

            val levelJson = it.getString("levelData")
            if (!levelJson.isNullOrEmpty()) {
                level = Gson().fromJson(levelJson, Level::class.java)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_survival_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        player = view.findViewById(R.id.player)
        gameArea = view.findViewById(R.id.mainGameFrame)
        pauseText = view.findViewById(R.id.pauseText)
        livesText = view.findViewById(R.id.livesText)

        // Initialize SoundPool once
        val soundPool = SoundPool.Builder().setMaxStreams(5).build()
        SoundEffectsManager.soundPool = soundPool

        // Load shooting sound
        shootSoundId = soundPool.load(requireContext(), R.raw.cannon_shot, 1)

        // Load enemy shooting sound
        enemyfireId = soundPool.load(requireContext(), R.raw.shoot, 1)

        // Load enemy killed sound
        enemykilledId = soundPool.load(requireContext(), R.raw.invaderkilled, 1)

        // Load explosion sound
        explosionId = soundPool.load(requireContext(), R.raw.explosion, 1)

        // Load game over sound
        gameOverSoundId = soundPool.load(requireContext(), R.raw.game_over, 1)

        // Restore effects volume from prefs
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Apply skin once here
        applyEquippedSkin()

        // Example: load level 1 from API
        ApiClient.instance.getLevel(1).enqueue(object : Callback<Level> {
            override fun onResponse(call: Call<Level>, response: Response<Level>) {
                if (response.isSuccessful) {
                    response.body()?.let { loadedLevel ->
                        level = loadedLevel // update fragment's level
                        Toast.makeText(requireContext(), "Loaded level: ${loadedLevel.levelNumber}", Toast.LENGTH_SHORT).show()

                        // Must run on UI thread
                        requireActivity().runOnUiThread {
                            spawnEnemies()
                        }
                    }
                } else {
                    Log.e("API_ERROR", "Response code: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Level>, t: Throwable) {
                Log.e("API_ERROR", "Call failed", t)
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })

        // Initialize game (player positioning)
        gameArea.post {
            playerX = (gameArea.width - player.width) / 2f
            playerY = (gameArea.height - player.height).toFloat()
            player.x = playerX
            player.y = playerY
        }

        // Initialize UI
        updateLivesUI()
        updateScoreUI()
        updateHighScoreUI()
    }

    override fun onResume() {
        super.onResume()

        // Apply skin once here
        applyEquippedSkin()

        handler.post(gameRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop ALL pending tasks (not just the game loop)
        handler.removeCallbacksAndMessages(null)

        SoundEffectsManager.soundPool?.release()
        SoundEffectsManager.soundPool = null
    }

    // Player movement
    fun movePlayerLeft() {
        if (!isPaused) {
            playerX = max(0f, playerX - moveStep)
            player.x = playerX
        }
    }

    fun movePlayerRight() {
        if (!isPaused) {
            val maxRight = gameArea.width - player.width
            playerX = min(maxRight.toFloat(), playerX + moveStep)
            player.x = playerX
        }
    }

    private fun updateGame() {
        // Player bullets
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            bullet.y -= bulletSpeed
            if (bullet.y + bullet.height < 0) {
                gameArea.removeView(bullet)
                bulletIterator.remove()
                continue
            }
        }
    }

    fun togglePause(): Boolean {
        isPaused = !isPaused
        pauseText.visibility = if (isPaused) View.VISIBLE else View.GONE
        if (isPaused) pauseText.bringToFront()
        return isPaused
    }

    private fun updateLivesUI() {
        livesText.text = "x$lives"
    }

    private fun updateScoreUI() {
        scoreText.text = "SCORE: $score"
    }

    private fun updateHighScoreUI() {

    }

    private fun applyEquippedSkin() {
        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val equippedSkinName = prefs.getString("equippedSkin", "Moth") ?: "Moth"

        // Map name -> drawable key
        val drawableKey = equippedSkinName.lowercase().replace(" ", "_")

        // Player sprite
        val drawableId = requireContext().resources.getIdentifier(
            drawableKey,
            "drawable",
            requireContext().packageName
        )
        player.setImageResource(if (drawableId != 0) drawableId else R.drawable.moth)

        // Bullets
        currentBulletDrawable = drawableKey + "_blast"
    }

    // Player shooting
    fun shoot() {
        if (isPaused) return
        val ctx = context ?: return
        val ga = gameArea ?: return
        val p = player ?: return

        val bulletResId = requireContext().resources.getIdentifier(
            currentBulletDrawable,
            "drawable",
            requireContext().packageName
        )

        val bullet = ImageView(ctx).apply { setImageResource(if (bulletResId != 0) bulletResId else R.drawable.moth_blast) }
        val bulletSize = 40
        val params = FrameLayout.LayoutParams(bulletSize, bulletSize)
        ga.addView(bullet, params)
        bullet.x = p.x + p.width / 2f - bulletSize / 2f
        bullet.y = p.y - bulletSize
        bullets.add(bullet)

        // Play shooting sound
        if (shootSoundId != 0) SoundEffectsManager.playSound(shootSoundId)
    }

    private fun spawnEnemies() {
        level?.let { lvl ->
            for (enemy in lvl.enemies) {
                val enemyView = ImageView(requireContext())
                val drawableId = requireContext().resources.getIdentifier(
                    enemy.type, "drawable", requireContext().packageName
                )
                enemyView.setImageResource(if (drawableId != 0) drawableId else R.drawable.spider_blue)

                val params = FrameLayout.LayoutParams(100, 100)
                gameArea.addView(enemyView, params)

                enemyView.x = enemy.spawnX
                enemyView.y = enemy.spawnY

                when (enemy.pattern) {
                    "straight" -> { /* animation code */ }
                    "zigzag"   -> { /* animation code */ }
                    "swoop"    -> { /* animation code */ }
                    "cluster"  -> { /* animation code */ }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SurvivalGameFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}