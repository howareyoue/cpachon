package com.example.capchon;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.LocationTrackingMode;
import android.content.pm.PackageManager;

import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.ArrayList;
import java.util.List;

public class MapsNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    private PathOverlay currentPath; // 현재 경로를 저장할 변수
    private Marker destinationMarker; // 목적지 마커를 저장할 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // 지도 초기화
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map_view);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map_view, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 위치 소스 초기화
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // 버튼 및 입력 필드 설정
        EditText etDestination = findViewById(R.id.et_destination);
        Button btnSetDestination = findViewById(R.id.btn_find_destination);

        // 버튼 클릭 이벤트 (목적지 설정)
        btnSetDestination.setOnClickListener(v -> {
            String destinationAddress = etDestination.getText().toString();
            if (!destinationAddress.isEmpty()) {
                getGeocode(destinationAddress); // Geocoding API 호출
            } else {
                Toast.makeText(this, "목적지를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap map) {
        naverMap = map;

        // 현재 위치 활성화
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
    }

    // 주소를 위도/경도로 변환하는 함수
    private void getGeocode(String address) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GeocodingService service = retrofit.create(GeocodingService.class);
        Call<GeocodingResponse> call = service.getGeocode("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET", address);

        call.enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<GeocodingResult> results = response.body().getResults();
                    if (!results.isEmpty()) {
                        LatLng destination = new LatLng(results.get(0).getLatitude(), results.get(0).getLongitude());
                        drawPathToDestination(destination);

                        // 카메라 이동
                        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(destination);
                        naverMap.moveCamera(cameraUpdate);
                    } else {
                        Toast.makeText(MapsNaverActivity.this, "목적지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MapsNaverActivity.this, "Geocoding API 호출 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Toast.makeText(MapsNaverActivity.this, "API 호출 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 경로 그리기 함수
    private void drawPathToDestination(LatLng destination) {
        Location currentLocation = locationSource.getLastLocation();

        if (currentLocation != null) {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            // 이전 경로 및 마커 제거
            if (currentPath != null) {
                currentPath.setMap(null);
            }
            if (destinationMarker != null) {
                destinationMarker.setMap(null);
            }

            // 새로운 경로를 요청
            requestRoute(currentLatLng, destination);
        } else {
            Toast.makeText(this, "현재 위치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // 네이버 API를 사용하여 경로 요청
    private void requestRoute(LatLng start, LatLng destination) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverDirectionsService service = retrofit.create(NaverDirectionsService.class);

        String startCoords = start.latitude + "," + start.longitude; // "위도,경도"
        String goalCoords = destination.latitude + "," + destination.longitude; // "위도,경도"

        Call<DirectionsResponse> call = service.getWalkingRoute("YOUR_CLIENT_ID", "YOUR_CLIENT_SECRET",
                startCoords, goalCoords, ""); // 옵션 비워두기

        call.enqueue(new Callback<DirectionsResponse>() {
            @Override
            public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // 경로를 받아와서 PathOverlay로 그리기
                    List<LatLng> pathCoords = new ArrayList<>();
                    for (RoutePoint point : response.body().getRoutePoints()) {
                        pathCoords.add(new LatLng(point.getLatitude(), point.getLongitude()));
                    }
                    drawPath(pathCoords);
                } else {
                    Toast.makeText(MapsNaverActivity.this, "경로를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                Toast.makeText(MapsNaverActivity.this, "API 호출 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void drawPath(List<LatLng> pathCoords) {
        // 기존 경로 제거
        if (currentPath != null) {
            currentPath.setMap(null);
        }

        // 새 경로 그리기
        currentPath = new PathOverlay();
        currentPath.setCoords(pathCoords);
        currentPath.setMap(naverMap);

        // 목적지 마커 표시
        if (destinationMarker != null) {
            destinationMarker.setMap(null);
        }
        destinationMarker = new Marker();
        destinationMarker.setPosition(pathCoords.get(pathCoords.size() - 1)); // 경로의 마지막 지점
        destinationMarker.setMap(naverMap);
    }

    // 위치 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults);
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
