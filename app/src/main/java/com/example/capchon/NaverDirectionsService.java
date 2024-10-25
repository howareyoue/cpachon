package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverDirectionsService {
    @GET("map-direction/v1/walking")
    Call<DirectionsResponse> getWalkingRoute(
            @Header("u6nzkkp800") String clientId,
            @Header("IcZEWMnaSNuwEzEuebVII3IUUUlzxoGZvz23NaNR") String clientSecret,
            @Query("start") String start,
            @Query("goal") String goal,
            @Query("option") String option
    );
}
