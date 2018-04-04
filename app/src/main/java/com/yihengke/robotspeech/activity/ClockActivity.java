package com.yihengke.robotspeech.activity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.MediaListener;
import com.yihengke.robotspeech.utils.MyConstants;

/**
 * Created by Administrator on 2018/3/26.
 *
 * @author yhx
 */

public class ClockActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private MediaListener mediaListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clock);
        sendBroadcast(new Intent(MyConstants.SPEECH_WAKE_UP_ROBOT_SCREEN));
        initAnim();
        initAlarmMedia();
    }

    private void initAnim() {
        ImageView imageView = (ImageView) findViewById(R.id.image_clock_anim);
        imageView.setImageResource(R.drawable.clock_anim);
        AnimationDrawable animationDrawable = (AnimationDrawable) imageView.getDrawable();
        animationDrawable.start();
    }

    private void initAlarmMedia() {
        mediaListener = new MediaListener();
        mediaPlayer = MediaPlayer.create(ClockActivity.this, R.raw.music_clock);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnBufferingUpdateListener(mediaListener);
        mediaPlayer.setOnPreparedListener(mediaListener);
        mediaPlayer.setOnCompletionListener(mediaListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaListener != null) {
            mediaListener.stopPlay();
        }
    }
}
