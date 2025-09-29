package com.example.arachnophobia_galaxy_infestation

data class SurvivalEnemy (
    val type: String = "spider_blue",
    val spawnX: Float,
    val spawnY: Float,
    val speed: Float,
    val pattern: String = "straight"
)