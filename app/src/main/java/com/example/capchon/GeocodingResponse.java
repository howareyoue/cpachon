package com.example.capchon;

import java.util.List;

public class GeocodingResponse {
    public List<Address> addresses;

    public static class Address {
        public String roadAddress;
        public String jibunAddress;
        public double x; // 경도
        public double y; // 위도
    }
}
