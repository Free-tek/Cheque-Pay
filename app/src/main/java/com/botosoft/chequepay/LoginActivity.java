package com.botosoft.chequepay;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    TextView forgotPassword, signUp ;
    EditText uniqueId, password;
    Button login;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    final DatabaseReference usersID = database.getReference().child("userId");

    ProgressDialog progressDialog;

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String UserIdKey = "UserIdKey";

    SharedPreferences sharedpreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        initUi();

    }

    private void initUi() {

        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        forgotPassword = (TextView) findViewById(R.id.forgotPassword);

        uniqueId = (EditText) findViewById(R.id.user_id);
        password = (EditText) findViewById(R.id.password);

        login = (Button) findViewById(R.id.login);
        signUp = (TextView) findViewById(R.id.signUp);

        forgotPassword.setAlpha(0);
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(signUp);
                finish();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.show();

                if(validate_entry()){
                    usersID.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.child(uniqueId.getText().toString()).exists()){
                                String _password = String.valueOf(password.getText());
                                if(dataSnapshot.child(uniqueId.getText().toString()).child("password").getValue().equals(_password)){

                                    SharedPreferences.Editor editor = sharedpreferences.edit();
                                    editor.putString(UserIdKey, uniqueId.getText().toString());
                                    editor.commit();



                                    Intent signUp = new Intent(LoginActivity.this, MainActivity.class);
                                    startActivity(signUp);
                                    finish();
                                }else{
                                    Toast.makeText(LoginActivity.this, "Invalid password 4 trials left", Toast.LENGTH_SHORT).show();
                                }

                            }else{
                                Toast.makeText(LoginActivity.this, "User ID does not exist", Toast.LENGTH_SHORT).show();
                            }

                            progressDialog.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }else{
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this, "Invalid Unique Id", Toast.LENGTH_SHORT).show();
                }



            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signUp = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(signUp);
                finish();
            }
        });
    }

    public boolean validate_entry(){
        boolean valid = true;

        if(uniqueId.getText().length() != 10){
            uniqueId.setError("Invalid Unique Id");
            valid = false;
        }

        return valid;
    }
}
