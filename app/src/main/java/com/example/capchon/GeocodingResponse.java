package com.example.capchon;

import java.util.List;

public class GeocodingResponse {
    public List<Address> addresses;

    public static class Address {
        public String roadAddress; // 도로명 주소
        public double x; // 경도
        public double y; // 위도
    }
}
