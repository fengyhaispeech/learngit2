package com.yihengke.robotspeech.utils;

public class SampleConstants {

    /**
     * VAD资源
     */
    public static String vad_res = "vad_aihome_v0.7.bin";

    /**
     * AEC资源设置
     */
    public static String echo_config = "AEC_ch2-2-ch1_1ref_common_20170710_v0.8.1.bin";

    /**
     * 识别相关资源
     */
    public static String ebnfc_res = "ebnfc.aicar.1.2.0.bin";
    public static String ebnfr_res = "ebnfr.aicar.1.2.0.bin";

    /**
     * 唤醒资源文件
     */
    public static String BEAMFORMING_CFG = "UDA_asr_chan2-2-mic2_30mm_20180323.bin";
    public static String WAKEUP_RES_BIN = "wakeup_aifar_comm_20180104.bin";

    /**
     * 合成资源文件
     */
    public static String tts_res = "qianranc_common_param_ce_local.v2.018.bin";
    public static String tts_dict = "aitts_sent_dict_v3.20.db";

    /**
     * 服务器资源
     */
    public static String server_production = "ws://s.api.aispeech.com:1028,ws://s.api.aispeech.com:80";
    public static String server_gray = "ws://s-test.api.aispeech.com:10000";
    public static String res_home = "aihome";
    public static String res_car = "aicar";
    public static String res_robot = "airobot";
}
