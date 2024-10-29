package com.example.capchon;

import java.util.List;

public class GeocodingResponse {
    public List<Address> addresses;

    public static class Address {
        public String roadAddress; // 도로명 주소
        public String jibunAddress; // 지번 주소
        public String x; // 경도
        public String y; // 위도
    }
}