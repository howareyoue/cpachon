package com.example.capchon;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    @SerializedName("route")
    public Route route;

    public static class Route {
        @SerializedName("traoptimal")
        public List<Traoptimal> traoptimal;
    }

    public static class Traoptimal {
        @SerializedName("summary")
        public Summary summary;
        @SerializedName("path")
        public List<List<Double>> path;
    }

    public static class Summary {
        @SerializedName("start")
        public Coord start;
        @SerializedName("goal")
        public Coord goal;
    }

    public static class Coord {
        @SerializedName("x")
        public Double x;
        @SerializedName("y")
        public Double y;
    }
}
