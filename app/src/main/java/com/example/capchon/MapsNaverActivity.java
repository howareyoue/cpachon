package com.example.capchon;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.LocationTrackingMode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsNaverActivity extends AppCompatActivity implements OnMapReadyCallback, NaverMap.OnLocationChangeListener {

    private static final String CLIENT_ID = "u6nzkkp800"; // 네이버 Client ID
    private static final String CLIENT_SECRET = "pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1"; // 네이버 Client Secret
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private static final float THRESHOLD_DISTANCE = 20;  // 경로에서 벗어난 거리 임계값 (단위: 미터)

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private NaverDirectionsService directionsService;
    private GeocodingService geocodingService;
    private EditText etDestination;
    private Button btnRecommendRoute;

    private List<LatLng> routeCoords;  // 경로 좌표 저장 리스트
    private int currentStepIndex = 0;  // 현재 경로의 단계

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // Naver Map SDK 초기화
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        // View 초기화
        mapView = findViewById(R.id.map_view);
        etDestination = findViewById(R.id.et_destination);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        polyline = new PolylineOverlay(); // 경로 표시를 위한 PolylineOverlay 객체 초기화

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        directionsService = retrofit.create(NaverDirectionsService.class);
        geocodingService = retrofit.create(GeocodingService.class);

        // 경로 추천 버튼 클릭 리스너
        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                recommendWalkingRoute();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        // 현재 위치 가져오기
        naverMap.addOnLocationChangeListener(this);
    }

    @Override
    public void onLocationChange(@NonNull Location location) {
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatLng);
        naverMap.moveCamera(cameraUpdate);

        if (routeCoords != null && !routeCoords.isEmpty()) {
            guideUserAlongRoute(location);  // 실시간 경로 안내
        }
    }

    // 경로 추천 기능
    private void recommendWalkingRoute() {
        LatLng currentLatLng = new LatLng(34.896049, 126.589651); // 현재 위치 (국립목포대학교 공과대학 4호관 좌표)
        LatLng goalLatLng = new LatLng(37.566084, 126.977243);    // 청계중학교 좌표

        if (currentLatLng != null && goalLatLng != null) {
            String start = currentLatLng.longitude + "," + currentLatLng.latitude;
            String goal = goalLatLng.longitude + "," + goalLatLng.latitude;

            Call<DirectionsResponse> call = directionsService.getWalkingRoute(CLIENT_ID, CLIENT_SECRET, start, goal, "shortest");

            call.enqueue(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DirectionsResponse directionsResponse = response.body();
                        if (directionsResponse != null && directionsResponse.route != null) {
                            List<List<Double>> path = directionsResponse.route.path;
                            if (path != null && !path.isEmpty()) {
                                routeCoords = new ArrayList<>();
                                for (List<Double> point : path) {
                                    if (point.size() >= 2) {
                                        routeCoords.add(new LatLng(point.get(1), point.get(0)));
                                    }
                                }

                                // 경로를 지도에 표시
                                polyline.setMap(null);
                                polyline.setCoords(routeCoords);
                                polyline.setMap(naverMap);

                                // 안내 메시지
                                Toast.makeText(MapsNaverActivity.this, "경로를 따라 이동하세요.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MapsNaverActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e("NAVER_MAPS", "경로 요청 실패: " + response.message());
                        Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    Log.e("NAVER_MAPS", "경로 요청 실패: " + t.getMessage(), t);
                    Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // 경로를 따라가며 실시간 안내
    private void guideUserAlongRoute(Location location) {
        if (currentStepIndex < routeCoords.size()) {
            LatLng target = routeCoords.get(currentStepIndex);
            Location targetLocation = new Location("");
            targetLocation.setLatitude(target.latitude);
            targetLocation.setLongitude(target.longitude);

            float distanceToNextStep = location.distanceTo(targetLocation);

            if (distanceToNextStep < THRESHOLD_DISTANCE) {
                currentStepIndex++;  // 다음 경유지로 넘어가기
                if (currentStepIndex < routeCoords.size()) {
                    LatLng nextStep = routeCoords.get(currentStepIndex);
                    Toast.makeText(MapsNaverActivity.this, "다음 경유지로 이동: " + nextStep.latitude + ", " + nextStep.longitude, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MapsNaverActivity.this, "목적지에 도착했습니다!", Toast.LENGTH_LONG).show();
                }
            } else {
                // 경로에서 벗어났는지 확인
                if (distanceToNextStep > THRESHOLD_DISTANCE * 5) {
                    Toast.makeText(MapsNaverActivity.this, "경로에서 벗어났습니다. 다시 경로를 탐색합니다.", Toast.LENGTH_SHORT).show();
                    recommendWalkingRoute();  // 경로 재계산
                }
            }
        }
    }
}
