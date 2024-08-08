package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverDirectionsService {
    @GET("/map-direction/v1/driving")
    Call<DirectionsResponse> getDrivingRoute(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Query("start") String start,
            @Query("goal") String goal,
            @Query("option") String option
    );

    @GET("/map-direction/v1/walking")  // 도보 경로 API 엔드포인트
    Call<DirectionsResponse> getWalkingRoute(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Query("start") String start,
            @Query("goal") String goal,
            @Query("option") String option
    );
}
