package com.example.capchon;

import java.util.List;

public class DirectionsResponse {

    public Route route;

    public static class Route {
        public List<List<Double>> path; // 경로 좌표 리스트 (경로를 따라 그릴 좌표들)
    }
}
