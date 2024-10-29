package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NaverGeocodingService {
    @GET("map-geocode/v2/geocode")
    Call<GeocodingResponse> getCoordinates(
            @Query("query") String destination
    );
}
