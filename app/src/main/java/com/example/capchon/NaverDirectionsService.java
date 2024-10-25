package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverDirectionsService {

    @GET("map-direction/v1/driving")
    Call<DirectionsResponse> getWalkingRoute(
            @Header("u6nzkkp800") String clientId,
            @Header("pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1") String clientSecret,
            @Query("start") String start,
            @Query("goal") String goal,
            @Query("option") String option // 예: "traoptimal" 최적 경로
    );
}
