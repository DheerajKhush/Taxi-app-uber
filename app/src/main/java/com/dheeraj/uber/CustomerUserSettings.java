package com.dheeraj.uber;

import androidx.annotation.IntegerRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class CustomerUserSettings extends AppCompatActivity {


    private EditText nameEditTxt, phoneEditTxt;
    private ImageButton confirmbutton;
    private ImageButton backbutton;
    private ImageView profileButton;
    private FirebaseAuth mAuth;
    private DatabaseReference customerDatabaseReference;
    String userID;
    private Uri resultIamgeUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_user_settings);
        nameEditTxt=findViewById(R.id.nameEditTxt);
        phoneEditTxt=findViewById(R.id.phoneEditTxt);
        confirmbutton=findViewById(R.id.confirmbutton);
        backbutton=findViewById(R.id.backButton);
        profileButton=findViewById(R.id.profile_image);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(Intent.ACTION_PICK);
                intent.setType("iamge/");
                startActivityForResult(intent,1);
            }
        });
        backbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//
                CustomerUserSettings.this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                CustomerUserSettings.this.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
                finish();
            }
        });
        mAuth= FirebaseAuth.getInstance();
        userID=mAuth.getCurrentUser().getUid();
        customerDatabaseReference= FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);
        getUserInfo();
        confirmbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserInfo();
                Toast.makeText(CustomerUserSettings.this,"Profile Updated Successfully",Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==1&& resultCode== Activity.RESULT_OK){
            final Uri imageURi=data.getData();
            resultIamgeUri=imageURi;
            profileButton.setImageURI(resultIamgeUri);


        }
    }

    private void updateUserInfo(){
        String name=nameEditTxt.getText().toString();
        String phone= phoneEditTxt.getText().toString();
        Map<String, Object> userInfo= new HashMap<>();
        userInfo.put("name",name);
        userInfo.put("phone",phone);
        customerDatabaseReference.updateChildren(userInfo);
    }
    private void getUserInfo(){
        customerDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&& dataSnapshot.getChildrenCount()>0){
                    Map<String, Object> map= (Map<String, Object>)dataSnapshot.getValue();
                    if(map.get("name")!=null){
                        String name=map.get("name").toString();
                        nameEditTxt.setText(name);
                    }
                    if(map.get("phone")!=null){
                        String phone=map.get("phone").toString();
                        phoneEditTxt.setText(phone);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

}

