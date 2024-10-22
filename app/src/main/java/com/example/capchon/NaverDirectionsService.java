package com.example.capchon;

import com.example.capchon.DirectionsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverDirectionsService {
    @GET("/map-direction/v1/driving")
    Call<DirectionsResponse> getWalkingRoute(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Query("start") String start,
            @Query("goal") String goal,
            @Query("option") String option
    );
}
