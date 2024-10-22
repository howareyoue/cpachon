package com.example.capchon;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuestActivity extends AppCompatActivity {

    private static final String TAG = "QuestActivity";
    private static final String PREFS_NAME = "QuestPrefs";
    private static final String PREFS_KEY_DATE = "lastQuestDate";
    private static final String PREFS_KEY_QUEST1 = "quest1";
    private static final String PREFS_KEY_QUEST2 = "quest2";
    private static final String PREFS_KEY_QUEST3 = "quest3";

    private TextView quest1;
    private TextView quest2;
    private TextView quest3;
    private Button btnCam;

    private DatabaseReference dbRef;  // Firebase Realtime Database Reference
    private List<String> allQuests = new ArrayList<>(); // 퀘스트를 담을 리스트

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest);

        quest1 = findViewById(R.id.quest1);
        quest2 = findViewById(R.id.quest2);
        quest3 = findViewById(R.id.quest3);

        btnCam = findViewById(R.id.btn_cam);
        btnCam.setOnClickListener(view -> {
            Intent intent = new Intent(QuestActivity.this, AiActivity.class);
            startActivityForResult(intent, 1);
        });

        dbRef = FirebaseDatabase.getInstance().getReference("quests"); // Realtime Database 참조 설정

        // Realtime Database에서 퀘스트 데이터 로드
        loadQuestsFromDatabase();
    }

    private void loadQuestsFromDatabase() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allQuests.clear(); // 리스트 초기화
                for (DataSnapshot questSnapshot : dataSnapshot.getChildren()) {
                    String questName = questSnapshot.child("name").getValue(String.class);
                    if (questName != null) {
                        Log.d(TAG, "Retrieved quest: " + questName); // 가져온 퀘스트 로그
                        allQuests.add(questName);
                    }
                }

                // 최소 3개의 퀘스트가 있는지 확인
                if (allQuests.size() < 3) {
                    Toast.makeText(QuestActivity.this, "Not enough quests available", Toast.LENGTH_SHORT).show();
                } else {
                    selectRandomQuests();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "loadQuests:onCancelled", databaseError.toException());
            }
        });
    }

    private void selectRandomQuests() {
        Collections.shuffle(allQuests); // 리스트를 무작위로 섞음

        // 3개의 랜덤 퀘스트 선택
        quest1.setText(allQuests.get(0)); // quest1의 텍스트 설정
        quest2.setText(allQuests.get(1)); // quest2의 텍스트 설정
        quest3.setText(allQuests.get(2)); // quest3의 텍스트 설정
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                String recognizedQuest = data.getStringExtra("recognizedQuest");
                updateQuestStatus(recognizedQuest);
            }
        }
    }

    private void updateQuestStatus(String recognizedQuest) {
        if (recognizedQuest == null) return;

        // 인식된 퀘스트와 일치하는 퀘스트를 확인하고 완료 처리
        if (recognizedQuest.equals(quest1.getText().toString())) {
            completeQuest(quest1);
        } else if (recognizedQuest.equals(quest2.getText().toString())) {
            completeQuest(quest2);
        } else if (recognizedQuest.equals(quest3.getText().toString())) {
            completeQuest(quest3);
        }
    }

    private void completeQuest(TextView questView) {
        questView.setPaintFlags(questView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // 텍스트에 취소선 추가
    }

    // 쓰레기 수집 퀘스트의 진행 상황 업데이트
    private void updateTrashQuestProgress(String questId) {
        // Realtime Database에서 업데이트 구현
    }

    // 퀘스트 완료 알림
    private void completeQuest() {
        Toast.makeText(this, "You have completed the trash collection quest!", Toast.LENGTH_SHORT).show();
    }
}
