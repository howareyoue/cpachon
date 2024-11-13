package com.example.capchon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class WalkActivity extends AppCompatActivity implements SensorEventListener {
    private static final String DEVICE_NAME = "CAPSTONE";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private TextView stepsTextView, caloriesTextView;
    private Button connectButton, disconnectButton;
    private int steps = 0;
    private double calories = 0.0;
    private static final double CALORIES_PER_STEP = 0.04;
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isWalking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        stepsTextView = findViewById(R.id.stepsTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        connectButton = findViewById(R.id.connectButton);
        disconnectButton = findViewById(R.id.disconnectButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

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
                    Toast.makeText(this, "Bluetooth 연결 성공", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, "Bluetooth 연결 해제", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e("WalkActivity", "Bluetooth 연결 해제 오류", e);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z);
            if (acceleration > 12) {  // 움직임 감지 기준 값, 필요시 조정 가능
                if (!isWalking) {
                    isWalking = true;
                    steps++;
                    calories = steps * CALORIES_PER_STEP;
                    updateUI();
                }
            } else {
                isWalking = false;
            }
        }
    }

    private void updateUI() {
        stepsTextView.setText("걸음 수: " + steps);
        caloriesTextView.setText(String.format("소모된 칼로리: %.2f kcal", calories));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 정확도 변화는 필요시 처리
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
