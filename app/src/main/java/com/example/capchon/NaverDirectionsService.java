package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverDirectionsService {
    @GET("map-direction/v1/driving")
    Call<DirectionsResponse> getDirections(
            @Query("start") String start,
            @Query("end") String end,
            @Query("option") String option,
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret
    );
}

