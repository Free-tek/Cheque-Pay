package com.botosoft.chequepay;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

public class SignUpActivity extends AppCompatActivity {

    EditText userId, cardNo, expDate, cvv, password;
    Button signUp;
    TextView signIn;

    ProgressDialog progressDialog;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    final DatabaseReference usersID = database.getReference().child("userId");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initUi();



    }

    public void initUi(){
        userId = (EditText) findViewById(R.id.user_id);
        cardNo = (EditText) findViewById(R.id.card_no);
        expDate = (EditText) findViewById(R.id.exp_date);
        cvv = (EditText) findViewById(R.id.cvv);
        password = (EditText) findViewById(R.id.password);
        signIn = (TextView) findViewById(R.id.signIn);

        signUp = (Button) findViewById(R.id.signUp);



        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog = new ProgressDialog(SignUpActivity.this);
                progressDialog.setMessage("Creating Account...");
                progressDialog.setCanceledOnTouchOutside(true);
                progressDialog.show();

                if(validated()){
                    String uID = generateUniqueId();
                    usersID.child(userId.getText().toString()).child("cardNo").setValue(cardNo.getText().toString());
                    usersID.child(userId.getText().toString()).child("expiry_date").setValue(expDate.getText().toString());
                    usersID.child(userId.getText().toString()).child("cvv").setValue(cvv.getText().toString());
                    usersID.child(userId.getText().toString()).child("password").setValue(password.getText().toString());
                    progressDialog.dismiss();
                    Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                    startActivity(intent);
                }



            }
        });


    }

    public boolean validated(){
        boolean valid = true;
        String expdate= expDate.getText().toString();

        if(userId.getText().toString().length()!= 10){
            userId.setError("Invalid");
            valid = false;
            Toast.makeText(this, "Invalid User ID", Toast.LENGTH_SHORT).show();
        }else if(cardNo.getText().toString().length()!= 16){
            cardNo.setError("Invalid");
            valid = false;
            Toast.makeText(this, "Invalid card ID", Toast.LENGTH_SHORT).show();
        }else if(expDate.getText().toString().length()!= 5){
            expDate.setError("Invalid");
            valid = false;
            Toast.makeText(this, "Invalid expiry date", Toast.LENGTH_SHORT).show();
        }else if(password.getText().toString().length() <= 7){
            expDate.setError("Invalid");
            valid = false;
            Toast.makeText(this, "Password should be longer tha 7 digits", Toast.LENGTH_SHORT).show();
        }else if(!expdate.contains("/")){
            expDate.setError("Invalid date");
            valid = false;
            Toast.makeText(this, "Invalid date format use mm/yy ", Toast.LENGTH_SHORT).show();
        }else if(cvv.getText().toString().length()!= 3){
            cvv.setError("Invalid");
            valid = false;
            Toast.makeText(this, "Invalid cvv", Toast.LENGTH_SHORT).show();
        }else{
            String[] parts = expdate.split("/");
            Log.e("check", String.valueOf(parts));
            String part1 = parts[0];
            String part2 = parts[1];

            if(!(part1.matches("[0-9]+"))){
                expDate.setError("Invalid");
                valid = false;
                Toast.makeText(this, "Invalid Month", Toast.LENGTH_SHORT).show();
            }else if(!(part2.matches("[0-9]+"))){
                expDate.setError("Invalid");
                valid = false;
                Toast.makeText(this, "Invalid Year", Toast.LENGTH_SHORT).show();
            }

        }



        progressDialog.dismiss();
        return valid;
    }

    public String generateUniqueId(){
        Random rand = new Random();
        int n1 = rand.nextInt(9);
        int n2 = rand.nextInt(9);
        int n3 = rand.nextInt(9);
        int n4 = rand.nextInt(9);
        int n5 = rand.nextInt(9);
        int n6 = rand.nextInt(9);
        int n7 = rand.nextInt(9);
        int n8 = rand.nextInt(9);
        int n9 = rand.nextInt(9);

        final String uID = ""+n1+n2+n3+n4+n5+n6+n7+n8+n9;

        usersID.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.child(uID).exists()){
                    generateUniqueId();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return uID;
    }
}
