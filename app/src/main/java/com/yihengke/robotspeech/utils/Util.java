package com.yihengke.robotspeech.utils;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.KeyEvent;

import com.yihengke.robotspeech.activity.SdsActivity;

import java.util.List;

/**
 * Created by Administrator on 2017/11/15.
 *
 * @author Administrator
 */

public class Util {

    private static String mainApkPackage = "com.wyt.launcher.hkxingkong";
    private static String mainApkQinziActivity = "com.wyt.launcher.hkxingkong.xueqian.FlashMainActivity";
    private static String mainApkCameraPackage = "com.jb.zcamera";
    private static String mainApkCameraActivity = "com.jb.zcamera.camera.MainActivity";

    /**
     * 判断activity是否在前台
     *
     * @param context
     * @param className
     * @return
     */
    public static boolean isForeground(Context context, String className) {
        if (TextUtils.isEmpty(className)) {
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测前台activity的类名
     *
     * @param context
     * @return
     */
    public static String getForeActivity(Context context) {
        String activity = null;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            return cpn.getClassName();
        }
        return activity;
    }

    /**
     * 判断指定应用是否在后台
     *
     * @param context
     * @param apkPackageName
     * @return
     */
    public static boolean isBackground(Context context, String apkPackageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(apkPackageName)) {
                /*
                BACKGROUND=400 EMPTY=500 FOREGROUND=100
                GONE=1000 PERCEPTIBLE=130 SERVICE=300 ISIBLE=200
                 */
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
//                    Log.i(context.getPackageName(), "处于后台" + appProcess.processName);
                    return true;
                } else {
//                    Log.i(context.getPackageName(), "处于前台" + appProcess.processName);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 模拟点击拍照按键
     */
    public static void takePicture() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Instrumentation inst = new Instrumentation();
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_CAMERA);
            }
        });
        t.start();
    }

    /**
     * 启动程序
     *
     * @param packageName
     */
    public static void startApp(Context context, String packageName) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            // 已安装包 直接启动
            context.startActivity(intent);
        }
    }

    /**
     * 打开主页面二级菜单
     *
     * @param context
     * @param activityName
     */
    public static void startMianApkMenuActivity(Context context, String activityName) {
        try {
            Intent intent = new Intent();
            ComponentName cn = new ComponentName(mainApkPackage, activityName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (activityName.equals(mainApkQinziActivity)) {
                intent.putExtra("module", "亲子互动");
            }
            intent.setComponent(cn);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开照相机
     */
    public static void startCameraActivity(Context context) {
        try {
            Intent intent = new Intent();
            ComponentName cn = new ComponentName(mainApkCameraPackage, mainApkCameraActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setComponent(cn);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startVoiceActivity(Context context) {
        Intent mIntent = new Intent(context, SdsActivity.class);
        mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mIntent.putExtra(MyConstants.KEY_START_SDS_ACTIVITY, 0);
        context.startActivity(mIntent);
    }
}
