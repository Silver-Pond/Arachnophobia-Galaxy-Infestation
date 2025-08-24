package com.example.arachnophobia_galaxy_infestation

import android.widget.ImageView

class Enemy(val imageView: ImageView,
            var isAlive: Boolean = true,
            val startX: Float,
            val startY: Float,
            val isShooter: Boolean = false){}