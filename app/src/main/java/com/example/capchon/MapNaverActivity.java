package com.example.capchon;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.geometry.LatLng;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/";
    private static final String GOOGLE_API_KEY = "AIzaSyBtWaGATq4iQEsKT710EkGPkuNiRf84YHU"; // 자신의 Google API 키 입력
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000; // 추가된 상수

    private MapView mapView;
    private EditText etStartLocation, etDestination;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        mapView = findViewById(R.id.map_view);
        etStartLocation = findViewById(R.id.et_start_location);
        etDestination = findViewById(R.id.et_destination);
        Button btnMeasureDistance = findViewById(R.id.btn_measure_distance);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        btnMeasureDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                measureDistance();
            }
        });
    }

    private void measureDistance() {
        String startLocation = etStartLocation.getText().toString().trim();
        String destination = etDestination.getText().toString().trim();

        if (startLocation.isEmpty() || destination.isEmpty()) {
            Toast.makeText(this, "출발지와 목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 사용자가 입력한 출발지로 좌표 요청
        requestNaverGeocoding(startLocation, true); // 출발지 요청
        requestNaverGeocoding(destination, false); // 목적지 요청
    }

    private void requestNaverGeocoding(String query, boolean isStart) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);
        Call<GeocodingResponse> call = geocodingService.getCoordinates(query, "qeg3laengo", "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG");

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeocodingResponse.Address address = response.body().addresses.get(0);
                    LatLng location = new LatLng(Double.parseDouble(address.y), Double.parseDouble(address.x));

                    if (isStart) {
                        requestGoogleDirections(location, null); // 출발지 설정
                    } else {
                        requestGoogleDirections(null, location); // 목적지 설정
                    }
                } else {
                    Toast.makeText(MapNaverActivity.this, "주소를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Toast.makeText(MapNaverActivity.this, "주소 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestGoogleDirections(LatLng start, LatLng destination) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleDirectionsService directionsService = retrofit.create(GoogleDirectionsService.class);

        if (start != null && destination != null) {
            String startLocation = start.latitude + "," + start.longitude;
            String destinationLocation = destination.latitude + "," + destination.longitude;

            Call<GoogleDirectionsResponse> call = directionsService.getDirections(
                    startLocation,
                    destinationLocation,
                    "walking",
                    GOOGLE_API_KEY
            );

            call.enqueue(new Callback<GoogleDirectionsResponse>() {
                @Override
                public void onResponse(Call<GoogleDirectionsResponse> call, Response<GoogleDirectionsResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().routes != null && !response.body().routes.isEmpty()) {
                        GoogleDirectionsResponse.Route route = response.body().routes.get(0);
                        GoogleDirectionsResponse.Leg leg = route.legs.get(0);
                        String duration = leg.duration.text; // 소요 시간
                        String distance = leg.distance.text; // 거리

                        Toast.makeText(MapNaverActivity.this, "소요 시간: " + duration + "\n거리: " + distance, Toast.LENGTH_LONG).show();
                    } else {
                        Log.e("MapNaverActivity", "경로 요청 실패: " + response.errorBody());
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
    }

    @Override
    public void onMapReady(NaverMap naverMap) {
        this.naverMap = naverMap;

        // 위치 소스를 설정합니다.
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        naverMap.setLocationSource(locationSource);

        // 위치 권한 확인 및 요청
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            naverMap.setLocationTrackingMode(LocationTrackingMode.Follow); // 현재 위치 추적 모드 설정
        }

        // 기타 지도 설정...
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
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow); // 권한이 허용된 경우 현재 위치 추적 모드 설정
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
