package com.example.capchon;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
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

public class MapsNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "u6nzkkp800";
    private static final String CLIENT_SECRET = "pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1"; // 본인의 Naver Client Secret로 변경
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private NaverDirectionsService directionsService;
    private EditText etDestination; // 목적지 입력 필드 추가
    private Button btnRecommendRoute;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // 목적지 입력 필드 초기화
        etDestination = findViewById(R.id.et_destination);

        // Naver Map SDK 초기화
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        // View 초기화
        mapView = findViewById(R.id.map_view);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        directionsService = retrofit.create(NaverDirectionsService.class);

        // 버튼 클릭 리스너 설정
        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                recommendWalkingRoute();
            }
        });

        polyline = new PolylineOverlay(); // 경로 표시를 위한 PolylineOverlay 객체 초기화
    }

    @Override
    public void onMapReady(NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        // 위치 추적 모드 설정
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow); // 위치 추적 모드 설정
    }

    // 경로 추천 기능 추가
    private void recommendWalkingRoute() {
        String destination = etDestination.getText().toString().trim();

        if (destination.isEmpty()) {
            Toast.makeText(this, "목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 사용자 입력 목적지의 좌표를 얻는 로직
        LatLng goalLatLng = getLatLngFromDestination(destination);

        if (goalLatLng == null) {
            Toast.makeText(this, "유효하지 않은 목적지입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 현재 위치가 존재하는 경우에만 경로 요청
        if (currentLatLng != null) {
            String start = currentLatLng.longitude + "," + currentLatLng.latitude;
            String goal = goalLatLng.longitude + "," + goalLatLng.latitude;

            Call<DirectionsResponse> call = directionsService.getWalkingRoute(CLIENT_ID, CLIENT_SECRET, start, goal, "traoptimal");

            call.enqueue(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DirectionsResponse directionsResponse = response.body();
                        if (directionsResponse != null && directionsResponse.route != null) {
                            List<List<Double>> path = directionsResponse.route.path; // 기존 경로 경로 가져오기

                            if (path != null && !path.isEmpty()) {
                                List<LatLng> coords = new ArrayList<>();
                                for (List<Double> point : path) {
                                    if (point.size() >= 2) {
                                        coords.add(new LatLng(point.get(1), point.get(0))); // 위도, 경도 순서
                                    }
                                }

                                // 기존 경로 지우기
                                polyline.setMap(null);

                                // 새로운 경로 설정
                                polyline.setCoords(coords);
                                polyline.setMap(naverMap);

                                Toast.makeText(MapsNaverActivity.this, "최적의 경로를 추천합니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(MapsNaverActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Log.e(TAG, "경로 요청 실패: " + response.message());
                        Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    Log.e(TAG, "경로 요청 실패: " + t.getMessage(), t);
                    Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 목적지를 좌표로 변환하는 메소드 (실제 서비스에서는 Geocoding API 등을 이용해 구현해야 함)
    private LatLng getLatLngFromDestination(String destination) {
        // 여기서 목적지를 Geocoding API를 통해 좌표로 변환할 수 있습니다.
        // 예를 들어 Naver 또는 Google Geocoding API를 사용해 목적지명을 좌표로 변환합니다.
        // 현재는 예시로 서울시청의 좌표를 반환하는 로직입니다.
        return new LatLng(37.5665, 126.9780); // 서울시청 좌표
    }
}
