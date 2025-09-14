package com.example.arachnophobia_galaxy_infestation

import android.content.Context
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
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
import kotlin.math.max
import kotlin.math.min

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [GameFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GameFragment : Fragment() {
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

    // Levels and waves
    private val enemySets = mutableListOf<List<Enemy>>()
    private var currentSetIndex = 0
    private var currentLevel = 1
    private val maxLevels = 20
    private val setsPerLevel = 4
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
        }
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

    override fun onResume() {
        super.onResume()
        handler.post(gameRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(gameRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop ALL pending tasks (not just the game loop)
        handler.removeCallbacksAndMessages(null)

        SoundEffectsManager.soundPool?.release()
        SoundEffectsManager.soundPool = null

        bullets.clear()
        enemies.clear()
        enemyBullets.clear()
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

        val bullet = ImageView(ctx).apply { setImageResource(R.drawable.moth_blast) }
        val bulletSize = 40
        val params = FrameLayout.LayoutParams(bulletSize, bulletSize)
        ga.addView(bullet, params)
        bullet.x = p.x + p.width / 2f - bulletSize / 2f
        bullet.y = p.y - bulletSize
        bullets.add(bullet)

        // Play sound (already loaded at startup)
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

                // Special zig-zag enemy: only 1 in row 0, col 2, on levels 10,20,30...
                val isZigZagger = (currentLevel % 10 == 0 && row == 0 && col == 2)

                // Shooter rules:
                // Row 1 becomes shooter from level 5+
                // Row 2 becomes shooter only from level 15–20
                val isShooter =
                    !isZigZagger && ( // zig-zag overrides shooter type
                            (currentLevel >= 5 && row == 1) ||
                                    (currentLevel in 15..20 && row == 2)
                            )

                // Pick sprite based on type
                enemyView.setImageResource(
                    when {
                        isZigZagger -> R.drawable.spider_purple
                        isShooter -> R.drawable.spider_maroon
                        else -> R.drawable.spider_blue
                    }
                )

                // Enemy position
                val x = startX + col * (enemyWidth + spacingX)
                val y = startY + row * (enemyHeight + spacingY)

                // Enemy size
                val params = FrameLayout.LayoutParams(enemyWidth, enemyHeight)
                enemyView.layoutParams = params
                enemyView.x = x.toFloat()
                enemyView.y = y.toFloat()

                gameArea.addView(enemyView)

                // Add enemy with zig-zag flag
                val enemy = Enemy(
                    imageView = enemyView,
                    isAlive = true,
                    startX = x.toFloat(),
                    startY = y.toFloat(),
                    isShooter = isShooter,
                    isZigZagger = isZigZagger // <-- new flag in Enemy class
                )

                set.add(enemy)
                enemies.add(enemy)
            }
        }

        // Save this set
        if (currentSetIndex < enemySets.size) {
            enemySets[currentSetIndex] = set
        } else {
            enemySets.add(set)
        }

        // Enemy speed scaling
        val offset = (currentLevel % 5)
        if (offset == 0) {
            enemySpeed = 5f
            enemyFallSpeed = 1f
        } else {
            enemySpeed = 5f + (offset * 1.2f)
            enemyFallSpeed = 1f + (offset * 1.005f)
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

            val hitEnemy = enemies.firstOrNull { it.isAlive &&
                    bullet.x < it.imageView.x + it.imageView.width &&
                    bullet.x + bullet.width > it.imageView.x &&
                    bullet.y < it.imageView.y + it.imageView.height &&
                    bullet.y + bullet.height > it.imageView.y }

            if (hitEnemy != null) {
                hitEnemy.isAlive = false
                hitEnemy.imageView.setImageResource(R.drawable.spider_death)

                // Play enemy death sound (already loaded at startup)
                if (enemykilledId != 0) SoundEffectsManager.playSound(enemykilledId)

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
                if (enemy.isShooter && (0..1000).random() < 5) {
                    shootEnemyBullet(enemy)

                    // Play enemy shooting sound (already loaded at startup)
                    if (enemyfireId != 0) {
                        SoundEffectsManager.playSound(enemyfireId)
                    }
                }

                if (enemy.isZigZagger) {
                    // Moves independently in zig-zag
                    enemy.imageView.x += enemyDirection * (enemySpeed * 2f)
                    enemy.imageView.y += enemyFallSpeed * 6.5f

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

        // Enemy bullets movement
        val enemyBulletIterator = enemyBullets.iterator()
        while (enemyBulletIterator.hasNext()) {
            val eBullet = enemyBulletIterator.next()
            eBullet.y += enemyBulletSpeed

            if (eBullet.y > gameArea.height) {
                gameArea.removeView(eBullet)
                enemyBulletIterator.remove()
                continue
            }

            // Bullet collides with player
            if (playerHitBy(eBullet)) {
                gameArea.removeView(eBullet)
                enemyBulletIterator.remove()
                handlePlayerHit()
                return
            }
        }

        // Level progression
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

        player.setImageResource(R.drawable.moth_death)
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
        player.setImageResource(R.drawable.moth)
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
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: "Guest"  // fallback for guest users
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val storedHighscore = snapshot.child("highscore").getValue(Int::class.java) ?: 0
                highScoreText.text = "HIGHSCORE: $storedHighscore"
            }

            override fun onCancelled(error: DatabaseError) {
                highScoreText.text = "HIGHSCORE: 0"
            }
        })
    }


    fun togglePause(): Boolean {
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

    private fun saveHighScore() {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return  // Ensure user is logged in
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Guest"

                val showToasts = username.isNotBlank() && !username.equals("guest", ignoreCase = true)

                if (snapshot.exists()) {
                    val existingHighscore = snapshot.child("highscore").getValue(Int::class.java) ?: 0

                    if (score > existingHighscore) {
                        dbRef.child("highscore").setValue(score)
                            .addOnSuccessListener {
                                if (showToasts) {
                                    Toast.makeText(requireContext(), "New Highscore!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                if (showToasts) {
                                    Toast.makeText(requireContext(), "Failed to update highscore", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    val user = mapOf(
                        "id" to uid,
                        "username" to username.ifBlank { "Guest" },
                        "email" to (snapshot.child("email").getValue(String::class.java) ?: ""),
                        "password" to (snapshot.child("password").getValue(String::class.java) ?: ""),
                        "spider_silk" to (snapshot.child("spider_silk").getValue(String::class.java)),
                        "highscore" to score
                    )

                    dbRef.setValue(user)
                        .addOnSuccessListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "Player created with highscore!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "Failed to save highscore", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveInGameCurrency() {
        val spider_silk = (score * 0.5) / 100
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return
        val dbRef = FirebaseDatabase.getInstance().getReference("players").child(uid)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "Guest"
                val showToasts = username.isNotBlank() && !username.equals("Guest", ignoreCase = true)

                if (snapshot.exists()) {
                    val currentSilk = snapshot.child("spider_silk").getValue(Double::class.java) ?: 0.0
                    val updatedSilk = currentSilk + spider_silk

                    dbRef.child("spider_silk").setValue(updatedSilk)
                        .addOnSuccessListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "You have earned $spider_silk silk!", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    val user = mapOf(
                        "id" to uid,
                        "username" to username.ifBlank { "Guest" },
                        "email" to (snapshot.child("email").getValue(String::class.java) ?: ""),
                        "password" to (snapshot.child("password").getValue(String::class.java) ?: ""),
                        "highscore" to (snapshot.child("highscore").getValue(Int::class.java) ?: 0),
                        "spider_silk" to spider_silk
                    )

                    dbRef.setValue(user)
                        .addOnSuccessListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "Player created with spider silk!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            if (showToasts) {
                                Toast.makeText(requireContext(), "Failed to save spider silk!", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
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

        // Save & show toast only for NEW trophies
        trophiesToAward.forEach { trophy ->
            dbRef.child(trophy.id).get().addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    // Award trophy in Firebase
                    dbRef.child(trophy.id).setValue(true)

                    // Show toast at the top
                    val toast = Toast.makeText(
                        requireContext(),
                        "🏆 ${trophy.name} unlocked!\n${trophy.description}",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 100)
                    toast.show()
                }
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

        // Save Highscore & Currency
        saveHighScore()
        saveInGameCurrency()

        // Award trophies
        checkAndAwardTrophies()

        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().finish()
        }, 2000)

        // Play sound
        if (gameOverSoundId != 0) SoundEffectsManager.playSound(gameOverSoundId)
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

        // Save Highscore & Currency
        saveHighScore()
        saveInGameCurrency()

        // Award trophies
        checkAndAwardTrophies()

        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().finish()
        }, 2000)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            GameFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}