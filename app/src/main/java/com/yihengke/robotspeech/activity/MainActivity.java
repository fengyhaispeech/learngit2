package com.yihengke.robotspeech.activity;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.WriteDataUtils;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        startService(new Intent(MainActivity.this, SpeechService.class));

    }

    public void btnStart(View v) {
//        startService(new Intent(MainActivity.this, SpeechService.class));

//        Random mRandom = new Random();
//        int next = mRandom.nextInt(6);
//        Log.e("MainActivity", "next = " + next);
//
//        int i = WriteDataUtils.native_ear_light_control(0, next, 0);
//        Log.e("main", "return i = " + i);
    }

    private MediaPlayer mediaPlayer;
    private int currentSong = 0;

    private int[] ljl = new int[]{0, 2, 3, 1, 2, 1, 0, 3, 2, 3, 1, 2};
    private int[] phw = new int[]{1, 0, 3, 2, 1, 2, 1, 0, 2, 3, 1, 2};

    private Timer mTimer;

    private void initMedia(int resId) {

//        if (mediaPlayer != null) {
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.stop();
//            }
//            mediaPlayer.release();
//            mediaPlayer = MediaPlayer.create(this, resId);
//            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
//            mediaPlayer.setOnBufferingUpdateListener(this);
//            mediaPlayer.setOnPreparedListener(this);
//            mediaPlayer.setOnCompletionListener(this);
//            mediaPlayer.reset();
//            try {
//                mediaPlayer.prepare();
//            } catch (IllegalArgumentException e) {
//                e.printStackTrace();
//            } catch (IllegalStateException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else {
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
//        }

        /*mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.reset();
        try {
            mediaPlayer.prepare();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void btnStartOne(View v) {
        currentSong = 1;
        initMedia(R.raw.lanjingling);
    }

    public void btnStartTwo(View v) {
        currentSong = 2;
        initMedia(R.raw.penghuwan);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
//        WriteDataUtils.native_ear_light_control(0, 4, 0);
//        mediaPlayer.start();
//        if (mTimer != null) {
//            mTimer.cancel();
//        } else {
//            mTimer = new Timer();
//        }
//        mTimer.scheduleAtFixedRate(new MediaTask(), 0, 3 * 1000);
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
                if (times >= ljl.length) {
                    times = 0;
                }
                int step = phw[times];
                WriteDataUtils.native_ear_light_control(0, step, 0);

                times++;
            } else {

            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
        WriteDataUtils.native_ear_light_control(0, 4, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WriteDataUtils.native_ear_light_control(0, 4, 0);
    }

}
