package com.example.capchon;

import java.util.List;

public class GoogleDirectionsResponse {
    public String status;
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;
    }

    public static class Leg {
        public List<Step> steps;
        public Distance distance;
        public Duration duration;
    }

    public static class Step {
        public Location start_location;
        public Location end_location;
    }

    public static class Location {
        public double lat;
        public double lng;
    }

    public static class Distance {
        public String text;
        public int value;
    }

    public static class Duration {
        public String text;
        public int value;
    }
}
