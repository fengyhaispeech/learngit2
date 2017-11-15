package com.yihengke.robotspeech.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yihengke.robotspeech.service.SpeechService;

/**
 * Created by Administrator on 2017/6/29.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    static final String ACTION_NOISY = "android.media.AUDIO_BECOMING_NOISY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION)) {
            Log.e("BootBroadcastReceiver", "robotspeech开机完成的action...");
//            Intent mIntent = new Intent(context, SpeechService.class);
//            context.startService(mIntent);
        }
        if (intent.getAction().equals(ACTION_NOISY)) {
            Log.e("BootBroadcastReceiver", "robotspeech开机铃声的action...");
            Intent mIntent = new Intent(context, SpeechService.class);
            context.startService(mIntent);
        }
    }
}
