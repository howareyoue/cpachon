package com.example.capchon;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeocodingResponse {
    @SerializedName("addresses")
    public List<Address> addresses;

    public static class Address {
        @SerializedName("x")
        public String x; // 경도
        @SerializedName("y")
        public String y; // 위도
    }
}
