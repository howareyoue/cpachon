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
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/";
    private static final String GOOGLE_API_KEY = "AIzaSyBtWaGATq4iQEsKT710EkGPkuNiRf84YHU";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private EditText etStartLocation, etDestination;
    private Button btnRecommendRoute;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapView = findViewById(R.id.map_view);
        etStartLocation = findViewById(R.id.et_start_location);
        etDestination = findViewById(R.id.et_destination);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        polyline = new PolylineOverlay();

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                LatLng startLatLng = currentLatLng; // 현재 위치를 출발지로 설정
                String destination = etDestination.getText().toString();
                getCoordinatesFromNaver(startLatLng, destination);
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

    private void getCoordinatesFromNaver(LatLng start, String destination) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);

        Call<GeocodingResponse> call = geocodingService.getCoordinates(
                destination,
                CLIENT_ID,
                "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG"
        );

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                    LatLng destinationLatLng = new LatLng(
                            Double.parseDouble(response.body().addresses.get(0).y),
                            Double.parseDouble(response.body().addresses.get(0).x)
                    );
                    Log.d("MapNaverActivity", "목적지 좌표: " + destinationLatLng.latitude + ", " + destinationLatLng.longitude);
                    setDestinationMarker(destinationLatLng);
                    requestGoogleDirections(start, destinationLatLng);
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

        Log.d("MapNaverActivity", "출발지: " + startPoint + ", 도착지: " + endPoint);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleDirectionsService directionsService = retrofit.create(GoogleDirectionsService.class);

        directionsService.getDirections(startPoint, endPoint, "driving", GOOGLE_API_KEY)
                .enqueue(new Callback<GoogleDirectionsResponse>() {
                    @Override
                    public void onResponse(Call<GoogleDirectionsResponse> call, Response<GoogleDirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String responseStatus = response.body().status;

                            Log.d("MapNaverActivity", "응답 상태: " + responseStatus);
                            if ("OK".equals(responseStatus)) {
                                List<LatLng> routeCoords = new ArrayList<>();
                                for (GoogleDirectionsResponse.Route route : response.body().routes) {
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
                            } else if ("ZERO_RESULTS".equals(responseStatus)) {
                                Toast.makeText(MapNaverActivity.this, "해당 거리에서는 경로가 없습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("MapNaverActivity", "경로 요청 실패: " + responseStatus);
                                Toast.makeText(MapNaverActivity.this, "경로 요청 실패: " + responseStatus, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e("MapNaverActivity", "경로 요청 실패: 상태 코드 - " + response.code());
                            Toast.makeText(MapNaverActivity.this, "경로 요청 실패", Toast.LENGTH_SHORT).show();
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
