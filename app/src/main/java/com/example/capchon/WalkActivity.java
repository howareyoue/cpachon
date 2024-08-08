package com.example.capchon;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class WalkActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView stepsTextView;
    private TextView caloriesTextView;
    private Button connectButton;
    private static final double CALORIES_PER_STEP = 0.04; // 걸음 당 소모 칼로리

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_walk);

        stepsTextView = findViewById(R.id.stepsTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);
        connectButton = findViewById(R.id.connectButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkBluetoothPermissions()) {
                    connectToBluetoothDevice("HC-05");
                }
            }
        });
    }

    private boolean checkBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                connectToBluetoothDevice("HC-05");
            } else {
                Toast.makeText(this, "Bluetooth permissions are required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectToBluetoothDevice(String deviceName) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(deviceName)) {
                    try {
                        bluetoothSocket = device.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")); // Standard SerialPortService ID
                        bluetoothSocket.connect();
                        inputStream = bluetoothSocket.getInputStream();
                        Toast.makeText(this, "Connected to Bluetooth device", Toast.LENGTH_LONG).show();
                        new Thread(this::readFromBluetooth).start();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to connect to Bluetooth device", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
    }

    private void readFromBluetooth() {
        byte[] buffer = new byte[1024];
        int bytes;
        try {
            while (true) {
                if (inputStream != null && (bytes = inputStream.read(buffer)) > 0) {
                    String readMessage = new String(buffer, 0, bytes).trim();
                    int steps = Integer.parseInt(readMessage);
                    double calories = calculateCalories(steps);
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(() -> {
                        stepsTextView.setText("Steps: " + steps);
                        caloriesTextView.setText(String.format("Calories burned: %.2f kcal", calories));
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private double calculateCalories(int steps) {
        return steps * CALORIES_PER_STEP;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
