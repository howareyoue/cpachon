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
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.NaverMapSdk;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "qeg3laengo";
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/"; // 수정된 BASE_URL
    private static final String GOOGLE_API_KEY = "AIzaSyBtWaGATq4iQEsKT710EkGPkuNiRf84YHU";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private EditText etDestination;
    private Button btnRecommendRoute;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapView = findViewById(R.id.map_view);
        etDestination = findViewById(R.id.et_destination);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        polyline = new PolylineOverlay();

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                getCoordinatesFromNaver(etDestination.getText().toString());
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        naverMap.addOnLocationChangeListener(location -> {
            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        });
    }

    // 목적지 좌표 검색 메서드
    private void getCoordinatesFromNaver(String destination) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")  // Naver Geocoding API 주소
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);

        // Naver Geocoding API 호출 시 필요한 헤더를 추가합니다.
        Call<GeocodingResponse> call = geocodingService.getCoordinates(
                destination,
                CLIENT_ID, // 클라이언트 ID 입력
                "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG" // 시크릿 키 입력
        );

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                    LatLng destinationLatLng = new LatLng(
                            Double.parseDouble(response.body().addresses.get(0).y), // y값을 문자열로 변환 후 파싱
                            Double.parseDouble(response.body().addresses.get(0).x)  // x값을 문자열로 변환 후 파싱
                    );
                    Log.d("MapNaverActivity", "목적지 좌표: " + destinationLatLng.latitude + ", " + destinationLatLng.longitude);
                    setDestinationMarker(destinationLatLng);
                    requestGoogleDirections(currentLatLng, destinationLatLng);
                } else {
                    Log.e("MapNaverActivity", "서버 응답 실패: " + response.errorBody());
                    Toast.makeText(MapNaverActivity.this, "목적지의 좌표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Log.e("MapNaverActivity", "지오코딩 요청 실패: " + t.getMessage());
                Toast.makeText(MapNaverActivity.this, "지오코딩 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setDestinationMarker(LatLng destinationLatLng) {
        destinationMarker = new Marker();
        destinationMarker.setPosition(destinationLatLng);
        destinationMarker.setMap(naverMap);
        naverMap.moveCamera(CameraUpdate.scrollTo(destinationLatLng));
    }

    private void requestGoogleDirections(LatLng start, LatLng end) {
        String startPoint = start.latitude + "," + start.longitude;
        String endPoint = end.latitude + "," + end.longitude;

        Log.d("MapNaverActivity", "출발지: " + startPoint + ", 도착지: " + endPoint); // 로그 추가

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL) // BASE_URL은 그대로 유지
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleDirectionsService directionsService = retrofit.create(GoogleDirectionsService.class);

        // 차량 경로 요청 (mode는 driving으로 설정)
        directionsService.getDirections(startPoint, endPoint, "driving", GOOGLE_API_KEY) // API 호출 시 경로 추가
                .enqueue(new Callback<GoogleDirectionsResponse>() {
                    @Override
                    public void onResponse(Call<GoogleDirectionsResponse> call, Response<GoogleDirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            GoogleDirectionsResponse directionsResponse = response.body();
                            Log.d("GoogleDirections", "응답: " + directionsResponse.toString()); // 응답 로그 출력
                            Log.d("GoogleDirections", "상태: " + directionsResponse.status); // 상태 로그 추가

                            // 경로가 존재하는지 확인
                            if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                                // 경로가 있는 경우
                                List<LatLng> routeCoords = new ArrayList<>();
                                for (GoogleDirectionsResponse.Route route : directionsResponse.routes) {
                                    for (GoogleDirectionsResponse.Leg leg : route.legs) {
                                        for (GoogleDirectionsResponse.Step step : leg.steps) {
                                            routeCoords.add(new LatLng(step.start_location.lat, step.start_location.lng));
                                        }
                                    }
                                }

                                if (routeCoords.size() >= 2) {
                                    polyline.setMap(null);
                                    polyline.setCoords(routeCoords);
                                    polyline.setMap(naverMap);
                                    naverMap.moveCamera(CameraUpdate.scrollTo(end));
                                } else {
                                    Toast.makeText(MapNaverActivity.this, "경로 좌표가 충분하지 않습니다.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // 경로가 없는 경우
                                Toast.makeText(MapNaverActivity.this, "응답에 경로가 없습니다.", Toast.LENGTH_SHORT).show();
                                Log.e("MapNaverActivity", "응답에 경로가 없습니다.");
                            }
                        } else {
                            // 응답이 성공적이지 않은 경우
                            Log.e("MapNaverActivity", "경로 요청 실패: " + response.errorBody());
                            try {
                                String errorResponse = response.errorBody().string();
                                Log.e("MapNaverActivity", "응답 내용: " + errorResponse);
                            } catch (Exception e) {
                                Log.e("MapNaverActivity", "오류 발생: " + e.getMessage());
                            }
                            Toast.makeText(MapNaverActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GoogleDirectionsResponse> call, Throwable t) {
                        Log.e("MapNaverActivity", "경로 요청 실패: " + t.getMessage());
                        Toast.makeText(MapNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
