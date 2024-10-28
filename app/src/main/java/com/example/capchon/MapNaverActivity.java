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
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.LocationTrackingMode;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "u6nzkkp800";
    private static final String CLIENT_SECRET = "IcZEWMnaSNuwEzEuebVII3IUUUlzxoGZvz23NaNR";
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private NaverDirectionsService directionsService;
    private EditText etDestination;
    private Button btnRecommendRoute;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        // NaverMap SDK 초기화
        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapView = findViewById(R.id.map_view);
        etDestination = findViewById(R.id.et_destination);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        polyline = new PolylineOverlay();

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        directionsService = retrofit.create(NaverDirectionsService.class);

        btnRecommendRoute.setOnClickListener(v -> {
            if (naverMap != null && currentLatLng != null) {
                getDirections();
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

    private void getDirections() {
        String destination = etDestination.getText().toString();
        if (destination.isEmpty()) {
            Toast.makeText(this, "목적지를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (destinationMarker != null) {
            destinationMarker.setMap(null);
        }

        directionsService.getCoordinates(destination, CLIENT_ID, CLIENT_SECRET).enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().addresses != null && !response.body().addresses.isEmpty()) {
                    LatLng destinationLatLng = new LatLng(response.body().addresses.get(0).y, response.body().addresses.get(0).x);

                    destinationMarker = new Marker();
                    destinationMarker.setPosition(destinationLatLng);
                    destinationMarker.setMap(naverMap);

                    naverMap.moveCamera(CameraUpdate.scrollTo(destinationLatLng));
                    requestDirections(currentLatLng, destinationLatLng);
                } else {
                    Toast.makeText(MapNaverActivity.this, "목적지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("MapNaverActivity", "잘못된 지오코딩 응답: " + (response.body() != null ? response.body().addresses : "응답 없음"));
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                Log.e("MapNaverActivity", "지오코딩 요청 실패: " + t.getMessage());
                Toast.makeText(MapNaverActivity.this, "지오코딩 요청 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestDirections(LatLng start, LatLng end) {
        String startPoint = start.latitude + "," + start.longitude;
        String endPoint = end.latitude + "," + end.longitude;

        directionsService.getDirections(startPoint, endPoint, "real_road", CLIENT_ID, CLIENT_SECRET)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().routes != null && !response.body().routes.isEmpty()) {
                            List<LatLng> routeCoords = new ArrayList<>();
                            DirectionsResponse.Route route = response.body().routes.get(0);

                            for (DirectionsResponse.Route.Leg leg : route.legs) {
                                for (DirectionsResponse.Route.Leg.Step step : leg.steps) {
                                    for (DirectionsResponse.Route.Leg.Step.Point point : step.path) {
                                        routeCoords.add(new LatLng(point.lat, point.lng)); // lat와 lng으로 수정
                                    }
                                }
                            }

                            polyline.setCoords(routeCoords);
                            polyline.setMap(naverMap);
                            naverMap.moveCamera(CameraUpdate.scrollTo(end));
                        } else {
                            drawDefaultRoute(start, end);
                            Log.e("MapNaverActivity", "경로 응답 실패: " + (response.body() != null ? response.body() : "응답 없음"));
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e("MapNaverActivity", "경로 요청 실패: " + t.getMessage());
                        drawDefaultRoute(start, end);
                        Toast.makeText(MapNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawDefaultRoute(LatLng start, LatLng end) {
        List<LatLng> routeCoords = new ArrayList<>();
        routeCoords.add(start);
        routeCoords.add(end);

        polyline.setCoords(routeCoords);
        polyline.setMap(naverMap);
        naverMap.moveCamera(CameraUpdate.scrollTo(end));
        Toast.makeText(this, "기본 경로를 표시했습니다.", Toast.LENGTH_SHORT).show();
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
}
