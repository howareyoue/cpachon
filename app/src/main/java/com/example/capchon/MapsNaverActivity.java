package com.example.capchon;

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

public class MapsNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "u6nzkkp800"; // 네이버 Client ID
    private static final String CLIENT_SECRET = "pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1"; // 네이버 Client Secret
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private NaverDirectionsService directionsService;
    private GeocodingService geocodingService;
    private EditText etDestination;
    private Button btnRecommendRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        etDestination = findViewById(R.id.et_destination);

        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

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
        geocodingService = retrofit.create(GeocodingService.class);

        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                recommendWalkingRoute();
            }
        });

        polyline = new PolylineOverlay(); // 경로 표시를 위한 PolylineOverlay 객체 초기화
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        naverMap.addOnLocationChangeListener(location -> {
            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLatLng);
            naverMap.moveCamera(cameraUpdate);
        });
    }

    private void recommendWalkingRoute() {
        String destination = etDestination.getText().toString().trim();

        if (destination.isEmpty()) {
            Toast.makeText(this, "목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        getLatLngFromGeocoding(destination, goalLatLng -> {
            if (goalLatLng == null) {
                Toast.makeText(MapsNaverActivity.this, "유효하지 않은 목적지입니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentLatLng != null) {
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
                                    List<LatLng> coords = new ArrayList<>();
                                    for (List<Double> point : path) {
                                        if (point.size() >= 2) {
                                            coords.add(new LatLng(point.get(1), point.get(0)));
                                        }
                                    }

                                    polyline.setMap(null);
                                    polyline.setCoords(coords);
                                    polyline.setMap(naverMap);

                                    Toast.makeText(MapsNaverActivity.this, "최적의 경로를 추천합니다.", Toast.LENGTH_SHORT).show();
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
            } else {
                Toast.makeText(MapsNaverActivity.this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getLatLngFromGeocoding(String destination, GeocodingCallback callback) {
        Call<GeocodingResponse> call = geocodingService.getGeocode(CLIENT_ID, CLIENT_SECRET, destination);
        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeocodingResponse geocodingResponse = response.body();
                    if (geocodingResponse != null && geocodingResponse.addresses != null && !geocodingResponse.addresses.isEmpty()) {
                        GeocodingResponse.Address address = geocodingResponse.addresses.get(0);
                        LatLng latLng = new LatLng(Double.parseDouble(address.y), Double.parseDouble(address.x));
                        callback.onLatLngReceived(latLng);
                    } else {
                        Log.e("Geocoding", "유효하지 않은 주소입니다."); // 유효하지 않은 주소 로깅
                        callback.onLatLngReceived(null);
                    }
                } else {
                    Log.e("Geocoding", "API 요청 실패: " + response.message()); // API 요청 실패 로깅
                    callback.onLatLngReceived(null);
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Log.e("Geocoding", "API 호출 실패: " + t.getMessage(), t);
                callback.onLatLngReceived(null);
            }
        });
    }

    private interface GeocodingCallback {
        void onLatLngReceived(LatLng latLng);
    }
}
