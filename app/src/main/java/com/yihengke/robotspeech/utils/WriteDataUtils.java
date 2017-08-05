package com.yihengke.robotspeech.utils;

import android.util.Log;

import com.yihengke.robotspeech.BuildConfig;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Created by Administrator on 2017/8/5.
 */

public class WriteDataUtils {

    private static String TAG = "WriteDataUtils";
    private static boolean isDebugLog = BuildConfig.DEBUG_LOG;

    private static PrintStream logStream;

    private static boolean initialized = false;

    private static final String filePath = "/dev/motor_nb666";

    //初始化 PrintStream
    public static void init() {
        try {
            logStream = new PrintStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void writeData(int commandId) {
        if (!initialized)
            init();
        if (logStream == null || logStream.checkError()) {
            initialized = false;
//            init();
            return;
        } else {
            initialized = true;
        }

        logStream.println(commandId);
        if (isDebugLog) Log.e(TAG, "向文件中写入数据command = " + commandId);
    }

    static {
        System.loadLibrary("jniearlight");
    }

    /**
     * 写入控制命令
     *
     * @param id1
     * @param commandId
     * @param id2
     * @return
     */
    public static native int native_ear_light_control(int id1, int commandId, int id2);

}
