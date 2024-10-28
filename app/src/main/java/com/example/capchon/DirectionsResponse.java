package com.example.capchon;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public Summary summary;  // 요약 정보만 사용하여 최단 경로 표시
    }

    public static class Summary {
        public Coordinate start;
        public Coordinate end;
    }

    public static class Coordinate {
        public double x;
        public double y;
    }
}

