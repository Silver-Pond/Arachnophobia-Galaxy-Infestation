package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.graphics.RectF
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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

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
    private var playerLives = 3
    private var isPlayerDead = false
    private var score = 0
    private val handler = Handler(Looper.getMainLooper())
    private val bullets = mutableListOf<ImageView>()
    private val enemies = mutableListOf<SurvivalEnemy>()
    private var username: String? = null
    private var level: Level? = null
    private var currentLevel = 1
    private var currentWave = 1
    private val maxWaves = 3
    private val enemiesPerWave = 15

    private var currentBulletDrawable: String = "moth_blast"
    private var bulletSpeed = 15f
    private val enemySpeed = 5f
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
                    level = response.body()
                    Toast.makeText(requireContext(), "Loaded level: ${level?.levelNumber}", Toast.LENGTH_SHORT).show()

                    // Clear old enemies from screen
                    for (enemy in enemies) {
                        gameArea.removeView(enemy.view)
                    }
                    enemies.clear()

                    // Spawn new wave
                    spawnWave()
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
        val enemyIterator = enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            val view = enemy.view

            // Movement by pattern
            when (enemy.pattern) {
                "straight" -> {
                    view.y += enemy.speed
                }
                "zigzag" -> {
                    view.y += enemy.speed
                    view.x += (enemy.directionX * enemy.speed)

                    // Bounce on left/right edges
                    if (view.x <= 0f || view.x + view.width >= gameArea.width) {
                        enemy.directionX *= -1
                    }
                }
                "swoop" -> {
                    view.y += enemy.speed * 1.5f
                    view.x = enemy.spawnX + (cos(view.y / 40.0) * 50.0).toFloat()
                }
                "cluster" -> {
                    view.y += enemy.speed
                }
            }

            // Collision with player
            val playerRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            val enemyRect = RectF(view.x, view.y, view.x + view.width, view.y + view.height)
            if (playerRect.intersect(enemyRect) && !isPlayerDead) {
                handlePlayerDeath()
                continue
            }

            // Off-screen cleanup
            if (view.y > gameArea.height) {
                gameArea.removeView(view)
                enemyIterator.remove()
            }
        }

        // Handle bullets hitting enemies
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            bullet.y -= bulletSpeed

            val hitEnemy = enemies.find { e ->
                val enemyRect = RectF(e.view.x, e.view.y, e.view.x + e.view.width, e.view.y + e.view.height)
                val bulletRect = RectF(bullet.x, bullet.y, bullet.x + bullet.width, bullet.y + bullet.height)
                enemyRect.intersect(bulletRect)
            }

            if (hitEnemy != null) {
                gameArea.removeView(hitEnemy.view)
                enemies.remove(hitEnemy)

                gameArea.removeView(bullet)
                bulletIterator.remove()

                score += 10
                updateScoreUI()
                SoundEffectsManager.playSound(enemykilledId)
                continue
            }

            if (bullet.y + bullet.height < 0) {
                gameArea.removeView(bullet)
                bulletIterator.remove()
            }
        }

        // Level / Wave progression
        if (enemies.isEmpty() && !isPlayerDead) {
            if (currentWave < maxWaves) {
                currentWave++
                spawnWave()
            } else {
                currentLevel++
                currentWave = 1
                spawnWave()

                pauseText.text = "LEVEL $currentLevel"
                pauseText.visibility = View.VISIBLE
                isPaused = true

                Handler(Looper.getMainLooper()).postDelayed({
                    pauseText.visibility = View.GONE
                    isPaused = false
                }, 1000)
            }
        }
    }

    // Enemy Spawn Code
    private fun spawnEnemies() {
        level?.let { lvl ->
            for (enemy in lvl.enemies) {
                val enemyView = ImageView(requireContext())
                val drawableId = requireContext().resources.getIdentifier(
                    enemy.type, "drawable", requireContext().packageName
                )
                enemyView.setImageResource(if (drawableId != 0) drawableId else R.drawable.spider_blue)

                val size = 100
                val params = FrameLayout.LayoutParams(size, size)
                gameArea.addView(enemyView, params)

                // Clamp spawnX so it’s always on screen
                val safeX = enemy.spawnX.coerceIn(0f, gameArea.width - size.toFloat())
                val safeY = enemy.spawnY

                enemyView.x = safeX
                enemyView.y = safeY

                enemies.add(
                    SurvivalEnemy(
                        view = enemyView,
                        type = enemy.type,
                        spawnX = safeX,
                        spawnY = safeY,
                        speed = enemy.speed,
                        pattern = enemy.pattern
                    )
                )
            }
        }
    }

    // Enemy Spawn Waves Code
    private fun spawnWave() {
        enemies.clear() // remove leftover enemies from previous wave

        repeat(enemiesPerWave) {
            val enemyView = ImageView(requireContext())
            val drawableId = requireContext().resources.getIdentifier(
                "spider_blue", "drawable", requireContext().packageName
            )
            enemyView.setImageResource(if (drawableId != 0) drawableId else R.drawable.spider_blue)

            val size = 100
            val params = FrameLayout.LayoutParams(size, size)
            gameArea.addView(enemyView, params)

            val spawnX = (50..(gameArea.width - size - 50)).random().toFloat()
            val spawnY = (-500..-100).random().toFloat()

            enemyView.x = spawnX
            enemyView.y = spawnY

            enemies.add(
                SurvivalEnemy(
                    view = enemyView,
                    type = "spider_blue",
                    spawnX = spawnX,
                    spawnY = spawnY,
                    speed = (3..6).random().toFloat(),
                    pattern = listOf("straight", "zigzag", "swoop").random()
                )
            )
        }
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

    // Player Death Code
    private fun handlePlayerDeath() {
        isPlayerDead = true
        isPaused = true // 🔥 freeze game temporarily
        playerLives--
        updateLivesUI()

        // Show explosion sprite
        player.setImageResource(R.drawable.moth_death)
        SoundEffectsManager.playSound(explosionId)

        if (playerLives <= 0) {
            // Immediately end game
            gameOver()
            return
        }

        // Otherwise, respawn after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            // Respawn player at bottom center
            val centerX = (gameArea.width - player.width) / 2f
            val bottomY = (gameArea.height - player.height).toFloat()

            playerX = centerX
            playerY = bottomY
            player.x = playerX
            player.y = playerY

            // Reapply equipped skin
            applyEquippedSkin()

            // Reset enemies to original spawn positions
            enemies.forEach { enemy ->
                enemy.view.x = enemy.spawnX
                enemy.view.y = enemy.spawnY
            }

            // Resume game
            isPlayerDead = false
            isPaused = false
        }, 2000) // freeze duration before respawn
    }

    // Skins Code
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

    fun togglePause(): Boolean {
        isPaused = !isPaused
        pauseText.visibility = if (isPaused) View.VISIBLE else View.GONE
        if (isPaused) pauseText.bringToFront()
        return isPaused
    }

    private fun updateLivesUI() {
        livesText.text = "x$playerLives"
    }

    private fun updateScoreUI() {
        scoreText.text = "SCORE: $score"
    }

    private fun updateHighScoreUI() {

    }

    // Game Over Code
    private fun gameOver() {
        isPaused = true

        // Stop the game loop
        handler.removeCallbacks(gameRunnable)

        pauseText.text = "GAME OVER"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().finish()
        }, 2000)

        // Play sound
        if (gameOverSoundId != 0) SoundEffectsManager.playSound(gameOverSoundId)
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