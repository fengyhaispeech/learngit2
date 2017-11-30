package com.yihengke.robotspeech.utils;

/**
 * Created by Administrator on 2017/11/15.
 *
 * @author Administrator
 */

public class MyConstants {

    public static final int MOTOR_LEFT = 0;
    public static final int MOTOR_RIGHT = 1;
    public static final int MOTOR_FORWARD = 2;
    public static final int MOTOR_BACK = 3;
    public static final int MOTOR_STOP = 4;

    public static final String HELP_TIP = "有什么我可以帮您？";
    public static final String[] SDS_ERRO_TIP = new String[]{"你没有事，我就去休息了", "我没有听清楚",
            "请再说一次", "请重新说"};
    public static final String MP_COMPLET = "播放完成";
    public static final String MEDIA_STOPED = "已停止";

    public static final String[] HEAD_TIPS = new String[]{"好舒服啊", "好喜欢你呀，是不是觉得我萌萌哒", "好害羞啊",
            "好痒啊", "有什么可以帮助你"};
    public static final String[] HAND_TIPS = new String[]{"你是在和我做运动吗", "握握手，你是我的好朋友", "有什么可以帮助你",
            "哎呀，我的手要散架了"};
    public static final String[] TimeoutNoTouch = new String[]{"人呢人呢，我在等你跟我玩呢", "啊，你去哪里了，我还没有玩够呢"};

    public static final String HEAD_TOUCH_ACTION = "com.yinghengke.headtouch";
    public static final String HAND_TOUCH_ACTION = "com.yinghengke.handtouch";

    public static final String SP_NAME = "SpeechService";
    public static final String SP_GRAMMAR_INITED_KEY = "sp_grammar_inited_key";

    public static final String ACTION_DANCE_STARTED = "action_dance_started";//接收
    public static final String ACTION_DANCE_STOPED = "action_dance_stoped";//接收
    public static final String ACTION_DANCE_SERVICE_PAUSED = "action_dance_service_paused";//发送
    public static final String ACTION_DANCE_SERVICE_STOP = "action_dance_service_stop";//发送
    public static final String ACTION_DANCE_SERVICE_GO_ON = "action_dance_service_go_on";//发送
    public static final String ACTION_DANCE_SERVICE_NEXT_UP = "action_dance_service_next_up";//发送

    public static final String SPEECH_WAKE_UP_ROBOT_SCREEN = "speech_wake_up_robot_screen";//发送

    public static final String systemSettingName = "com.yihengke.systemsettings";
    public static final String systemMainActivity = "com.yihengke.systemsettings.activity.MainActivity";
    public static final String robotMainActivity = "com.yihengke.robotspeech.activity.MainActivity";
    public static final String mainApkPackage = "com.wyt.launcher.hkxingkong";
    public static final String mainApkActivity = "com.wyt.launcher.hkxingkong.XueqianActivity";
    //    public static final String apkVoiceActivity = "com.wyt.launcher.hkxingkong.VoiceActivity";//机器人脸的页面
    public static final String apkVoiceActivity = "com.yihengke.robotspeech.activity.SdsActivity";//机器人脸的页面

    public static final String mainApkYuyanActivity = "com.wyt.launcher.hkxingkong.xueqian.Pic_YuyanqimengActivity";
    public static final String mainApkVideoActivity = "com.wyt.launcher.hkxingkong.xueqian.VideoMainActivity";
    public static final String mainApkKalaOkActivity = "com.wyt.launcher.hkxingkong.xueqian.KaLaOKActivity";
    public static final String mainApkDuoyuanActivity = "com.wyt.launcher.hkxingkong.xueqian.FlashDyznActivity";
    public static final String mainApkMengteActivity = "com.wyt.launcher.hkxingkong.xueqian.FlashMtslActivity";
    public static final String mainApkQinziActivity = "com.wyt.launcher.hkxingkong.xueqian.FlashMainActivity";
    public static final String mainApkXiaoXuePackage = "com.wyt.zxp.xx";
    public static final String mainApkXiaoXueActivity = "com.wyt.gelingpad.lauchermain.MainActivity";
    public static final String mainApkCameraPackage = "com.jb.zcamera";
    public static final String mainApkCameraActivity = "com.jb.zcamera.camera.MainActivity";
    public static final String mainApkKalaokPlayActivity = "com.wyt.launcher.hkxingkong.module.mediaplayer.player.VideoPalyActivity";
    public static final String mainApkLocalVedioActivity = "com.wyt.launcher.hkxingkong.module.mediaplayer.player.VideoPlayerActivity1";

    public static final String ACTION_LOCAL_VEDIO_PAUSE = "action_local_vedio_pause";
    public static final String ACTION_LOCAL_VEDIO_GO_ON = "action_local_vedio_go_on";
    public static final String ACTION_ROBOT_CAN_NOT_UNDERSTAND = "action_robot_can_not_understand";
    public static final String ACTION_ROBOT_WAKE_UP = "action_robot_wake_up";
    public static final String ACTION_ROBOT_GO_TO_SLEEP = "action_robot_go_to_sleep";

    public static final String qqHdPartPackage = "com.tencent.yhkdeviceapp";//"com.tencent";
    public static final String qqHdPackageName = "com.tencent.yhkdeviceapp";//"com.tencent.minihd.qq";
    public static final String qqHdAvActivity = "com.tencent.yhkdeviceapp.VideoChatActivityHW";//"com.tencent.av.ui.AVActivity";

    public static final String ACTION_BIAOQING_ZHUANGTAI = "action_biaoqing_zhuangtai";
    public static final String KEY_BIAOQING_SIGN = "key_biaoqing_sign";

    public static final int happyAnim = 0;//开心
    public static final int chargeAnim = 1;//充电
    public static final int shyAnim = 2;//害羞
    public static final int cryAnim = 3;//哭
    public static final int coolAnim = 4;//酷
    public static final int lowBatteryAnim = 5;//低电
    public static final int angryAnim = 6;//生气
    public static final int naughtyAnim = 7;//调皮
    public static final int wrongedAnim = 8;//委屈
    public static final int confusedAnim = 9;//晕
    public static final int shockAnim = 10;//震惊
    public static final String ACTION_SDS_ACTIVITY_FINISHED = "action_sds_activity_finished";
    public static final String ACTION_FINISH_SDS_ACTIVITY = "action_finish_sds_activity";
    public static final String ACTION_START_SDS_ACTIVITY = "action_start_sds_activity";
    public static final String KEY_START_SDS_ACTIVITY = "key_start_sds_activity";
}
