package com.example.capchon;

import java.util.List;

public class DirectionsResponse {
    public Route route;

    public static class Route {
        public List<List<Double>> path;
    }
}
