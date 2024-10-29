package com.example.capchon;

import java.util.ArrayList;
import java.util.List;
import com.naver.maps.geometry.LatLng;

// DirectionsResponse 클래스
public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;

        public static class Leg {
            public List<Step> steps;

            public static class Step {
                public Location startLocation;
                public List<Location> path;

                public List<LatLng> getLatLngPath() {
                    List<LatLng> latLngPath = new ArrayList<>();
                    for (Location location : path) {
                        latLngPath.add(new LatLng(location.latitude, location.longitude));
                    }
                    return latLngPath;
                }
            }
        }
    }

    public static class Location {
        public double latitude;
        public double longitude;
    }
}
