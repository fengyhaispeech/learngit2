package com.yihengke.robotspeech.utils;

import android.media.MediaPlayer;

/**
 * Created by Administrator on 2017/11/14.
 *
 * @author Administrator
 */

public class MediaListener implements MediaPlayer.OnBufferingUpdateListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {
    private MediaPlayer tempMediaPlayer;

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        tempMediaPlayer = null;
        mediaPlayer.release();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        tempMediaPlayer = mediaPlayer;
        mediaPlayer.start();
    }

    private boolean isPlaying() {
        if (tempMediaPlayer != null && tempMediaPlayer.isPlaying()) {
            return true;
        }
        return false;
    }

    public void stopPlay() {
        if (isPlaying()) {
            tempMediaPlayer.stop();
            tempMediaPlayer.release();
            tempMediaPlayer = null;
        }
    }
}