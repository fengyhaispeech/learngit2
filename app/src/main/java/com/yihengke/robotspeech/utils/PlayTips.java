package com.yihengke.robotspeech.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.yihengke.robotspeech.R;

/**
 * Created by Administrator on 2017/11/20.
 *
 * @author Administrator
 */

public class PlayTips implements MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener {

    public MediaPlayer mediaPlayer;
    private Context mContext;

    public PlayTips(Context context) {
        mContext = context;
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(mContext, R.raw.wake_up);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
        }
    }

    public void palyWakeUp() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(mContext, R.raw.wake_up);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
        }
        mediaPlayer.start();
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
    }
}
