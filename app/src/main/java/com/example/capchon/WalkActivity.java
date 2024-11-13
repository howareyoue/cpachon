package com.example.capchon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class WalkActivity extends AppCompatActivity {
    private static final String DEVICE_NAME = "CAPSTONE";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView stepsTextView, caloriesTextView, sensorDataTextView;
    private Button connectButton, disconnectButton;
    private int steps = 0;
    private double calories = 0.0;
    private static final double CALORIES_PER_STEP = 0.04;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        stepsTextView = findViewById(R.id.stepsTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        sensorDataTextView = findViewById(R.id.sensorDataTextView);
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        connectButton.setOnClickListener(view -> connectToBluetoothDevice());
        disconnectButton.setOnClickListener(view -> disconnectBluetooth());
    }

    private void connectToBluetoothDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth가 비활성화되어 있습니다. Bluetooth를 활성화해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(DEVICE_NAME)) {
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    bluetoothSocket.connect();
                    inputStream = bluetoothSocket.getInputStream();
                    Toast.makeText(this, "Bluetooth 연결 성공", Toast.LENGTH_SHORT).show();
                    startReadingData();
                    break;
                } catch (IOException e) {
                    Toast.makeText(this, "Bluetooth 연결 실패", Toast.LENGTH_SHORT).show();
                    Log.e("WalkActivity", "Bluetooth 연결 오류", e);
                }
            }
        }
    }

    private void disconnectBluetooth() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                bluetoothSocket = null;
                inputStream = null;
                Toast.makeText(this, "Bluetooth 연결 해제", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e("WalkActivity", "Bluetooth 연결 해제 오류", e);
            }
        }
    }

    private void startReadingData() {
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            int bytes;
            while (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        String data = new String(buffer, 0, bytes).trim();
                        String[] dataParts = data.split(",");
                        if (dataParts.length == 2) {
                            try {
                                steps = Integer.parseInt(dataParts[0].trim());
                                calories = Double.parseDouble(dataParts[1].trim());
                                handler.post(this::updateUI);
                            } catch (NumberFormatException e) {
                                Log.e("WalkActivity", "데이터 파싱 오류", e);
                            }
                        }
                    }
                    Thread.sleep(3000);
                } catch (IOException | InterruptedException e) {
                    Log.e("WalkActivity", "데이터 읽기 오류", e);
                }
            }
        }).start();
    }

    private void updateUI() {
        stepsTextView.setText("걸음 수: " + steps);
        caloriesTextView.setText(String.format("소모된 칼로리: %.2f kcal", calories));
        sensorDataTextView.setText("센서 데이터: " + steps + ", " + calories);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectToBluetoothDevice();
        } else {
            Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
