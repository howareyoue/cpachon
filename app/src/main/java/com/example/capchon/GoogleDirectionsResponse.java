package com.example.capchon;

import java.util.List;

public class GoogleDirectionsResponse {
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
    }

    public static class Location {
        public double lat;
        public double lng;
    }
}
