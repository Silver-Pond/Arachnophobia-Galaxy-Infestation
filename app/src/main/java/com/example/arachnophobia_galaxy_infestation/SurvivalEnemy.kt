package com.example.arachnophobia_galaxy_infestation

import android.widget.ImageView

data class SurvivalEnemy(
    val view: ImageView,
    val type: String,
    val spawnX: Float,
    val spawnY: Float,
    val speed: Float,
    val pattern: String,
    var directionX: Int = 1 // +1 = right, -1 = left
)