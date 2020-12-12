package com.app.schat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.VideoView;

import java.io.File;

public class VideoActivity extends AppCompatActivity {
    private VideoView video_view;
    private String file_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        //intent
        Intent intent = this.getIntent();
        file_path = intent.getStringExtra("file");
        Log.d("videoplay" , "file_path:" + file_path);
        //widget
        video_view = (VideoView)this.findViewById(R.id.vv_video_act);

        //go
        //Uri uri = Uri.parse("android.resource://" + getPackageName() + "/"+R.raw.cs);
        Uri uri = Uri.fromFile(new File(file_path));
        Log.d("videoplay" , "url:" + uri.toString());
        video_view.setVideoURI(uri);
        video_view.setMediaController(new MediaController(this));
        video_view.seekTo(0);
        video_view.requestFocus();
        video_view.setZOrderOnTop(true);
        //video_view.start();



        //listener
        //listen
        video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mediaPlayer.setLooping(false);
            }
        });

        video_view.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //AppConfig.PrintInfo(getBaseContext() , "done");
            }
        });


    }
}