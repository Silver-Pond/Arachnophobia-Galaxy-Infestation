package com.example.arachnophobia_galaxy_infestation

import android.widget.ImageView

data class EnemyProjectile(
    val imageView: ImageView,
    var isAlive: Boolean = true
)