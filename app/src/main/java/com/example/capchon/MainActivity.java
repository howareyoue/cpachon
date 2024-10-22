package com.example.capchon;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mFirebaseAuth; // Firebase 인증 객체


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();

        Button btn_q = findViewById(R.id.btn_q);
        btn_q.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), QuestActivity.class);
                startActivity(intent);
            }
        });
        Button btn_w = findViewById(R.id.btn_w);
        btn_w.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), WalkActivity.class);
                startActivity(intent);
            }
        });
//        Button btn_m = findViewById(R.id.btn_m);
//        btn_m.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), MapsNaverActivity.class);startActivity(intent);
//            }
//        });
        Button btn_c = findViewById(R.id.btn_c);
        btn_c.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), CommunicationActivity.class);
                startActivity(intent);
            }
        });
    }
}