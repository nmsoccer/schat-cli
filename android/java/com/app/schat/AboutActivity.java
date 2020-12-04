package com.app.schat;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    private TextView tv_version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //widget
        tv_version = (TextView)this.findViewById(R.id.tv_about_version);
        tv_version.setText("version:" + AppConfig.version);

    }
}