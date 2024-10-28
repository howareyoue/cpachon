package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface NaverDirectionsService {
    @GET("https://naveropenapi.apigw.gov-ntruss.com/map-geocode/v2/geocode")
    Call<GeocodingResponse> getCoordinates(
            @Query("query") String query,
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret
    );

    @GET("https://naveropenapi.apigw.gov-ntruss.com/map-direction/v1/driving")
    Call<DirectionsResponse> getDirections(
            @Query("start") String start,
            @Query("goal") String end,
            @Query("option") String option,
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret
    );
}
