package com.example.arachnophobia_galaxy_infestation

import com.google.gson.annotations.SerializedName

data class Player(
    val id: String = "",
    @SerializedName("username")
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val highscore: Int = 0,
    val survivalhighscore: Int = 0,
    val spider_silk: Double = 0.00,

    // Existing trophies
    val trophies: List<Trophy> = emptyList(),

    // Updated: list of skins the player owns
    val ownedSkins: List<String> = listOf("Moth", "Super Mario", "Space Invader"),

    // Track currently equipped skin
    val equippedSkin: String = "Moth" // already owned skin
)