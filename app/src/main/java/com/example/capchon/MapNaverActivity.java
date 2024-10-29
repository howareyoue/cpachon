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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "qeg3laengo";
    private static final String CLIENT_SECRET = "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG";
    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com/";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    private MapView mapView;
    private NaverMap naverMap;
    private FusedLocationSource locationSource;
    private LatLng currentLatLng;
    private PolylineOverlay polyline;
    private EditText etDestination;
    private Button btnRecommendRoute;
    private Marker destinationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));

        mapView = findViewById(R.id.map_view);
        etDestination = findViewById(R.id.et_destination);
        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
        polyline = new PolylineOverlay();

        mapView.getMapAsync(this);
        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

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

    private Retrofit createRetrofitClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .addHeader("X-NCP-APIGW-API-KEY-ID", CLIENT_ID)
                            .addHeader("X-NCP-APIGW-API-KEY", CLIENT_SECRET)
                            .build();
                    return chain.proceed(request);
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
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

        getCoordinates(destination);
    }

    private void getCoordinates(String destination) {
        Retrofit retrofit = createRetrofitClient();
        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);

        geocodingService.getCoordinates(destination).enqueue(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                    LatLng destinationLatLng = new LatLng(response.body().addresses.get(0).y, response.body().addresses.get(0).x);
                    setDestinationMarker(destinationLatLng);
                    requestDirections(currentLatLng, destinationLatLng);
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

    private void requestDirections(LatLng start, LatLng end) {
        String startPoint = start.latitude + "," + start.longitude;
        String endPoint = end.latitude + "," + end.longitude;

        Retrofit retrofit = createRetrofitClient();
        NaverDirectionsService directionsService = retrofit.create(NaverDirectionsService.class);

        directionsService.getDirections(startPoint, endPoint, "normal", CLIENT_ID, CLIENT_SECRET)
                .enqueue(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            DirectionsResponse directionsResponse = response.body();
                            if (directionsResponse.routes != null && !directionsResponse.routes.isEmpty()) {
                                List<LatLng> routeCoords = new ArrayList<>();
                                for (DirectionsResponse.Route route : directionsResponse.routes) {
                                    for (DirectionsResponse.Route.Leg leg : route.legs) {
                                        for (DirectionsResponse.Route.Leg.Step step : leg.steps) {
                                            routeCoords.addAll(step.getLatLngPath());

                                            Marker marker = new Marker();
                                            marker.setPosition(new LatLng(step.startLocation.latitude, step.startLocation.longitude));
                                            marker.setMap(naverMap);
                                        }
                                    }
                                }

                                polyline.setMap(null);
                                polyline.setCoords(routeCoords);
                                polyline.setMap(naverMap);
                                naverMap.moveCamera(CameraUpdate.scrollTo(end));
                            } else {
                                Toast.makeText(MapNaverActivity.this, "유효한 경로를 찾을 수 없습니다. 현재 위치부터 목적지까지 직선 경로를 그립니다.", Toast.LENGTH_SHORT).show();
                                drawDefaultRoute(start, end);
                            }
                        } else {
                            Toast.makeText(MapNaverActivity.this, "서버 응답 실패: " + response.message(), Toast.LENGTH_SHORT).show();
                            Log.e("MapNaverActivity", "서버 응답 실패: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                        Log.e("MapNaverActivity", "경로 요청 실패: " + t.getMessage());
                        Toast.makeText(MapNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawDefaultRoute(LatLng start, LatLng end) {
        List<LatLng> defaultRoute = new ArrayList<>();
        defaultRoute.add(start);
        defaultRoute.add(end);

        polyline.setMap(null);
        polyline.setCoords(defaultRoute);
        polyline.setMap(naverMap);
        naverMap.moveCamera(CameraUpdate.scrollTo(end));
    }

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override
    protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override
    protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
