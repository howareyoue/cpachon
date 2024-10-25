package com.example.capchon;

import java.util.List;

public class GeocodingResponse {
    public List<GeocodingResult> addresses;

    public static class GeocodingResult {
        public String roadAddress;
        public String jibunAddress;
        public Double x; // 경도
        public Double y; // 위도
    }
}
