package com.yihengke.robotspeech.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.yihengke.robotspeech.BuildConfig;
import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.WriteDataUtils;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/8/8.
 */

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private String TAG = "MainActivity";
    private String SONG_INDEX = "SONG_INDEX";
    private boolean isDebugLog = BuildConfig.DEBUG_LOG;

    private static final String ACTION_DANCE_STARTED = "action_dance_started";//发送
    private static final String ACTION_DANCE_STOPED = "action_dance_stoped";//发送
    private static final String ACTION_DANCE_SERVICE_PAUSED = "action_dance_service_paused";//接收
    private static final String ACTION_DANCE_SERVICE_STOP = "action_dance_service_stop";//接收
    private static final String ACTION_DANCE_SERVICE_GO_ON = "action_dance_service_go_on";//接收
    private static final String ACTION_DANCE_SERVICE_NEXT_UP = "action_dance_service_next_up";//接收

    private MainReceiver mainReceiver;

    private MediaPlayer mediaPlayer;
    private int currentSong = 0;
    private int[] ljl = new int[]{0, 2, 3, 1, 2, 1, 0, 3, 0, 3, 1, 2};
    private int[] fsj = new int[]{2, 0, 1, 2, 1, 0, 3, 1, 2, 3, 0, 2};

    private Timer mTimer;

    private AnimationDrawable animationDrawable;
    private ImageView imageDance;
    private FrameLayout frameLayout;
    private long lastTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_robot);

        int index = getIntent().getIntExtra(SONG_INDEX, -1);
        if (index == -1) {
            Random mRandom = new Random();
            index = mRandom.nextInt(2);
        } else {
            sendBroadcast(new Intent(ACTION_DANCE_STARTED));
        }
        currentSong = index;
        initMedia(currentSong);

        initViews();
//        startService(new Intent(MainActivity.this, SpeechService.class));
        initReceiver();
    }

    private void initViews() {
        imageDance = (ImageView) findViewById(R.id.image_dance_anim);
        startAnimation();
        frameLayout = (FrameLayout) findViewById(R.id.fl_click_layout);
        frameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis() / 1000;
                if ((currentTime - lastTime) <= 1) {
                    if (isDebugLog)
                        Log.e(TAG, "lastTime = " + lastTime + "  currentTime = " + currentTime);
                    destroyFields();
                    finish();
                } else {
                    if (isDebugLog)
                        Log.e(TAG, "lastTime = " + lastTime + "  currentTime = " + currentTime);
                    lastTime = currentTime;
                }
            }
        });
    }

    /**
     * 开始播放音乐
     *
     * @param index
     */
    private void initMedia(int index) {
        int resId = 0;
        if (index == 0) {
            resId = R.raw.fenshuajiang;
        } else if (index == 1) {
            resId = R.raw.lanjingling;
        } else {
            currentSong = 0;
            resId = R.raw.fenshuajiang;
        }
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
//            mediaPlayer.reset();
        mediaPlayer.start();

        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new MediaTask(), 0, 3 * 1000);
        }
    }

    /**
     * 音乐播放时机器人运动的定时任务
     */
    class MediaTask extends TimerTask {
        int times = 0;

        @Override
        public void run() {
            if (currentSong == 1) {
                if (times >= ljl.length) {
                    times = 0;
                }
                int step = ljl[times];
                WriteDataUtils.native_ear_light_control(0, step, 0);

                times++;
            } else if (currentSong == 0) {
                if (times >= fsj.length) {
                    times = 0;
                }
                int step = fsj[times];
                WriteDataUtils.native_ear_light_control(0, step, 0);

                times++;
            } else {

            }
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
//        mp.release();
        destroyFields();
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    private void initReceiver() {
        mainReceiver = new MainReceiver();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ACTION_DANCE_SERVICE_PAUSED);
        mFilter.addAction(ACTION_DANCE_SERVICE_STOP);
        mFilter.addAction(ACTION_DANCE_SERVICE_GO_ON);
        mFilter.addAction(ACTION_DANCE_SERVICE_NEXT_UP);
        registerReceiver(mainReceiver, mFilter);
    }

    class MainReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_DANCE_SERVICE_PAUSED)) {
                mediaPlayer.pause();
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                WriteDataUtils.native_ear_light_control(0, 4, 0);
            } else if (action.equals(ACTION_DANCE_SERVICE_GO_ON)) {
                mediaPlayer.start();
                if (mTimer == null) {
                    mTimer = new Timer();
                    mTimer.scheduleAtFixedRate(new MediaTask(), 0, 3 * 1000);
                }
            } else if (action.equals(ACTION_DANCE_SERVICE_STOP)) {
                destroyFields();
                finish();
            } else if (action.equals(ACTION_DANCE_SERVICE_NEXT_UP)) {
                if (currentSong == 0) {
                    currentSong = 1;
                    initMedia(currentSong);
                } else if (currentSong == 1) {
                    currentSong = 0;
                    initMedia(currentSong);
                } else {
                    currentSong = 0;
                    initMedia(currentSong);
                }
            }
        }
    }

    /**
     * finish页面前先清除设置的变量
     */
    private void destroyFields() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        if (mainReceiver != null) {
            unregisterReceiver(mainReceiver);
            mainReceiver = null;
        }
        WriteDataUtils.native_ear_light_control(0, 4, 0);
    }

    private void startAnimation() {
        if (animationDrawable == null) {
            imageDance.setImageResource(R.drawable.dance_image);
            animationDrawable = (AnimationDrawable) imageDance.getDrawable();
            animationDrawable.start();
        } else {
            animationDrawable.start();
        }
    }

    private void stopAnimation() {
        if (animationDrawable != null) {
            animationDrawable.stop();
        }
    }

    @Override
    public void onBackPressed() {
        destroyFields();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        sendBroadcast(new Intent(ACTION_DANCE_STOPED));
        destroyFields();
        stopAnimation();
        super.onDestroy();
    }
}
