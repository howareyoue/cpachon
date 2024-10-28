package com.example.capchon;

import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Coordinate> path;

        public static class Coordinate {
            public double x; // 경도 (longitude)
            public double y; // 위도 (latitude)
        }
    }
}


