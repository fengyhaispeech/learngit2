package com.yihengke.robotspeech.activity;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.yihengke.robotspeech.BuildConfig;
import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.RotateAnim;
import com.yihengke.robotspeech.utils.WriteDataUtils;

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

    private MediaPlayer mediaPlayer;
    private int currentSong = 0;
    private int[] ljl = new int[]{0, 2, 3, 1, 2, 1, 0, 3, 2, 3, 1, 2};
    private int[] phw = new int[]{1, 0, 3, 2, 1, 2, 1, 0, 2, 3, 2, 0};
    private int[] fsj = new int[]{2, 0, 1, 2, 1, 0, 3, 1, 2, 3, 0, 2};

    private Timer mTimer, mAnimTimer, mBlinkTimer;

    private ImageView imageOpenEye, imageBlinkEye, imageLeftEye, imageRightEye, imageRedUp;
    private FrameLayout frameLayout;
    private MyHandler handler;
    private int currentAnim = 0;
    private int currentEye = -1;
    private long lastTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_robot);

        int index = getIntent().getIntExtra(SONG_INDEX, 1);
        currentSong = index;
        initMedia(currentSong);

        handler = new MyHandler();
        initViews();
//        startService(new Intent(MainActivity.this, SpeechService.class));
    }

    private void initViews() {
        imageOpenEye = (ImageView) findViewById(R.id.image_open_eye);
        imageBlinkEye = (ImageView) findViewById(R.id.image_blink_eye);
        imageLeftEye = (ImageView) findViewById(R.id.image_left_eye);
        imageRightEye = (ImageView) findViewById(R.id.image_right_eye);
        imageRedUp = (ImageView) findViewById(R.id.image_red_up);
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

        startAnim();
    }

    /**
     * 开始动画执行
     */
    private void startAnim() {
        if (mAnimTimer == null)
            mAnimTimer = new Timer();
        mAnimTimer.scheduleAtFixedRate(new AnimationTask(), 0, 10 * 1000);
        startBlinkEyes();
    }

    /**
     * 动画执行的定时任务
     */
    class AnimationTask extends TimerTask {
        @Override
        public void run() {
            currentAnim++;
            if (currentAnim > 1) {
                currentAnim = 0;
            }
            handler.sendEmptyMessage(0);
        }
    }

    /**
     * 开始执行眨眼动画
     */
    private void startBlinkEyes() {
        if (mBlinkTimer == null)
            mBlinkTimer = new Timer();
        mBlinkTimer.scheduleAtFixedRate(new BlinkTask(), 0, 1000);
    }

    /**
     * 停止眨眼动画
     */
    private void stopBlinkEyes() {
        if (mBlinkTimer != null) {
            mBlinkTimer.cancel();
            mBlinkTimer = null;
        }

        imageOpenEye.setVisibility(View.INVISIBLE);
        imageBlinkEye.setVisibility(View.INVISIBLE);
    }

    /**
     * 眨眼动画的定时任务
     */
    class BlinkTask extends TimerTask {
        @Override
        public void run() {
            currentEye++;
            if (currentEye > 2) {
                currentEye = 0;
            }
            handler.sendEmptyMessage(1);
        }
    }

    /**
     * 开始执行旋转动画
     */
    private void startRotateAnim() {
        imageLeftEye.setVisibility(View.VISIBLE);
        imageRightEye.setVisibility(View.VISIBLE);
        imageRedUp.setVisibility(View.VISIBLE);

        RotateAnimation animation = RotateAnim.loadAnimation();
        imageLeftEye.startAnimation(animation);
        imageRightEye.startAnimation(animation);
    }

    /**
     * 停止旋转动画
     */
    private void stopRoateAnim() {
        imageLeftEye.clearAnimation();
        imageRightEye.clearAnimation();

        imageLeftEye.setVisibility(View.INVISIBLE);
        imageRightEye.setVisibility(View.INVISIBLE);
        imageRedUp.setVisibility(View.INVISIBLE);
    }

    /**
     * 开始播放音乐
     *
     * @param index
     */
    private void initMedia(int index) {
        int resId = 0;
        if (index == 1) {
            resId = R.raw.penghuwan;
        } else if (index == 2) {
            resId = R.raw.lanjingling;
        } else if (index == 3) {
            resId = R.raw.fenshuajiang;
        } else {
            currentSong = 1;
            resId = R.raw.penghuwan;
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
            } else if (currentSong == 2) {
                if (times >= phw.length) {
                    times = 0;
                }
                int step = phw[times];
                WriteDataUtils.native_ear_light_control(0, step, 0);

                times++;
            } else if (currentSong == 3) {
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
        mp.release();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        WriteDataUtils.native_ear_light_control(0, 4, 0);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {

    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (currentAnim == 0) {
                        stopBlinkEyes();
                        startRotateAnim();
                    } else if (currentAnim == 1) {
                        stopRoateAnim();
                        startBlinkEyes();
                    }
                    break;
                case 1:
                    if (currentEye == 0) {

                    } else if (currentEye == 1) {
                        imageOpenEye.setVisibility(View.INVISIBLE);
                        imageBlinkEye.setVisibility(View.VISIBLE);
                    } else if (currentEye == 2) {
                        imageOpenEye.setVisibility(View.VISIBLE);
                        imageBlinkEye.setVisibility(View.INVISIBLE);
                    }
                    break;
                case 2:

                    break;
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
        if (mAnimTimer != null) {
            mAnimTimer.cancel();
            mAnimTimer = null;
        }
        if (mBlinkTimer != null) {
            mBlinkTimer.cancel();
            mBlinkTimer = null;
        }
        WriteDataUtils.native_ear_light_control(0, 4, 0);
    }

    @Override
    public void onBackPressed() {
        destroyFields();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WriteDataUtils.native_ear_light_control(0, 4, 0);
    }
}
