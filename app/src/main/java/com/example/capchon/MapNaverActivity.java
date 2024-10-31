package com.example.capchon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
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
    private TextView tvDistanceTime;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapsView = findViewById(R.id.maps_view);
        etStartLocation = findViewById(R.id.start_location);
        etDestination = findViewById(R.id.destination);
        tvDistanceTime = findViewById(R.id.tv_distance_time);
        Button btnSetMarkers = findViewById(R.id.btn_markers);

        mapsView.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();

        btnSetMarkers.setOnClickListener(v -> {
            String startLocation = etStartLocation.getText().toString();
            String destinationLocation = etDestination.getText().toString();

            if (!startLocation.isEmpty() && !destinationLocation.isEmpty()) {
                setMarkersByAddress(startLocation, destinationLocation);
            } else {
                Toast.makeText(this, "출발지와 목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        showCurrentLocation();
    }

    private void setupLocationUpdates() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult != null && locationResult.getLocations().size() > 0) {
                    for (Location location : locationResult.getLocations()) {
                        updateCurrentLocationMarker(location);
                    }
                }
            }
        };
    }

    private void updateCurrentLocationMarker(Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (currentLocationMarker == null) {
            currentLocationMarker = new Marker();
            currentLocationMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_current_location));
        }
        currentLocationMarker.setPosition(currentLatLng);
        currentLocationMarker.setMap(naverMap);

        naverMap.moveCamera(CameraUpdate.scrollTo(currentLatLng));
    }

    private void setMarkersByAddress(String startAddress, String endAddress) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);

        geocodingService.getCoordinates(startAddress, CLIENT_ID, "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG")
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                            GeocodingResponse.Address startLocation = response.body().addresses.get(0);
                            LatLng startLatLng = new LatLng(Double.parseDouble(startLocation.y), Double.parseDouble(startLocation.x));

                            if (startMarker == null) {
                                startMarker = new Marker();
                            }
                            startMarker.setPosition(startLatLng);
                            startMarker.setMap(naverMap);

                            geocodingService.getCoordinates(endAddress, CLIENT_ID, "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG")
                                    .enqueue(new Callback<GeocodingResponse>() {
                                        @Override
                                        public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                                            if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                                                GeocodingResponse.Address endLocation = response.body().addresses.get(0);
                                                LatLng endLatLng = new LatLng(Double.parseDouble(endLocation.y), Double.parseDouble(endLocation.x));

                                                if (destinationMarker == null) {
                                                    destinationMarker = new Marker();
                                                }
                                                destinationMarker.setPosition(endLatLng);
                                                destinationMarker.setMap(naverMap);

                                                LatLng midPoint = new LatLng(
                                                        (startLatLng.latitude + endLatLng.latitude) / 2,
                                                        (startLatLng.longitude + endLatLng.longitude) / 2
                                                );
                                                naverMap.moveCamera(CameraUpdate.scrollTo(midPoint));

                                                Location startLoc = new Location("startLoc");
                                                startLoc.setLatitude(startLatLng.latitude);
                                                startLoc.setLongitude(startLatLng.longitude);

                                                Location destLoc = new Location("destLoc");
                                                destLoc.setLatitude(endLatLng.latitude);
                                                destLoc.setLongitude(endLatLng.longitude);

                                                float distance = startLoc.distanceTo(destLoc);
                                                int walkingSpeedMps = 1;
                                                int travelTimeSec = (int) (distance / walkingSpeedMps);

                                                // 시간을 초에서 분 단위로 변환합니다.
                                                int travelTimeMin = travelTimeSec / 60;
                                                int travelTimeRemainingSec = travelTimeSec % 60;

                                                String distanceText = String.format("거리: %.2f m", distance);
                                                String timeText = String.format("예상 소요 시간: %d 분 %d 초", travelTimeMin, travelTimeRemainingSec);
                                                tvDistanceTime.setText(distanceText + "\n" + timeText);

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateCurrentLocationMarker(location);
                    } else {
                        Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapsView.onLowMemory();
    }
}