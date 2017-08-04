package com.yihengke.robotspeech.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.service.SpeechService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(MainActivity.this, SpeechService.class));
    }

    public void btnStart(View v) {
        startService(new Intent(MainActivity.this, SpeechService.class));
    }
}
