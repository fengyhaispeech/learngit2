package com.yihengke.robotspeech;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.aispeech.AISpeech;

/**
 * Created by Administrator on 2017/8/3.
 */

public class RobotSpeechApp extends Application {

    private String TAG = "RobotSpeechApp";
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        AISpeech.openLog();
        int result = AISpeech.init(getApplicationContext(), AppKey.APPKEY, AppKey.SECRETKEY);//这里的context一定要传Application的context，它的生命周期会贯穿整个应用的生命周期
        if (result == 0) {
            Log.d(TAG, "AISpeech sdk init ok");
        } else {
            Log.d(TAG, "AISpeech sdk init error : " + AISpeech.getErrorText(result));
        }
//		AISpeech.setEchoEnable(true);// 打开AEC
//		AISpeech.setEchoCfgFile("MIC2_2_MIC1_AEC_1ref_mute0_512.bin");// 设置AEC的配置文件
//		AISpeech.setAecSavedPath("/sdcard/aispeech/aecPcmFile/");//设置aec保存的音频文件路径
//		AISpeech.setRecChannel(2);// 默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）若设置为2,
        // 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
    }

    public static Context getContext() {
        return mContext;
    }

}
