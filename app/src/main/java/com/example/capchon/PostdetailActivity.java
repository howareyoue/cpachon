package com.example.capchon;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PostdetailActivity extends AppCompatActivity {
    private TextView textViewTitle;
    private TextView textViewContents;
    private TextView textViewName;
    private EditText editComment;
    private Button buttonSubmitComment;
    private RecyclerView recyclerViewComments;
    private CommentAdapter commentAdapter;
    private List<Comment> commentList;
    private DatabaseReference commentReference;
    private DatabaseReference userReference;
    private DatabaseReference postUserReference;

    private String emailId;
    private String postAuthorId;
    private String communityTitle;
    private String communityContents;
    private String postId;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_postdetail);

        textViewTitle = findViewById(R.id.detail_title);
        textViewContents = findViewById(R.id.detail_contents);
        textViewName = findViewById(R.id.detail_name);
        editComment = findViewById(R.id.edit_comment);
        buttonSubmitComment = findViewById(R.id.button_submit_comment);
        recyclerViewComments = findViewById(R.id.recycler_view_comments);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            communityTitle = extras.getString("Title");
            communityContents = extras.getString("Contents");
            postAuthorId = extras.getString("name");

            postId = extras.getString("postId");
            commentReference = FirebaseDatabase.getInstance().getReference("Comments").child(postId);
            postUserReference = FirebaseDatabase.getInstance().getReference("UserAccount").child(postAuthorId);

            postUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserAccount postAuthor = dataSnapshot.getValue(UserAccount.class);
                    if (postAuthor != null) {
                        String authorName = postAuthor.getName();

                        textViewTitle.setText("제목: " + communityTitle);
                        textViewName.setText("작성자: " + authorName);
                        textViewContents.setText("내용: " + communityContents);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Toast.makeText(PostdetailActivity.this, "작성자 정보를 불러오는 데 실패했습니다.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // RecyclerView 설정
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(commentList);
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewComments.setAdapter(commentAdapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            emailId = user.getUid();
            userReference = FirebaseDatabase.getInstance().getReference("UserAccount").child(emailId);
        }

        buttonSubmitComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitComment();
            }
        });

        loadComment();
    }

    private void submitComment() {
        String commentText = editComment.getText().toString();
        if (TextUtils.isEmpty(commentText)) {
            Toast.makeText(this, "댓글을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserAccount userAccount = dataSnapshot.getValue(UserAccount.class);
                if (userAccount != null) {
                    String username = userAccount.getName();
                    Comment comment = new Comment(username, commentText);
                    commentReference.push().setValue(comment).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            editComment.setText("");
                            Toast.makeText(PostdetailActivity.this, "댓글이 등록되었습니다.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PostdetailActivity.this, "댓글 등록에 실패했습니다.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostdetailActivity.this, "댓글 등록에 실패했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadComment() {
        commentReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                commentList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Comment comment = snapshot.getValue(Comment.class);
                    commentList.add(comment);
                }
                commentAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PostdetailActivity.this, "댓글을 불러오는 데 실패했습니다.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
