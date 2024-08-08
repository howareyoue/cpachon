package com.example.capchon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationSource;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String TAG = "MapsNaverActivity";
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com";
    private static final String CLIENT_ID = "u6nzkkp800"; // 여기에 본인의 Client ID 입력
    private static final String CLIENT_SECRET = "pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1"; // 여기에 본인의 Client Secret 입력

    private MapView mapView;
    private NaverMap naverMap;
    private LocationSource locationSource;
    private Button btnRecommendRoute;
    private Button btnReturnToCurrentLocation;
    private EditText etWalkDuration;
    private Marker currentLocationMarker;
    private LatLng currentLatLng;

    private PolylineOverlay polyline; // 경로 표시를 위한 PolylineOverlay 객체

    private NaverDirectionsService directionsService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        mapView = findViewById(R.id.map_view);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        btnReturnToCurrentLocation = findViewById(R.id.btn_return_to_current_location);
        etWalkDuration = findViewById(R.id.et_walk_duration);
        mapView.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // Retrofit 설정
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        directionsService = retrofit.create(NaverDirectionsService.class);

        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                String durationStr = etWalkDuration.getText().toString();
                if (!durationStr.isEmpty()) {
                    int duration = Integer.parseInt(durationStr);
                    recommendWalkingRoute(duration);
                } else {
                    Toast.makeText(this, "산책 시간을 입력하세요", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReturnToCurrentLocation.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(currentLatLng));
            } else {
                Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        polyline = new PolylineOverlay(); // PolylineOverlay 객체 초기화
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);

        enableLocationTracking();

        // 초기 마커 설정
        currentLocationMarker = new Marker();
    }

    private void enableLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            LocationOverlay locationOverlay = naverMap.getLocationOverlay();
            locationOverlay.setVisible(true);

            LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(LocationRequest.create()
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                            .setInterval(10000)
                            .setFastestInterval(5000), new LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull LocationResult locationResult) {
                            Location location = locationResult.getLastLocation();
                            if (location != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // Check for valid latitude and longitude
                                if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
                                    currentLatLng = new LatLng(latitude, longitude);
                                    locationOverlay.setPosition(currentLatLng);

                                    // 현재 위치 마커 업데이트
                                    currentLocationMarker.setPosition(currentLatLng);
                                    if (currentLocationMarker.getMap() == null) {
                                        currentLocationMarker.setMap(naverMap);
                                    }

                                    // 카메라 위치도 현재 위치로 이동
                                    naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(currentLatLng));
                                } else {
                                    // Handle invalid coordinates
                                    Toast.makeText(MapsNaverActivity.this, "위치를 찾을수 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }, getMainLooper());
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void recommendWalkingRoute(int durationMinutes) {
        if (currentLatLng != null) {
            String start = currentLatLng.longitude + "," + currentLatLng.latitude;
            // 목표 지점을 현재 위치로부터 일정 거리의 위치로 설정 (예: 1km)
            LatLng goalLatLng = new LatLng(currentLatLng.latitude + 0.01, currentLatLng.longitude + 0.01);
            String goal = goalLatLng.longitude + "," + goalLatLng.latitude;

            Call<DirectionsResponse> call = directionsService.getWalkingRoute(CLIENT_ID, CLIENT_SECRET, start, goal, "traoptimal");

            call.enqueue(new Callback<DirectionsResponse>() {
                @Override
                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        DirectionsResponse directionsResponse = response.body();
                        List<DirectionsResponse.Traoptimal> routes = directionsResponse.route.traoptimal;
                        if (!routes.isEmpty()) {
                            List<LatLng> path = new ArrayList<>();
                            for (List<Double> point : routes.get(0).path) {
                                path.add(new LatLng(point.get(1), point.get(0)));
                            }

                            // 기존 경로 지우기
                            polyline.setMap(null);

                            // 새로운 경로 설정
                            polyline.setCoords(path);
                            polyline.setMap(naverMap);

                            Toast.makeText(MapsNaverActivity.this, durationMinutes + "분 산책 경로를 추천합니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapsNaverActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationTracking();
            } else {
                Toast.makeText(this, "위치 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
