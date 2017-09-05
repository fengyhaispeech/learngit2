package com.yihengke.robotspeech.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.MediaController;
import android.widget.VideoView;

import com.yihengke.robotspeech.BuildConfig;
import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.WriteDataUtils;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/8/24.
 */

public class MainActivityOld extends AppCompatActivity {

    private String TAG = "MainActivityOld";
    private String SONG_INDEX = "SONG_INDEX";
    private boolean isDebugLog = BuildConfig.DEBUG_LOG;

    private MainReceiver mainReceiver;

    private static final String ACTION_DANCE_STARTED = "action_dance_started";//发送
    private static final String ACTION_DANCE_STOPED = "action_dance_stoped";//发送
    private static final String ACTION_DANCE_SERVICE_PAUSED = "action_dance_service_paused";//接收
    private static final String ACTION_DANCE_SERVICE_STOP = "action_dance_service_stop";//接收
    private static final String ACTION_DANCE_SERVICE_GO_ON = "action_dance_service_go_on";//接收
    private static final String ACTION_DANCE_SERVICE_NEXT_UP = "action_dance_service_next_up";//接收

    private int currentSong;
    private int[] ljl = new int[]{0, 2, 3, 1, 2, 1, 0, 3, 0, 3, 1, 2};
    private int[] fsj = new int[]{2, 0, 1, 2, 1, 0, 3, 1, 2, 3, 0, 2};
    private Timer mTimer;

    private VideoView mVideoView;
    String uriLanjingling;
    String uriFenshuajiang;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);
        uriLanjingling = "android.resource://" + getPackageName() + "/" + R.raw.lanjingling;
        uriFenshuajiang = "android.resource://" + getPackageName() + "/" + R.raw.fenshuajiang;

        mVideoView = (VideoView) findViewById(R.id.videoView_dance);
        mVideoView.setMediaController(new MediaController(this));
        Random mRandom = new Random();
        currentSong = mRandom.nextInt(2);
        initVideo(currentSong);
        initReceiver();
        sendBroadcast(new Intent(ACTION_DANCE_STARTED));
        //startService(new Intent(MainActivity.this, SpeechService.class));
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                WriteDataUtils.native_ear_light_control(0, 4, 0);
                finish();
            }
        });
    }

    private void initVideo(int index) {
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
        }
        if (index == 0) {
            mVideoView.setVideoURI(Uri.parse(uriLanjingling));
        } else {
            mVideoView.setVideoURI(Uri.parse(uriFenshuajiang));
        }
        mVideoView.start();
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new MediaTask(), 0, 3 * 1000);
        }
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
                mVideoView.pause();
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                WriteDataUtils.native_ear_light_control(0, 4, 0);
            } else if (action.equals(ACTION_DANCE_SERVICE_GO_ON)) {
                mVideoView.start();
                if (mTimer == null) {
                    mTimer = new Timer();
                    mTimer.scheduleAtFixedRate(new MediaTask(), 0, 3 * 1000);
                }
            } else if (action.equals(ACTION_DANCE_SERVICE_STOP)) {
                mVideoView.stopPlayback();
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                WriteDataUtils.native_ear_light_control(0, 4, 0);
                finish();
            } else if (action.equals(ACTION_DANCE_SERVICE_NEXT_UP)) {
                if (currentSong == 0) {
                    currentSong = 1;
                    initVideo(currentSong);
                } else {
                    currentSong = 0;
                    initVideo(currentSong);
                }
            }
        }
    }

    /**
     * 音乐播放时机器人运动的定时任务
     */
    class MediaTask extends TimerTask {
        int times = 0;

        @Override
        public void run() {
            if (currentSong == 0) {
                if (times >= ljl.length) {
                    times = 0;
                }
                int step = ljl[times];
                WriteDataUtils.native_ear_light_control(0, step, 0);

                times++;
            } else if (currentSong == 1) {
                if (times >= fsj.length) {
                    times = 0;
                }
                int step = fsj[times];
                WriteDataUtils.native_ear_light_control(0, step, 0);

                times++;
            }
        }
    }

    @Override
    public void onBackPressed() {
        mVideoView.stopPlayback();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        WriteDataUtils.native_ear_light_control(0, 4, 0);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        WriteDataUtils.native_ear_light_control(0, 4, 0);
        sendBroadcast(new Intent(ACTION_DANCE_STOPED));
        if (mainReceiver != null) {
            unregisterReceiver(mainReceiver);
        }
        super.onDestroy();
    }

}
