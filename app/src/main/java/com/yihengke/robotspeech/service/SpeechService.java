package com.yihengke.robotspeech.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
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
import com.aispeech.export.engines.AILocalGrammarEngine;
import com.aispeech.export.engines.AILocalTTSEngine;
import com.aispeech.export.engines.AILocalWakeupDnnEngine;
import com.aispeech.export.engines.AIMixASREngine;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIAuthListener;
import com.aispeech.export.listeners.AILocalGrammarListener;
import com.aispeech.export.listeners.AILocalWakeupDnnListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.speech.AIAuthEngine;
import com.yihengke.robotspeech.AppKey;
import com.yihengke.robotspeech.BuildConfig;
import com.yihengke.robotspeech.utils.GrammarHelper;
import com.yihengke.robotspeech.utils.MPOnCompletionListener;
import com.yihengke.robotspeech.utils.NetworkUtil;
import com.yihengke.robotspeech.utils.RobotMediaPlayer;
import com.yihengke.robotspeech.utils.SampleConstants;
import com.yihengke.robotspeech.utils.WriteDataUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Random;

/**
 * Created by Administrator on 2017/8/3.
 */

public class SpeechService extends Service implements MPOnCompletionListener {

    /*1、需要一个service在后台等待

    2、service启动时 检验授权 没有授权则执行授权

    3. 授权后初始化本地唤醒引擎、本地语音合成引擎和云端对话引擎等

    4. 通过唤醒词唤醒，唤醒后首先播放提示语，然后开始云端对话（播放返回的对话内容、播放音乐、控制机器人运动等）

    5. 如果超过设定的时间，停止云端对话引擎，进入休眠（需要唤醒词重新唤醒 开始对话和控制）

    6. 在机器人息屏后，开始等待唤醒，唤醒后点亮屏幕，开始云端对话（再执行上面的流程）*/

    private String TAG = "SpeechService";
    private boolean isDebugLog = BuildConfig.DEBUG_LOG;
    private Context mContext;

    private static int MOTOR_LEFT = 0;
    private static int MOTOR_RIGHT = 1;
    private static int MOTOR_FORWARD = 2;
    private static int MOTOR_BACK = 3;
    private static int MOTOR_STOP = 4;

    /**
     * 要读出的话
     */
    private String CN_PREVIEW = "";
    private String HELP_TIP = "有什么我可以帮您？";
    private String[] SDS_ERRO_TIP = new String[]{"你没有事，我就去休息了", "网络异常，请检查网络"};
    private String MP_COMPLET = "播放完成";
    private String GO_TO_SLEEP = "我要去休息了";

    private String[] HEAD_TIPS = new String[]{"不要摸我的头", "有什么事么", "好讨厌"};
    private String[] HAND_TIPS = new String[]{"你要跟我握手么", "你好啊", "有什么事么"};

    private String HEAD_TOUCH_ACTION = "com.yinghengke.headtouch";
    private String HAND_TOUCH_ACTION = "com.yinghengke.handtouch";

    private MyHandler mHandler;
    private boolean isInited = false;
    private boolean isAuthed = false;
    private boolean isScreenOFF = false;
    private boolean isUsedMediaPlayer = false;
    private boolean isGoSleeping = false;
    private RobotReceiver mRobotReceiver;
    private PowerManager.WakeLock wakeLock = null;

    private AIAuthEngine mAiAuthEngine;
    private AILocalWakeupDnnEngine mAiLocalWakeupDnnEngine;
    private AILocalTTSEngine mAiLocalTTSEngine;
    private AIMixASREngine mAiMixASREngine;
    private AILocalGrammarEngine mAiLocalGrammarEngine;

    private RobotMediaPlayer mRobotMediaPlayer;
    private boolean isMpOpause = false;
    private boolean isFromWake = false;

    private static final String SP_NAME = "SpeechService";
    private static final String SP_AUTHED_KEY = "sp_authed_key";

    @Override
    public void onCreate() {
        super.onCreate();
        if (isDebugLog) Log.e(TAG, "service created ...");
        mContext = this;
        initReceiver();
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

        return START_STICKY;
    }

    private void initReceiver() {
        mRobotReceiver = new RobotReceiver();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mFilter.addAction(Intent.ACTION_SCREEN_ON);
        mFilter.addAction(HEAD_TOUCH_ACTION);
        mFilter.addAction(HAND_TOUCH_ACTION);

        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);//
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        registerReceiver(mRobotReceiver, mFilter);
        Log.e(TAG, "registerReceiver......");
    }

    class RobotReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                if (isDebugLog) Log.e(TAG, "监听到屏幕关闭...");
                isScreenOFF = true;
                if (isAuthed) {
                    if (isDebugLog) Log.e(TAG, "已经获取了授权（息屏后监测判断）,等待唤醒...");
                    mAiLocalTTSEngine.stop();//暂停播放语音和云端对话
                    mAiMixASREngine.stopRecording();
//                    mAiLocalWakeupDnnEngine.start();//息屏后判断如果已经获取了授权，就开始等待唤醒
                }

            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (isDebugLog) Log.e(TAG, "监听到屏幕点亮,释放wakelock...");
                isScreenOFF = false;
                releaseWakeLock();
            } else if (action.equals(HEAD_TOUCH_ACTION)) {
                if (isDebugLog) Log.e(TAG, "监听到触摸机器人 头 的广播");
                Random mRandom = new Random();
                int index = mRandom.nextInt(HEAD_TIPS.length);
                CN_PREVIEW = HEAD_TIPS[index];
                if (isAuthed) {
                    speakTips();
                }
            } else if (action.equals(HAND_TOUCH_ACTION)) {
                if (isDebugLog) Log.e(TAG, "监听到触摸机器人 手 的广播");
                Random mRandom = new Random();
                int index = mRandom.nextInt(HAND_TIPS.length);
                CN_PREVIEW = HAND_TIPS[index];
                if (isAuthed) {
                    speakTips();
                }
            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_ENABLED) {// 如果开启

                    if (isDebugLog) Log.e(TAG, "wifi打开了");
                } else if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    if (isDebugLog) Log.e(TAG, "wifi关闭了");
                }
            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                ConnectivityManager mConnectivityManager = (ConnectivityManager) context.
                        getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWiFiNetworkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                // 只有网络连接成功时，条件才成立
                if (mWiFiNetworkInfo.getState() != null && NetworkInfo.State.CONNECTED == mWiFiNetworkInfo.getState()) {
                    if (isInited) {
                        if (isAuthed) {
                            if (isDebugLog)
                                Log.e(TAG, "WIFI connected susccess... isInited == true, isAuthed == true");
                        } else {
                            if (isDebugLog)
                                Log.e(TAG, "WIFI connected susccess... isInited == true, isAuthed == false");
                            init();
                        }
                    } else {
                        if (isDebugLog)
                            Log.e(TAG, "WIFI connected susccess... isInited == false");
                        init();
                    }

                } else if (NetworkInfo.State.DISCONNECTED == mWiFiNetworkInfo.getState()) {
                    if (isDebugLog) Log.e(TAG, "wifi断开了");
                }
            }
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    if (isDebugLog) Log.e(TAG, "超时没有接收到语音，进入休眠,等待唤醒...");
                    isGoSleeping = true;
                    CN_PREVIEW = GO_TO_SLEEP;
                    if (isAuthed) {
                        speakTips();
                        mAiLocalTTSEngine.stop();//暂停播放语音和云端对话
                        mAiMixASREngine.stopRecording();
                    }
                    break;
                case 1:
                    int callback = WriteDataUtils.native_ear_light_control(0, MOTOR_STOP, 0);
                    if (isDebugLog) Log.e(TAG, "Handler 检测运动超时，MOTOR_STOP 的返回值是: " + callback);
                    if (isAuthed) {
                        mAiMixASREngine.start();
                    }
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

        mHandler = new MyHandler();

        boolean isAuthedOnceTime = getSpAuthData();
        if (!isAuthedOnceTime) {
            if (!NetworkUtil.isWifiConnected(SpeechService.this)) {
                if (isDebugLog) Log.e(TAG, "WiFi网络没有连接,没有注册过");
            } else {
                if (isDebugLog) Log.e(TAG, "WiFi网络连接正常,第一次启动，开始注册");

                isInited = true;
                mAiAuthEngine = AIAuthEngine.getInstance(getApplicationContext());
                //设置自定义路径，请将相关文件预先放到该目录下
                //mEngine.setResStoragePath("/system/vender/aispeech");
                try {
                    mAiAuthEngine.init(AppKey.APPKEY, AppKey.SECRETKEY, "");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }// TODO 换成您的APPKEY和SECRETKEY

                mAiAuthEngine.setOnAuthListener(new RobotAIAuthListener());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDebugLog) Log.e(TAG, "开始执行授权...");
                        mAiAuthEngine.doAuth();
                    }
                }).start();
            }
        } else {
            if (isDebugLog) Log.e(TAG, "此设备已经注册过了");
            isInited = true;
            isAuthed = true;
            initWakeupDnnEngine();
            initAILocalTTSEngine();
//            initAiLocalGrammarEngine();
            initAiMixASREngine();//第一次编译资源完成不再需要编译
        }
    }

    /**
     * 获取是否注册成功
     *
     * @return
     */
    private boolean getSpAuthData() {
        SharedPreferences mPreferences = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        return mPreferences.getBoolean(SP_AUTHED_KEY, false);
    }

    /**
     * 注册成功后存储成功结果
     */
    private void setSpAuthData() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(SP_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(SP_AUTHED_KEY, true).commit();
    }

    /**
     * 授权监听
     */
    class RobotAIAuthListener implements AIAuthListener {
        @Override
        public void onAuthSuccess() {
            if (isDebugLog) Log.e(TAG, "授权成功...初始化三个引擎");
            isAuthed = true;
            setSpAuthData();

            initWakeupDnnEngine();
            initAILocalTTSEngine();
            initAiLocalGrammarEngine();
//            initAiMixASREngine();
        }

        @Override
        public void onAuthFailed(String s) {
            if (isDebugLog) Log.e(TAG, "授权失败...");
        }
    }

    /**
     * 初始化唤醒引擎
     */
    private void initWakeupDnnEngine() {
        if (isDebugLog) Log.e(TAG, "initWakeupDnnEngine 开始初始化唤醒引擎");
        mAiLocalWakeupDnnEngine = AILocalWakeupDnnEngine.createInstance();
        mAiLocalWakeupDnnEngine.setResBin(SampleConstants.res_wake);
        mAiLocalWakeupDnnEngine.init(this, new RobotAILocalWakeupDnnListener(), AppKey.APPKEY, AppKey.SECRETKEY);
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
                if (isDebugLog) Log.e(TAG, "唤醒引擎初始化成功...开始等待唤醒...");
                mAiLocalWakeupDnnEngine.start();
            } else {
                if (isDebugLog) Log.e(TAG, "唤醒引擎初始化 失败...");
//                mAiLocalWakeupDnnEngine.init(mContext, new RobotAILocalWakeupDnnListener(), AppKey.APPKEY, AppKey.SECRETKEY);
            }
        }

        @Override
        public void onError(AIError aiError) {
            if (isDebugLog) Log.e(TAG, "唤醒回调显示失败...");
//            mAiLocalWakeupDnnEngine.start();
        }

        @Override
        public void onWakeup(String s, double v, String s1) {
            if (isDebugLog) Log.e(TAG, "唤醒成功...");
            if (isScreenOFF) {
                //点亮屏幕
                acquireWakeLock();
            }
            isGoSleeping = false;
            isFromWake = true;
            if (isUsedMediaPlayer && mRobotMediaPlayer != null) {
                isMpOpause = true;
                mRobotMediaPlayer.pause();
            }
            //播放提示语
            CN_PREVIEW = HELP_TIP;
            speakTips();
        }

        @Override
        public void onRmsChanged(float v) {
            if (isDebugLog) Log.i(TAG, "唤醒引擎 onRmsChanged rmsDb = " + v);

        }

        @Override
        public void onRecorderReleased() {
            if (isDebugLog) Log.e(TAG, "唤醒引擎 onRecorderReleased...");

        }

        @Override
        public void onReadyForSpeech() {
            if (isDebugLog) Log.e(TAG, "唤醒引擎 可以开始说话了...");

        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            if (isDebugLog) Log.i(TAG, "唤醒引擎 onBufferReceived bytes...");

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
        if (isDebugLog) Log.e(TAG, "initAILocalTTSEngine 开始初始化本地语音合成引擎");
        mAiLocalTTSEngine = AILocalTTSEngine.createInstance();//创建实例
        mAiLocalTTSEngine.setResource(SampleConstants.tts_res);
        mAiLocalTTSEngine.setDictDbName(SampleConstants.tts_dict);
        mAiLocalTTSEngine.init(this, new RobotAITTSListener(), AppKey.APPKEY, AppKey.SECRETKEY);//初始化合成引擎
        mAiLocalTTSEngine.setSpeechRate(0.90f);//设置语速
    }

    /**
     * 使用本地合成语音播放提示语
     */
    private void speakTips() {
        if (mAiLocalTTSEngine != null) {
            if (isDebugLog) Log.e(TAG, "开始播放提示语...");
//            mAiLocalTTSEngine.setSavePath(Environment.getExternalStorageDirectory() + "/linzhilin/"
//                    + System.currentTimeMillis() + ".wav");
            mAiLocalTTSEngine.speak(CN_PREVIEW, "1024");
        }
    }

    /**
     * 本地合成语音引擎监听
     */
    class RobotAITTSListener implements AITTSListener {
        @Override
        public void onInit(int status) {
            if (isDebugLog) Log.e(TAG, "RobotAITTSListener初始化完成，返回值：" + status);
            if (status == AIConstant.OPT_SUCCESS) {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener初始化成功...");
//                speakTips();
//                initAiLocalGrammarEngine();
            } else {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener初始化失败...");
//                mAiLocalTTSEngine.init(mContext, new RobotAITTSListener(), AppKey.APPKEY, AppKey.SECRETKEY);
            }
        }

        @Override
        public void onError(String s, AIError aiError) {
            if (isDebugLog) Log.e(TAG, "RobotAITTSListener onError...");
            mAiMixASREngine.start();
        }

        @Override
        public void onReady(String s) {
            if (isDebugLog) Log.e(TAG, "RobotAITTSListener onReady...");
        }

        @Override
        public void onCompletion(String s) {
            if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion = " + s);
            if (isUsedMediaPlayer) {
                if (isDebugLog)
                    Log.e(TAG, "RobotAITTSListener onCompletion isUsedMediaPlayer = true");
                if (isFromWake) {
                    if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion isFromWake = true");
                    isFromWake = false;
                    mAiMixASREngine.start();
                }
            } else if (isGoSleeping) {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion isGoSleeping = true");
            } else if (isScreenOFF) {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion isScreenOFF = true");
            } else {
                if (isDebugLog)
                    Log.e(TAG, "RobotAITTSListener onCompletion mAiMixASREngine.start()");
                mAiMixASREngine.start();
            }
        }

        @Override
        public void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {
            if (isDebugLog)
                Log.i(TAG, "RobotAITTSListener onProgress 当前:" + currentTime + "ms, 总计:" + totalTime + "ms," +
                        " 可信度:" + isRefTextTTSFinished);
        }
    }

    /**
     * 初始化编译引擎
     */
    private void initAiLocalGrammarEngine() {
        if (isDebugLog) Log.e(TAG, "initAiLocalGrammarEngine 开始初始化资源编译引擎");
        mAiLocalGrammarEngine = AILocalGrammarEngine.createInstance();
        //设置自定义路径，请将相关文件预先放到该目录下
        //mGrammarEngine.setResStoragePath("/system/vender/aispeech");
        mAiLocalGrammarEngine.setResFileName(SampleConstants.ebnfc_res);
        mAiLocalGrammarEngine.init(this, new RobotAILocalGrammarListener(), AppKey.APPKEY, AppKey.SECRETKEY);
    }

    public class RobotAILocalGrammarListener implements AILocalGrammarListener {
        @Override
        public void onInit(int status) {
            if (isDebugLog) Log.e(TAG, "RobotAILocalGrammarListener onInit status = " + status);
            if (status == AIConstant.OPT_SUCCESS) {
                if (isDebugLog) Log.e(TAG, "mAiLocalGrammarEngine 本地资源编译引擎初始化成功");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDebugLog) Log.e(TAG, "开始编译资源 startResGen");
                        startResGen();//开始编译
                    }
                }).start();
            } else {
                if (isDebugLog) Log.e(TAG, "mAiLocalGrammarEngine 本地资源编译引擎初始化 失败");
            }
        }

        @Override
        public void onError(AIError aiError) {
            if (isDebugLog)
                Log.e(TAG, "资源生成发生错误 onError = " + aiError.toString());
        }

        @Override
        public void onUpdateCompleted(String recordId, String path) {
            if (isDebugLog) Log.e(TAG, "资源生成/更新成功\npath=" + path + "\n重新加载识别引擎...");
            initAiMixASREngine();
        }
    }

    /**
     * 开始生成识别资源
     */
    private void startResGen() {
        // 生成ebnf语法
        GrammarHelper gh = new GrammarHelper(this);
        String appString = gh.getApps();
        String ebnf = gh.importAssets("", appString, "asr.xbnf");
        if (isDebugLog) Log.e(TAG, ebnf);
//        System.out.println(ebnf);
        // 设置ebnf语法
        mAiLocalGrammarEngine.setEbnf(ebnf);
        // 启动语法编译引擎，更新资源
        mAiLocalGrammarEngine.update();
    }

    /**
     * 初始化混合识别引擎
     */
    private void initAiMixASREngine() {
        if (isDebugLog) Log.e(TAG, "initmAiMixASREngine 开始初始化混合识别引擎");
        mAiMixASREngine = AIMixASREngine.createInstance();
//      mAiMixASREngine.setResStoragePath("/system/vender/aispeech");//设置自定义路径，请将相关文件预先放到该目录下
        mAiMixASREngine.setResBin(SampleConstants.ebnfr_res);
        mAiMixASREngine.setNetBin(AILocalGrammarEngine.OUTPUT_NAME, true);

        mAiMixASREngine.setVadResource(SampleConstants.vad_res);
        mAiMixASREngine.setServer(SampleConstants.server_production);//产品环境 //灰度环境
        mAiMixASREngine.setUseXbnfRec(true);
        mAiMixASREngine.setRes(SampleConstants.res_robot);
        mAiMixASREngine.setUsePinyin(true);
        mAiMixASREngine.setUseForceout(false);
        mAiMixASREngine.setAthThreshold(0.6f);
        mAiMixASREngine.setIsRelyOnLocalConf(true);
        mAiMixASREngine.setIsPreferCloud(false);
        mAiMixASREngine.setLocalBetterDomains(new String[]{"airobot"});
//        mAiMixASREngine.setCloudNotGoodAtDomains(new String[]{"phonecall","weixin"});
//        mAiMixASREngine.putCloudLocalDomainMap("weixin", "wechat");
//        mAiMixASREngine.putCloudLocalDomainMap("phonecall", "phone");
        mAiMixASREngine.setWaitCloudTimeout(2000);
        mAiMixASREngine.setPauseTime(700);
        mAiMixASREngine.setUseConf(true);
//        mAiMixASREngine.setVersion("1.0.4"); //设置资源的版本号
        mAiMixASREngine.setNoSpeechTimeOut(15 * 1000);
        mAiMixASREngine.setMaxSpeechTimeS(20);
//        mAiMixASREngine.setDeviceId(Util.getIMEI(this));
        mAiMixASREngine.setCloudVadEnable(false);
//        if(this.getExternalCacheDir() != null) {
//        	mAiMixASREngine.setUploadEnable(true);//设置上传音频使能
//        	mAiMixASREngine.setTmpDir(this.getExternalCacheDir().getAbsolutePath());//设置上传的音频保存在本地的目录
//        }
        mAiMixASREngine.init(this, new RobotAIASRListener(), AppKey.APPKEY, AppKey.SECRETKEY);
        mAiMixASREngine.setUseCloud(true);//该方法必须在init之后
//        mAiMixASREngine.setUserId("AISPEECH"); //填公司名字
        mAiMixASREngine.setCoreType("cn.sds"); //cn.sds为云端对话服务，cn.dlg.ita为云端语义服务，
//          默认为云端语义,想要访问对话服务时，才设置为cn.sds，否则不用设置
    }

    /**
     * 混合识别引擎回调接口
     */
    public class RobotAIASRListener implements AIASRListener {

        @Override
        public void onInit(int staus) {
            if (isDebugLog) Log.e(TAG, "mAiMixASREngine onIinit staus = " + staus);
            if (staus == AIConstant.OPT_SUCCESS) {
                if (isDebugLog) Log.e(TAG, "mAiMixASREngine 初始化成功");
//                mAiMixASREngine.start();
            } else {
                if (isDebugLog) Log.e(TAG, "mAiMixASREngine 初始化失败");
//                mAiMixASREngine.init(new RobotAIASRListener());
            }
        }

        @Override
        public void onError(AIError aiError) {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener onError = " + aiError.toString());

            int errorId = aiError.getErrId();
            if (errorId == 70603) {//"error":"Websocket connect timeout","errId"
                CN_PREVIEW = SDS_ERRO_TIP[1];
            } else if (errorId == 70610) {//"error":"Network abnormal.connection closed"
                CN_PREVIEW = SDS_ERRO_TIP[1];
            } else if (errorId == 70910) {//"error":"等待云端结果超时"
                CN_PREVIEW = SDS_ERRO_TIP[1];
            } else if (errorId == 70904) {//"error":"没有检测到语音"
                CN_PREVIEW = SDS_ERRO_TIP[0];
                mAiMixASREngine.stopRecording();
                isGoSleeping = true;
            } else {
                CN_PREVIEW = HELP_TIP;
            }
            speakTips();
        }

        @Override
        public void onResults(AIResult aiResult) {
            if (aiResult.isLast()) {
                if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                    if (isDebugLog)
                        Log.e(TAG, "result JSON = " + aiResult.getResultObject().toString());
                    parseData(aiResult);
                }
            }
        }

        @Override
        public void onRmsChanged(float v) {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener onRmsChanged = " + v);
        }

        @Override
        public void onReadyForSpeech() {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener 可以开始说话了");
        }

        @Override
        public void onBeginningOfSpeech() {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener 检测到说话了");
        }

        @Override
        public void onEndOfSpeech() {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener 检测到语音停止，开始识别");
        }

        @Override
        public void onRecorderReleased() {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener onRecorderReleased...");
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            if (isDebugLog) Log.i(TAG, "RobotAIASRListener onBufferReceived...");
        }

        @Override
        public void onNotOneShot() {
            if (isDebugLog) Log.e(TAG, "RobotAIASRListener onNotOneShot...");
        }
    }

    /**
     * 解析网络返回的数据
     *
     * @param aiResult
     */
    private void parseData(AIResult aiResult) {
        JSONResultParser parser = new JSONResultParser(aiResult.getResultObject().toString());
        JSONObject result = parser.getResult();
        try {
            JSONObject sdsJsonObj = result.optJSONObject("sds");
            String domain = sdsJsonObj.optString("domain");
            if (TextUtils.isEmpty(domain)) {
                if (isDebugLog) Log.e(TAG, "解析json， domain 是空...");
                mAiMixASREngine.start();
                return;
            }
            if (domain.equals("netfm") || domain.equals("story") || domain.equals("music") || domain.equals("poetry")) {
                JSONObject dataJsonObj = sdsJsonObj.optJSONObject("data");
                if (dataJsonObj == null) {
                    if (isDebugLog) Log.e(TAG, "domain = netfm，data == null");
                    mAiMixASREngine.start();
                    return;
                }
                String output = sdsJsonObj.optString("output");
                JSONArray dbdataJsArray = dataJsonObj.optJSONArray("dbdata");
                if (dbdataJsArray != null && dbdataJsArray.length() > 0) {
                    if (!TextUtils.isEmpty(output)) {
                        isUsedMediaPlayer = true;
                        isFromWake = false;
                        isMpOpause = false;
                        CN_PREVIEW = output;
                        speakTips();
                    }

                    JSONObject netfmData = dbdataJsArray.optJSONObject(0);
                    String url = netfmData.optString("playUrl32");
                    if (TextUtils.isEmpty(url)) {
                        if (isDebugLog) Log.e(TAG, "playUrl32 isEmpty...");
                        url = netfmData.optString("playUrl64");
                        if (TextUtils.isEmpty(url)) {
                            if (isDebugLog) Log.e(TAG, "playUrl64 isEmpty...");
                            url = netfmData.optString("url");
                            if (TextUtils.isEmpty(url)) {
                                if (isDebugLog) Log.e(TAG, "获取 url isEmpty...");
                                //没有音乐文件
                                isUsedMediaPlayer = false;
                                isFromWake = false;
                                mAiMixASREngine.start();
                            } else {
                                if (isDebugLog) Log.e(TAG, "开始播放音乐...");
                                isUsedMediaPlayer = true;
                                isFromWake = false;
                                mRobotMediaPlayer = new RobotMediaPlayer();
                                mRobotMediaPlayer.setOnCompletionListener(this);
                                mRobotMediaPlayer.playUrl(url);
                                mAiMixASREngine.stopRecording();
                            }
                        } else {
                            if (isDebugLog) Log.e(TAG, "开始播放音乐...");
                            isUsedMediaPlayer = true;
                            isFromWake = false;
                            mRobotMediaPlayer = new RobotMediaPlayer();
                            mRobotMediaPlayer.setOnCompletionListener(this);
                            mRobotMediaPlayer.playUrl(url);
                            mAiMixASREngine.stopRecording();
                        }
                    } else {
                        if (isDebugLog) Log.e(TAG, "开始播放音乐...");
                        isUsedMediaPlayer = true;
                        isFromWake = false;
                        mRobotMediaPlayer = new RobotMediaPlayer();
                        mRobotMediaPlayer.setOnCompletionListener(this);
                        mRobotMediaPlayer.playUrl(url);
                        mAiMixASREngine.stopRecording();
                    }
                } else {
                    isUsedMediaPlayer = false;
                    isFromWake = false;
                    mAiMixASREngine.start();
                }
            } else if (domain.equals("chat") || domain.equals("weather") || domain.equals("calendar") || domain.equals("calculator")) {
                if (isDebugLog) Log.e(TAG, "domain 是 chat 或 weather，domain = " + domain);
                String outPut = sdsJsonObj.optString("output");
                String input = result.optString("input");

                if (input.contains("向前") || input.contains("向后") || input.contains("往前") || input.contains("往后")
                        || input.contains("向左") || input.contains("向右") || input.contains("往左") || input.contains("往右")
                        || input.contains("前进") || input.contains("后退")) {
                    if (isDebugLog)
                        Log.e(TAG, "domain.equals(\"chat\"), input.contains 方向\n" + input);
                    controlRobot(input);
                } else {
                    if (!TextUtils.isEmpty(outPut)) {
                        //使用本地合成语音播放返回的内容
                        CN_PREVIEW = outPut;
                        speakTips();
                    } else {
                        if (isDebugLog) Log.e(TAG, "获取到的返回语音为空");
                        mAiMixASREngine.start();
                    }
                }
            } else if (domain.equals("motionctrl")) {//运动控制
                if (isDebugLog) Log.e(TAG, "domain 运动控制，domain = " + domain);
//                mAiMixASREngine.start();
                controlRobot(result.optString("input"));
            } else if (domain.equals("command")) {//中控
                if (isDebugLog) Log.e(TAG, "domain 中控，domain = " + domain);
                controlRobot(result.optString("input"));
//                mAiMixASREngine.start();
            } else {
                if (isDebugLog) Log.e(TAG, "domain 是其他的，domain = " + domain);
                mAiMixASREngine.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletionListener() {
        //在音乐播放完成时重新开始对话
        if (isDebugLog) Log.e(TAG, "音乐播放完成, 重新开始对话...");
        isUsedMediaPlayer = false;
        isMpOpause = false;
        CN_PREVIEW = MP_COMPLET;
        speakTips();
//        mAiMixASREngine.start();
    }

    private void controlRobot(String ctrl_str) {
        if (ctrl_str.contains("左")) {
            int callback = WriteDataUtils.native_ear_light_control(0, MOTOR_LEFT, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_LEFT 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("右")) {
            int callback = WriteDataUtils.native_ear_light_control(0, MOTOR_RIGHT, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_RIGHT 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("前")) {
            int callback = WriteDataUtils.native_ear_light_control(0, MOTOR_FORWARD, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_FORWARD 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("后")) {
            int callback = WriteDataUtils.native_ear_light_control(0, MOTOR_BACK, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_BACK 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("停")) {
            if (isUsedMediaPlayer && mRobotMediaPlayer != null) {
                if (isDebugLog) Log.e(TAG, "ctrl_str.contains(\"停\") 停止播放");
                mRobotMediaPlayer.stop();
                isUsedMediaPlayer = false;
                isMpOpause = false;
                mAiMixASREngine.start();
            } else {
                int callback = WriteDataUtils.native_ear_light_control(0, MOTOR_STOP, 0);
                if (isDebugLog) Log.e(TAG, "MOTOR_STOP 的返回值是: " + callback);

                mHandler.removeMessages(1);
                mHandler.sendEmptyMessage(1);
            }
        } else if (ctrl_str.contains("继续")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续");
            if (isUsedMediaPlayer && mRobotMediaPlayer != null && isMpOpause) {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续 播放");
                isMpOpause = false;
                mRobotMediaPlayer.play();
            } else {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续 else 开始对话");
                mAiMixASREngine.start();
            }
        } else {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains else .. = " + ctrl_str);
            mAiMixASREngine.start();
        }
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
        if (mRobotReceiver != null) {
            unregisterReceiver(mRobotReceiver);
        }
        if (mAiLocalTTSEngine != null) {
            mAiLocalTTSEngine.destroy();
            mAiLocalTTSEngine = null;
        }
        if (mAiLocalGrammarEngine != null) {
            mAiLocalGrammarEngine.destroy();
            mAiLocalGrammarEngine = null;
        }
        if (mAiMixASREngine != null) {
            mAiMixASREngine.destroy();
            mAiMixASREngine = null;
        }
        if (isDebugLog) Log.e(TAG, "SpeechService destroied...");
    }
}
