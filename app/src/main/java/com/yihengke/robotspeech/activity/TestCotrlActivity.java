package com.yihengke.robotspeech.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ISteeringService;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.SteeringUtil;
import com.yihengke.robotspeech.utils.WriteDataUtils;

/**
 * Created by Administrator on 2017/11/14.
 *
 * @author Administrator
 */

public class TestCotrlActivity extends AppCompatActivity implements View.OnClickListener {

    private static int MOTOR_LEFT = 0;
    private static int MOTOR_RIGHT = 1;
    private static int MOTOR_FORWARD = 2;
    private static int MOTOR_BACK = 3;
    private static int MOTOR_STOP = 4;

    private MyHandler myHandler;
    private ISteeringService iSteeringService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_ctrl);
        initViews();
        //initSteering();
        myHandler = new MyHandler();
    }

    private void initViews() {
        Button btnForward = (Button) findViewById(R.id.btn_forward);
        Button btnBack = (Button) findViewById(R.id.btn_back);
        Button btnLeft = (Button) findViewById(R.id.btn_left);
        Button btnRight = (Button) findViewById(R.id.btn_right);
        Button btnStop = (Button) findViewById(R.id.btn_stop);
        Button btnHeadLeft = (Button) findViewById(R.id.btn_head_left);
        Button btnHeadRight = (Button) findViewById(R.id.btn_head_right);
        btnForward.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnLeft.setOnClickListener(this);
        btnRight.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnHeadLeft.setOnClickListener(this);
        btnHeadRight.setOnClickListener(this);
    }

    private void initSteering() {
        iSteeringService = SteeringUtil.getInstance();
        if (iSteeringService != null) {
            try {
                int temp = iSteeringService.openDev();
                Log.e("robot test", "iSteeringService openDev: " + temp);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_forward:
                myHandler.removeMessages(0);
                WriteDataUtils.native_ear_light_control(0, MOTOR_FORWARD, 0);
                myHandler.sendEmptyMessageDelayed(0, 3000);
                break;
            case R.id.btn_back:
                myHandler.removeMessages(0);
                WriteDataUtils.native_ear_light_control(0, MOTOR_BACK, 0);
                myHandler.sendEmptyMessageDelayed(0, 3000);
                break;
            case R.id.btn_left:
                myHandler.removeMessages(0);
                WriteDataUtils.native_ear_light_control(0, MOTOR_LEFT, 0);
                myHandler.sendEmptyMessageDelayed(0, 3000);
                break;
            case R.id.btn_right:
                myHandler.removeMessages(0);
                WriteDataUtils.native_ear_light_control(0, MOTOR_RIGHT, 0);
                myHandler.sendEmptyMessageDelayed(0, 3000);
                break;
            case R.id.btn_stop:
                myHandler.removeMessages(0);
                WriteDataUtils.native_ear_light_control(0, MOTOR_STOP, 0);
                break;
            case R.id.btn_head_left:
                if (iSteeringService != null) {
                    try {
                        int targetPosition = iSteeringService.getPosition() - 15;
                        if (targetPosition > 10)
                            iSteeringService.rotate(targetPosition, 15 * 10);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case R.id.btn_head_right:
                if (iSteeringService != null) {
                    try {
                        int targetPosition = iSteeringService.getPosition() + 15;
                        if (targetPosition < 170)
                            iSteeringService.rotate(targetPosition, 15 * 10);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }

    @SuppressLint("HandlerLeak")
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                //WriteDataUtils.native_ear_light_control(0, MOTOR_STOP, 0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        WriteDataUtils.native_ear_light_control(0, MOTOR_STOP, 0);
        if (iSteeringService != null) {
            try {
                iSteeringService.closeDev();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        startApp("com.DeviceTest");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WriteDataUtils.native_ear_light_control(0, MOTOR_STOP, 0);
        if (iSteeringService != null) {
            try {
                iSteeringService.closeDev();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 启动程序
     *
     * @param packageName
     */
    public void startApp(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            // 已安装包 直接启动
            startActivity(intent);
        }
    }
}
