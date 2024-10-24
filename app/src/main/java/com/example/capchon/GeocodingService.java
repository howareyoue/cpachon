package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface GeocodingService {
    @GET("/map-geocode/v2/geocode")
    Call<GeocodingResponse> getGeocode(
            @Header("X-NCP-APIGW-API-KEY-ID") String clientId,
            @Header("X-NCP-APIGW-API-KEY") String clientSecret,
            @Query("query") String query
    );
}
