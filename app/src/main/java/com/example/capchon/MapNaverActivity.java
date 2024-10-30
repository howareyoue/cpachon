package com.example.capchon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "qeg3laengo"; // Naver Cloud Platform 클라이언트 ID
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/"; // Naver API 기본 URL
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapsView;
    private NaverMap naverMap;

    private Marker startMarker;
    private Marker destinationMarker;
    private Marker currentLocationMarker;

    private EditText etStartLocation;
    private EditText etDestination;

    private FusedLocationProviderClient fusedLocationClient;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // 네이버 지도 SDK 클라이언트 설정
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapsView = findViewById(R.id.maps_view);
        etStartLocation = findViewById(R.id.start_location);
        etDestination = findViewById(R.id.destination);
        Button btnSetMarkers = findViewById(R.id.btn_markers);

        // 지도 비동기 초기화
        mapsView.getMapAsync(this);

        // FusedLocationProviderClient 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 버튼 클릭 시 마커 설정
        btnSetMarkers.setOnClickListener(v -> {
            String startLocation = etStartLocation.getText().toString();
            String destinationLocation = etDestination.getText().toString();

            if (!startLocation.isEmpty() && !destinationLocation.isEmpty()) {
                setMarkersByAddress(startLocation, destinationLocation);
            } else {
                Toast.makeText(this, "출발지와 목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 현재 위치 표시 호출
        showCurrentLocation();
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
    }

    private void setMarkersByAddress(String startAddress, String endAddress) {
        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);

        // 출발지 좌표 요청
        geocodingService.getCoordinates(startAddress, CLIENT_ID, "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG")
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                            GeocodingResponse.Address startLocation = response.body().addresses.get(0);
                            LatLng startLatLng = new LatLng(Double.parseDouble(startLocation.y), Double.parseDouble(startLocation.x));

                            // 출발지 마커 설정
                            if (startMarker == null) {
                                startMarker = new Marker();
                            }
                            startMarker.setPosition(startLatLng);
                            startMarker.setMap(naverMap);

                            // 목적지 좌표 요청
                            geocodingService.getCoordinates(endAddress, CLIENT_ID, "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG")
                                    .enqueue(new Callback<GeocodingResponse>() {
                                        @Override
                                        public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                                            if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                                                GeocodingResponse.Address endLocation = response.body().addresses.get(0);
                                                LatLng endLatLng = new LatLng(Double.parseDouble(endLocation.y), Double.parseDouble(endLocation.x));

                                                // 목적지 마커 설정
                                                if (destinationMarker == null) {
                                                    destinationMarker = new Marker();
                                                }
                                                destinationMarker.setPosition(endLatLng);
                                                destinationMarker.setMap(naverMap);

                                                // 카메라를 출발지와 목적지 중심으로 이동
                                                LatLng midPoint = new LatLng(
                                                        (startLatLng.latitude + endLatLng.latitude) / 2,
                                                        (startLatLng.longitude + endLatLng.longitude) / 2
                                                );
                                                naverMap.moveCamera(CameraUpdate.scrollTo(midPoint));
                                            } else {
                                                Toast.makeText(MapNaverActivity.this, "목적지 좌표 요청 실패", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                                            Toast.makeText(MapNaverActivity.this, "목적지 좌표 요청 실패", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(MapNaverActivity.this, "출발지 좌표 요청 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                        Toast.makeText(MapNaverActivity.this, "출발지 좌표 요청 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showCurrentLocation() {
        // 위치 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없을 경우 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // 현재 위치 가져오기
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // 현재 위치 마커 설정
                        if (currentLocationMarker == null) {
                            currentLocationMarker = new Marker();
                        }
                        currentLocationMarker.setPosition(currentLatLng);
                        currentLocationMarker.setMap(naverMap);

                        // 벡터 드로어블을 사용하여 아이콘 설정
                        currentLocationMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_current_location));

                        // 카메라를 현재 위치로 이동
                        naverMap.moveCamera(CameraUpdate.scrollTo(currentLatLng));
                    } else {
                        Toast.makeText(MapNaverActivity.this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 승인된 경우 현재 위치를 다시 표시
                showCurrentLocation();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapsView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapsView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapsView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapsView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapsView.onDestroy();
    }
}
