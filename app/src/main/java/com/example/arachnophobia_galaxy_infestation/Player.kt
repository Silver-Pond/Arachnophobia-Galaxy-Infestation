package com.example.arachnophobia_galaxy_infestation

import com.google.gson.annotations.SerializedName

data class Player(
    val id: String = "",
    @SerializedName("username")
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val highscore: Int = 0,
    val spider_silk: Double = 0.00,
    val trophies: List<Trophy> = emptyList()
)