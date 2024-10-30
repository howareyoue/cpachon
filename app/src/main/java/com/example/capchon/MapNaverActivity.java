package com.example.capchon;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "qeg3laengo"; // 클라이언트 ID
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/";
    private static final String GOOGLE_API_KEY = "AIzaSyBtWaGATq4iQEsKT710EkGPkuNiRf84YHU";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;

    private LatLng startLatLng = new LatLng(35.237196, 126.411049); // 전남 무안군 청계면 도림리 산61-5
    private LatLng endLatLng = new LatLng(35.235671, 126.409723); // 전남 무안군 청계면 도림길 15-5 무안청계중학교
    private Marker destinationMarker;

    // TextViews for displaying distance and duration
    private TextView tvDistance;
    private TextView tvDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // Initialize TextViews
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);

        // Naver Maps SDK 클라이언트 설정
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapView = findViewById(R.id.map_view);
        Button btnRecommendRoute = findViewById(R.id.btn_recommend_route);

        mapView.getMapAsync(this);

        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null) {
                requestGoogleDirections(startLatLng, endLatLng);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // 출발지 마커 설정
        Marker startMarker = new Marker();
        startMarker.setPosition(startLatLng);
        startMarker.setMap(naverMap);

        // 목적지 마커 설정
        destinationMarker = new Marker();
        destinationMarker.setPosition(endLatLng);
        destinationMarker.setMap(naverMap);

        naverMap.moveCamera(CameraUpdate.scrollTo(startLatLng)); // 출발지로 카메라 이동
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

        // 드라이빙 경로 요청
        directionsService.getDirections(startPoint, endPoint, "driving", GOOGLE_API_KEY)
                .enqueue(new Callback<GoogleDirectionsResponse>() {
                    @Override
                    public void onResponse(Call<GoogleDirectionsResponse> call, Response<GoogleDirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            GoogleDirectionsResponse.Route route = response.body().routes.get(0);
                            GoogleDirectionsResponse.Leg leg = route.legs.get(0);

                            String distance = leg.distance.text;
                            String duration = leg.duration.text;

                            tvDistance.setText("거리: " + distance);
                            tvDuration.setText("이동 시간: " + duration);
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
}
