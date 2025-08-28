package com.example.arachnophobia_galaxy_infestation;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitInterface {
    @POST("/create")
    Call<Void> executeSignUp(@Body HashMap<String, String> map);
    @POST("/login")
    Call<Player> executeLogin(@Body HashMap<String, String> map);
}
