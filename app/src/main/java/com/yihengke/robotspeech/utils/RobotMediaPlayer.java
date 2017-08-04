package com.yihengke.robotspeech.utils;

/**
 * Created by Administrator on 2017/8/3.
 */

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.yihengke.robotspeech.BuildConfig;

import java.io.IOException;

public class RobotMediaPlayer implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    private String TAG = "RobotMediaPlayer";
    private boolean isDebugLog = BuildConfig.DEBUG_LOG;
    public MediaPlayer mediaPlayer;

    private MPOnCompletionListener mpOnCompletionListener;

    public RobotMediaPlayer() {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
        } catch (Exception e) {
            if (isDebugLog) Log.e(TAG, "error", e);
        }
    }

    public void play() {
        mediaPlayer.start();
    }

    public void playUrl(String videoUrl) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(videoUrl);
            mediaPlayer.prepare();//prepare之后自动播放
            //mediaPlayer.start();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void pause() {
        mediaPlayer.pause();
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 通过onPrepared播放
     */
    @Override
    public void onPrepared(MediaPlayer arg0) {
        if (isDebugLog) Log.e(TAG, "onPrepared 开始播放");
        arg0.start();
    }

    @Override
    public void onCompletion(MediaPlayer arg0) {
        if (isDebugLog) Log.e(TAG, "onCompletion 播放完成");
        arg0.release();
        mpOnCompletionListener.onCompletionListener();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer arg0, int bufferingProgress) {
        if (isDebugLog) Log.i(TAG, "duration = " + mediaPlayer.getDuration());
    }

    public void setOnCompletionListener(MPOnCompletionListener mpOnCompletionListener) {
        this.mpOnCompletionListener = mpOnCompletionListener;
    }
}
