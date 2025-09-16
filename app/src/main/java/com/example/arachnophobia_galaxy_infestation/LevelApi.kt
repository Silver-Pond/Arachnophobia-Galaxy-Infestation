package com.example.arachnophobia_galaxy_infestation

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface LevelApi {
    @GET("api/levels/{levelNumber}")
    fun getLevel(@Path("levelNumber") levelNumber: Int): Call<Level>
}
