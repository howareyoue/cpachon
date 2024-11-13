package com.example.capchon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class WalkActivity extends AppCompatActivity {
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 2;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView stepsTextView;
    private TextView caloriesTextView;
    private TextView sensorDataTextView;
    private Button connectButton;
    private Button disconnectButton;
    private static final double CALORIES_PER_STEP = 0.04;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        stepsTextView = findViewById(R.id.stepsTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        sensorDataTextView = findViewById(R.id.sensorDataTextView); // 추가된 TextView
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    connectToBluetoothDevice(" CAPSTONE");
                }
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnectBluetooth();
            }
        });
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION || requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToBluetoothDevice(" CAPSTONE");
            } else {
                Toast.makeText(this, "Bluetooth 및 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void connectToBluetoothDevice(String deviceName) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth가 비활성화되어 있습니다. Bluetooth를 활성화해주세요.", Toast.LENGTH_SHORT).show();
            Log.d("WalkActivity", "Bluetooth is disabled.");
            return;
        }

        Set<BluetoothDevice> pairedDevices;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                pairedDevices = bluetoothAdapter.getBondedDevices();
                Log.d("WalkActivity", "Paired devices found: " + pairedDevices.size());
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
        } catch (SecurityException e) {
            Log.e("WalkActivity", "Bluetooth permission not granted for getBondedDevices", e);
            return;
        }

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                Log.d("WalkActivity", "Checking device: " + device.getName());
                if (device.getName().equals(deviceName)) {
                    Log.d("WalkActivity", "Device matched: " + device.getName());
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        Log.d("WalkActivity", "Attempting to connect to Bluetooth device...");
                        bluetoothSocket.connect();
                        inputStream = bluetoothSocket.getInputStream();
                        Toast.makeText(this, "Bluetooth 장치에 연결되었습니다.", Toast.LENGTH_LONG).show();
                        Log.d("WalkActivity", "Bluetooth connection successful.");
                        new Thread(this::readFromBluetooth).start();
                        break;
                    } catch (IOException e) {
                        Log.e("WalkActivity", "Failed to connect to Bluetooth device", e);
                        Toast.makeText(this, "Bluetooth 장치 연결 실패", Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            Log.d("WalkActivity", "No paired Bluetooth devices found.");
            Toast.makeText(this, "연결된 Bluetooth 장치가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }


    private void disconnectBluetooth() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                bluetoothSocket = null;
                inputStream = null;
                Toast.makeText(this, "Bluetooth 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Bluetooth 연결 해제 실패", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "현재 연결된 Bluetooth 장치가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void readFromBluetooth() {
        byte[] buffer = new byte[1024];
        int bytes;
        StringBuilder dataBuffer = new StringBuilder();

        try {
            while (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                if (inputStream != null) {
                    // 데이터를 읽고 문자열로 변환
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String readMessage = new String(buffer, 0, bytes);
                        Log.d("WalkActivity", "Received Data: " + readMessage);
                        dataBuffer.append(readMessage);

                        // '\n'을 포함할 때까지 데이터가 누적될 때만 처리
                        int endOfLineIndex = dataBuffer.indexOf("\n");
                        while (endOfLineIndex != -1) {
                            // 한 줄을 잘라내어 전체 데이터가 끝날 때마다 출력
                            String completeData = dataBuffer.substring(0, endOfLineIndex).trim();
                            dataBuffer.delete(0, endOfLineIndex + 1);

                            if (!completeData.isEmpty()) {
                                try {
                                    // 예상되는 데이터 포맷 확인 및 파싱
                                    String[] dataParts = completeData.split(",");
                                    if (dataParts.length >= 2) {
                                        int steps = Integer.parseInt(dataParts[0].trim());
                                        double calories = Double.parseDouble(dataParts[1].trim());

                                        Log.d("WalkActivity", "Parsed Steps: " + steps + ", Calories: " + calories);

                                        // 메인 스레드에서 UI 업데이트 실행
                                        Handler handler = new Handler(Looper.getMainLooper());
                                        handler.post(() -> {
                                            stepsTextView.setText("걸음 수: " + steps);
                                            caloriesTextView.setText(String.format("소모된 칼로리: %.2f kcal", calories));
                                            sensorDataTextView.setText("센서 데이터: " + completeData);
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e("WalkActivity", "Data parsing error", e);
                                }
                            }
                            endOfLineIndex = dataBuffer.indexOf("\n");
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e("WalkActivity", "Error reading Bluetooth data", e);
        }
    }


    private double calculateCalories(int steps) {
        return steps * CALORIES_PER_STEP;
    }
}
