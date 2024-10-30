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
        etStartLocation = findViewById(R.id.start_location);  // 수정된 부분
        etDestination = findViewById(R.id.destination);
        Button btnSetMarkers = findViewById(R.id.btn_markers);

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
        // start 및 end 문자열을 경도/위도 좌표로 변환하는 코드 추가 필요 (지오코딩 API 활용 가능)

        // 예시 좌표 설정 - 실제 지오코딩 API로 변환 필요
        LatLng startLatLng = new LatLng(35.237196, 126.411049); // 샘플 출발지 좌표
        LatLng endLatLng = new LatLng(35.235671, 126.409723); // 샘플 목적지 좌표

        // 출발지 마커 설정
        if (startMarker == null) {
            startMarker = new Marker();
        }
        startMarker.setPosition(startLatLng);
        startMarker.setMap(naverMap);

        // 목적지 마커 설정
        if (destinationMarker == null) {
            destinationMarker = new Marker();
        }
        destinationMarker.setPosition(endLatLng);
        destinationMarker.setMap(naverMap);

        // 카메라를 출발지로 이동
        naverMap.moveCamera(CameraUpdate.scrollTo(startLatLng));
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
