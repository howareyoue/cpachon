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
    private TextView stepsTextView;
    private Button connectButton;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepsTextView = findViewById(R.id.stepsTextView);
        connectButton = findViewById(R.id.connectButton);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToBluetoothDevice("HC-05");
            }
        });
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
                        readFromBluetooth();
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void readFromBluetooth() {
        byte[] buffer = new byte[1024];
        int bytes;
        try {
            bytes = inputStream.read(buffer);
            final String readMessage = new String(buffer, 0, bytes);
            runOnUiThread(() -> stepsTextView.setText("Steps: " + readMessage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
