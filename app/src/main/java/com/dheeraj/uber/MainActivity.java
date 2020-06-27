package com.dheeraj.uber;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageButton mDriver, mCustomer;
    public VideoView videoView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //videoView
        videoView=findViewById(R.id.video_view);

        MediaController mediaController= new MediaController(this);
        mediaController.setAnchorView(videoView);

        //specify the location of media file
        String path="android.resource://"+getPackageName()+"/" ;
        //Setting MediaController and URI, then starting the videoView
        videoView.setVideoURI(Uri.parse(path+R.raw.videonew));
        videoView.requestFocus();
        videoView.start();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.start();
            }
        });




        mDriver=findViewById(R.id.Driver_button);
        mCustomer=findViewById(R.id.Customer_button);
        mDriver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this,DriverLoginActivity.class);
                startActivity(intent);
                finish();

            }
        });
        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(MainActivity.this,CustomerLoginActivity.class);
                startActivity(intent);
                finish();

            }
        });
    }
}
