package com.example.capchon;

import com.naver.maps.geometry.LatLng; // 네이버 지도 LatLng
import java.util.ArrayList;
import java.util.List;

public class DirectionsResponse {
    public List<Route> routes;

    public static class Route {
        public List<Leg> legs;

        public static class Leg {
            public List<Step> steps;

            public static class Step {
                public List<List<Double>> path;  // 각 경로 점 (위도, 경도)

                // path 리스트를 LatLng로 변환할 수 있도록 메서드 추가
                public List<LatLng> getLatLngPath() {
                    List<LatLng> latLngPath = new ArrayList<>();
                    for (List<Double> point : path) {
                        if (point.size() >= 2) {
                            latLngPath.add(new LatLng(point.get(1), point.get(0)));  // 위도, 경도 순서
                        }
                    }
                    return latLngPath;
                }
            }
        }
    }
}
