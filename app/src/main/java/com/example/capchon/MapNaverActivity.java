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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
                getCoordinatesAndSetMarkers(startLocation, destinationLocation);
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

        // 카메라를 사용자 위치로 이동
        naverMap.moveCamera(CameraUpdate.scrollTo(new LatLng(0, 0))); // 초기 카메라 위치 설정
    }

    private void getCoordinatesAndSetMarkers(String start, String end) {
        // Google Directions API를 사용하여 주소를 위도/경도로 변환하는 비동기 작업 수행
        new Thread(() -> {
            try {
                LatLng startLatLng = getLatLngFromGoogleDirections(start);
                LatLng endLatLng = getLatLngFromGoogleDirections(end);

                runOnUiThread(() -> setMarkers(startLatLng, endLatLng));
            } catch (Exception e) {
                Log.e("MapNaverActivity", "Error getting coordinates: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "좌표를 가져오는 중 오류 발생", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private LatLng getLatLngFromGoogleDirections(String location) throws Exception {
        String apiKey = "AIzaSyBtWaGATq4iQEsKT710EkGPkuNiRf84YHU"; // 여기에 Google API 키를 입력하세요.
        String urlString = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
                location.replace(" ", "+") + "&key=" + apiKey;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // JSON 파싱
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray results = jsonResponse.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject locationJson = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location");
                double lat = locationJson.getDouble("lat");
                double lng = locationJson.getDouble("lng");
                return new LatLng(lat, lng);
            }
        }
        throw new Exception("좌표를 찾을 수 없습니다.");
    }

    private void setMarkers(LatLng startLatLng, LatLng endLatLng) {
        // 기존 마커 제거
        if (startMarker != null) {
            startMarker.setMap(null);
        }
        if (destinationMarker != null) {
            destinationMarker.setMap(null);
        }

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
