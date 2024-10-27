package com.example.capchon;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("routes")
    public List<Route> routes;

    public class Route {
        @SerializedName("legs")
        public List<Leg> legs;
    }

    public class Leg {
        @SerializedName("steps")
        public List<Step> steps;
    }

    public class Step {
        @SerializedName("path")
        public List<Path> path;

        public class Path {
            @SerializedName("latitude")
            public double latitude;

            @SerializedName("longitude")
            public double longitude;
        }
    }
}
