//package com.example.capchon;
//
//import static android.content.ContentValues.TAG;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.pm.PackageManager;
//import android.location.Location;
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.naver.maps.geometry.LatLng;
//import com.naver.maps.map.LocationSource;
//import com.naver.maps.map.MapView;
//import com.naver.maps.map.NaverMap;
//import com.naver.maps.map.NaverMapSdk;
//import com.naver.maps.map.OnMapReadyCallback;
//import com.naver.maps.map.overlay.Marker;
//import com.naver.maps.map.overlay.PolylineOverlay;
//import com.naver.maps.map.util.FusedLocationSource;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//import retrofit2.http.GET;
//import retrofit2.http.Header;
//import retrofit2.http.Query;
//
//public class MapsNaverActivity extends AppCompatActivity implements OnMapReadyCallback {
//
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    private static final String CLIENT_ID = "u6nzkkp800";
//    private static final String CLIENT_SECRET = "pTQBJXJxzwgiafqynJnFv3kWloFQKTdBUjkFukt1";
//    private static final String BASE_URL = "https://naveropenapi.apigw.ntruss.com";
//
//    private MapView mapView;
//    private NaverMap naverMap;
//    private LocationSource locationSource;
//    private Button btnRecommendRoute;
//    private Marker currentLocationMarker;
//    private LatLng currentLatLng;
//    private PolylineOverlay polyline;
//
//    private NaverDirectionsService directionsService;
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_maps_naver);
//
//        // Naver Map SDK 초기화
//        NaverMapSdk.getInstance(this).setClient(new NaverMapSdk.NaverCloudPlatformClient(CLIENT_ID));
//
//        // View 초기화
//        mapView = findViewById(R.id.map_view);
//        btnRecommendRoute = findViewById(R.id.btn_recommend_route);
//
//        mapView.getMapAsync(this);
//        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
//
//        // Retrofit 설정
//        Retrofit retrofit = new Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create())
//                .build();
//        directionsService = retrofit.create(NaverDirectionsService.class);
//
//        // 버튼 클릭 리스너 설정
//        btnRecommendRoute.setOnClickListener(v -> {
//            if (naverMap != null && currentLatLng != null) {
//                recommendWalkingRoute();
//            }
//        });
//
//        polyline = new PolylineOverlay(); // 경로 표시를 위한 PolylineOverlay 객체 초기화
//    }
//
//    @Override
//    public void onMapReady(@NonNull NaverMap naverMap) {
//        this.naverMap = naverMap;
//        naverMap.setLocationSource(locationSource);
//
//        ActivityCompat.requestPermissions(this, new String[]{
//                Manifest.permission.ACCESS_FINE_LOCATION,
//                Manifest.permission.ACCESS_COARSE_LOCATION
//        }, LOCATION_PERMISSION_REQUEST_CODE);
//
//        enableLocationTracking();  // 위치 추적 활성화
//        currentLocationMarker = new Marker(); // 현재 위치 마커 초기화
//    }
//
//    private void enableLocationTracking() {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//
//            LocationRequest locationRequest = LocationRequest.create();
//            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//            locationRequest.setInterval(10000);
//            locationRequest.setFastestInterval(5000);
//
//            LocationServices.getFusedLocationProviderClient(this)
//                    .requestLocationUpdates(locationRequest, new LocationCallback() {
//                        @Override
//                        public void onLocationResult(@NonNull LocationResult locationResult) {
//                            Location location = locationResult.getLastLocation();
//                            if (location != null) {
//                                double latitude = location.getLatitude();
//                                double longitude = location.getLongitude();
//
//                                currentLatLng = new LatLng(latitude, longitude);
//
//                                // 현재 위치 마커 업데이트
//                                if (currentLocationMarker != null) {
//                                    currentLocationMarker.setPosition(currentLatLng);
//                                    if (currentLocationMarker.getMap() == null) {
//                                        currentLocationMarker.setMap(naverMap);
//                                    }
//                                }
//
//                                // 현재 위치로 카메라 이동
//                                naverMap.moveCamera(com.naver.maps.map.CameraUpdate.scrollTo(currentLatLng));
//                            } else {
//                                Toast.makeText(MapsNaverActivity.this, "위치 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }, getMainLooper());
//        } else {
//            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    // 경로 추천 기능 추가
//    private void recommendWalkingRoute() {
//        if (currentLatLng != null) {
//            String start = currentLatLng.longitude + "," + currentLatLng.latitude;
//            LatLng goalLatLng = new LatLng(currentLatLng.latitude + 0.01, currentLatLng.longitude + 0.01); // 목적지 설정
//            String goal = goalLatLng.longitude + "," + goalLatLng.latitude;
//
//            Call<DirectionsResponse> call = directionsService.getWalkingRoute(CLIENT_ID, CLIENT_SECRET, start, goal, "traoptimal");
//
//            call.enqueue(new Callback<DirectionsResponse>() {
//                @Override
//                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//                    if (response.isSuccessful() && response.body() != null) {
//                        DirectionsResponse directionsResponse = response.body();
//                        if (directionsResponse != null && directionsResponse.route != null) {
//                            List<DirectionsResponse.Route.Traoptimal> routes = directionsResponse.route.traoptimal;
//                            if (routes != null && !routes.isEmpty()) {
//                                List<LatLng> path = new ArrayList<>();
//                                for (List<Double> point : routes.get(0).path) {
//                                    if (point.size() >= 2) {
//                                        path.add(new LatLng(point.get(1), point.get(0)));
//                                    }
//                                }
//
//                                // 기존 경로 지우기
//                                polyline.setMap(null);
//
//                                // 새로운 경로 설정
//                                polyline.setCoords(path);
//                                polyline.setMap(naverMap);
//
//                                Toast.makeText(MapsNaverActivity.this, "최적의 경로를 추천합니다.", Toast.LENGTH_SHORT).show();
//                            } else {
//                                Toast.makeText(MapsNaverActivity.this, "경로를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    } else {
//                        Log.e(TAG, "경로 요청 실패: " + response.message());
//                        Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + response.message(), Toast.LENGTH_SHORT).show();
//                    }
//                }
//
//                @Override
//                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
//                    Log.e(TAG, "경로 요청 실패: " + t.getMessage(), t);
//                    Toast.makeText(MapsNaverActivity.this, "경로 요청 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            });
//        } else {
//            Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @SuppressLint("MissingSuperCall")
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                enableLocationTracking();
//            } else {
//                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//}
