package com.example.arachnophobia_galaxy_infestation

import com.google.gson.annotations.SerializedName

data class Player(
    val id: String = "",
    @SerializedName("username")
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val highscore: Int = 0
)