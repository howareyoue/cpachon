package com.example.capchon;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;

        public static class Leg {
            public List<Step> steps;

            public static class Step {
                public List<Point> path; // 경로 점 리스트

                public static class Point {
                    public double latitude;  // 위도
                    public double longitude; // 경도
                }
            }
        }
    }
}
