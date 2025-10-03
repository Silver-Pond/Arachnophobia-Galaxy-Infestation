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
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
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
    private var isGameFinished = false

    private val handler = Handler(Looper.getMainLooper())
    private val bullets = mutableListOf<ImageView>()
    private val enemies = mutableListOf<SurvivalEnemy>()
    private val projectiles = mutableListOf<EnemyProjectile>()

    private var username: String? = null
    private var level: Level? = null
    private var currentLevel = 1
    private var currentWave = 1
    private val maxWaves = 3
    private var baseEnemySpeed = 3f
    private var currentEnemySpeed = baseEnemySpeed
    private var projectileSpeed = 10f

    private var currentBulletDrawable: String = "moth_blast"
    private var bulletSpeed = 15f
    private var shootSoundId: Int = 0
    private var enemyfireId: Int = 0
    private var enemykilledId: Int = 0
    private var purpkilledId: Int = 0
    private var explosionId: Int = 0
    private var purpleAppearId: Int = 0
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

        // Load enemy killed sound
        purpkilledId = soundPool.load(requireContext(), R.raw.ufo_highpitch, 1)

        // Spider Purple movement sound
        purpleAppearId = soundPool.load(requireContext(), R.raw.ufo_lowpitch, 1)

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
                    // Toast.makeText(requireContext(), "Loaded level: ${level?.levelNumber}", Toast.LENGTH_SHORT).show()

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

    // ================= Survival Game Loop =================

    private fun updateGame() {
        if (isPaused || isGameFinished) return

        // === Enemy Movement ===
        val enemyIterator = enemies.iterator()
        while (enemyIterator.hasNext()) {
            val enemy = enemyIterator.next()
            if (!enemy.isAlive) continue

            val view = enemy.view

            // Movement patterns
            when (enemy.pattern) {
                "straight" -> view.y += enemy.speed
                "zigzag" -> {
                    view.y += enemy.speed
                    view.x += (enemy.directionX * enemy.speed)
                    if (view.x <= 0f || view.x + view.width >= gameArea.width) {
                        enemy.directionX *= -1
                    }
                }
                "swoop" -> {
                    view.y += enemy.speed * 1.5f
                    view.x = enemy.spawnX + (cos(view.y / 40.0) * 50.0).toFloat()
                }
            }

            // Enemy collides with player
            if (!isPlayerDead && playerHitBy(view)) {
                handlePlayerDeath()
                return
            }

            // Remove offscreen
            if (view.y > gameArea.height) {
                gameArea.removeView(view)
                enemyIterator.remove()
            }
        }

        // === Player Bullets ===
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val bullet = bulletIterator.next()
            bullet.y -= bulletSpeed

            if (bullet.y + bullet.height < 0) {
                gameArea.removeView(bullet)
                bulletIterator.remove()
                continue
            }

            val hitEnemy = enemies.firstOrNull { it.isAlive &&
                    bullet.x < it.view.x + it.view.width &&
                    bullet.x + bullet.width > it.view.x &&
                    bullet.y < it.view.y + it.view.height &&
                    bullet.y + bullet.height > it.view.y }

            if (hitEnemy != null) {
                hitEnemy.isAlive = false
                hitEnemy.view.setImageResource(R.drawable.spider_death)

                // Play enemy death sound
                if (hitEnemy.type == "spider_purple") {
                    if (purpkilledId != 0) {
                        SoundEffectsManager.playSound(purpkilledId)
                    }
                } else {
                    if (enemykilledId != 0) {
                        SoundEffectsManager.playSound(enemykilledId)
                    }
                }

                handler.postDelayed({
                    if (isAdded && gameArea.isAttachedToWindow) {
                        gameArea.removeView(hitEnemy.view)
                    }
                }, 300)

                enemies.remove(hitEnemy)
                gameArea.removeView(bullet)
                bulletIterator.remove()

                // === Score system ===
                score += when (hitEnemy.type) {
                    "spider_maroon" -> 15
                    "spider_purple" -> {
                        playerLives += 1
                        updateLivesUI()
                        50
                    }
                    else -> 10
                }

                updateScoreUI()
            }
        }

        // === Enemy Projectiles ===
        updateEnemyProjectiles()

        // === Level / Wave Progression ===
        if (enemies.isEmpty() && !isPlayerDead) {
            if (currentWave < maxWaves) {
                currentWave++
                spawnWave()
            } else {
                currentLevel++
                currentWave = 1

                // Adjust enemy speed scaling
                currentEnemySpeed = when {
                    currentLevel % 5 == 0 -> baseEnemySpeed
                    currentLevel in 10..15 -> baseEnemySpeed + 1f
                    else -> currentEnemySpeed + 1f
                }

                pauseText.text = "LEVEL $currentLevel"
                pauseText.visibility = View.VISIBLE
                isPaused = true

                Handler(Looper.getMainLooper()).postDelayed({
                    pauseText.visibility = View.GONE
                    isPaused = false
                    spawnWave()
                }, 1000)
            }
        }
    }

    // ================= Enemy Projectiles =================
    private fun updateEnemyProjectiles() {
        val projIterator = projectiles.iterator()
        while (projIterator.hasNext()) {
            val proj = projIterator.next()
            proj.imageView.y += projectileSpeed

            if (proj.imageView.y > gameArea.height) {
                gameArea.removeView(proj.imageView)
                projIterator.remove()
                continue
            }

            if (!isPaused && !isPlayerDead && playerHitBy(proj.imageView)) {
                gameArea.removeView(proj.imageView)
                projIterator.remove()
                handlePlayerDeath()
                return
            }
        }
    }

    private fun shootEnemyProjectile(enemy: SurvivalEnemy) {
        val ctx = context ?: return

        val bullet = ImageView(ctx).apply {
            setImageResource(R.drawable.spider_web_shot)
        }
        val size = 30
        val params = FrameLayout.LayoutParams(size, size)
        gameArea.addView(bullet, params)

        bullet.x = enemy.view.x + enemy.view.width / 2f - size / 2f
        bullet.y = enemy.view.y + enemy.view.height

        projectiles.add(EnemyProjectile(bullet))
    }

    private fun startMaroonShooting(enemy: SurvivalEnemy) {
        val handler = Handler(Looper.getMainLooper())

        val shootRunnable = object : Runnable {
            override fun run() {
                // Only shoot if game is active AND player is alive AND enemy still exists
                if (!isPaused && !isPlayerDead && enemies.contains(enemy)) {
                    shootEnemyProjectile(enemy)

                    // Play shooting sound
                    if (enemyfireId != 0) {
                        SoundEffectsManager.playSound(enemyfireId)
                    }
                }

                // Schedule next attempt only if enemy still exists
                if (enemies.contains(enemy)) {
                    handler.postDelayed(this, (1000..3000).random().toLong())
                }
            }
        }

        handler.post(shootRunnable)
    }

    // ================= Player Death Handling =================
    private fun handlePlayerDeath() {
        if (isPaused || isPlayerDead || isGameFinished) return

        isPlayerDead = true
        isPaused = true
        playerLives--
        updateLivesUI()

        if (explosionId != 0) SoundEffectsManager.playSound(explosionId)

        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val equippedSkin = prefs.getString("equippedSkin", "Moth") ?: "Moth"
        val deathKey = equippedSkin.lowercase().replace(" ", "_") + "_death"
        val deathResId = requireContext().resources.getIdentifier(
            deathKey, "drawable", requireContext().packageName
        )
        player.setImageResource(if (deathResId != 0) deathResId else R.drawable.moth_death)

        if (playerLives > 0) {
            enemies.forEach { gameArea.removeView(it.view) }
            enemies.clear()
            projectiles.forEach { gameArea.removeView(it.imageView) }
            projectiles.clear()

            Handler(Looper.getMainLooper()).postDelayed({
                resetSurvivalGameState()
            }, 1200)
        } else {
            isGameFinished = true
            pauseText.text = "GAME OVER"
            pauseText.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                gameOver()
            }, 1000)
        }
    }

    private fun resetSurvivalGameState() {
        applyEquippedSkin()

        playerX = (gameArea.width - player.width) / 2f
        playerY = (gameArea.height - player.height).toFloat()
        player.x = playerX
        player.y = playerY

        enemies.clear()
        spawnWave()
        projectiles.clear()

        isPlayerDead = false
        isPaused = false
    }

    // ================= Collision Helper =================
    private fun playerHitBy(view: ImageView): Boolean {
        if (isPaused || isPlayerDead) return false

        val vw = if (view.width > 0) view.width else (view.layoutParams?.width ?: 0)
        val vh = if (view.height > 0) view.height else (view.layoutParams?.height ?: 0)
        val pw = if (player.width > 0) player.width else (player.layoutParams?.width ?: 0)
        val ph = if (player.height > 0) player.height else (player.layoutParams?.height ?: 0)

        if (vw <= 0 || vh <= 0 || pw <= 0 || ph <= 0) return false

        val vx1 = view.x
        val vy1 = view.y
        val vx2 = vx1 + vw
        val vy2 = vy1 + vh

        val px1 = player.x
        val py1 = player.y
        val px2 = px1 + pw
        val py2 = py1 + ph

        return vx1 < px2 && vx2 > px1 && vy1 < py2 && vy2 > py1
    }

    // Enemy Spawn Waves Code
    private fun spawnWave() {
        val ctx = context ?: return
        val setSize = 20

        // Clear any leftovers
        enemies.forEach { gameArea.removeView(it.view) }
        enemies.clear()

        // Decide a random index for spider_purple if level 10
        val purpleIndex = if (currentLevel == 10) (0 until setSize).random() else -1

        // Spawn for this wave
        repeat(setSize) { i ->
            val enemyView = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(100, 100)
            }

            // Decide type
            val enemyType = when {
                i == purpleIndex -> "spider_purple" // only one in the set
                currentLevel in 5..10 && i < setSize / 4 -> "spider_maroon"
                else -> "spider_blue"
            }

            // Position
            val spawnX = (50..(gameArea.width - 150)).random().toFloat()
            val spawnY = (-500..-100).random().toFloat()

            // Drawable
            val drawableRes = when (enemyType) {
                "spider_maroon" -> R.drawable.spider_maroon
                "spider_purple" -> R.drawable.spider_purple
                else -> R.drawable.spider_blue
            }
            enemyView.setImageResource(drawableRes)

            // Add to screen
            gameArea.addView(enemyView)

            // Play sound once if spider_purple
            if (enemyType == "spider_purple" && purpleAppearId != 0) {
                SoundEffectsManager.playSound(purpleAppearId)
            }

            // Glow / pulse effect for spider_purple
            if (enemyType == "spider_purple") {
                val pulse = AlphaAnimation(0.5f, 1.0f).apply {
                    duration = 500
                    repeatMode = Animation.REVERSE
                    repeatCount = Animation.INFINITE
                }
                enemyView.startAnimation(pulse)
            }

            // Create enemy
            val enemy = SurvivalEnemy(
                view = enemyView,
                type = enemyType,
                spawnX = spawnX,
                spawnY = spawnY,
                speed = currentEnemySpeed,
                pattern = listOf("straight", "zigzag", "swoop").random()
            )

            enemyView.x = spawnX
            enemyView.y = spawnY
            enemies.add(enemy)

            // Special behavior
            if (enemyType == "spider_maroon") {
                startMaroonShooting(enemy)
            }
        }
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
        val username = arguments?.getString("username") ?: "Guest"

        if (username.equals("Guest", ignoreCase = true) || username.isEmpty()) {
            highScoreText.text = "HIGHSCORE: 0"
        } else {
            val auth = FirebaseAuth.getInstance()
            val uid = auth.currentUser?.uid ?: return  // No UID, just return
            val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

            dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Use the correct field name ("survivalHighscore")
                    val storedHighscore = snapshot.child("survivalHighscore").getValue(Int::class.java) ?: 0
                    highScoreText.text = "HIGHSCORE: $storedHighscore"
                }

                override fun onCancelled(error: DatabaseError) {
                    highScoreText.text = "HIGHSCORE: 0"
                }
            })
        }
    }

    private fun saveHighScore() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // Not logged in → just skip
            return
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        // Use global network state
        if (!NetworkUtils.isOnline) {
            // Offline → store locally
            val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            prefs.edit().putInt("pending_survival_highscore", score).apply()

            Toast.makeText(requireContext(), "No internet. Survival high score saved locally.", Toast.LENGTH_SHORT).show()
            return
        }

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingHighscore = snapshot.child("survivalHighscore").getValue(Int::class.java) ?: 0

                // Only update if higher
                if (score > existingHighscore) {
                    dbRef.child("survivalHighscore").setValue(score)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "New Survival Highscore!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update survival highscore", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun onNewSurvivalHighScoreAchieved(newSurvivalScore: Int) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Not logged in. Cannot save survival high score.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use HighScoreManager to handle offline/online logic
        HighScoreManager.saveSurvivalHighScore(requireContext(), newSurvivalScore)
    }

    // Game Over Code
    private fun gameOver() {
        isPaused = true

        // Stop the game loop
        handler.removeCallbacks(gameRunnable)

        pauseText.text = "GAME OVER"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        // Save Highscore & Currency
        saveHighScore()

        // Sync new highscore in case internet was lost during play
        onNewSurvivalHighScoreAchieved(score)

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