package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

class SurvivalGameFragment : Fragment() {
    private lateinit var player: ImageView
    private lateinit var gameArea: FrameLayout
    private lateinit var pauseText: TextView
    private lateinit var livesText: TextView
    private lateinit var timerText: TextView

    private var elapsedSeconds = 0
    private var timerRunning = false
    private var isFragmentActive = false
    private var apiConnected = false // Add this at class level
    private var playerX = 0f
    private var playerY = 0f
    private val moveStep = 40f
    private var isPaused = false
    private var playerLives = 3
    private var isPlayerDead = false
    private var score = 0
    private var isGameFinished = false

    private val handler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())
    private val bullets = mutableListOf<ImageView>()
    private val enemies = mutableListOf<SurvivalEnemy>()
    private val projectiles = mutableListOf<EnemyProjectile>()
    private val meteorites = mutableListOf<ImageView>()
    private var meteorSpawnHandler = Handler(Looper.getMainLooper())

    private var username: String? = null
    private var level: Level? = null
    var currentLevel = 1
    private var currentWave = 1
    private val maxWaves = 3
    private var baseEnemySpeed = 2f
    private var currentEnemySpeed = baseEnemySpeed
    private var projectileSpeed = 10f
    private var bulletSpeed = 15f
    private var lastMeteorSpawnTime = 0L
    private var meteorSpawnDelay = 1500L  // 1.5 seconds between meteors
    private var lastMeteorX = -1000f  // Track last position
    private var currentBulletDrawable: String = "moth_blast"

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
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
        timerText = view.findViewById(R.id.timer)
        timerText.text = "Time: 00:00"

        // Get username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        // Sync player data if user is not Guest (Android Developers, 2025; Firebsae, 2025)
        if (!username.equals("Guest", ignoreCase = true) && username.isNotBlank()) {
            MainActivity.PlayerDataSync.syncPlayerData(requireContext()) {
                // Apply skin once here
                applyEquippedSkin()
            }
        } else {
            // Default skin
            player.setImageResource(R.drawable.moth)
        }

        // Initialize SoundPool once
        val soundPool = SoundPool.Builder().setMaxStreams(5).build()
        SoundEffectsManager.soundPool = soundPool

        // Load sounds
        shootSoundId = soundPool.load(requireContext(), R.raw.cannon_shot, 1)
        enemyfireId = soundPool.load(requireContext(), R.raw.shoot, 1)
        enemykilledId = soundPool.load(requireContext(), R.raw.invaderkilled, 1)
        purpkilledId = soundPool.load(requireContext(), R.raw.ufo_highpitch, 1)
        purpleAppearId = soundPool.load(requireContext(), R.raw.ufo_lowpitch, 1)
        explosionId = soundPool.load(requireContext(), R.raw.explosion, 1)
        gameOverSoundId = soundPool.load(requireContext(), R.raw.game_over, 1)

        // Restore effects volume from prefs (Android Developers, 2025; Firebsae, 2025)
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Reset level and wave
        currentLevel = 1
        currentWave = 1
        currentEnemySpeed = baseEnemySpeed

        // Load first level from API (Android Developers, 2025; Firebsae, 2025)
        ApiClient.instance.getLevel(1).enqueue(object : Callback<Level> {
            override fun onResponse(call: Call<Level>, response: Response<Level>) {
                if (response.isSuccessful) {
                    level = response.body()
                    apiConnected = true

                    enemies.clear()
                    for (enemy in enemies) {
                        gameArea.removeView(enemy.view)
                    }

                    // Delay 500ms for initial setup (Android Developers, 2025; Firebsae, 2025)
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded && view != null) {
                            // Show initial pause text
                            pauseText.text = "LEVEL $currentLevel"
                            pauseText.visibility = View.VISIBLE

                            // Hide text after 1 second and start first wave
                            Handler(Looper.getMainLooper()).postDelayed({
                                pauseText.visibility = View.GONE
                                spawnWave()
                                if (!timerRunning) startTimer()
                                handler.post(gameRunnable)
                            }, 1000)
                        }
                    }, 500)
                } else {
                    Toast.makeText(requireContext(), "Error loading level", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Level>, t: Throwable) {
                Toast.makeText(requireContext(), "Cannot reach server", Toast.LENGTH_SHORT).show()
            }
        })

        // Initialize player position after layout is ready
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

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isAdded && view != null && !isPaused) {
                updateGame()
            }
            // Re-post ONLY if still added and the view exists (Android Developers, 2025; Firebsae, 2025)
            if (isAdded && view != null) {
                handler.postDelayed(this, 16)
            }
        }
    }

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isGameFinished) {
                timerRunning = false
                return
            }

            if (!isPaused && isFragmentActive) {
                elapsedSeconds++

                // Format the time as MM:SS
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                val formattedTime = String.format("Time: %02d:%02d", minutes, seconds)
                timerText.text = formattedTime

                // Add 10 points every 15 seconds
                if (elapsedSeconds % 15 == 0) {
                    score += 10
                    updateScoreUI()
                }
            }

            // Keep the timer running every second
            timerHandler.postDelayed(this, 1000)
        }
    }

    private fun startTimer() {
        if (!timerRunning) {
            timerRunning = true
            timerHandler.postDelayed(timerRunnable, 1000)
        }
    }

    override fun onResume() {
        super.onResume()

        // Mark fragment as active early
        isFragmentActive = true

        // Stop any pending tasks before resetting
        handler.removeCallbacks(gameRunnable)

        // Full safe reset if restarting from beginning
        currentLevel = 1
        currentWave = 1
        score = 0
        updateScoreUI()

        // Ensure pauseText matches the correct level
        pauseText.text = "LEVEL $currentLevel"

        // Get username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        // Sync player data if user is not Guest (Android Developers, 2025; Firebsae, 2025)
        if (!username.equals("Guest", ignoreCase = true) && username.isNotBlank()) {
            MainActivity.PlayerDataSync.syncPlayerData(requireContext()) {
                // Apply skin once here
                applyEquippedSkin()
            }
        } else {
            // Default skin
            player.setImageResource(R.drawable.moth)
        }
    }

    override fun onPause() {
        super.onPause()

        // Mark fragment as inactive so game loop & spawns stop
        isFragmentActive = false

        // Stop timer but preserve its value (don’t reset)
        timerRunning = false
        timerHandler.removeCallbacks(timerRunnable)

        // Pause game loop cleanly
        handler.removeCallbacks(gameRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Full deactivation to prevent any background updates
        isFragmentActive = false
        apiConnected = false

        // Stop ALL delayed tasks or loop callbacks (Android Developers, 2025; Firebsae, 2025)
        handler.removeCallbacksAndMessages(null)
        timerHandler.removeCallbacksAndMessages(null)
        meteorSpawnHandler.removeCallbacksAndMessages(null)
        meteorites.clear()

        // Clean up all enemies from view hierarchy
        enemies.forEach { gameArea.removeView(it.view) }
        enemies.clear()

        // Release all sound resources to avoid memory leaks (Android Developers, 2025; Firebsae, 2025)
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
                    view.x += enemy.directionX * enemy.speed
                    if (view.x <= 0f || view.x + view.width >= gameArea.width) enemy.directionX *= -1
                }
                "swoop" -> {
                    view.y += enemy.speed * 1.5f
                    view.x = enemy.spawnX + (cos(view.y / 40.0) * 50.0).toFloat()
                }
            }

            // Player collision
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

                // Play correct death sound
                val soundId = if (hitEnemy.type == "spider_purple") purpkilledId else enemykilledId
                if (soundId != 0) SoundEffectsManager.playSound(soundId)

                handler.postDelayed({
                    if (isAdded && gameArea.isAttachedToWindow) gameArea.removeView(hitEnemy.view)
                }, 300)

                enemies.remove(hitEnemy)
                gameArea.removeView(bullet)
                bulletIterator.remove()

                // === Score & Lives ===
                score += when (hitEnemy.type) {
                    "spider_maroon" -> 15
                    "spider_purple" -> {
                        playerLives++
                        updateLivesUI()
                        50
                    }
                    else -> 10
                }

                updateScoreUI()
                checkAndAwardTrophies()
            }
        }

        // === Enemy Projectiles ===
        updateEnemyProjectiles()

        // === METEORITES ===
        // === METEORITE SPAWNING ===
        val now = System.currentTimeMillis()
        if (now - lastMeteorSpawnTime >= meteorSpawnDelay) {
            spawnMeteorite()
            lastMeteorSpawnTime = now
        }

        val meteorIter = meteorites.iterator()
        while (meteorIter.hasNext()) {
            val meteor = meteorIter.next()
            meteor.y += 14f  // falling speed

            if (meteor.y > gameArea.height) {
                gameArea.removeView(meteor)
                meteorIter.remove()
                continue
            }

            // Instant player kill
            if (!isPlayerDead && playerHitBy(meteor)) {
                gameArea.removeView(meteor)
                meteorIter.remove()
                handlePlayerDeath()
                return
            }
        }

        // === Level / Wave Progression ===
        if (enemies.isEmpty() && !isPlayerDead) {
            if (currentWave < maxWaves) {
                currentWave++
                spawnWave()
            } else {
                currentLevel++
                currentWave = 1

                // Adjust enemy speed scaling
                currentEnemySpeed = baseEnemySpeed + when {
                    currentLevel <= 20 -> (currentLevel - 1) * 1f
                    else -> (19 * 1f) + ((currentLevel - 20) * 2f)
                }

                // Update UI
                pauseText.text = "LEVEL $currentLevel"
                pauseText.visibility = View.VISIBLE

                Handler(Looper.getMainLooper()).postDelayed({
                    pauseText.visibility = View.GONE
                    if (enemies.isEmpty()) spawnWave()
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

    // ================= Enemy Shooting =================
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
                // Only shoot if game is active AND player is alive AND enemy still exists (Android Developers, 2025; Firebsae, 2025)
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
        // Get username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        // Sync player data if user is not Guest (Android Developers, 2025; Firebsae, 2025)
        if (!username.equals("Guest", ignoreCase = true) && username.isNotBlank()) {
            // Apply skin once here
            applyEquippedSkin()
        } else {
            // Default skin
            player.setImageResource(R.drawable.moth)
        }

        playerX = (gameArea.width - player.width) / 2f
        playerY = (gameArea.height - player.height).toFloat()
        player.x = playerX
        player.y = playerY

        enemies.clear()
        spawnWave()
        projectiles.clear()
        meteorites.forEach { gameArea.removeView(it) }
        meteorites.clear()
        stopMeteoriteDrops()

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
        if (!apiConnected || !isFragmentActive) {
            Log.w("GAME", "Wave spawn blocked: apiConnected=$apiConnected, active=$isFragmentActive")
            return
        }

        val ctx = context ?: return
        val setSize = 20
        enemies.forEach { gameArea.removeView(it.view) }
        enemies.clear()

        // Update speed scaling (Android Developers, 2025; Firebsae, 2025)
        currentEnemySpeed = if (currentLevel <= 20)
            baseEnemySpeed + (currentLevel - 1)
        else
            baseEnemySpeed + 19 + (currentLevel - 20) * 2

        val purpleIndex = if (currentLevel % 10 == 0) (0 until setSize).random() else -1

        repeat(setSize) { i ->
            val enemyType = when {
                i == purpleIndex -> "spider_purple"
                currentLevel in 5..15 && i < setSize / 4 -> "spider_maroon"
                else -> "spider_blue"
            }

            val enemyView = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(100, 100)
                setImageResource(
                    when (enemyType) {
                        "spider_maroon" -> R.drawable.spider_maroon
                        "spider_purple" -> R.drawable.spider_purple
                        else -> R.drawable.spider_blue
                    }
                )
            }

            val spawnX = (50..(gameArea.width - 150)).random().toFloat()
            val spawnY = (-500..-100).random().toFloat()
            enemyView.x = spawnX
            enemyView.y = spawnY

            gameArea.addView(enemyView)
            if (enemyType == "spider_purple" && purpleAppearId != 0)
                SoundEffectsManager.playSound(purpleAppearId)

            val enemy = SurvivalEnemy(
                view = enemyView,
                type = enemyType,
                spawnX = spawnX,
                spawnY = spawnY,
                speed = currentEnemySpeed,
                pattern = listOf("straight", "zigzag", "swoop").random()
            )
            enemies.add(enemy)

            if (enemyType == "spider_maroon") startMaroonShooting(enemy)
        }
        // Meteorites spawn only in level ranges 1–4, 10–14, 20–24, etc. (Android Developers, 2025; ChatGPT-4, 2025)
        val levelGroup = (currentLevel - 1) / 10
        val levelInGroup = currentLevel - levelGroup * 10
        if (levelInGroup in 1..4) {
            startMeteoriteDrops()
        } else {
            stopMeteoriteDrops()
        }
    }

    // Meteorite spawn logic (Android Developers, 2025; ChatGPT-4, 2025)
    private fun spawnMeteorite() {
        val ctx = context ?: return
        val ga = gameArea ?: return

        val meteor = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(120, 120)
            setImageResource(R.drawable.web_bomb)
        }

        // Better randomness + anti-clustering (Android Developers, 2025; ChatGPT-4, 2025)
        var spawnX: Float
        do {
            spawnX = ThreadLocalRandom.current().nextInt(0, ga.width - 150).toFloat()
        } while (abs(spawnX - lastMeteorX) < 200f)

        lastMeteorX = spawnX

        meteor.x = spawnX
        meteor.y = -200f

        ga.addView(meteor)
        meteorites.add(meteor)
    }

    // Meteorite spawn logic (Android Developers, 2025; ChatGPT-4, 2025)
    private fun startMeteoriteDrops() {
        meteorSpawnHandler.removeCallbacksAndMessages(null)

        val dropRunnable = object : Runnable {
            override fun run() {
                if (!isPaused && !isPlayerDead && isFragmentActive) {
                    spawnMeteorite()
                }
                meteorSpawnHandler.postDelayed(this, 2000L) // Drop every 2 seconds
            }
        }

        meteorSpawnHandler.post(dropRunnable)
    }

    // Meteorite stop logic (Android Developers, 2025; ChatGPT-4, 2025)
    private fun stopMeteoriteDrops() {
        meteorSpawnHandler.removeCallbacksAndMessages(null)
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

    private fun saveInGameCurrency() {
        val username = arguments?.getString("username") ?: "Guest"

        // Guest users do not earn spider silk (Android Developers, 2025; Firebsae, 2025)
        if (username.equals("Guest", ignoreCase = true) || username.isEmpty()) return

        val spiderSilkEarned = (score * 0.5) / 100.0
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null) return // Fragment is not active (Android Developers, 2025; Firebsae, 2025)

                val showToasts = username.isNotBlank() && !username.equals("Guest", ignoreCase = true)

                if (snapshot.exists()) {
                    val currentSilk = snapshot.child("spider_silk").getValue(Double::class.java) ?: 0.0
                    if (currentSilk < 100_000) {
                        val updatedSilk = (currentSilk + spiderSilkEarned).coerceAtMost(100_000.0)

                        dbRef.child("spider_silk").setValue(updatedSilk)
                            .addOnSuccessListener {
                                if (showToasts) {
                                    Toast.makeText(
                                        requireContext(),
                                        "You earned ${"%.1f".format(spiderSilkEarned)} silk!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener {
                                if (showToasts) {
                                    Toast.makeText(requireContext(), "Failed to update spider silk.", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        if (showToasts) {
                            Toast.makeText(requireContext(), "Max silk reached (100,000).", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // If the player record doesn't exist, create it (Android Developers, 2025; Firebsae, 2025)
                    val userMap = mapOf(
                        "id" to uid,
                        "username" to username.ifBlank { "Guest" },
                        "email" to (snapshot.child("email").getValue(String::class.java) ?: ""),
                        "password" to (snapshot.child("password").getValue(String::class.java) ?: ""),
                        "highscore" to (snapshot.child("highscore").getValue(Int::class.java) ?: 0),
                        "survivalHighscore" to (snapshot.child("survivalHighscore").getValue(Int::class.java) ?: 0),
                        "spider_silk" to spiderSilkEarned.coerceAtMost(100_000.0)
                    )

                    dbRef.setValue(userMap)
                        .addOnSuccessListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "Player record created with spider silk!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "Failed to save spider silk.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || view == null) return
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkAndAwardTrophies() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("players")
            .child(uid)
            .child("trophies")

        val trophiesToAward = mutableListOf<Trophy>()

        // Level-based trophies for Survival Mode (Android Developers, 2025; ChatGPT-4, 2025)
        when (currentLevel) {
            1 -> trophiesToAward.add(Trophy("trophy12", "Survivor lvl 1", "Completed Level 1 of Survival Mode!", "lvl_trophy"))
            4 -> trophiesToAward.add(Trophy("trophy13", "Survivor lvl 4", "Completed Level 4 of Survival Mode!", "lvl_trophy"))
            9 -> trophiesToAward.add(Trophy("trophy14", "Survivor lvl 9", "Completed Level 9 of Survival Mode!", "lvl_trophy"))
            14 -> trophiesToAward.add(Trophy("trophy15", "Survivor lvl 14", "Completed Level 14 of Survival Mode!", "lvl_trophy"))
            19 -> trophiesToAward.add(Trophy("trophy16", "Survivor lvl 19", "Completed Level 19 of Survival Mode!", "lvl_trophy"))
        }

        // Score-based trophies for Survival Mode (Android Developers, 2025; ChatGPT-4, 2025)
        if (score >= 2000) trophiesToAward.add(Trophy("trophy17", "Arachno-Loser", "Obtained a score of 2000 in Survival Mode!", "score_trophy"))
        if (score >= 4000) trophiesToAward.add(Trophy("trophy18", "New Survivor", "Obtained a score of 4000 in Survival Mode!", "score_trophy"))
        if (score >= 6000) trophiesToAward.add(Trophy("trophy19", "Spider Hunter lvl 1", "Obtained a score of 6000 in Survival Mode!", "score_trophy"))
        if (score >= 8000) trophiesToAward.add(Trophy("trophy20", "Spider Hunter lvl...Legit", "Obtained a score of 8000 in Survival Mode!", "score_trophy"))

        // 🏆 Save & notify only NEW trophies (Android Developers, 2025; ChatGPT-4, 2025)
        trophiesToAward.forEach { trophy ->
            dbRef.child(trophy.id).get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    dbRef.child(trophy.id).setValue(true)

                    val snackbar = Snackbar.make(
                        requireView(),
                        "🏆 ${trophy.name} unlocked!\n${trophy.description}",
                        Snackbar.LENGTH_LONG
                    )

                    // Place Snackbar at top center (Android Developers, 2025; ChatGPT-4, 2025)
                    val snackbarView = snackbar.view
                    val params = snackbarView.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    params.topMargin = 100
                    snackbarView.layoutParams = params

                    snackbarView.setBackgroundColor(
                        ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_primary_variant)
                    )
                    snackbar.show()
                }
            }
        }
    }

    // Game Over Code
    private fun gameOver() {
        if (!isFragmentActive) return // Prevent running after fragment is destroyed
        isPaused = true

        // Stop timer
        timerRunning = false
        timerHandler.removeCallbacks(timerRunnable)

        // Stop game loop
        handler.removeCallbacks(gameRunnable)

        // Stop enemy spawns (safety)
        enemies.forEach { gameArea.removeView(it.view) }
        enemies.clear()

        // Display GAME OVER text
        pauseText.text = "GAME OVER"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        // Play sound (only once)
        if (gameOverSoundId != 0) {
            SoundEffectsManager.playSound(gameOverSoundId)
        }

        // Save progress safely
        saveHighScore()
        saveInGameCurrency()
        checkAndAwardTrophies()

        // Delay return to Game Menu
        Handler(Looper.getMainLooper()).postDelayed({
            if (isFragmentActive && isAdded) {
                requireActivity().finish()
            }
        }, 2000)
    }
}
/*
* Reference List
*
* Android Developers, 2025. AppCompatActivity. [online]. Available at:
* https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Fragment. [online]. Available at:
* https://developer.android.com/reference/androidx/fragment/app/Fragment
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. MediaPlayer. [online]. Available at:
* https://developer.android.com/reference/android/media/MediaPlayer
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. TextView. [online]. Available at:
* https://developer.android.com/reference/android/widget/TextView
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Toast. [online]. Available at:
* https://developer.android.com/reference/android/widget/Toast
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. SharedPreferences. [online]. Available at:
* https://developer.android.com/reference/android/content/SharedPreferences
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Configuration. [online]. Available at:
* https://developer.android.com/reference/android/content/res/Configuration
* [Accessed: 7 October 2025].
*
* Android Developers, 2025. Locale. [online]. Available at:
* https://developer.android.com/reference/java/util/Locale
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseAuth. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/auth/FirebaseAuth
* [Accessed: 7 October 2025].
*
* Firebase, 2025. FirebaseDatabase. [online]. Available at:
* https://firebase.google.com/docs/reference/android/com/google/firebase/database/FirebaseDatabase
* [Accessed: 7 October 2025].
*
* ChatGPT-4, 2025. OpenAI. [online]. Available at:
* https://chatgpt.com/?model=auto
* [Accessed: 10 November 2025].
*/