package com.example.capchon;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        ImageView loadingImage = findViewById(R.id.loading_image);

        // Glide를 사용하여 GIF 로드
        Glide.with(this)
                .load(R.drawable.logding) // loading은 GIF 리소스의 이름입니다.
                .into(loadingImage);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LoadingActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            }
        }, 5000); // 5000ms = 5초
    }
}
