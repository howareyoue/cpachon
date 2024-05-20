package com.example.capchon;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private FirebaseAuth mFirebaseAuth; //파이어베이스 인증
    private DatabaseReference mDatabaseRef ; //실시간데이터베이스
    private EditText mEtmail,mEtPwd, mEtname, mEtrank, mEtnumber,mEtcompany;
    private Button mBtnregister;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mEtmail = findViewById(R.id.et_email);
        mEtPwd = findViewById(R.id.et_pwd);
        mEtname = findViewById(R.id.et_name);
        mEtnumber = findViewById(R.id.et_number);
        mEtcompany= findViewById(R.id.et_companyname);

        mBtnregister = findViewById(R.id.btn_register);

        mBtnregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String strEmail = mEtmail.getText().toString();
                String strPwd = mEtPwd.getText().toString();
                String strName = mEtname.getText().toString();
                String strNumber = mEtnumber.getText().toString();
                String strCompany = mEtcompany.getText().toString();

                //firebaseAuth 진행
                mFirebaseAuth.createUserWithEmailAndPassword(strEmail,strPwd).addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
                            UserAccount account = new UserAccount();
                            account.setIdToken(firebaseUser.getUid());
                            account.setEmailId(firebaseUser.getEmail());
                            account.setPassword(strPwd);
                            account.setName(strName);
                            account.setNumber(strNumber);
                            //account.setCompanyname(strCompany);

                            //setValue : 데이터베이스에 삽입행위
                            mDatabaseRef.child("UserAccount").child(firebaseUser.getUid()).setValue(account);

                            Toast.makeText(RegisterActivity.this, "회원가입에 성공하셨습니다.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, loginActivity.class);
                            startActivity(intent);

                        }
                        else {
                            Toast.makeText(RegisterActivity.this, "회원가입에 실패하셨습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        });

    }
}