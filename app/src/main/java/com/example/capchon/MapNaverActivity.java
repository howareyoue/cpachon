package com.example.capchon;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.NaverMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapNaverActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String CLIENT_ID = "qeg3laengo";
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/";
    private static final String GOOGLE_API_KEY = "AIzaSyBtWaGATq4iQEsKT710EkGPkuNiRf84YHU";

    private MapView mapView;
    private NaverMap naverMap;

    private EditText etStartLocation, etDestination;
    private TextView tvDistance, tvDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_naver);

        etStartLocation = findViewById(R.id.et_start_location);
        etDestination = findViewById(R.id.et_destination);
        tvDistance = findViewById(R.id.tv_distance);
        tvDuration = findViewById(R.id.tv_duration);

        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));
        mapView = findViewById(R.id.map_view);
        Button btnMeasureDistance = findViewById(R.id.btn_measure_distance);

        mapView.getMapAsync(this);

        btnMeasureDistance.setOnClickListener(v -> {
            String startAddress = etStartLocation.getText().toString();
            String endAddress = etDestination.getText().toString();
            if (!startAddress.isEmpty() && !endAddress.isEmpty()) {
                fetchCoordinatesAndCalculateDistance(startAddress, endAddress);
            } else {
                Toast.makeText(this, "출발지와 목적지를 입력하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
    }

    private void fetchCoordinatesAndCalculateDistance(String startAddress, String endAddress) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://naveropenapi.apigw.ntruss.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaverGeocodingService geocodingService = retrofit.create(NaverGeocodingService.class);

        geocodingService.getCoordinates(startAddress, CLIENT_ID, "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG")
                .enqueue(new Callback<GeocodingResponse>() {
                    @Override
                    public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                            GeocodingResponse.Address startAddr = response.body().addresses.get(0);
                            LatLng startLatLng = new LatLng(Double.parseDouble(startAddr.y), Double.parseDouble(startAddr.x));

                            geocodingService.getCoordinates(endAddress, CLIENT_ID, "Lgx060Lao80eixwSkcQLMBp8R8TuA8q0gok01dgG")
                                    .enqueue(new Callback<GeocodingResponse>() {
                                        @Override
                                        public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                                            if (response.isSuccessful() && response.body() != null && !response.body().addresses.isEmpty()) {
                                                GeocodingResponse.Address endAddr = response.body().addresses.get(0);
                                                LatLng endLatLng = new LatLng(Double.parseDouble(endAddr.y), Double.parseDouble(endAddr.x));
                                                calculateDistanceAndDuration(startLatLng, endLatLng);
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

    private void calculateDistanceAndDuration(LatLng start, LatLng end) {
        String startPoint = start.latitude + "," + start.longitude;
        String endPoint = end.latitude + "," + end.longitude;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        GoogleDirectionsService directionsService = retrofit.create(GoogleDirectionsService.class);

        directionsService.getDirections(startPoint, endPoint, "driving", GOOGLE_API_KEY)
                .enqueue(new Callback<GoogleDirectionsResponse>() {
                    @Override
                    public void onResponse(Call<GoogleDirectionsResponse> call, Response<GoogleDirectionsResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            GoogleDirectionsResponse.Route route = response.body().routes.get(0);
                            GoogleDirectionsResponse.Leg leg = route.legs.get(0);

                            tvDistance.setText("거리: " + leg.distance.text);
                            tvDuration.setText("이동 시간: " + leg.duration.text);
                        } else {
                            Toast.makeText(MapNaverActivity.this, "거리 계산 요청 실패", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GoogleDirectionsResponse> call, Throwable t) {
                        Toast.makeText(MapNaverActivity.this, "거리 계산 요청 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
