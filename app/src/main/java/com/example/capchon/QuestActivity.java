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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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

    private FirebaseFirestore db;
    private List<String> allQuests = new ArrayList<>(); // Firestore에서 가져온 퀘스트 목록

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest);

        // 퀘스트 TextView 초기화
        quest1 = findViewById(R.id.quest1);
        quest2 = findViewById(R.id.quest2);
        quest3 = findViewById(R.id.quest3);

        // 카메라 버튼 초기화 및 클릭 리스너 설정
        btnCam = findViewById(R.id.btn_cam);
        btnCam.setOnClickListener(view -> {
            // AiActivity로 이동
            Intent intent = new Intent(QuestActivity.this, AiActivity.class);
            startActivityForResult(intent, 1);
        });

        // Firestore 인스턴스 초기화
        db = FirebaseFirestore.getInstance();

        // Firestore에서 퀘스트 데이터 로드
        loadQuestsFromFirestore();
    }

    private void loadQuestsFromFirestore() {
        CollectionReference questsRef = db.collection("quests");
        questsRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        allQuests.add(document.getString("name"));
                    }

                    // 퀘스트가 로드되었으면 날짜를 확인하고 새로운 퀘스트를 설정
                    checkAndSetNewQuests();
                } else {
                    Log.w(TAG, "Error getting documents.", task.getException());
                }
            }
        });
    }

    private void checkAndSetNewQuests() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastQuestDate = prefs.getLong(PREFS_KEY_DATE, 0);
        long currentTime = System.currentTimeMillis();

        // 현재 날짜가 마지막 퀘스트 날짜와 다른지 확인
        if (!isSameDay(lastQuestDate, currentTime)) {
            // 새로운 퀘스트 선택
            selectNewQuests(prefs);
        } else {
            // 저장된 퀘스트 불러오기
            loadQuests(prefs);
        }
    }

    private boolean isSameDay(long time1, long time2) {
        return (time1 / (1000 * 60 * 60 * 24)) == (time2 / (1000 * 60 * 60 * 24));
    }

    private void selectNewQuests(SharedPreferences prefs) {
        if (allQuests.isEmpty()) return;

        Collections.shuffle(allQuests);

        String selectedQuest1 = allQuests.get(0);
        String selectedQuest2 = allQuests.get(1);
        String selectedQuest3 = allQuests.get(2);

        quest1.setText(selectedQuest1);
        quest2.setText(selectedQuest2);
        quest3.setText(selectedQuest3);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREFS_KEY_DATE, System.currentTimeMillis());
        editor.putString(PREFS_KEY_QUEST1, selectedQuest1);
        editor.putString(PREFS_KEY_QUEST2, selectedQuest2);
        editor.putString(PREFS_KEY_QUEST3, selectedQuest3);
        editor.apply();
    }

    private void loadQuests(SharedPreferences prefs) {
        quest1.setText(prefs.getString(PREFS_KEY_QUEST1, ""));
        quest2.setText(prefs.getString(PREFS_KEY_QUEST2, ""));
        quest3.setText(prefs.getString(PREFS_KEY_QUEST3, ""));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null) {
                // AI 인식 결과로 완료된 퀘스트 정보를 받습니다.
                String recognizedQuest = data.getStringExtra("recognizedQuest");

                // 퀘스트 완료 상태를 업데이트합니다.
                updateQuestStatus(recognizedQuest);
            }
        }
    }

    private void updateQuestStatus(String recognizedQuest) {
        if (recognizedQuest == null) return;

        if (recognizedQuest.equals(quest1.getText().toString())) {
            completeQuest(quest1);
        } else if (recognizedQuest.equals(quest2.getText().toString())) {
            completeQuest(quest2);
        } else if (recognizedQuest.equals(quest3.getText().toString())) {
            completeQuest(quest3);
        }
    }

    private void completeQuest(TextView questView) {
        questView.setPaintFlags(questView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }
}
