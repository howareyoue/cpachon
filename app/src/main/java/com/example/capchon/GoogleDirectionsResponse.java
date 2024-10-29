package com.example.capchon;

import java.util.List;

public class GoogleDirectionsResponse {
    public String status; // 추가된 status 필드
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
    }

    public static class Leg {
        public List<Step> steps;
    }

    public static class Step {
        public Location start_location;
        public Location end_location;
        // 추가 필드가 필요하면 여기에 작성하세요
    }

    public static class Location {
        public double lat;
        public double lng;
    }
}
