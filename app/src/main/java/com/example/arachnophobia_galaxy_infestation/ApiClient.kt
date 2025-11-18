package com.example.arachnophobia_galaxy_infestation

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // Base URL for the API (Android Developers, 2025)
    private const val BASE_URL = "https://arachnophobia-api.onrender.com"

    val instance: LevelApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Logs requests & responses
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // Retrofit instance (Android Developers, 2025)
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(LevelApi::class.java)
    }
}
/*
 * Reference List
 *
 * Android Developers, 2025. Developer centers. [online]. Available at:
 * https://developer.android.com/
 * [Accessed: 6 October 2025].
 */