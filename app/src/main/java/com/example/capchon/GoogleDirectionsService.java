package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface GoogleDirectionsService {
    @GET("directions/json") // 경로에서 'maps/api/'를 제거했습니다.
    Call<GoogleDirectionsResponse> getDirections(
            @Query("origin") String origin,
            @Query("destination") String destination,
            @Query("mode") String mode,
            @Query("key") String apiKey // API 키 추가
    );
}