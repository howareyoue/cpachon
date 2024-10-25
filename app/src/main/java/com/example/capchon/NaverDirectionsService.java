package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface NaverDirectionsService {

    // Geocoding API
    @Headers("X-Naver-Client-Id: " + MapsNaverActivity.CLIENT_ID + "\nX-Naver-Client-Secret: " + MapsNaverActivity.CLIENT_SECRET)
    @GET("v1/geocode")
    Call<GeocodingResponse> getCoordinates(@Query("query") String address, String clientSecret, String s);

    // Directions API
    @Headers("X-Naver-Client-Id: " + MapsNaverActivity.CLIENT_ID + "\nX-Naver-Client-Secret: " + MapsNaverActivity.CLIENT_SECRET)
    @GET("v1/directions")
    Call<DirectionsResponse> getWalkingRoute(@Query("start") String start, @Query("goal") String goal, @Query("option") String option, String s, String shortest);
}
