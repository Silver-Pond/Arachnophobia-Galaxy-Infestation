package com.example.arachnophobia_galaxy_infestation

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.18.57:5000/"
    // For Android Emulator. Use your PC’s local IP if testing on real device, e.g. "http://192.168.1.42:5000/"

    val instance: LevelApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(LevelApi::class.java)
    }
}
