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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.max
import kotlin.math.min

class GameFragment : Fragment() {
    private lateinit var player: ImageView
    private lateinit var gameArea: FrameLayout
    private lateinit var pauseText: TextView
    private lateinit var livesText: TextView

    private var isGameLoopRunning = false
    private var playerX = 0f
    private var playerY = 0f
    private val moveStep = 40f
    private var isPaused = false
    private var takingHit = false
    private var lives = 3
    private var score = 0
    private var isGameFinished = false
    private val handler = Handler(Looper.getMainLooper())
    private val bullets = mutableListOf<ImageView>()
    private val enemyBullets = mutableListOf<ImageView>()
    private val enemies = mutableListOf<Enemy>()
    private var bulletSpeed = 15f
    private var enemyBulletSpeed = 10f
    private var enemySpeed = 5f
    private var enemyDirection = 1
    private var enemyFallSpeed = 1f
    private var currentBulletDrawable: String = "moth_blast"

    // Levels and waves
    private val enemySets = mutableListOf<List<Enemy>>()
    private var currentSetIndex = 0
    private var currentLevel = 1
    private val maxLevels = 30
    private val setsPerLevel = 4

    // Sound Effects
    private var clickbuttonSoundId: Int = 0
    private var shootSoundId: Int = 0
    private var enemyfireId: Int = 0
    private var enemykilledId: Int = 0
    private var purpkilledId: Int = 0
    private var zigzagMoveSoundId: Int = 0
    private var explosionId: Int = 0
    private var gameOverSoundId: Int = 0

    //
    private val scoreText: TextView
        get() = requireActivity().findViewById(R.id.scoreText)
    private val highScoreText: TextView
        get() = requireActivity().findViewById(R.id.highscoreText)

    // Game loop
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isGameLoopRunning) return

            if (isAdded && view != null && !isPaused && !isGameFinished) {
                updateGame()
            }

            if (isGameLoopRunning && isAdded && view != null) {
                handler.postDelayed(this, 16L)
            }
        }
    }

    private fun startGameLoop() {
        if (isGameLoopRunning) return

        isGameLoopRunning = true
        handler.removeCallbacks(gameRunnable)
        handler.post(gameRunnable)
    }

    private fun stopGameLoop() {
        isGameLoopRunning = false
        handler.removeCallbacks(gameRunnable)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {}
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        player = view.findViewById(R.id.player)
        gameArea = view.findViewById(R.id.mainGameFrame)
        pauseText = view.findViewById(R.id.pauseText)
        livesText = view.findViewById(R.id.livesText)

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

        // Button click sound
        clickbuttonSoundId = soundPool.load(requireContext(), R.raw.button_click, 1)

        // Load shooting sound
        shootSoundId = soundPool.load(requireContext(), R.raw.cannon_shot, 1)

        // Load enemy shooting sound
        enemyfireId = soundPool.load(requireContext(), R.raw.shoot, 1)

        // Load enemy killed sound
        enemykilledId = soundPool.load(requireContext(), R.raw.invaderkilled, 1)

        // Load enemy killed sound
        purpkilledId = soundPool.load(requireContext(), R.raw.ufo_highpitch, 1)

        // Spider Purple movement sound
        zigzagMoveSoundId = soundPool.load(requireContext(), R.raw.ufo_lowpitch, 1)

        // Load explosion sound
        explosionId = soundPool.load(requireContext(), R.raw.explosion, 1)

        // Load game over sound
        gameOverSoundId = soundPool.load(requireContext(), R.raw.game_over, 1)

        // Restore effects volume from prefs (Android Developers, 2025)
        val prefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedEffectsVolume = prefs.getFloat("effects_volume", 1.0f)
        SoundEffectsManager.updateVolume(savedEffectsVolume)

        // Initialize game
        gameArea.post {
            playerX = (gameArea.width - player.width) / 2f
            playerY = (gameArea.height - player.height).toFloat()
            player.x = playerX
            player.y = playerY

            spawnCurrentSet()
        }

        // Initialize UI
        updateLivesUI()
        updateScoreUI()
        updateHighScoreUI()
    }

    // Apply skin to player based on equipped skin
    private fun applyEquippedSkin() {
        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val equippedSkinName = prefs.getString("equippedSkin", "Moth") ?: "Moth"

        // Map name -> drawable key (Android Developers, 2025; Firebsae, 2025)
        val drawableKey = equippedSkinName.lowercase().replace(" ", "_")

        // Player sprite (Android Developers, 2025; Firebsae, 2025)
        val drawableId = requireContext().resources.getIdentifier(
            drawableKey,
            "drawable",
            requireContext().packageName
        )
        player.setImageResource(if (drawableId != 0) drawableId else R.drawable.moth)

        // Bullets
        currentBulletDrawable = drawableKey + "_blast"
    }

    override fun onResume() {
        super.onResume()

        // Get username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        // Sync player data if user is not Guest
        if (!username.equals("Guest", ignoreCase = true) && username.isNotBlank()) {
            MainActivity.PlayerDataSync.syncPlayerData(requireContext()) {
                if (isAdded && view != null) {
                    applyEquippedSkin()
                }
            }
        } else {
            // Default skin
            player.setImageResource(R.drawable.moth)
        }

        // Restart only one game loop
        startGameLoop()
    }

    override fun onPause() {
        super.onPause()

        // Stop the game loop while the app is in the background
        stopGameLoop()
    }

    override fun onDestroyView() {
        stopGameLoop()

        handler.removeCallbacksAndMessages(null)

        SoundEffectsManager.soundPool?.release()
        SoundEffectsManager.soundPool = null

        bullets.clear()
        enemies.clear()
        enemyBullets.clear()

        super.onDestroyView()
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

    // Enemy spawn logic
    private fun spawnCurrentSet() {
        val ctx = context ?: return

        // Clear old enemies
        enemies.forEach { gameArea.removeView(it.imageView) }
        enemies.clear()

        val set = mutableListOf<Enemy>()
        val enemyWidth = 100
        val enemyHeight = 100
        val spacingX = 40
        val spacingY = 40
        val startX = 50
        val startY = 50

        for (row in 0 until 3) {
            for (col in 0 until 5) {
                val enemyView = ImageView(ctx)

                // --- Special phases --- (Android Developers, 2025; Firebsae, 2025)
                val isSpecialBluePhase = currentLevel in 21..25
                val isSpecialMaroonPhase = currentLevel in 26..30

                // Zig-zagger only outside the blue-only phase
                val isZigZagger = !isSpecialBluePhase &&
                        (currentLevel % 10 == 0 && currentSetIndex == 0 && row == 0 && col == 2)

                // Shooter rules
                val isShooter = when {
                    isSpecialBluePhase -> false
                    isSpecialMaroonPhase -> (row == 1) // middle row shooters
                    else -> !isZigZagger && (
                            (currentLevel >= 5 && row == 1) ||
                                    (currentLevel in 15..20 && row == 2)
                            )
                }

                // Fire rate logic
                val fireChance = when {
                    isSpecialBluePhase -> 0
                    !isShooter -> 0
                    currentLevel >= 15 -> 15
                    else -> 5
                }

                // Pick sprite
                enemyView.setImageResource(
                    when {
                        isSpecialBluePhase -> R.drawable.spider_blue
                        isZigZagger -> R.drawable.spider_purple
                        isShooter -> R.drawable.spider_maroon
                        else -> R.drawable.spider_blue
                    }
                )

                // Position
                val x = startX + col * (enemyWidth + spacingX)
                val y = startY + row * (enemyHeight + spacingY)
                val params = FrameLayout.LayoutParams(enemyWidth, enemyHeight)
                enemyView.layoutParams = params
                enemyView.x = x.toFloat()
                enemyView.y = y.toFloat()
                gameArea.addView(enemyView)

                // Add enemy
                val enemy = Enemy(
                    imageView = enemyView,
                    isAlive = true,
                    startX = x.toFloat(),
                    startY = y.toFloat(),
                    isShooter = isShooter,
                    isZigZagger = isZigZagger,
                    fireChance = fireChance
                )

                set.add(enemy)
                enemies.add(enemy)

                // --- Special ZigZagger Effects --- (Android Developers, 2025; Firebsae, 2025)
                if (isZigZagger) {
                    // Looping sound effect
                    if (zigzagMoveSoundId != 0) SoundEffectsManager.playSound(zigzagMoveSoundId)
                }
            }
        }

        // Save this set
        if (currentSetIndex < enemySets.size) {
            enemySets[currentSetIndex] = set
        } else {
            enemySets.add(set)
        }

        // --- Speed Scaling --- (Android Developers, 2025; Firebsae, 2025)
        val offset = (currentLevel % 5)

        if (currentLevel in 21..25) {
            // Double base speed for blue-only levels
            if (offset == 0) {
                enemySpeed = 10f
                enemyFallSpeed = 2f
            } else {
                enemySpeed = 10f + (offset * 1.2f)
                enemyFallSpeed = 2f + (offset * 1.005f)
            }
        } else {
            // Normal scaling
            if (offset == 0) {
                enemySpeed = 5f
                enemyFallSpeed = 1f
            } else {
                enemySpeed = 5f + (offset * 1.2f)
                enemyFallSpeed = 1f + (offset * 1.005f)
            }
        }
    }

    private fun updateGame() {
        // Player bullets movement (Android Developers, 2025; Firebsae, 2025)
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
                    bullet.x < it.imageView.x + it.imageView.width &&
                    bullet.x + bullet.width > it.imageView.x &&
                    bullet.y < it.imageView.y + it.imageView.height &&
                    bullet.y + bullet.height > it.imageView.y }

            if (hitEnemy != null) {
                hitEnemy.isAlive = false
                hitEnemy.imageView.setImageResource(R.drawable.spider_death)

                // Play correct death sound
                if (hitEnemy.imageView.drawable.constantState ==
                    ContextCompat.getDrawable(requireContext(), R.drawable.spider_purple)?.constantState
                ) {
                    if (purpkilledId != 0) {
                        SoundEffectsManager.playSound(purpkilledId)
                    }
                } else {
                    if (enemykilledId != 0) {
                        SoundEffectsManager.playSound(enemykilledId)
                    }
                }

                handler.postDelayed({
                    val ga = gameArea
                    if (isAdded && ga != null && ga.isAttachedToWindow) {
                        ga.removeView(hitEnemy.imageView)
                    }
                }, 300)
                enemies.remove(hitEnemy)

                gameArea.removeView(bullet)
                bulletIterator.remove()

                score += when {
                    hitEnemy.isShooter -> 15
                    else -> 10
                }
                // Award bonus for zig-zag
                if (hitEnemy.isZigZagger) {
                    lives += 1
                    updateLivesUI()
                    score += 50
                }
                // Update UI
                updateScoreUI()

                // Award trophies
                checkAndAwardTrophies()
            }
        }

        // Enemy zig-zag
        var leftmost = Float.MAX_VALUE
        var rightmost = Float.MIN_VALUE
        enemies.forEach {
            if (it.isAlive) {
                leftmost = min(leftmost, it.imageView.x)
                rightmost = max(rightmost, it.imageView.x + it.imageView.width)
            }
        }
        if (leftmost <= 0f) enemyDirection = 1
        else if (rightmost >= gameArea.width) enemyDirection = -1

        enemies.forEach { enemy ->
            if (enemy.isAlive) {
                enemy.imageView.x += enemyDirection * enemySpeed
                enemy.imageView.y += enemyFallSpeed

                // Shooters fire randomly
                if (enemy.isShooter && (0..1000).random() < enemy.fireChance) {
                    shootEnemyBullet(enemy)

                    // Play enemy shooting sound (already loaded at startup) (Android Developers, 2025; Firebsae, 2025)
                    if (enemyfireId != 0) {
                        SoundEffectsManager.playSound(enemyfireId)
                    }
                }

                if (enemy.isZigZagger) {
                    // Moves independently in zig-zag (Android Developers, 2025; Firebsae, 2025)
                    enemy.imageView.x += enemyDirection * (enemySpeed * 2f)
                    enemy.imageView.y += enemyFallSpeed * 0f

                    if (enemy.imageView.x <= 0f || enemy.imageView.x + enemy.imageView.width >= gameArea.width) {
                        enemyDirection *= -1
                    }
                }

                // Enemy collides with player
                if (playerHitBy(enemy.imageView)) {
                    handlePlayerHit()
                    return
                }

                // Remove enemy if it goes offscreen
                if (enemy.imageView.y > gameArea.height) {
                    enemy.isAlive = false
                    gameArea.removeView(enemy.imageView)
                }
            }
        }

        // Enemy bullets movement (Android Developers, 2025; Firebsae, 2025)
        val enemyBulletIterator = enemyBullets.iterator()
        while (enemyBulletIterator.hasNext()) {
            val eBullet = enemyBulletIterator.next()
            eBullet.y += enemyBulletSpeed

            if (eBullet.y > gameArea.height) {
                gameArea.removeView(eBullet)
                enemyBulletIterator.remove()
                continue
            }

            // Bullet collides with player (Android Developers, 2025; Firebsae, 2025)
            if (playerHitBy(eBullet)) {
                gameArea.removeView(eBullet)
                enemyBulletIterator.remove()
                handlePlayerHit()
                return
            }
        }

        // Level progression (Android Developers, 2025; Firebsae, 2025)
        if (!isGameFinished && enemies.none { it.isAlive }) {
            if (currentSetIndex < setsPerLevel - 1) {
                currentSetIndex++
                spawnCurrentSet()
            } else {
                if (currentLevel < maxLevels) {
                    currentLevel++
                    currentSetIndex = 0
                    spawnCurrentSet()
                    pauseText.text = "LEVEL $currentLevel"
                    pauseText.visibility = View.VISIBLE
                    Handler(Looper.getMainLooper()).postDelayed({
                        pauseText.visibility = View.GONE
                    }, 1000)
                } else {
                    isGameFinished = true  // prevent gameOver from firing too
                    gameWin()
                }
            }
        }
    }

    private fun shootEnemyBullet(enemy: Enemy) {
        val ctx = context ?: return

        val bullet = ImageView(ctx).apply { setImageResource(R.drawable.spider_web_shot) }
        val bulletSize = 30
        val params = FrameLayout.LayoutParams(bulletSize, bulletSize)
        gameArea.addView(bullet, params)
        bullet.x = enemy.imageView.x + enemy.imageView.width / 2f - bulletSize / 2f
        bullet.y = enemy.imageView.y + enemy.imageView.height
        enemyBullets.add(bullet)
    }

    // Player hit logic (Android Developers, 2025; ChatGPT-4, 2025)
    private fun playerHitBy(view: ImageView): Boolean {
        if (isPaused || takingHit) return false

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

    private fun handlePlayerHit() {
        if (isPaused || isGameFinished) return  // prevent double calls

        // Play sound (already loaded at startup)
        if (explosionId != 0) SoundEffectsManager.playSound(explosionId)

        val prefs = requireActivity().getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)
        val equippedSkin = prefs.getString("equippedSkin", "Moth") ?: "Moth"
        val deathKey = equippedSkin.lowercase().replace(" ", "_") + "_death"
        val deathResId = requireContext().resources.getIdentifier(
            deathKey, "drawable", requireContext().packageName
        )
        player.setImageResource(if (deathResId != 0) deathResId else R.drawable.moth_death)
        loseLife()

        if (lives > 0) {
            isPaused = true
            enemies.forEach { gameArea.removeView(it.imageView) }
            enemies.clear()
            enemyBullets.forEach { gameArea.removeView(it) }
            enemyBullets.clear()

            Handler(Looper.getMainLooper()).postDelayed({
                resetGameState()
            }, 1000)
        } else {
            isGameFinished = true  // mark game as finished
            gameOver()
        }
    }

    private fun resetGameState() {
        // Get username from arguments
        val username = arguments?.getString("username") ?: "Guest"

        // Sync player data if user is not Guest (Android Developers, 2025; Firebsae, 2025)
        if (!username.equals("Guest", ignoreCase = true) && username.isNotBlank()) {
            // Apply skin once here
            applyEquippedSkin()
        } else {
            // Default skin (Android Developers, 2025; Firebsae, 2025)
            player.setImageResource(R.drawable.moth)
        }

        playerX = (gameArea.width - player.width) / 2f
        playerY = (gameArea.height - player.height).toFloat()
        player.x = playerX
        player.y = playerY

        enemies.clear()
        val set = enemySets[currentSetIndex]
        set.forEach {
            if (it.isAlive) {
                it.imageView.x = it.startX
                it.imageView.y = it.startY
                gameArea.addView(it.imageView)
                enemies.add(it)
            }
        }
        enemyBullets.clear()
        isPaused = false
    }

    private fun updateLivesUI() {
        livesText.text = "x$lives"
    }

    private fun updateScoreUI() {
        scoreText.text = "SCORE: $score"
    }

    private fun updateHighScoreUI() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val uid = currentUser?.uid ?: run {
            highScoreText.text = "HIGHSCORE: 0"
            return
        }

        val dbRef = FirebaseDatabase.getInstance()
            .getReference("players") // 🔒 Updated secure path
            .child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || view == null || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return
                }

                val storedHighscore = snapshot.child("highscore").getValue(Int::class.java) ?: 0
                highScoreText.text = "HIGHSCORE: $storedHighscore"
            }

            override fun onCancelled(error: DatabaseError) {
                highScoreText.text = "HIGHSCORE: 0"
            }
        })
    }

    fun togglePause(): Boolean {
        // Play button click sound
        if (clickbuttonSoundId != 0) SoundEffectsManager.playSound(clickbuttonSoundId)

        isPaused = !isPaused
        pauseText.visibility = if (isPaused) View.VISIBLE else View.GONE
        if (isPaused) pauseText.bringToFront()
        return isPaused
    }

    fun loseLife() {
        if (lives > 0) {
            lives--
            updateLivesUI()
            if (lives == 0) gameOver()
        }
    }

    fun saveHighScore() {
        // Register network callback (Android Developers, 2025; Firebsae, 2025)
        HighScoreManager.saveHighScore(requireContext().applicationContext, score)
    }

    private fun saveInGameCurrency() {
        // Register network callback (Android Developers, 2025; Firebsae, 2025)
        CurrencyManager.saveInGameCurrency(requireContext().applicationContext, score)
    }

    private fun checkAndAwardTrophies() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val dbRef = FirebaseDatabase.getInstance()
            .getReference("players") // 🔐 Updated path
            .child(uid)
            .child("trophies")

        val trophiesToAward = mutableListOf<Trophy>()

        // Level-based trophies
        when (currentLevel) {
            1 -> trophiesToAward.add(Trophy("trophy01", "Hero lvl 1", "Completed Level 1!", "lvl_trophy"))
            4 -> trophiesToAward.add(Trophy("trophy02", "Hero lvl 4", "Completed Level 4!", "lvl_trophy"))
            9 -> trophiesToAward.add(Trophy("trophy03", "Hero lvl 9", "Completed Level 9!", "lvl_trophy"))
            14 -> trophiesToAward.add(Trophy("trophy04", "Hero lvl 14", "Completed Level 14!", "lvl_trophy"))
            19 -> trophiesToAward.add(Trophy("trophy05", "Hero lvl 19", "Completed Level 19!", "lvl_trophy"))
        }

        // Score-based trophies
        if (score >= 1000) trophiesToAward.add(Trophy("trophy06", "New User", "Obtained A Score of 1000!", "score_trophy"))
        if (score >= 2500) trophiesToAward.add(Trophy("trophy07", "Space Cadet", "Obtained A Score of 2500!", "score_trophy"))
        if (score >= 5000) trophiesToAward.add(Trophy("trophy08", "Space Lieutenant", "Obtained A Score of 5000!", "score_trophy"))
        if (score >= 7500) trophiesToAward.add(Trophy("trophy09", "Space Captain", "Obtained A Score of 7500!", "score_trophy"))
        if (score >= 9000) trophiesToAward.add(Trophy("trophy10", "Galactic Trooper", "Obtained A Score of 9000!", "score_trophy"))
        if (score >= 10000) trophiesToAward.add(Trophy("trophy11", "Space Invader", "Obtained A Score of 10000!", "score_trophy"))

        // Save & show only NEW trophies
        trophiesToAward.forEach { trophy ->
            dbRef.child(trophy.id).get().addOnSuccessListener { snapshot ->

                // 🔒 SAFETY: Fragment may be detached
                if (!isAdded || view == null || !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return@addOnSuccessListener
                }

                if (!snapshot.exists()) {
                    // Award trophy in Firebase
                    dbRef.child(trophy.id).setValue(true)

                    // Show Snackbar safely
                    val rootView = requireView()
                    val snackbar = Snackbar.make(
                        rootView,
                        "🏆 ${trophy.name} unlocked!\n${trophy.description}",
                        Snackbar.LENGTH_LONG
                    )

                    // Position at top center
                    val snackbarView = snackbar.view
                    val params = snackbarView.layoutParams as FrameLayout.LayoutParams
                    params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                    params.topMargin = 100
                    snackbarView.layoutParams = params

                    // Optional styling
                    snackbarView.setBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            com.google.android.material.R.color.design_default_color_primary_variant
                        )
                    )

                    snackbar.show()
                }
            }.addOnFailureListener {
                // Optional: log error
                Log.e("Trophies", "Failed to check trophy ${trophy.id}: ${it.message}")
            }
        }
    }

    private fun gameOver() {
        if (isGameFinished) return
        isGameFinished = true
        isPaused = true

        // Stop the game loop
        handler.removeCallbacks(gameRunnable)

        pauseText.text = "GAME OVER"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        // Play sound (only once)
        if (gameOverSoundId != 0) {
            SoundEffectsManager.playSound(gameOverSoundId)
        }

        // Save progress safely (Android Developers, 2025; Firebsae, 2025)
        saveHighScore()
        saveInGameCurrency()
        checkAndAwardTrophies()

        // Delay return to Game Menu
        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().finish()
        }, 2000)
    }

    private fun gameWin() {
        if (isGameFinished) return
        isGameFinished = true
        isPaused = true

        // Stop the game loop
        handler.removeCallbacks(gameRunnable)

        pauseText.text = "BOSS MOVES DUDE!"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        // Play sound (only once)
        if (gameOverSoundId != 0) {
            SoundEffectsManager.playSound(gameOverSoundId)
        }

        // Save progress safely (Android Developers, 2025; Firebsae, 2025)
        saveHighScore()
        saveInGameCurrency()
        checkAndAwardTrophies()

        // Delay return to Game Menu
        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().finish()
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