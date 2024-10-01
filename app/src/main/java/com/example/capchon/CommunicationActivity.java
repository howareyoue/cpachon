package com.example.capchon;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CommunicationActivity extends AppCompatActivity {
    private ListView list;
    private FloatingActionButton floatingActionButton;
    private EditText searchEditText;
    private ArrayAdapter<CommunicationInfo> adapter;
    private List<CommunicationInfo> communicationList;
    private List<CommunicationInfo> filteredList;

    private static final int POST_ACTIVITY_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communication);

        list = findViewById(R.id.list);
        floatingActionButton = findViewById(R.id.Post_floating);
        searchEditText = findViewById(R.id.searchEditText);

        communicationList = new ArrayList<>();
        filteredList = new ArrayList<>();

        // Adapter 설정
        adapter = new ArrayAdapter<CommunicationInfo>(this, R.layout.communication_list_item, filteredList) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.communication_list_item, parent, false);
                }

                CommunicationInfo communicationInfo = getItem(position);

                TextView titleTextView = convertView.findViewById(R.id.TitleTextView);
                TextView contentsTextView = convertView.findViewById(R.id.ContentsTextView);

                if (communicationInfo != null) {
                    titleTextView.setText("제목: " + communicationInfo.getTitle());
                    contentsTextView.setText("내용: " + communicationInfo.getContents());
                }

                return convertView;
            }
        };
        list.setAdapter(adapter);

        // Firebase 데이터 로드
        loadCommunicationData();

        // 아이템 클릭 시 상세 화면으로 이동
        // 아이템 클릭 시 상세 화면으로 이동
        list.setOnItemClickListener((adapterView, view, position, id) -> {
            CommunicationInfo selectedCommunication = filteredList.get(position);
            Intent detailIntent = new Intent(CommunicationActivity.this, PostdetailActivity.class);
            detailIntent.putExtra("Title", selectedCommunication.getTitle()); // 제목 전달
            detailIntent.putExtra("Contents", selectedCommunication.getContents()); // 내용 전달
            detailIntent.putExtra("postId", selectedCommunication.getId()); // ID 추가
            startActivity(detailIntent);
        });


        // 글 작성 화면으로 이동
        floatingActionButton.setOnClickListener(view -> {
            Intent intent = new Intent(CommunicationActivity.this, PostActivity.class);
            startActivityForResult(intent, POST_ACTIVITY_REQUEST_CODE);
        });

        // 검색 필터 기능 구현
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateFilteredList();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == POST_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            // Firebase에서 다시 데이터를 로드
            loadCommunicationData();
        }
    }

    // Firebase 데이터 로드 메서드
    private void loadCommunicationData() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("CommunicationInfo");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                communicationList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CommunicationInfo communicationInfo = snapshot.getValue(CommunicationInfo.class);
                    if (communicationInfo != null) {
                        String id = snapshot.getKey(); // Firebase의 자동 생성 ID
                        communicationInfo.setId(id); // ID 설정
                        communicationList.add(communicationInfo);
                    }
                }
                updateFilteredList();  // 필터링된 리스트 업데이트
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 오류 처리
            }
        });
    }

    // 검색어에 따라 필터링된 목록을 업데이트하는 메서드
    private void updateFilteredList() {
        String query = searchEditText.getText().toString().toLowerCase().trim();
        filteredList.clear();

        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(communicationList); // 검색어가 없으면 모든 항목 표시
        } else {
            for (CommunicationInfo info : communicationList) {
                if (info.getTitle().toLowerCase().contains(query) || info.getContents().toLowerCase().contains(query)) {
                    filteredList.add(info); // 제목이나 내용에 검색어가 포함되면 추가
                }
            }
        }

        // 어댑터에게 데이터 변경 알림
        adapter.notifyDataSetChanged();
    }
}
