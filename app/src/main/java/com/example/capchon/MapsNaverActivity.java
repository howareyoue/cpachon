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
    private static final String CLIENT_ID = "YOUR_REAL_CLIENT_ID"; // 여기에 본인의 Client ID 입력
    private static final String CLIENT_SECRET = "YOUR_REAL_CLIENT_SECRET"; // 여기에 본인의 Client Secret 입력

    private MapView mapView;
    private NaverMap naverMap;
    private LocationSource locationSource;
    private Button btnRecommendRoute;
    private Button btnReturnToCurrentLocation;
    private EditText etDestination;
    private Marker currentLocationMarker;
    private LatLng currentLatLng;

    private NaverDirectionsService directionsService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        mapView = findViewById(R.id.map_view);
        etDestination = findViewById(R.id.et_destination);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        btnReturnToCurrentLocation = findViewById(R.id.btn_return_to_current_location);
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
                String destination = etDestination.getText().toString();
                if (!destination.isEmpty()) {
                    recommendWalkingRoute(destination);
                } else {
                    Toast.makeText(this, "Please enter a destination.", Toast.LENGTH_SHORT).show();
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
                                    Toast.makeText(MapsNaverActivity.this, "Invalid location coordinates", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }, getMainLooper());
        } else {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void recommendWalkingRoute(String destination) {
        if (currentLatLng != null) {
            // 여기에서 도착지 주소를 지오코딩하여 위도와 경도로 변환하는 로직이 필요합니다.
            // 예를 들어, 도착지를 좌표로 변환하는 API를 사용합니다.
            LatLng destinationLatLng = getLatLngFromAddress(destination);
            if (destinationLatLng == null) {
                Toast.makeText(this, "Invalid destination address.", Toast.LENGTH_SHORT).show();
                return;
            }

            String start = currentLatLng.longitude + "," + currentLatLng.latitude;
            String goal = destinationLatLng.longitude + "," + destinationLatLng.latitude;

            directionsService.getDrivingRoute(CLIENT_ID, CLIENT_SECRET, start, goal, "trafast")
                    .enqueue(new Callback<DirectionsResponse>() {
                        @Override
                        public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                DirectionsResponse directionsResponse = response.body();
                                List<LatLng> path = new ArrayList<>();
                                for (List<Double> coord : directionsResponse.route.traoptimal.get(0).path) {
                                    path.add(new LatLng(coord.get(1), coord.get(0)));
                                }

                                PolylineOverlay polyline = new PolylineOverlay();
                                polyline.setCoords(path);
                                polyline.setMap(naverMap);

                                Toast.makeText(MapsNaverActivity.this, "경로를 추천합니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                try {
                                    String errorBody = response.errorBody().string();
                                    Toast.makeText(MapsNaverActivity.this, "경로를 가져오는 데 실패했습니다: " + errorBody, Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "경로 응답 실패: " + errorBody);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(MapsNaverActivity.this, "경로를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "경로 응답 실패", e);
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                            Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "경로 요청 실패", t);
                        }
                    });
        } else {
            Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 도착지 주소를 지오코딩하여 LatLng로 변환하는 메서드 (구현 필요)
    private LatLng getLatLngFromAddress(String address) {
        // 실제 구현에서는 지오코딩 API를 호출하여 주소를 LatLng로 변환합니다.
        // 여기서는 예시로 단순히 null을 반환합니다.
        // 네이버 지도 API의 지오코딩 서비스를 사용할 수 있습니다.
        return null;
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
