package com.yihengke.robotspeech;

import android.app.Application;

import com.aispeech.common.AIConstant;
import com.lzy.okgo.OkGo;


/**
 * Created by Administrator on 2017/8/3.
 */

public class RobotSpeechApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        //AIConstant.openLog();
        AIConstant.setAudioRecorderType(AIConstant.TYPE_COMMON_DUAL);//使用aec做在rom里的双麦
        OkGo.getInstance().init(this);
    }
}
