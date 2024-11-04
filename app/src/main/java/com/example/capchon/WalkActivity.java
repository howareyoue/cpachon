package com.example.capchon;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView stepsTextView, caloriesTextView, sensorDataTextView;
    private Button connectButton, disconnectButton;
    private static final double CALORIES_PER_STEP = 0.04; // 걸음당 칼로리 소모량
    private int stepCount = 0;
    private float previousX = 0; // 이전 x축 값

    @SuppressLint("MissingInflatedId")
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

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToBluetoothDevice("HC-06"); // HC-06 장치와 연결
            }
        });

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                disconnectBluetooth();
            }
        });
    }

    private void connectToBluetoothDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                        bluetoothSocket.connect();
                        inputStream = bluetoothSocket.getInputStream();
                        readFromBluetooth();
                        Toast.makeText(WalkActivity.this, "Bluetooth 연결 성공", Toast.LENGTH_SHORT).show();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(WalkActivity.this, "Bluetooth 연결 실패", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else {
            Toast.makeText(WalkActivity.this, "페어링된 장치가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectBluetooth() {
        try {
            if (inputStream != null) inputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
            Toast.makeText(WalkActivity.this, "Bluetooth 연결 해제", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFromBluetooth() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int bytes;
                while (true) {
                    try {
                        if (inputStream.available() > 0) {
                            bytes = inputStream.read(buffer);
                            final String readMessage = new String(buffer, 0, bytes);
                            updateSensorData(readMessage);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }

    private void updateSensorData(String data) {
        String[] values = data.split(",");
        if (values.length > 0) {
            try {
                float x = Float.parseFloat(values[0].trim());
                incrementStepsIfNeeded(x);
                runOnUiThread(() -> sensorDataTextView.setText("Sensor Data: " + data));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void incrementStepsIfNeeded(float x) {
        if (Math.abs(x - previousX) > 0.5) { // x축의 변동이 일정 값 이상일 때
            stepCount++;
            previousX = x;
            final double calories = calculateCalories(stepCount);
            runOnUiThread(() -> {
                stepsTextView.setText("Steps: " + stepCount);
                caloriesTextView.setText(String.format("Calories burned: %.2f kcal", calories));
            });
        }
    }

    private double calculateCalories(int steps) {
        return steps * CALORIES_PER_STEP;
    }
}
