package com.example.capchon;

import java.util.List;

public class GoogleDirectionsResponse {
    public String status;
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
    }

    public static class Leg {
        public Distance distance; // 거리 정보 추가
        public Duration duration; // 소요 시간 정보 추가
        public List<Step> steps;
    }

    public static class Distance {
        public String text; // 거리
        public int value; // 거리 (미터 단위)
    }

    public static class Duration {
        public String text; // 소요 시간
        public int value; // 소요 시간 (초 단위)
    }

    public static class Step {
        public Location start_location;
        public Location end_location;
    }

    public static class Location {
        public double lat;
        public double lng;
    }
}
