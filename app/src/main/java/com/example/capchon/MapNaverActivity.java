package com.example.capchon;

import android.annotation.SuppressLint;
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
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "qeg3laengo"; // Naver Cloud Platform 클라이언트 ID
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapsView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;

    private Marker startMarker;
    private Marker destinationMarker;
    private Marker userLocationMarker;

    private EditText etStartLocation;
    private EditText etDestination;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // 네이버 지도 SDK 클라이언트 설정
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapsView = findViewById(R.id.maps_view);
        etStartLocation = findViewById(R.id.start_location);  // 출발지 EditText
        etDestination = findViewById(R.id.destination);  // 목적지 EditText
        Button btnSetMarkers = findViewById(R.id.btn_markers);  // 마커 설정 버튼

        // 지도 비동기 초기화
        mapsView.getMapAsync(this);

        // 버튼 클릭 시 마커 설정
        btnSetMarkers.setOnClickListener(v -> {
            String startLocation = etStartLocation.getText().toString();
            String destinationLocation = etDestination.getText().toString();

            if (!startLocation.isEmpty() && !destinationLocation.isEmpty()) {
                setMarkers(startLocation, destinationLocation);
            } else {
                Toast.makeText(this, "출발지와 목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // 사용자 위치 업데이트를 위한 FusedLocationSource 설정
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        naverMap.setLocationSource(locationSource);

        // 실시간 사용자 위치 마커
        userLocationMarker = new Marker();
        naverMap.addOnLocationChangeListener(location -> {
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            userLocationMarker.setPosition(userLocation);
            userLocationMarker.setMap(naverMap);
        });
    }

    private void setMarkers(String start, String end) {
        // 기존 마커 제거
        if (startMarker != null) {
            startMarker.setMap(null);
        }
        if (destinationMarker != null) {
            destinationMarker.setMap(null);
        }

        // 사용자가 입력한 출발지와 목적지에 대한 경도/위도 좌표 변환 코드 필요
        // 예시: 지오코딩 API를 활용하여 입력된 주소를 좌표로 변환

        // 좌표 변환이 완료된 후 마커를 설정합니다.
        LatLng startLatLng = getLatLngFromLocation(start); // 사용자가 입력한 출발지의 좌표
        LatLng endLatLng = getLatLngFromLocation(end); // 사용자가 입력한 목적지의 좌표

        // 출발지 마커 설정
        startMarker = new Marker();
        startMarker.setPosition(startLatLng);
        startMarker.setMap(naverMap);

        // 목적지 마커 설정
        destinationMarker = new Marker();
        destinationMarker.setPosition(endLatLng);
        destinationMarker.setMap(naverMap);

        // 카메라를 출발지로 이동
        naverMap.moveCamera(CameraUpdate.scrollTo(startLatLng));
    }

    private LatLng getLatLngFromLocation(String location) {
        // 여기에 지오코딩 API를 통해 주소를 위도/경도로 변환하는 로직을 추가해야 합니다.
        // 현재는 예시로 기본 좌표를 반환합니다.
        return new LatLng(0, 0); // 실제 구현 필요
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
