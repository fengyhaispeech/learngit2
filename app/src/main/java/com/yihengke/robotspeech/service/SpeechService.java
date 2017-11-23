package com.yihengke.robotspeech.service;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
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
import android.view.WindowManager;

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
import com.yihengke.robotspeech.activity.MainActivity;
import com.yihengke.robotspeech.activity.SdsActivity;
import com.yihengke.robotspeech.utils.GrammarHelper;
import com.yihengke.robotspeech.utils.MPOnCompletionListener;
import com.yihengke.robotspeech.utils.MyConstants;
import com.yihengke.robotspeech.utils.NetworkUtil;
import com.yihengke.robotspeech.utils.RobotMediaPlayer;
import com.yihengke.robotspeech.utils.SampleConstants;
import com.yihengke.robotspeech.utils.Util;
import com.yihengke.robotspeech.utils.WriteDataUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/8/3.
 *
 * @author Administrator
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

    /**
     * 要读出的话
     */
    private String CN_PREVIEW = "";

    private MyHandler mHandler;
    private boolean isInited = false;
    private boolean isAuthed = false;
    private boolean isScreenOFF = false;
    private boolean isUsedMediaPlayer = false;
    private boolean isGoSleeping = true;
    private RobotReceiver mRobotReceiver;
    private PowerManager.WakeLock wakeLock = null;

    private AIAuthEngine mAiAuthEngine;
    private AILocalWakeupDnnEngine mAiLocalWakeupDnnEngine;
    private AILocalTTSEngine mAiLocalTTSEngine;
    private AIMixASREngine mAiMixASREngine;
    private AILocalGrammarEngine mAiLocalGrammarEngine;

    private RobotMediaPlayer mRobotMediaPlayer;
    private boolean isMpOnpause = false;
    private boolean isFromWake = false;
    private long mediaPauseTime;

    private boolean isMainDancing = false;
    private boolean isMainOnPause = false;

    private AudioManager audioManager;
    private int currentVolume, maxVolume;

    private long lastTime;

    private int asrTimes;
    private boolean isSystemSleeped;
    private Timer speechTimer;
    private boolean isStoped;

    private String dlgDomain, contextId;

    @Override
    public void onCreate() {
        super.onCreate();
        if (isDebugLog) Log.e(TAG, "service created ...");
        mContext = this;
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
        if (speechTimer == null) {
            speechTimer = new Timer();
            speechTimer.scheduleAtFixedRate(new SpeechTiemrTask(), 0, 5 * 1000);
        }
        return START_STICKY;
    }

    private void initReceiver() {
        mRobotReceiver = new RobotReceiver();
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mFilter.addAction(Intent.ACTION_SCREEN_ON);
        mFilter.addAction(MyConstants.HEAD_TOUCH_ACTION);
        mFilter.addAction(MyConstants.HAND_TOUCH_ACTION);
        mFilter.addAction(MyConstants.ACTION_DANCE_STARTED);
        mFilter.addAction(MyConstants.ACTION_DANCE_STOPED);

        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);//
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);//
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

        registerReceiver(mRobotReceiver, mFilter);
        if (isDebugLog) Log.e(TAG, "registerReceiver......");
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
                    if (isStoped) {
                        if (isDebugLog) Log.e(TAG, "监听到屏幕熄灭，但是isStoped == true");
                        mAiLocalTTSEngine.stop();//暂停播放语音和云端对话
                        mAiMixASREngine.stopRecording();
                    }
                    mHandler.removeMessages(3);
                    mHandler.sendEmptyMessageDelayed(3, 10 * 60 * 1000);
                    dlgDomain = "";
                    contextId = "";
                }
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                if (isDebugLog) Log.e(TAG, "监听到屏幕点亮...");
                isScreenOFF = false;
//                releaseWakeLock();
                mHandler.removeMessages(3);
                if (!Util.isBackground(mContext, MyConstants.qqHdPackageName)
                        || !Util.isBackground(mContext, MyConstants.mainApkCameraPackage)) {
                    String temp = Util.getForeActivity(mContext);
                    if (!TextUtils.isEmpty(temp)) {
                        if (temp.contains(MyConstants.qqHdPartPackage) ||
                                temp.contains(MyConstants.mainApkCameraPackage)) {
                            if (isDebugLog) Log.e(TAG, "系统之前是深度休眠, 但是相机或QQ在前台，不需要初始化引擎");
                            return;
                        }
                    }
                }
                if (Util.isForeground(mContext, MyConstants.qqHdAvActivity)) {
                    if (isDebugLog) Log.e(TAG, "系统之前是深度休眠, 但是QQ通话在前台，不需要初始化引擎");
                    return;
                }
                if (isSystemSleeped) {
                    if (isAuthed) {
                        if (isDebugLog) Log.e(TAG, "系统之前是深度休眠，现在要重新初始化引擎");
                        initWakeupDnnEngine();
                        initAiMixASREngine();
                    }
                }
                isSystemSleeped = false;
            } else if (action.equals(MyConstants.HEAD_TOUCH_ACTION)) {
                if (isDebugLog) Log.e(TAG, "监听到触摸机器人 头 的广播");
                Random mRandom = new Random();
                int index = mRandom.nextInt(MyConstants.HEAD_TIPS.length);
                CN_PREVIEW = MyConstants.HEAD_TIPS[index];
                sendBiaoQingSign(MyConstants.shyAnim);
                if (isAuthed) {
                    if (isUsedMediaPlayer && mRobotMediaPlayer != null) {
                        mRobotMediaPlayer.pause();
                        isMpOnpause = true;
                        mediaPauseTime = System.currentTimeMillis();
                    }
                    if (isMainDancing && !isMainOnPause) {
                        sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_PAUSED));
                        isMainOnPause = true;
                    }
                    long currentTime = System.currentTimeMillis() / 1000;
                    if ((currentTime - lastTime) <= 1) {
                        lastTime = currentTime;
                    } else {
                        if (Util.isForeground(mContext, MyConstants.mainApkKalaokPlayActivity)
                                || Util.isForeground(mContext, MyConstants.mainApkLocalVedioActivity)) {
                            sendBroadcast(new Intent(MyConstants.ACTION_LOCAL_VEDIO_PAUSE));
                        }
                        asrTimes = 1;
                        isGoSleeping = false;
                        mAiLocalTTSEngine.stop();
                        speakTips();
                        lastTime = currentTime;
                    }
                }
            } else if (action.equals(MyConstants.HAND_TOUCH_ACTION)) {
                if (isDebugLog) Log.e(TAG, "监听到触摸机器人 手 的广播");
                Random mRandom = new Random();
                int index = mRandom.nextInt(MyConstants.HAND_TIPS.length);
                CN_PREVIEW = MyConstants.HAND_TIPS[index];
                if (index == 3) {
                    sendBiaoQingSign(MyConstants.cryAnim);
                } else {
                    sendBiaoQingSign(MyConstants.happyAnim);
                }
                if (isAuthed) {
                    if (isUsedMediaPlayer && mRobotMediaPlayer != null) {
                        mRobotMediaPlayer.pause();
                        isMpOnpause = true;
                        mediaPauseTime = System.currentTimeMillis();
                    }
                    if (isMainDancing && !isMainOnPause) {
                        sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_PAUSED));
                        isMainOnPause = true;
                    }
                    long currentTime = System.currentTimeMillis() / 1000;
                    if ((currentTime - lastTime) <= 1) {
                        lastTime = currentTime;
                    } else {
                        if (Util.isForeground(mContext, MyConstants.mainApkKalaokPlayActivity)
                                || Util.isForeground(mContext, MyConstants.mainApkLocalVedioActivity)) {
                            sendBroadcast(new Intent(MyConstants.ACTION_LOCAL_VEDIO_PAUSE));
                        }
                        asrTimes = 1;
                        isGoSleeping = false;
                        mAiLocalTTSEngine.stop();
                        speakTips();
                        lastTime = currentTime;
                    }
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
            } else if (action.equals(MyConstants.ACTION_DANCE_STARTED)) {
                if (isDebugLog) Log.e(TAG, "接收到主页面 开始 跳舞的广播");
                isMainDancing = true;
                isMainOnPause = false;
            } else if (action.equals(MyConstants.ACTION_DANCE_STOPED)) {
                if (isDebugLog) Log.e(TAG, "接收到主页面 结束 跳舞的广播");
                isMainDancing = false;
                isMainOnPause = false;
                CN_PREVIEW = "跳舞结束";
                speakTips();
            }
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    int callback = WriteDataUtils.native_ear_light_control(0, MyConstants.MOTOR_STOP, 0);
                    if (isDebugLog) Log.e(TAG, "Handler 检测运动超时，MOTOR_STOP 的返回值是: " + callback);
                    if (isAuthed) {
                        CN_PREVIEW = "请指示";
                        speakTips();
                    }
                    break;
                case 2:
                    //语音对话停止后释放锁
                    if (isDebugLog) Log.e(TAG, "语音对话停止后释放锁...");
                    releaseWakeLock();
                    break;
                case 3:
                    if (isDebugLog) Log.e(TAG, "息屏超过指定时间，销毁引擎使系统进入深度休眠");
                    isSystemSleeped = true;
                    mAiLocalTTSEngine.stop();
//                    mAiLocalTTSEngine.destroy();
                    mAiMixASREngine.stopRecording();
                    mAiMixASREngine.destroy();
                    mAiLocalWakeupDnnEngine.stop();
                    mAiLocalWakeupDnnEngine.destroy();
                    break;
                case 4:
                    /*if (Util.isForeground(mContext, MyConstants.robotMainActivity) && isMainOnPause) {
                        if (isDebugLog) Log.e(TAG, "在跳舞页面问答超出限制次数，并且没有继续语音对话超出10秒，继续跳舞");
                        sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_GO_ON));
                        isMainOnPause = false;
                    } else if (Util.isForeground(mContext, MyConstants.mainApkKalaokPlayActivity)
                            || Util.isForeground(mContext, MyConstants.mainApkLocalVedioActivity)) {
                        if (isDebugLog) Log.e(TAG, "在本地视频或卡拉OK播放页面问答超出限制次数，并且没有继续语音对话超出10秒，继续播放");
                        sendBroadcast(new Intent(MyConstants.ACTION_LOCAL_VEDIO_GO_ON));
                    }*/
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

        if (!NetworkUtil.isWifiConnected(SpeechService.this)) {
            if (isDebugLog) Log.e(TAG, "WiFi网络没有连接,等待网络...");
        } else {
            if (isDebugLog) Log.e(TAG, "WiFi网络连接正常，开始判断是否注册，开始初始化");

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
            if (mAiAuthEngine.isAuthed()) {
                isAuthed = true;

                initWakeupDnnEngine();
                initAILocalTTSEngine();
                //有可能是清除了应用的数据或是调试代码卸载了应用
                if (getSpGrammarInited()) {
                    if (isDebugLog) Log.e(TAG, "本地识别资源已经编译过了，直接初始化混合识别引擎");
                    initAiMixASREngine();//第一次编译资源完成不再需要编译
                } else {
                    if (isDebugLog) Log.e(TAG, "本地识别资源还没编译，开始编译本地识别资源");
                    initAiLocalGrammarEngine();
                }
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDebugLog) Log.e(TAG, "开始执行授权...");
                        mAiAuthEngine.doAuth();
                    }
                }).start();
            }
        }
    }

    /**
     * 获取本地识别资源是否编译完成过
     *
     * @return
     */
    private boolean getSpGrammarInited() {
        SharedPreferences mPreferences = getSharedPreferences(MyConstants.SP_NAME, MODE_PRIVATE);
        return mPreferences.getBoolean(MyConstants.SP_GRAMMAR_INITED_KEY, false);
    }

    /**
     * 本地识别资源编译完成后保存
     */
    private void setSpGrammarInited() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(MyConstants.SP_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(MyConstants.SP_GRAMMAR_INITED_KEY, true).commit();
    }

    /**
     * 授权监听
     */
    class RobotAIAuthListener implements AIAuthListener {
        @Override
        public void onAuthSuccess() {
            if (isDebugLog) Log.e(TAG, "授权成功...初始化三个引擎");
            isAuthed = true;

            initWakeupDnnEngine();
            initAILocalTTSEngine();
            initAiLocalGrammarEngine();
//            initAiMixASREngine();
        }

        @Override
        public void onAuthFailed(String s) {
            if (isDebugLog) Log.e(TAG, "授权失败..." + s);
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
                if (isDebugLog) Log.e(TAG, "唤醒引擎初始化 失败..." + status);
//                mAiLocalWakeupDnnEngine.init(mContext, new RobotAILocalWakeupDnnListener(), AppKey.APPKEY, AppKey.SECRETKEY);
            }
        }

        @Override
        public void onError(AIError aiError) {
            if (isDebugLog) Log.e(TAG, "唤醒回调显示失败..." + aiError.toString());
//            mAiLocalWakeupDnnEngine.start();
        }

        @Override
        public void onWakeup(String s, double v, String s1) {
            if (isDebugLog) Log.e(TAG, "唤醒成功...");
            if (isScreenOFF) {
                //点亮屏幕
                sendBroadcast(new Intent(MyConstants.SPEECH_WAKE_UP_ROBOT_SCREEN));
            }
            isGoSleeping = false;
            isFromWake = true;
            mAiMixASREngine.cancel();
            mAiMixASREngine.stopRecording();
            mAiLocalTTSEngine.stop();
            //播放提示语
            CN_PREVIEW = MyConstants.HELP_TIP;
            speakTips();
            if (isUsedMediaPlayer && mRobotMediaPlayer != null) {
                mRobotMediaPlayer.pause();
                isMpOnpause = true;
                mediaPauseTime = System.currentTimeMillis();
            }
            if (isMainDancing && !isMainOnPause) {
                sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_PAUSED));
                isMainOnPause = true;
            }
            if (!Util.isForeground(mContext, MyConstants.apkVoiceActivity) &&
                    Util.isForeground(mContext, MyConstants.mainApkActivity)) {
                //在主页面打开表情页面
                Intent intent = new Intent(mContext, SdsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else if (Util.isForeground(mContext, MyConstants.mainApkKalaokPlayActivity)
                    || Util.isForeground(mContext, MyConstants.mainApkLocalVedioActivity)) {
                //在播放本地视频或卡拉OK页面，发送暂停命令
                sendBroadcast(new Intent(MyConstants.ACTION_LOCAL_VEDIO_PAUSE));
            } else if (Util.isForeground(mContext, MyConstants.apkVoiceActivity)) {
                //在表情页面发送唤醒命令，切换眨眼动画
//                sendBroadcast(new Intent(MyConstants.ACTION_ROBOT_WAKE_UP));
            }
            asrTimes = 0;
            acquireWakeLock();//实现在对话中不息屏
            mHandler.removeMessages(4);
            mHandler.removeMessages(2);
            mHandler.sendEmptyMessageDelayed(2, 90 * 1000);
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
            if (isDebugLog) Log.e(TAG, "onWakeupEngineStopped...");
            mAiLocalWakeupDnnEngine.start();
        }
    }

    /**
     * 获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
     */
    private void acquireWakeLock() {
        if (null == wakeLock) {
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TAG");
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
            } else {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener初始化失败...");
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
                } else if (isMpOnpause) {
                    if (isDebugLog)
                        Log.e(TAG, "RobotAITTSListener onCompletion isMpOnpause = true");
                    mAiMixASREngine.start();
                    long currentTimes = System.currentTimeMillis();
                    if ((currentTimes - mediaPauseTime) > 30 * 1000) {
                        mRobotMediaPlayer.stop();
                        isUsedMediaPlayer = false;
                        isMpOnpause = false;
                    }
                }
            } else if (isGoSleeping) {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion isGoSleeping = true");
                mHandler.removeMessages(4);
                mHandler.sendEmptyMessageDelayed(4, 10 * 1000);
            } else if (isScreenOFF) {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion isScreenOFF = true");
            } else if (isStoped) {
                if (isDebugLog) Log.e(TAG, "RobotAITTSListener onCompletion isStoped = true");
            } else {
                if (isDebugLog)
                    Log.e(TAG, "RobotAITTSListener onCompletion mAiMixASREngine.start()");
                isNeedStopRecording();
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
            if (isDebugLog) Log.e(TAG, "资源生成发生错误 onError = " + aiError.toString());
        }

        @Override
        public void onUpdateCompleted(String recordId, String path) {
            if (isDebugLog) Log.e(TAG, "资源生成/更新成功\npath=" + path + "\n重新加载识别引擎...");
            initAiMixASREngine();
            setSpGrammarInited();//保存编译完成的信息
        }
    }

    /**
     * 开始生成识别资源
     */
    private void startResGen() {
        // 生成ebnf语法
        GrammarHelper gh = new GrammarHelper(this);
        String appString = gh.getApps();
        String ebnf = gh.importAssets("", "", "asr.xbnf");
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
//        mAiMixASREngine.setUsePinyin(true);
        mAiMixASREngine.setUseForceout(false);
        mAiMixASREngine.setAthThreshold(0.7f);
        mAiMixASREngine.setIsRelyOnLocalConf(true);
        mAiMixASREngine.setIsPreferCloud(false);
        mAiMixASREngine.setLocalBetterDomains(new String[]{"robotctrl"});
//        mAiMixASREngine.setCloudNotGoodAtDomains(new String[]{"phonecall","weixin"});
//        mAiMixASREngine.putCloudLocalDomainMap("weixin", "wechat");
//        mAiMixASREngine.putCloudLocalDomainMap("phonecall", "phone");
        mAiMixASREngine.setCloudNotGoodAtDomains(new String[]{"motionctrl", "command"});
        mAiMixASREngine.putCloudLocalDomainMap("motionctrl", "robotctrl");
        mAiMixASREngine.putCloudLocalDomainMap("command", "robotctrl");
        mAiMixASREngine.setWaitCloudTimeout(2000);
        mAiMixASREngine.setPauseTime(500);
//        mAiMixASREngine.setHttpTransferTimeout(5);
        mAiMixASREngine.setUseConf(true);
//        mAiMixASREngine.setVersion("1.0.4"); //设置资源的版本号
        mAiMixASREngine.setNoSpeechTimeOut(10 * 1000);
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
            //"error":"Websocket connect timeout","errId"
            //"error":"Network abnormal.connection closed"
            //"error":"等待云端结果超时"
            if (errorId == 70603 || errorId == 70610 || errorId == 70910) {
                sendBiaoQingSign(MyConstants.wrongedAnim);
                mAiMixASREngine.cancel();
                mAiLocalTTSEngine.stop();
                if (errorId == 70603) {
                    CN_PREVIEW = MyConstants.SDS_ERRO_TIP[1];
                } else if (errorId == 70610) {
                    CN_PREVIEW = MyConstants.SDS_ERRO_TIP[2];
                } else {
                    CN_PREVIEW = MyConstants.SDS_ERRO_TIP[3];
                }
                speakTips();
            } else if (errorId == 70904) {
                //"error":"没有检测到语音"
                mAiMixASREngine.stopRecording();
                if (isUsedMediaPlayer && isMpOnpause) {
                    if (isDebugLog) Log.e(TAG, "没有检测到声音，开始播放暂停的歌曲");
                    long currentTimes = System.currentTimeMillis();
                    if ((currentTimes - mediaPauseTime) > 30 * 1000) {
                        mRobotMediaPlayer.stop();
                        CN_PREVIEW = MyConstants.SDS_ERRO_TIP[0];
                        isGoSleeping = true;
                        isUsedMediaPlayer = false;
                        isMpOnpause = false;
//                        if (Util.isForeground(mContext, MyConstants.apkVoiceActivity))
//                            sendBroadcast(new Intent(MyConstants.ACTION_ROBOT_GO_TO_SLEEP));
                        speakTips();
                    } else {
                        isMpOnpause = false;
                        mRobotMediaPlayer.play();
                    }
                } else if (Util.isForeground(mContext, MyConstants.robotMainActivity) && isMainOnPause) {
                    if (isDebugLog) Log.e(TAG, "没有检测到声音，开始继续跳舞");
                    sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_GO_ON));
                    isMainOnPause = false;
                } else if (Util.isForeground(mContext, MyConstants.mainApkKalaokPlayActivity)
                        || Util.isForeground(mContext, MyConstants.mainApkLocalVedioActivity)) {
                    if (isDebugLog) Log.e(TAG, "没有检测到声音，开始播放暂停的本地视频或卡拉OK");
                    sendBroadcast(new Intent(MyConstants.ACTION_LOCAL_VEDIO_GO_ON));
                } else {
                    mAiLocalTTSEngine.stop();
                    CN_PREVIEW = MyConstants.SDS_ERRO_TIP[0];
                    isGoSleeping = true;
                    isUsedMediaPlayer = false;
                    isMpOnpause = false;
                    if (Util.isForeground(mContext, MyConstants.apkVoiceActivity)) {
//                        sendBroadcast(new Intent(MyConstants.ACTION_ROBOT_GO_TO_SLEEP));
                        Random mRandom = new Random();
                        int index = mRandom.nextInt(MyConstants.TimeoutNoTouch.length);
                        CN_PREVIEW = MyConstants.TimeoutNoTouch[index];
                        sendBiaoQingSign(MyConstants.wrongedAnim);
                    }
                    speakTips();
                }
                dlgDomain = "";
                contextId = "";
            } else {
                mAiLocalTTSEngine.stop();
                CN_PREVIEW = MyConstants.HELP_TIP;
                speakTips();
            }
            asrTimes++;
        }

        @Override
        public void onResults(AIResult aiResult) {
            if (aiResult.isLast()) {
                if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                    if (isDebugLog)
                        Log.e(TAG, "result JSON = " + aiResult.getResultObject().toString());
                    asrTimes++;
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

        JSONObject sdsJsonObj = result.optJSONObject("sds");

        if (sdsJsonObj == null) {
            boolean isLocalData = localParseResult(result);
            if (!isLocalData) {
                if (isDebugLog) Log.e(TAG, "本地解析没有解析出来，不符合本地数据格式");
                if (!NetworkUtil.isWifiConnected(SpeechService.this)) {
                    CN_PREVIEW = "请连接网络";
                } else {
                    CN_PREVIEW = "我们换个问题聊聊吧";
                    sendBiaoQingSign(MyConstants.confusedAnim);
                }
                speakTips();
            }
        } else {
            boolean isCloudData = cloudParseResult(result);
            if (!isCloudData) {
                if (isDebugLog) Log.e(TAG, "云端数据解析没有解析出来，不符合云端数据格式");
                if (!NetworkUtil.isWifiConnected(SpeechService.this)) {
                    CN_PREVIEW = "请连接网络";
                } else {
                    CN_PREVIEW = "我们换个问题聊聊吧";
                    sendBiaoQingSign(MyConstants.confusedAnim);
                }
                speakTips();
            }
        }
    }

    /**
     * 解析云端返回的数据
     *
     * @param result
     * @return
     */
    private boolean cloudParseResult(JSONObject result) {

        JSONObject sdsJsonObj = result.optJSONObject("sds");
        String domain = sdsJsonObj.optString("domain");

        if (TextUtils.isEmpty(domain)) {
            if (isDebugLog) Log.e(TAG, "云端数据解析json， domain == null...");
            return false;
        }
        contextId = sdsJsonObj.optString("contextId");
        if (!TextUtils.isEmpty(contextId)) {
            if (isDebugLog) Log.e(TAG, "contextId == " + contextId);
            mAiMixASREngine.setContextId(contextId);
        }
        JSONObject semanticsObj = result.optJSONObject("semantics");
        if (semanticsObj != null) {
            JSONObject requestObj = semanticsObj.optJSONObject("request");
            if (requestObj != null) {
                dlgDomain = requestObj.optString("domain");
            }
        }
        if (!TextUtils.isEmpty(dlgDomain)) {
            if (isDebugLog) Log.e(TAG, "dlgDomain == " + dlgDomain);
            mAiMixASREngine.setDlgDomain(dlgDomain);
            mAiMixASREngine.setPrevDomain(dlgDomain);
        }

        if (domain.equals("netfm") || domain.equals("story") || domain.equals("music") || domain.equals("poetry")) {
            JSONObject dataJsonObj = sdsJsonObj.optJSONObject("data");
            if (dataJsonObj == null) {
                if (isDebugLog) Log.e(TAG, "domain = netfm... ，data == null");
                return false;
            }
            String output = sdsJsonObj.optString("output");
            JSONArray dbdataJsArray = dataJsonObj.optJSONArray("dbdata");
            if (dbdataJsArray != null && dbdataJsArray.length() > 0) {
                if (!TextUtils.isEmpty(output)) {
                    isUsedMediaPlayer = true;
                    isFromWake = false;
                    isMpOnpause = false;
                    CN_PREVIEW = output;
                    speakTips();
                }
                JSONObject netfmData = dbdataJsArray.optJSONObject(0);
                if (netfmData == null) {
                    if (isDebugLog) Log.e(TAG, "domain = netfm... ，netfmData == null");
                    return false;
                }
                String url = netfmData.optString("playUrl32");
                if (TextUtils.isEmpty(url)) {
                    if (isDebugLog) Log.e(TAG, "playUrl32 isEmpty...");
                    url = netfmData.optString("playUrl64");
                    if (TextUtils.isEmpty(url)) {
                        if (isDebugLog) Log.e(TAG, "playUrl64 isEmpty...");
                        url = netfmData.optString("url");
                        if (TextUtils.isEmpty(url)) {
                            if (isDebugLog) Log.e(TAG, "获取 url isEmpty...没有音乐文件");
                            //没有音乐文件
                            isUsedMediaPlayer = false;
                            isFromWake = false;

                            return false;
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
                sendBiaoQingSign(MyConstants.coolAnim);
                return true;
            } else {
                isUsedMediaPlayer = false;
                isFromWake = false;

                return false;
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
                return true;
            } else if (input.equals("语言启蒙") | input.equals("美图酷拍") | input.equals("小视频") | input.equals("卡拉OK")
                    | input.equals("多元智能") | input.equals("蒙特梭利") | input.equals("亲子互动") | input.equals("小学教育")
                    | input.equals("卡拉")) {
                openMainMenu(input);
                return true;
            } else {
                if (!TextUtils.isEmpty(outPut)) {
                    //使用本地合成语音播放返回的内容
                    if (outPut.contains("是哪个漂亮的妹子")) {
                        CN_PREVIEW = "你好啊，小主人";
                    } else if (outPut.contains("我爸爸是思必驰")) {
                        CN_PREVIEW = "我爸爸是小精灵";
                    } else if (outPut.contains("爸是思必驰")) {
                        CN_PREVIEW = "我爸爸是小精灵";
                    } else if (outPut.contains("我爸爸叫思必驰")) {
                        CN_PREVIEW = "我爸爸是小精灵";
                    } else if (outPut.contains("爸叫思必驰")) {
                        CN_PREVIEW = "我爸爸是小精灵";
                    } else if (outPut.contains("思必驰是我爸爸")) {
                        CN_PREVIEW = "我爸爸是小精灵";
                    } else if (outPut.contains("思必驰的所有攻城狮，程序猿哥哥都是我的巴比")) {
                        CN_PREVIEW = "我爸爸是小精灵";
                    } else if (outPut.contains("我妈妈叫思必驰")) {
                        CN_PREVIEW = "我妈妈是小精灵";
                    } else if (outPut.contains("妈叫思必驰")) {
                        CN_PREVIEW = "我妈妈是小精灵";
                    } else if (outPut.contains("我妈妈是思必驰")) {
                        CN_PREVIEW = "我妈妈是小精灵";
                    } else if (outPut.contains("妈是思必驰")) {
                        CN_PREVIEW = "我妈妈是小精灵";
                    } else if (outPut.contains("我妈妈在思必驰")) {
                        CN_PREVIEW = "我妈妈在鲁奇亚";
                    } else if (outPut.contains("我是小驰")) {
                        CN_PREVIEW = "我是鲁奇亚";
                    } else if (outPut.contains("我叫小驰")) {
                        CN_PREVIEW = "我叫鲁奇亚";
                    } else if (outPut.contains("因为要做爱")) {
                        CN_PREVIEW = "也许是上帝的安排吧，为了制造浪漫和痛苦";
                    } else if (outPut.contains("小驰是我")) {
                        CN_PREVIEW = "小驰是我的朋友";
                    } else {
                        CN_PREVIEW = outPut;
                    }
                    if (outPut.contains("生气") || outPut.contains("不开心")) {
                        sendBiaoQingSign(MyConstants.angryAnim);
                    } else if (outPut.contains("震惊") || outPut.contains("惊恐") || outPut.contains("吃惊")
                            || outPut.contains("害怕") || outPut.contains("恐惧") || outPut.contains("恐怖")) {
                        sendBiaoQingSign(MyConstants.shockAnim);
                    } else if (outPut.contains("当然") || outPut.contains("酷") || outPut.contains("必须的")) {
                        sendBiaoQingSign(MyConstants.coolAnim);
                    }
                    if (TextUtils.isEmpty(input)) {
                        sendBiaoQingSign(MyConstants.confusedAnim);
                    }
                    speakTips();
                    return true;
                } else {
                    if (isDebugLog) Log.e(TAG, "获取到的返回语音为空");
                    return false;
                }
            }
        } else if (domain.equals("motionctrl")) {//运动控制
            if (isDebugLog) Log.e(TAG, "domain 运动控制，domain = " + domain);
            controlRobot(result.optString("input"));
            return true;
        } else if (domain.equals("command")) {//中控
            if (isDebugLog) Log.e(TAG, "domain 中控，domain = " + domain);
            String inputStr = result.optString("input");
            if (inputStr.equals("退出") || inputStr.equals("返回")) {
                openMainMenu(inputStr);
                return true;
            } else if (inputStr.equals("拍照")) {
                openMainMenu(inputStr);
                return true;
            }
            controlRobot(inputStr);
            return true;
        } else if (domain.equals("translation")) {
            if (isDebugLog) Log.e(TAG, "domain 翻译，domain = " + domain);
            return speakTranslate(sdsJsonObj);
        } else {
            if (isDebugLog) Log.e(TAG, "domain 是其他的，domain = " + domain);
            return false;
        }
    }

    /**
     * 返回的是本地解析数据
     *
     * @param result
     * @return
     */
    private boolean localParseResult(JSONObject result) {
        JSONObject postJsonObject = result.optJSONObject("post");
        if (postJsonObject == null) {
            if (isDebugLog) Log.e(TAG, "本地解析 postJsonObject == null");
            return false;
        } else {
            JSONObject semJsonObject = postJsonObject.optJSONObject("sem");
            if (semJsonObject == null) {
                if (isDebugLog) Log.e(TAG, "本地解析 semJsonObject == null");
                return false;
            } else {
                String motorDomain = semJsonObject.optString("domain");
                String stringCtrl = semJsonObject.optString("MOTOR_CTRL");
                if (TextUtils.isEmpty(stringCtrl)) {
                    stringCtrl = semJsonObject.optString("VOLUME_CTRL");
                    if (TextUtils.isEmpty(stringCtrl)) {
                        stringCtrl = semJsonObject.optString("NEXT_UP");
                        if (TextUtils.isEmpty(stringCtrl)) {
                            stringCtrl = semJsonObject.optString("OPEN_SETTING");
                            if (TextUtils.isEmpty(stringCtrl)) {
                                stringCtrl = semJsonObject.optString("OPEN_MENU");
                                if (!TextUtils.isEmpty(motorDomain) && !TextUtils.isEmpty(stringCtrl)) {
                                    if (isDebugLog)
                                        Log.e(TAG, "本地识别资源匹配，执行相应操作 motorCtrl = " + stringCtrl);
                                    openMainMenu(stringCtrl);
                                    return true;
                                }
                            }
                        }
                    }
                }
                if (!TextUtils.isEmpty(motorDomain) && !TextUtils.isEmpty(stringCtrl)) {
                    if (isDebugLog) Log.e(TAG, "本地识别资源匹配，执行相应操作 motorCtrl = " + stringCtrl);
                    controlRobot(stringCtrl);
                    return true;
                } else {
                    if (isDebugLog) Log.e(TAG, "本地解析 semJsonObject == null");
                    return false;
                }
            }
        }
    }

    @Override
    public void onCompletionListener() {
        //在音乐播放完成时重新开始对话
        if (isDebugLog) Log.e(TAG, "音乐播放完成, 重新开始对话...");
        isUsedMediaPlayer = false;
        isMpOnpause = false;
        isGoSleeping = true;
        sendBiaoQingSign(MyConstants.naughtyAnim);
        CN_PREVIEW = MyConstants.MP_COMPLET;
        speakTips();
    }

    private void controlRobot(String ctrl_str) {
        if (ctrl_str.contains("左")) {
            sendBiaoQingSign(MyConstants.coolAnim);
            int callback = WriteDataUtils.native_ear_light_control(0, MyConstants.MOTOR_LEFT, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_LEFT 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("右")) {
            sendBiaoQingSign(MyConstants.coolAnim);
            int callback = WriteDataUtils.native_ear_light_control(0, MyConstants.MOTOR_RIGHT, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_RIGHT 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("前")) {
            sendBiaoQingSign(MyConstants.coolAnim);
            int callback = WriteDataUtils.native_ear_light_control(0, MyConstants.MOTOR_FORWARD, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_FORWARD 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("后")) {
            sendBiaoQingSign(MyConstants.coolAnim);
            int callback = WriteDataUtils.native_ear_light_control(0, MyConstants.MOTOR_BACK, 0);
            if (isDebugLog) Log.e(TAG, "MOTOR_BACK 的返回值是: " + callback);

            mHandler.removeMessages(1);
            mHandler.sendEmptyMessageDelayed(1, 5 * 1000);
        } else if (ctrl_str.contains("停")) {
            if (isMainDancing && isMainOnPause) {
                if (isDebugLog) Log.e(TAG, "ctrl_str.contains(\"停\") 停止跳舞");
                sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_STOP));
                isMainDancing = false;
                isMainOnPause = false;
                CN_PREVIEW = MyConstants.MEDIA_STOPED;
                speakTips();
            } else if (isUsedMediaPlayer && mRobotMediaPlayer != null) {
                if (isDebugLog) Log.e(TAG, "ctrl_str.contains(\"停\") 停止播放");
                mRobotMediaPlayer.stop();
                isUsedMediaPlayer = false;
                isMpOnpause = false;
                //mAiMixASREngine.start();
                sendBiaoQingSign(MyConstants.naughtyAnim);
                CN_PREVIEW = MyConstants.MEDIA_STOPED;
                speakTips();
            } else {
                int callback = WriteDataUtils.native_ear_light_control(0, MyConstants.MOTOR_STOP, 0);
                if (isDebugLog) Log.e(TAG, "MOTOR_STOP 的返回值是: " + callback);

                mHandler.removeMessages(1);
                mHandler.sendEmptyMessage(1);
            }
        } else if (ctrl_str.contains("继续")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续");
            if (isMainDancing && isMainOnPause) {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续 跳舞");
                sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_GO_ON));
                isMainOnPause = false;
            } else if (isUsedMediaPlayer && mRobotMediaPlayer != null && isMpOnpause) {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续 播放");
                isMpOnpause = false;
                mRobotMediaPlayer.play();
                sendBiaoQingSign(MyConstants.coolAnim);
            } else if (Util.isForeground(mContext, MyConstants.mainApkKalaokPlayActivity)
                    || Util.isForeground(mContext, MyConstants.mainApkLocalVedioActivity)) {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续 播放本地视频或卡拉OK");
                sendBroadcast(new Intent(MyConstants.ACTION_LOCAL_VEDIO_GO_ON));
            } else {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 继续 else 开始对话");
                if (!NetworkUtil.isWifiConnected(SpeechService.this)) {
                    CN_PREVIEW = "请连接网络";
                } else {
                    CN_PREVIEW = "我们换个问题聊聊吧";
                    sendBiaoQingSign(MyConstants.confusedAnim);
                }
                speakTips();
            }
        } else if (ctrl_str.contains("跳舞") || ctrl_str.contains("跳个舞")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 跳舞/跳个舞");
            if (isMainDancing && isMainOnPause) {
                if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 跳舞/跳个舞，正在跳舞 继续跳");
                sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_GO_ON));
                isMainOnPause = false;
                return;
            }
            Intent mIntent = new Intent(SpeechService.this, MainActivity.class);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mIntent);
            isMainDancing = true;
            isMainOnPause = false;
        } else if (ctrl_str.contains("音量大一点") || ctrl_str.contains("大声一点") || ctrl_str.contains("音量加")
                || ctrl_str.contains("再大一点")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 音量大一点/大声一点/音量加");
            setUpVolume(0);
        } else if (ctrl_str.contains("音量小一点") || ctrl_str.contains("小声一点") || ctrl_str.contains("音量减")
                || ctrl_str.contains("再小一点")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 音量小一点/小声一点/音量减");
            setUpVolume(1);
        } else if (ctrl_str.contains("打开设置")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 打开设置");
            if (!Util.isForeground(mContext, MyConstants.systemMainActivity)) {
                Util.startApp(mContext, MyConstants.systemSettingName);
            }
            CN_PREVIEW = "设置";
            speakTips();
        } else if (ctrl_str.contains("关机")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 关机");
            showShutDownDialog();
        } else if (ctrl_str.contains("上一首") | ctrl_str.contains("下一首")) {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains : 上一首/下一首");
            if (isMainDancing && isMainOnPause) {
                Intent mIntent = new Intent(MyConstants.ACTION_DANCE_SERVICE_NEXT_UP);
                sendBroadcast(mIntent);
                isMainOnPause = false;
            }
        } else {
            if (isDebugLog) Log.e(TAG, "controlRobot ctrl_str contains else .. = " + ctrl_str);
            if (!NetworkUtil.isWifiConnected(SpeechService.this)) {
                CN_PREVIEW = "请连接网络";
            } else {
                CN_PREVIEW = "我们换个问题聊聊吧";
                sendBiaoQingSign(MyConstants.confusedAnim);
            }
            speakTips();
        }
    }

    /**
     * 读出翻译的词语
     *
     * @param sdsJsonObj
     */
    private boolean speakTranslate(JSONObject sdsJsonObj) {
        JSONObject dataJsonObj = sdsJsonObj.optJSONObject("data");
        if (dataJsonObj == null)
            return false;
        JSONArray dbdataArray = dataJsonObj.optJSONArray("dbdata");
        if (dbdataArray == null || dbdataArray.length() == 0)
            return false;
        String dst = dbdataArray.optJSONObject(0).optString("dst");
        if (TextUtils.isEmpty(dst))
            return false;
        CN_PREVIEW = dst;
        sendBiaoQingSign(MyConstants.coolAnim);
        speakTips();
        return true;
    }

    /**
     * 打开指定的二级页面
     *
     * @param ctrl_str
     */
    private void openMainMenu(String ctrl_str) {
        if (isDebugLog) Log.e(TAG, "打开指定页面: " + ctrl_str);
        if (ctrl_str.equals("语言启蒙")) {
            CN_PREVIEW = "语言启蒙";
            if (!Util.isForeground(mContext, MyConstants.mainApkYuyanActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkYuyanActivity);
        } else if (ctrl_str.equals("美图酷拍")) {
            CN_PREVIEW = "美图酷拍";
            if (!Util.isForeground(mContext, MyConstants.mainApkCameraActivity)) {
                Util.startCameraActivity(mContext);
            }
        } else if (ctrl_str.equals("小视频")) {
            CN_PREVIEW = "小视频";
            if (!Util.isForeground(mContext, MyConstants.mainApkVideoActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkVideoActivity);
        } else if (ctrl_str.equals("卡拉OK")) {
            CN_PREVIEW = "卡拉OK";
            if (!Util.isForeground(mContext, MyConstants.mainApkKalaOkActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkKalaOkActivity);
        } else if (ctrl_str.equals("卡拉")) {
            CN_PREVIEW = "卡拉OK";
            if (!Util.isForeground(mContext, MyConstants.mainApkKalaOkActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkKalaOkActivity);
        } else if (ctrl_str.equals("多元智能")) {
            CN_PREVIEW = "多元智能";
            if (!Util.isForeground(mContext, MyConstants.mainApkDuoyuanActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkDuoyuanActivity);
        } else if (ctrl_str.equals("蒙特梭利")) {
            CN_PREVIEW = "蒙特梭利";
            if (!Util.isForeground(mContext, MyConstants.mainApkMengteActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkMengteActivity);
        } else if (ctrl_str.equals("亲子互动")) {
            CN_PREVIEW = "亲子互动";
            if (!Util.isForeground(mContext, MyConstants.mainApkQinziActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkQinziActivity);
        } else if (ctrl_str.equals("小学教育")) {
            CN_PREVIEW = "小学教育";
            if (!Util.isForeground(mContext, MyConstants.mainApkXiaoXueActivity))
                Util.startApp(mContext, MyConstants.mainApkXiaoXuePackage);
        } else if (ctrl_str.equals("退出") || ctrl_str.equals("返回")) {
            CN_PREVIEW = "已退出";
            if (Util.isForeground(mContext, MyConstants.robotMainActivity)) {
                sendBroadcast(new Intent(MyConstants.ACTION_DANCE_SERVICE_STOP));
            }
            if (!Util.isForeground(mContext, MyConstants.mainApkActivity))
                Util.startMianApkMenuActivity(mContext, MyConstants.mainApkActivity);
        } else if (ctrl_str.equals("拍照")) {
            if (!Util.isForeground(mContext, MyConstants.mainApkCameraActivity)) {
                CN_PREVIEW = "美图酷拍";
                Util.startCameraActivity(mContext);
            } else {
                CN_PREVIEW = "拍照";
                Util.takePicture();
            }
        }
        speakTips();
    }

    private void isNeedStopRecording() {
        if (!Util.isForeground(mContext, MyConstants.mainApkActivity) && !Util.isForeground(mContext, MyConstants.apkVoiceActivity)) {
            if (asrTimes >= 2) {
                if (isDebugLog) Log.e(TAG, "现在不是在主页面或表情页面，超过了2次");
                mAiMixASREngine.stopRecording();

                mHandler.removeMessages(4);
                mHandler.sendEmptyMessageDelayed(4, 10 * 1000);
            } else {
                if (isDebugLog) Log.e(TAG, "现在不是在主页面或表情页面，没有到2次");
                mAiMixASREngine.start();
            }
        } else {
            if (asrTimes >= 6) {
                if (isDebugLog) Log.e(TAG, "现在是在主页面或表情页面，超过了6次");
                mAiMixASREngine.stopRecording();
            } else {
                if (isDebugLog) Log.e(TAG, "现在是在主页面或表情页面，没有到6次");
                mAiMixASREngine.start();
            }
        }
    }

    /**
     * 调节音量
     *
     * @param sign
     */
    private void setUpVolume(int sign) {
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (sign == 0) {//加大
            if (currentVolume == maxVolume) {
                CN_PREVIEW = "已经是最大音量";
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, currentVolume++, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume++, 0);
                CN_PREVIEW = "音量已调整";
            }
            speakTips();

        } else {//减小
            if (currentVolume == 0) {
                CN_PREVIEW = "已经是最小音量";
            } else {
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, currentVolume--, 0);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume--, 0);
                CN_PREVIEW = "音量已调整";
            }
            speakTips();
        }
    }

    /**
     * 检测是否需要停止语音引擎对麦克风的占用
     * 当前用到是QQ和极相机
     */
    class SpeechTiemrTask extends TimerTask {
        @Override
        public void run() {
            if (!Util.isBackground(mContext, MyConstants.qqHdPackageName)
                    || !Util.isBackground(mContext, MyConstants.mainApkCameraPackage)) {
                String temp = Util.getForeActivity(mContext);
                if (isDebugLog) Log.e(TAG, "temp activity : " + temp);
                if (!TextUtils.isEmpty(temp)) {
                    if (!temp.contains(MyConstants.qqHdPartPackage) &&
                            !temp.contains(MyConstants.mainApkCameraPackage)) {
                        return;
                    }
                }
                if (isDebugLog) Log.e(TAG, "检测到qq或相机在前台");
                if (!isStoped && isAuthed) {
                    if (isDebugLog) Log.e(TAG, "检测到需要停止唤醒和混合识别引擎");
                    isStoped = true;
                    mAiLocalTTSEngine.stop();
                    mAiMixASREngine.stopRecording();
                    mAiMixASREngine.destroy();
                    mAiLocalWakeupDnnEngine.stop();
                    mAiLocalWakeupDnnEngine.destroy();
                }
            } else {
                if (isStoped && isAuthed) {
                    if (Util.isForeground(mContext, MyConstants.qqHdAvActivity)) {
                        if (isDebugLog) Log.e(TAG, "现在处于QQ视频对话的页面...");
                        return;
                    }
                    if (isDebugLog) Log.e(TAG, "检测到需要开启唤醒引擎");
                    isStoped = false;
                    initAiMixASREngine();
                    initWakeupDnnEngine();
                }
            }
        }
    }

    /**
     * 发送表情切换的广播
     *
     * @param sign
     */
    private void sendBiaoQingSign(int sign) {
        Intent intent = new Intent(MyConstants.ACTION_BIAOQING_ZHUANGTAI);
        intent.putExtra(MyConstants.KEY_BIAOQING_SIGN, sign);
        sendBroadcast(intent);
    }

    /**
     * 弹框提示是否关机
     */
    private void showShutDownDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("\n小主人，您确定要关机么\n");
        builder.setNegativeButton("取消", null);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shutDown();
            }
        });
        final Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    private void shutDown() {
        Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
        intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
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
        if (speechTimer != null) {
            speechTimer.cancel();
            speechTimer = null;
        }
        releaseWakeLock();
        if (isDebugLog) Log.e(TAG, "SpeechService destroied...");
    }
}
