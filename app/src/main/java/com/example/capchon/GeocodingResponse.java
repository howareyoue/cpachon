package com.example.capchon;

import java.util.List;

public class GeocodingResponse {
    public List<Address> addresses; // 주소 정보 리스트

    public static class Address {
        public String x; // 경도 (Longitude)
        public String y; // 위도 (Latitude)
    }
}
