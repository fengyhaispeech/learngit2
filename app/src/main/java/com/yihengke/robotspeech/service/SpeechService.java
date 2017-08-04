package com.yihengke.robotspeech.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;
import com.aispeech.export.engines.AICloudSdsEngine;
import com.aispeech.export.engines.AILocalTTSEngine;
import com.aispeech.export.engines.AILocalWakeupDnnEngine;
import com.aispeech.export.listeners.AIAuthListener;
import com.aispeech.export.listeners.AILocalWakeupDnnListener;
import com.aispeech.export.listeners.AISdsListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.speech.AIAuthEngine;
import com.yihengke.robotspeech.BuildConfig;
import com.yihengke.robotspeech.utils.MPOnCompletionListener;
import com.yihengke.robotspeech.utils.RobotMediaPlayer;
import com.yihengke.robotspeech.utils.SampleConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Random;

/**
 * Created by Administrator on 2017/8/3.
 */

public class SpeechService extends Service implements MPOnCompletionListener {

    private String TAG = "SpeechService";
    private boolean isDebugLog = BuildConfig.DEBUG_LOG;

    /**
     * 要读出的话
     */
    private String CN_PREVIEW = "";
    private String HELP_TIP = "有什么我可以帮您？";

    private MyHandler mHandler;
    private boolean isInited = false;
    private boolean isAuthed = false;
    private ScreenReceiver mScreenReceiver;
    private PowerManager.WakeLock wakeLock = null;

    private AIAuthEngine mAiAuthEngine;
    private AILocalWakeupDnnEngine mAiLocalWakeupDnnEngine;
    private AILocalTTSEngine mAiLocalTTSEngine;
    private AICloudSdsEngine mAiCloudSdsEngine;
//    private CircularFifoQueue<byte[]> mFifoQueue = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (isDebugLog) Log.e(TAG, "service created ...");

        init();
        startForeground(0, null);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isDebugLog) Log.e(TAG, "service onStartCommand ...");
        if (!isInited) {
            init();
        }
        initReceiver();
        mHandler = new MyHandler();

        return START_STICKY;
    }

    private void initReceiver() {
        mScreenReceiver = new ScreenReceiver();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenReceiver, mFilter);
        Log.e(TAG, "registerReceiver......");
    }

    class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (isDebugLog) Log.e(TAG, "监听到屏幕关闭...");

                if (isAuthed) {
                    if (isDebugLog) Log.i(TAG, "已经获取了授权（息屏后监测判断）,等待唤醒...");
                    mAiLocalWakeupDnnEngine.start();//息屏后判断如果已经获取了授权，就开始等待唤醒
                }

            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (isDebugLog) Log.e(TAG, "监听到屏幕点亮,释放wakelock...");
                releaseWakeLock();
            }
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:

                    break;
                case 1:

                    break;
            }
        }
    }

    /**
     * 先初始化引擎，然后isAuthed()判断是否已经做过授权，如果已经授权，则可以使用其他功能，
     * 如果没有授过权，则需要在联网状态下调用doAuth()。
     * note:isAuthed()和 doAuth请确保在init方法执行之后再执行，
     * 否则可能会报{"errId":70724,"error":"Auth failed: pls check auth: Open provision file failed."}的错误。
     */
    private void init() {
        if (isDebugLog) Log.e(TAG, "SpeechService init auth...");
        isInited = true;

        mAiAuthEngine = AIAuthEngine.getInstance();
        try {
            mAiAuthEngine.init();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }// TODO 换成您的APPKEY和SECRETKEY

        mAiAuthEngine.setOnAuthListener(new RobotAIAuthListener());

        //判断是否取得授权，如果取得直接准备唤醒，如果没有等待授权成功后准备唤醒
        if (mAiAuthEngine.isAuthed()) {
            if (isDebugLog) Log.i(TAG, "已经获得授权...");
            isAuthed = true;
            waitForWakeUp();
//            initmAiCloudSdsEngine();
//            initAILocalTTSEngine();

        } else {
            if (isDebugLog) Log.i(TAG, "还没有获得授权...");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (isDebugLog) Log.e(TAG, "开始执行授权...");
                    mAiAuthEngine.doAuth();
                }
            }).start();
        }
    }

    /**
     * 授权监听
     */
    class RobotAIAuthListener implements AIAuthListener {
        @Override
        public void onAuthSuccess() {
            if (isDebugLog) Log.i(TAG, "授权成功...");
            isAuthed = true;
            //waitForWakeUp();
        }

        @Override
        public void onAuthFailed(String s) {
            if (isDebugLog) Log.i(TAG, "授权失败...");
        }
    }

    /**
     * 初始化唤醒
     */
    private void waitForWakeUp() {
        mAiLocalWakeupDnnEngine = AILocalWakeupDnnEngine.createInstance();
        mAiLocalWakeupDnnEngine.setResBin(SampleConstants.wake_up_res);
        mAiLocalWakeupDnnEngine.init(new RobotAILocalWakeupDnnListener());
//      mEngine.setEchoWavePath("/sdcard/speech"); //保存音频到/sdcard/speech/目录,请确保该目录存在
//      mAiLocalWakeupDnnEngine.setStopOnWakeupSuccess(true);//设置当检测到唤醒词后自动停止唤醒引擎
    }

    /**
     * 唤醒回调接口
     */
    class RobotAILocalWakeupDnnListener implements AILocalWakeupDnnListener {

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
                if (isDebugLog) Log.i(TAG, "唤醒引擎初始化成功...开始等待唤醒...");
//                mAiLocalWakeupDnnEngine.start();

            } else {
                if (isDebugLog) Log.e(TAG, "唤醒引擎初始化 失败...");

            }
        }

        @Override
        public void onError(AIError aiError) {
            if (isDebugLog) Log.e(TAG, "唤醒回调显示失败...");
        }

        @Override
        public void onWakeup(String s, double v, String s1) {
            if (isDebugLog) Log.e(TAG, "唤醒成功...");
            //点亮屏幕
            acquireWakeLock();
        }

        @Override
        public void onRmsChanged(float v) {
            if (isDebugLog) Log.i(TAG, "onRmsChanged rmsDb = " + v);

        }

        @Override
        public void onRecorderReleased() {
            if (isDebugLog) Log.i(TAG, "onRecorderReleased...");

        }

        @Override
        public void onReadyForSpeech() {
            if (isDebugLog) Log.i(TAG, "可以开始说话了...");

        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            if (isDebugLog) Log.i(TAG, "onBufferReceived bytes...");

        }

        @Override
        public void onWakeupEngineStopped() {
            mAiLocalWakeupDnnEngine.start();
        }
    }

    /**
     * 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
     */
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK, "PostLocationService");
            if (null != wakeLock) {
                wakeLock.acquire();
                if (isDebugLog) Log.e(TAG, "acquireWakeLock OK......");
                //重新点亮屏幕后，说出“有什么我可以帮您？”
//                initAILocalTTSEngine();
            }
        }
    }

    /**
     * 释放设备电源锁
     */
    private void releaseWakeLock() {
        if (null != wakeLock) {
            wakeLock.release();
            wakeLock = null;
            if (isDebugLog) Log.e(TAG, "releaseWakeLock OK......");
        }
    }

    /**
     * 初始化本地语音合成引擎
     */
    private void initAILocalTTSEngine() {
        if (mAiLocalTTSEngine != null) {
            mAiLocalTTSEngine.destroy();
        }
        mAiLocalTTSEngine = AILocalTTSEngine.createInstance();//创建实例
        mAiLocalTTSEngine.setResource(SampleConstants.model_res);
        mAiLocalTTSEngine.setDictDbName(SampleConstants.dict_db);
        mAiLocalTTSEngine.init(new RobotAITTSListener());//初始化合成引擎
        mAiLocalTTSEngine.setSpeechRate(0.85f);//设置语速
    }

    /**
     * 本地合成语音引擎监听
     */
    class RobotAITTSListener implements AITTSListener {
        @Override
        public void onInit(int status) {
            if (isDebugLog) Log.i(TAG, "RobotAITTSListener初始化完成，返回值：" + status);
            if (status == AIConstant.OPT_SUCCESS) {
                if (isDebugLog) Log.i(TAG, "RobotAITTSListener初始化成功...");
//                speakTips();
                initmAiCloudSdsEngine();

            } else {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener初始化失败...");

            }
        }

        @Override
        public void onError(String s, AIError aiError) {
            if (isDebugLog) Log.i(TAG, "RobotAITTSListener onError...");
        }

        @Override
        public void onReady(String s) {
            if (isDebugLog) Log.i(TAG, "RobotAITTSListener onReady...");
        }

        @Override
        public void onCompletion(String s) {
            if (isDebugLog) Log.i(TAG, "RobotAITTSListener onCompletion = " + s);
            mAiCloudSdsEngine.startWithRecording();
        }

        @Override
        public void onProgress(int i, int i1, boolean b) {
            if (isDebugLog)
                Log.i(TAG, "RobotAITTSListener onProgress i = " + i + ", i1 = " + i1 + ", b = " + b);
        }
    }

    /**
     * 使用本地合成语音播放提示语
     */
    private void speakTips() {
        if (mAiLocalTTSEngine != null) {
            if (isDebugLog) Log.e(TAG, "开始播放提示语...");
            mAiLocalTTSEngine.setSavePath(Environment.getExternalStorageDirectory() + "/linzhilin/"
                    + System.currentTimeMillis() + ".wav");
            mAiLocalTTSEngine.speak(CN_PREVIEW, "1024");
        }
    }

    /**
     * 初始化云端对话引擎
     */
    private void initmAiCloudSdsEngine() {
        mAiCloudSdsEngine = AICloudSdsEngine.createInstance();
        mAiCloudSdsEngine.setRes("airobot");
        mAiCloudSdsEngine.setServer("ws://s-test.api.aispeech.com:10000");
        mAiCloudSdsEngine.setVadResource(SampleConstants.vad_res);
//        mAiCloudSdsEngine.setUserId("AISPEECH"); //填公司名字
        mAiCloudSdsEngine.setServerConnectTimeout(5);
        mAiCloudSdsEngine.init(new RobotAISdsListener());
    }

    /**
     * 云端对话监听
     */
    class RobotAISdsListener implements AISdsListener {
        @Override
        public void onInit(int staus) {
            if (isDebugLog) Log.i(TAG, "mAiCloudSdsEngine onIinit staus = " + staus);
            if (staus == AIConstant.OPT_SUCCESS) {
                if (isDebugLog) Log.i(TAG, "mAiCloudSdsEngine 初始化成功");
                mAiCloudSdsEngine.startWithRecording();
            } else {
                if (isDebugLog) Log.e(TAG, "mAiCloudSdsEngine 初始话失败");

            }
        }

        @Override
        public void onError(AIError aiError) {
            if (isDebugLog) Log.e(TAG, "RobotAISdsListener onError = " + aiError.toString());
            mAiCloudSdsEngine.startWithRecording();//重新等待语音
        }

        @Override
        public void onResults(AIResult aiResult) {
            if (aiResult.isLast()) {
                if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                    if (isDebugLog)
                        Log.i(TAG, "result JSON = " + aiResult.getResultObject().toString());
                    parseData(aiResult);
                }
            }
        }

        @Override
        public void onRmsChanged(float v) {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener onRmsChanged = " + v);
        }

        @Override
        public void onReadyForSpeech() {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener 可以开始说话了");
        }

        @Override
        public void onBeginningOfSpeech() {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener 检测到说话了");
        }

        @Override
        public void onEndOfSpeech() {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener 检测到语音停止，开始识别");
        }

        @Override
        public void onRecorderReleased() {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener onRecorderReleased...");
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener onBufferReceived...");
        }

        @Override
        public void onNotOneShot() {
            if (isDebugLog) Log.i(TAG, "RobotAISdsListener onNotOneShot...");
        }
    }

    private void parseData(AIResult aiResult) {
        JSONResultParser parser = new JSONResultParser(aiResult.getResultObject().toString());
        JSONObject result = parser.getResult();
        try {
            JSONObject sdsJsonObj = result.getJSONObject("sds");
            String domain = sdsJsonObj.getString("domain");
            if (TextUtils.isEmpty(domain)) {
                if (isDebugLog) Log.e(TAG, "解析json， domain 是空...");
                return;
            }
            if (domain.equals("netfm") || domain.equals("story")) {
                JSONObject dataJsonObj = sdsJsonObj.getJSONObject("data");
                if (dataJsonObj == null) {
                    if (isDebugLog) Log.e(TAG, "domain = netfm，data == null");
                    return;
                }
                String output = sdsJsonObj.getString("output");
                if (!TextUtils.isEmpty(output)) {
//                    CN_PREVIEW = output;
//                    speakTips();
                }
                JSONArray dbdataJsArray = dataJsonObj.getJSONArray("dbdata");
                if (dbdataJsArray != null && dbdataJsArray.length() > 0) {
                    //随机播放网络资源
                    Random mRandom = new Random();
                    int randomInt = mRandom.nextInt(dbdataJsArray.length());
                    JSONObject netfmData = dbdataJsArray.getJSONObject(randomInt);
                    String url = netfmData.getString("playUrl32");
                    if (TextUtils.isEmpty(url)) {
                        if (isDebugLog) Log.e(TAG, "playUrl32 isEmpty...");
                        url = netfmData.getString("playUrl64");
                        if (TextUtils.isEmpty(url)) {
                            if (isDebugLog) Log.e(TAG, "playUrl64 isEmpty...");
                            //没有音乐文件

                        } else {
                            if (isDebugLog) Log.i(TAG, "开始播放音乐...");
                            RobotMediaPlayer mediaPlayer = new RobotMediaPlayer();
                            mediaPlayer.setOnCompletionListener(this);
                            mediaPlayer.playUrl(url);
                        }
                    } else {
                        if (isDebugLog) Log.i(TAG, "开始播放音乐...");
                        RobotMediaPlayer mediaPlayer = new RobotMediaPlayer();
                        mediaPlayer.setOnCompletionListener(this);
                        mediaPlayer.playUrl(url);
                    }
                }
            } else if (domain.equals("chat") || domain.equals("weather")) {
                if (isDebugLog) Log.e(TAG, "domain 是 chat 或 weather，domain = " + domain);
                String outPut = sdsJsonObj.getString("output");
                if (!TextUtils.isEmpty(outPut)) {
                    CN_PREVIEW = outPut;
//                    initAILocalTTSEngine();//使用本地合成语音播放返回的内容
                    speakTips();
                } else {
                    if (isDebugLog) Log.e(TAG, "获取到的返回语音为空");
                }
            } else {
                if (isDebugLog) Log.e(TAG, "domain 是其他的，domain = " + domain);
                mAiCloudSdsEngine.startWithRecording();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletionListener() {
        //在音乐播放完成时重新开始对话
        if (isDebugLog) Log.i(TAG, "音乐播放完成, 重新开始对话...");
        mAiCloudSdsEngine.startWithRecording();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isInited = false;
        if (mAiAuthEngine != null) {
            mAiAuthEngine.destroy();
            mAiAuthEngine = null;
        }
        if (mAiLocalWakeupDnnEngine != null) {
            mAiLocalWakeupDnnEngine.destroy();
            mAiLocalWakeupDnnEngine = null;
        }
        if (mScreenReceiver != null) {
            unregisterReceiver(mScreenReceiver);
        }
        if (mAiLocalTTSEngine != null) {
            mAiLocalTTSEngine.destroy();
            mAiLocalTTSEngine = null;
        }
        if (isDebugLog) Log.e(TAG, "SpeechService destroied...");
    }
}
