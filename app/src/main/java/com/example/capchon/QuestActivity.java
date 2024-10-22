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

    private DatabaseReference databaseRef;
    private List<String> allQuests = new ArrayList<>(); // List to hold quests retrieved from Realtime Database

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

        // Initialize Firebase Realtime Database
        databaseRef = FirebaseDatabase.getInstance().getReference("quests");

        // Load quest data from Realtime Database
        loadQuestsFromRealtimeDatabase();
    }

    private void loadQuestsFromRealtimeDatabase() {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allQuests.clear(); // Clear the list before adding new quests
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String questName = snapshot.child("name").getValue(String.class);
                    Log.d(TAG, "Retrieved quest: " + questName); // Log the retrieved quest
                    allQuests.add(questName);
                }
                // Ensure that at least 3 quests are available
                if (allQuests.size() < 3) {
                    Toast.makeText(QuestActivity.this, "Not enough quests available", Toast.LENGTH_SHORT).show();
                } else {
                    checkAndSetNewQuests();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Error getting data.", databaseError.toException());
            }
        });
    }

    private void checkAndSetNewQuests() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastQuestDate = prefs.getLong(PREFS_KEY_DATE, 0);
        long currentTime = System.currentTimeMillis();

        if (!isSameDay(lastQuestDate, currentTime)) {
            selectNewQuests(prefs);
        } else {
            loadQuests(prefs);
        }
    }

    private boolean isSameDay(long time1, long time2) {
        return (time1 / (1000 * 60 * 60 * 24)) == (time2 / (1000 * 60 * 60 * 24));
    }

    private void selectNewQuests(SharedPreferences prefs) {
        if (allQuests.size() < 3) {
            Toast.makeText(this, "Insufficient quests available!", Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.shuffle(allQuests);

        quest1.setText(allQuests.get(0)); // Set text for quest1
        quest2.setText(allQuests.get(1)); // Set text for quest2
        quest3.setText(allQuests.get(2)); // Set text for quest3

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(PREFS_KEY_DATE, System.currentTimeMillis());
        editor.putString(PREFS_KEY_QUEST1, allQuests.get(0));
        editor.putString(PREFS_KEY_QUEST2, allQuests.get(1));
        editor.putString(PREFS_KEY_QUEST3, allQuests.get(2));
        editor.apply();
    }

    private void loadQuests(SharedPreferences prefs) {
        quest1.setText(prefs.getString(PREFS_KEY_QUEST1, "")); // Load saved quest1
        quest2.setText(prefs.getString(PREFS_KEY_QUEST2, "")); // Load saved quest2
        quest3.setText(prefs.getString(PREFS_KEY_QUEST3, "")); // Load saved quest3
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

        // Check if recognized quest matches any of the displayed quests
        if (recognizedQuest.equals(quest1.getText().toString())) {
            completeQuest(quest1);
        } else if (recognizedQuest.equals(quest2.getText().toString())) {
            completeQuest(quest2);
        } else if (recognizedQuest.equals(quest3.getText().toString())) {
            completeQuest(quest3);
        }
    }

    private void completeQuest(TextView questView) {
        questView.setPaintFlags(questView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Add strikethrough to text
    }

    // Update progress for trash collection quest
    private void updateTrashQuestProgress(String questId) {
        DatabaseReference questRef = databaseRef.child(questId);

        questRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long currentCount = snapshot.child("count").getValue(Long.class);
                    if (currentCount != null && currentCount < 3) {
                        questRef.child("count").setValue(currentCount + 1)
                                .addOnSuccessListener(aVoid -> {
                                    if (currentCount + 1 == 3) {
                                        completeQuest(quest1);  // Logic for quest completion
                                    } else {
                                        Toast.makeText(QuestActivity.this, "Trash collected: " + (currentCount + 1), Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Log.w(TAG, "Error updating document", e));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "Error getting data.", databaseError.toException());
            }
        });
    }

    // Notification for quest completion
    private void completeQuest() {
        Toast.makeText(this, "You have completed the trash collection quest!", Toast.LENGTH_SHORT).show();
    }
}
