package com.example.arachnophobia_galaxy_infestation

import android.widget.ImageView

data class SurvivalEnemy(
    val view: ImageView,
    val type: String,
    val spawnX: Float,
    val spawnY: Float,
    var speed: Float,
    val pattern: String,
    var directionX: Int = 1,
    var isAlive: Boolean = true
)