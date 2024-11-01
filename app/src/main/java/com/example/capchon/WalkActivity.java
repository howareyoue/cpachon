package com.example.capchon;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class WalkActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;
    private TextView stepsTextView;
    private TextView caloriesTextView;
    private Button connectButton;
    private static final double CALORIES_PER_STEP = 0.04;
    private static final String DEVICE_NAME = "HMSoft";  // 연결할 BLE 장치 이름
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");  // HM-10 서비스 UUID
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");  // HM-10 캐릭터리스틱 UUID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        stepsTextView = findViewById(R.id.stepsTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        connectButton = findViewById(R.id.connectButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothEnabled();

        connectButton.setOnClickListener(view -> {
            if (checkBluetoothPermissions()) {
                connectToDevice(DEVICE_NAME);
            }
        });
    }

    private boolean checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void checkBluetoothEnabled() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "블루투스를 활성화하세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        boolean deviceFound = false;  // 장치 발견 여부를 추적합니다.
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName().equals(deviceName)) {
                deviceFound = true;
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
                break;
            }
        }

        if (!deviceFound) {
            StringBuilder availableDevices = new StringBuilder("사용 가능한 장치:\n");
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                availableDevices.append(device.getName()).append("\n");
            }
            Toast.makeText(this, "장치를 찾을 수 없습니다: " + deviceName + "\n" + availableDevices.toString(), Toast.LENGTH_LONG).show();
        }
    }


    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(@NonNull BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    Toast.makeText(WalkActivity.this, "블루투스 연결 성공", Toast.LENGTH_SHORT).show();
                    Toast.makeText(WalkActivity.this, "HM-10 모듈과 연결되었습니다.", Toast.LENGTH_SHORT).show();
                });
                if (ActivityCompat.checkSelfPermission(WalkActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(() -> Toast.makeText(WalkActivity.this, "블루투스 연결 끊김", Toast.LENGTH_SHORT).show());
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            if (service != null) {
                characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                if (ActivityCompat.checkSelfPermission(WalkActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.setCharacteristicNotification(characteristic, true);
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String data = new String(characteristic.getValue()).trim();
            int steps = Integer.parseInt(data);
            double calories = calculateCalories(steps);

            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                stepsTextView.setText("걸음 수: " + steps);
                caloriesTextView.setText(String.format("소모 칼로리: %.2f kcal", calories));
            });
        }
    };

    private void discoverDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter.startDiscovery(); // 블루투스 장치 검색 시작
        Toast.makeText(this, "장치 검색 중...", Toast.LENGTH_SHORT).show();
    }


    private double calculateCalories(int steps) {
        return steps * CALORIES_PER_STEP;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}
