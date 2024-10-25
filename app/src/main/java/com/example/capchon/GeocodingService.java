package com.example.capchon;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface GeocodingService {
    @GET("map-geocode/v2/geocode")
    Call<GeocodingResponse> getGeocode(
            @Header("u6nzkkp800") String clientId,
            @Header("pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1") String clientSecret,
            @Query("query") String address
    );
}
