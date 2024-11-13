package com.example.capchon;

import android.content.Intent;
import android.content.SharedPreferences;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class QuestActivity extends AppCompatActivity {

    private static final String TAG = "QuestActivity";
    private static final String PREFS_NAME = "QuestPrefs";
    private static final String LAST_UPDATE_DATE = "LastUpdateDate";
    private static final String TRASH_COUNT = "TrashCount";
    private static final String QUEST_COMPLETED_PREFIX = "completed_";

    private TextView quest1, quest2, quest3;
    private Button btnCam;

    private DatabaseReference dbRef;
    private List<String> allQuests = new ArrayList<>();
    private boolean questsLoaded = false;

    private int trashQuestCount = 0;
    private String trashQuestText;

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
        loadQuests();
        loadTrashQuestCount();
        applyStrikethroughForCompletedQuests();
    }

    private void loadQuests() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.contains("quest1") && prefs.contains("quest2") && prefs.contains("quest3")) {
            loadQuestsFromSharedPreferences(prefs);
        } else {
            checkAndLoadQuests();
        }
    }

    private void checkAndLoadQuests() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastUpdateDate = prefs.getString(LAST_UPDATE_DATE, "");

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = dateFormat.format(calendar.getTime());

        boolean isBeforeSixAM = calendar.get(Calendar.HOUR_OF_DAY) < 6;

        if (!currentDate.equals(lastUpdateDate) || isBeforeSixAM) {
            loadQuestsFromDatabase();
            prefs.edit().putString(LAST_UPDATE_DATE, currentDate).apply();
        } else {
            loadQuestsFromSharedPreferences(prefs);
        }
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
                    saveQuestsToSharedPreferences();
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

        String quest1Text = allQuests.get(0);
        String quest2Text = allQuests.get(1);
        String quest3Text = allQuests.get(2);

        quest1.setText(formatQuestText(quest1Text));
        quest2.setText(formatQuestText(quest2Text));
        quest3.setText(formatQuestText(quest3Text));

        if (quest1Text.equals("쓰레기 줍기")) {
            trashQuestText = quest1.getText().toString();
        } else if (quest2Text.equals("쓰레기 줍기")) {
            trashQuestText = quest2.getText().toString();
        } else if (quest3Text.equals("쓰레기 줍기")) {
            trashQuestText = quest3.getText().toString();
        }
    }

    private void saveQuestsToSharedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("quest1", quest1.getText().toString());
        editor.putString("quest2", quest2.getText().toString());
        editor.putString("quest3", quest3.getText().toString());
        editor.apply();
    }

    private void loadQuestsFromSharedPreferences(SharedPreferences prefs) {
        quest1.setText(prefs.getString("quest1", ""));
        quest2.setText(prefs.getString("quest2", ""));
        quest3.setText(prefs.getString("quest3", ""));

        // Apply strikethrough for completed quests
        applyStrikethroughForCompletedQuests();
    }

    private void loadTrashQuestCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        trashQuestCount = prefs.getInt(TRASH_COUNT, 0);
        updateTrashQuestText();
    }

    private void updateTrashQuestText() {
        String updatedText = "쓰레기 줍기 (" + trashQuestCount + "/3)";
        if (quest1.getText().toString().contains("쓰레기 줍기")) {
            quest1.setText(updatedText);
        } else if (quest2.getText().toString().contains("쓰레기 줍기")) {
            quest2.setText(updatedText);
        } else if (quest3.getText().toString().contains("쓰레기 줍기")) {
            quest3.setText(updatedText);
        }
    }

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
            if (recognizedQuest != null) {
                if (recognizedQuest.equals("success")) {
                    String quest = data.getStringExtra("questName");
                    if (quest != null) {
                        markQuestAsCompleted(quest);
                        if (quest.equals("쓰레기 줍기")) {
                            updateTrashQuestProgress();
                        }
                    }
                }
            }
        }
    }

    private void markQuestAsCompleted(String recognizedQuest) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 쓰레기 줍기 퀘스트 완료 처리
        if (trashQuestText != null && recognizedQuest.equals("쓰레기 줍기")) {
            updateTrashQuestProgress();
            editor.putString(QUEST_COMPLETED_PREFIX + "쓰레기 줍기", "true");
        } else {
            // 다른 퀘스트 완료 처리
            if (quest1.getText().toString().equals(recognizedQuest)) {
                quest1.setPaintFlags(quest1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                editor.putString(QUEST_COMPLETED_PREFIX + quest1.getText().toString(), "true");
            } else if (quest2.getText().toString().equals(recognizedQuest)) {
                quest2.setPaintFlags(quest2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                editor.putString(QUEST_COMPLETED_PREFIX + quest2.getText().toString(), "true");
            } else if (quest3.getText().toString().equals(recognizedQuest)) {
                quest3.setPaintFlags(quest3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                editor.putString(QUEST_COMPLETED_PREFIX + quest3.getText().toString(), "true");
            }
        }
        editor.apply();
    }

    private void updateTrashQuestProgress() {
        if (trashQuestCount >= 3) {
            Toast.makeText(this, "일일 할당량을 완료했습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        trashQuestCount++;
        updateTrashQuestText();

        if (trashQuestCount >= 3) {
            if (quest1.getText().toString().contains("쓰레기 줍기")) {
                quest1.setPaintFlags(quest1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest2.getText().toString().contains("쓰레기 줍기")) {
                quest2.setPaintFlags(quest2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest3.getText().toString().contains("쓰레기 줍기")) {
                quest3.setPaintFlags(quest3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(TRASH_COUNT, trashQuestCount);
        editor.apply();
    }

    private void applyStrikethroughForCompletedQuests() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getString(QUEST_COMPLETED_PREFIX + "쓰레기 줍기", "").equals("true")) {
            if (quest1.getText().toString().contains("쓰레기 줍기")) {
                quest1.setPaintFlags(quest1.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest2.getText().toString().contains("쓰레기 줍기")) {
                quest2.setPaintFlags(quest2.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else if (quest3.getText().toString().contains("쓰레기 줍기")) {
                quest3.setPaintFlags(quest3.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }
        }
    }
}
