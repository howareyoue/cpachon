package com.example.capchon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PostActivity extends AppCompatActivity {
    private EditText edit_title;
    private EditText edit_contents;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // EditText와 Firebase 레퍼런스 설정
        edit_title = findViewById(R.id.edit_title);
        edit_contents = findViewById(R.id.edit_contents);
        databaseReference = FirebaseDatabase.getInstance().getReference("CommunicationInfo");

        // 버튼 클릭 리스너 설정
        Button buttonSave = findViewById(R.id.post_button);
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 제목과 내용 가져오기
                String title = edit_title.getText().toString();
                String contents = edit_contents.getText().toString();

                // 제목 및 내용 검증
                if (title.isEmpty()) {
                    Toast.makeText(PostActivity.this, "제목을 작성해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                } else if (contents.isEmpty()) {
                    Toast.makeText(PostActivity.this, "내용을 작성해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 데이터 저장 메서드 호출
                saveCommunicationInfo(title, contents);
            }
        });
    }

    // CommunicationInfo 저장 메서드
    private void saveCommunicationInfo(String title, String contents) {
        // CommunicationInfo 객체 생성 및 데이터베이스에 저장
        CommunicationInfo communicationInfo = new CommunicationInfo(title, contents);
        String key = databaseReference.push().getKey();  // Firebase에 새로운 키 생성
        databaseReference.child(key).setValue(communicationInfo);

        // PostdetailActivity로 데이터 전달 및 전환
        Intent intent = new Intent(PostActivity.this, PostdetailActivity.class);
        intent.putExtra("title", title);  // Key는 통일하여 소문자 사용
        intent.putExtra("contents", contents);  // Key는 통일하여 소문자 사용
        startActivity(intent);  // PostdetailActivity로 이동

        // 액티비티 종료 코드가 없으므로 PostActivity가 유지됨
    }
}
