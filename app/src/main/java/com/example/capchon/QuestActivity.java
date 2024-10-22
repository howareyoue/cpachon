package com.example.capchon;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestActivity extends AppCompatActivity {

    private static final String TAG = "QuestActivity";

    private TextView quest1;
    private TextView quest2;
    private TextView quest3;
    private Button btnCam;

    private List<String> allQuests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest);

        quest1 = findViewById(R.id.quest1);
        quest2 = findViewById(R.id.quest2);
        quest3 = findViewById(R.id.quest3);

        btnCam = findViewById(R.id.btn_cam);
        btnCam.setOnClickListener(view -> {
            // AiActivity로 이동
            Intent intent = new Intent(QuestActivity.this, AiActivity.class);
            startActivityForResult(intent, 1); // 결과를 받기 위해 AiActivity로 이동
        });

        // 퀘스트 목록 설정 (이 부분은 Firestore에서 받아오는 것으로 설정되어 있음)
        allQuests.add("Garbage_CigaretteButt");
        allQuests.add("Garbage_CoffeeCup");
        allQuests.add("Garbage_PlasticBag");

        // 초기 퀘스트 선택 (여기서는 임시로 세 개를 선택)
        Collections.shuffle(allQuests);
        quest1.setText(allQuests.get(0));
        quest2.setText(allQuests.get(1));
        quest3.setText(allQuests.get(2));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // AiActivity에서 돌아와서 퀘스트 결과를 받음
            String recognizedQuest = data.getStringExtra("recognizedQuest");
            Log.d(TAG, "Recognized quest: " + recognizedQuest);

            // 퀘스트와 일치하는지 확인 후 완료 처리
            if (recognizedQuest != null) {
                checkQuestCompletion(recognizedQuest);
            }
        }
    }

    private void checkQuestCompletion(String recognizedQuest) {
        if (recognizedQuest.equals(quest1.getText().toString())) {
            quest1.setPaintFlags(quest1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else if (recognizedQuest.equals(quest2.getText().toString())) {
            quest2.setPaintFlags(quest2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else if (recognizedQuest.equals(quest3.getText().toString())) {
            quest3.setPaintFlags(quest3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
    }
}
