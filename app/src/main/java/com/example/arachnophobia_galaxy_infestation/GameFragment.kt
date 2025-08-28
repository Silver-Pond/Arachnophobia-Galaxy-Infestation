package com.example.arachnophobia_galaxy_infestation

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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
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
    private var highscore = 0

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

    private val scoreText: TextView
        get() = requireActivity().findViewById(R.id.scoreText)
    private val highScoreText: TextView
        get() = requireActivity().findViewById(R.id.highscoreText)

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (!isPaused) updateGame()
            handler.postDelayed(this, 16)
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

        player = view.findViewById(R.id.player)
        gameArea = view.findViewById(R.id.mainGameFrame)
        pauseText = view.findViewById(R.id.pauseText)
        livesText = view.findViewById(R.id.livesText)

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
        handler.post(gameRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(gameRunnable)
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

        val bullet = ImageView(requireContext())
        bullet.setImageResource(R.drawable.moth_blast)
        val bulletSize = 40
        val params = FrameLayout.LayoutParams(bulletSize, bulletSize)
        gameArea.addView(bullet, params)
        bullet.x = player.x + player.width / 2f - bulletSize / 2f
        bullet.y = player.y - bulletSize
        bullets.add(bullet)
    }

    // Enemy spawn logic
    private fun spawnCurrentSet() {
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
                val enemyView = ImageView(requireContext())

                val isShooter = (currentLevel >= 6 && row == 1) // Row 2 enemies shoot from level 6+
                val isShooterx2 = (currentLevel >= 10 && row == 2) // Row 3 enemies shoot from level 10+
                if (isShooterx2) {
                    enemyView.setImageResource(R.drawable.spider_maroon) // NEW type
                } else if(isShooter){
                    enemyView.setImageResource(R.drawable.spider_maroon) // NEW type
                } else {
                    enemyView.setImageResource(R.drawable.spider_blue)
                }
                // Enemy position
                val x = startX + col * (enemyWidth + spacingX)
                val y = startY + row * (enemyHeight + spacingY)

                // Enemy size
                val params = FrameLayout.LayoutParams(enemyWidth, enemyHeight)
                enemyView.layoutParams = params
                enemyView.x = x.toFloat()
                enemyView.y = y.toFloat()

                gameArea.addView(enemyView)

                val enemy = Enemy(enemyView, true, x.toFloat(), y.toFloat(), isShooter)
                set.add(enemy)
                enemies.add(enemy)
            }
        }
        // Enemy set index
        if (currentSetIndex < enemySets.size) {
            enemySets[currentSetIndex] = set
        } else {
            enemySets.add(set)
        }
        // Enemy speed restriction
        if(currentLevel == 5 || currentLevel == 10 || currentLevel == 15){
            enemySpeed = 5f
            enemyFallSpeed = 1f
        }
        // Enemy speed
        enemySpeed = 5f + (currentLevel - 1) * 1.2f
        // Enemy fall speed
        enemyFallSpeed = 1f + (currentLevel - 1) * 1.005f
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
                Handler(Looper.getMainLooper()).postDelayed({
                    gameArea.removeView(hitEnemy.imageView)
                }, 300)
                enemies.remove(hitEnemy)

                gameArea.removeView(bullet)
                bulletIterator.remove()

                score += when {
                    hitEnemy.isShooter -> 15
                    else -> 10
                }
                // Update UI
                updateScoreUI()
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
        if (enemies.none { it.isAlive }) {
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
                    gameWin()
                }
            }
        }
    }

    private fun shootEnemyBullet(enemy: Enemy) {
        // Create enemy bullet
        val bullet = ImageView(requireContext())
        bullet.setImageResource(R.drawable.spider_web_shot)
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
        if (isPaused) return

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

    private fun updateHighScoreUI(){

        val username = arguments?.getString("username").toString()
        val dbRef = FirebaseDatabase.getInstance().getReference("highscores").child(username)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val storedHighscore = snapshot.child("highscore").getValue(Int::class.java)
                    highScoreText.text = "HIGHSCORE: $storedHighscore"
                } else {
                    highScoreText.text = "HIGHSCORE: $highscore"
                }
            }

            override fun onCancelled(error: DatabaseError) {}
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
        // Save highscore to Firebase
        val dbRef = FirebaseDatabase.getInstance().getReference("highscores")
        val username = arguments?.getString("username") ?: "Guest"

        dbRef.child(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Username exists, check existing highscore
                    val existingHighscore = snapshot.child("highscore").getValue(Int::class.java) ?: 0

                    if (score > existingHighscore) {
                        // Update only if the new score is higher
                        dbRef.child(username).child("highscore").setValue(score)
                            .addOnSuccessListener {
                                // Toast.makeText(requireContext(), "Highscore updated!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                // Toast.makeText(requireContext(), "Failed to update highscore", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Toast.makeText(requireContext(), "Your score is not higher than the current highscore", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Username does not exist, create new entry
                    val user = mapOf(
                        "username" to username,
                        "highscore" to score
                    )

                    dbRef.child(username).setValue(user)
                        .addOnSuccessListener {
                            // Toast.makeText(requireContext(), "New user created with highscore!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            // Toast.makeText(requireContext(), "Failed to save highscore", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun gameOver() {
        isPaused = true
        pauseText.text = "GAME OVER"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        // Save Highscore
        saveHighScore()

        Handler(Looper.getMainLooper()).postDelayed({
            requireActivity().finish()
        }, 1000)
    }

    private fun gameWin() {
        isPaused = true
        pauseText.text = "VICTORY!"
        pauseText.visibility = View.VISIBLE
        pauseText.bringToFront()

        // Save Highscore
        saveHighScore()

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
