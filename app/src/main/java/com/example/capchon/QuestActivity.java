package com.example.capchon;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

    private TextView quest1, quest2, quest3;
    private Button btnCam;

    private DatabaseReference dbRef;
    private List<String> allQuests = new ArrayList<>();

    private int trashQuestCount = 0; // 쓰레기 줍기 퀘스트 카운터
    private String trashQuestText; // "쓰레기 줍기" 퀘스트 텍스트 저장

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

        dbRef = FirebaseDatabase.getInstance().getReference("quests");
        loadQuestsFromDatabase();
    }

    private void loadQuestsFromDatabase() {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allQuests.clear();
                for (DataSnapshot questSnapshot : dataSnapshot.getChildren()) {
                    String questName = questSnapshot.child("name").getValue(String.class);
                    if (questName != null) {
                        allQuests.add(questName);
                    }
                }

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
        Collections.shuffle(allQuests);

        String quest1Text = formatQuestText(allQuests.get(0));
        String quest2Text = formatQuestText(allQuests.get(1));
        String quest3Text = formatQuestText(allQuests.get(2));

        quest1.setText(quest1Text);
        quest2.setText(quest2Text);
        quest3.setText(quest3Text);

        // "쓰레기 줍기" 퀘스트가 선택된 경우에 대한 처리
        if (quest1Text.contains("쓰레기 줍기")) {
            trashQuestText = quest1Text; // 현재 "쓰레기 줍기" 퀘스트를 저장
        } else if (quest2Text.contains("쓰레기 줍기")) {
            trashQuestText = quest2Text; // 현재 "쓰레기 줍기" 퀘스트를 저장
        } else if (quest3Text.contains("쓰레기 줍기")) {
            trashQuestText = quest3Text; // 현재 "쓰레기 줍기" 퀘스트를 저장
        }
    }

    // "쓰레기 줍기" 퀘스트일 경우 (0/3)으로 표시
    private String formatQuestText(String questName) {
        if (questName.equals("쓰레기 줍기")) {
            return questName + " (0/3)";
        }
        return questName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String recognizedQuest = data.getStringExtra("recognizedQuest");
            if (recognizedQuest != null && recognizedQuest.equals("success")) {
                String quest = data.getStringExtra("questName");
                if (quest != null) {
                    markQuestAsCompleted(quest);
                }
            }
        }
    }

    private void markQuestAsCompleted(String recognizedQuest) {
        // 쓰레기 줍기 퀘스트인 경우 카운트 증가
        if (trashQuestText != null && recognizedQuest.equals("쓰레기 줍기")) {
            updateTrashQuestProgress();
        } else {
            // 다른 퀘스트는 일반 취소선 처리
            if (quest1.getText().toString().equals(recognizedQuest)) {
                quest1.setPaintFlags(quest1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest2.getText().toString().equals(recognizedQuest)) {
                quest2.setPaintFlags(quest2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest3.getText().toString().equals(recognizedQuest)) {
                quest3.setPaintFlags(quest3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }

    // "쓰레기 줍기" 퀘스트 진행 상황 업데이트
    private void updateTrashQuestProgress() {
        trashQuestCount++;
        String updatedText = "쓰레기 줍기 (" + trashQuestCount + "/3)";

        // 업데이트된 텍스트로 변경
        if (quest1.getText().toString().contains("쓰레기 줍기")) {
            quest1.setText(updatedText);
        } else if (quest2.getText().toString().contains("쓰레기 줍기")) {
            quest2.setText(updatedText);
        } else if (quest3.getText().toString().contains("쓰레기 줍기")) {
            quest3.setText(updatedText);
        }

        if (trashQuestCount == 3) {
            // 카운트가 3에 도달하면 취소선 적용
            if (quest1.getText().toString().contains("쓰레기 줍기")) {
                quest1.setPaintFlags(quest1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest2.getText().toString().contains("쓰레기 줍기")) {
                quest2.setPaintFlags(quest2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest3.getText().toString().contains("쓰레기 줍기")) {
                quest3.setPaintFlags(quest3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
            Toast.makeText(this, "쓰레기 줍기 퀘스트 완료!", Toast.LENGTH_SHORT).show();
        }
    }
}
