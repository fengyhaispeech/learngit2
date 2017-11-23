package com.yihengke.robotspeech.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.yihengke.robotspeech.R;
import com.yihengke.robotspeech.utils.MyConstants;

/**
 * Created by Administrator on 2017/11/16.
 *
 * @author Administrator
 */

public class SdsActivity extends AppCompatActivity {

    private static final int happyAnim = 0;//开心
    private static final int chargeAnim = 1;//充电
    private static final int shyAnim = 2;//害羞
    private static final int cryAnim = 3;//哭
    private static final int coolAnim = 4;//酷
    private static final int lowBatteryAnim = 5;//低电
    private static final int angryAnim = 6;//生气
    private static final int naughtyAnim = 7;//调皮
    private static final int wrongedAnim = 8;//委屈
    private static final int confusedAnim = 9;//晕
    private static final int shockAnim = 10;//震惊
    private ImageView imageAnim;
    private Context mContext;
    private MyReceiver myReceiver;
    private MyHandler myHandler;
    private final int animWhat = 0;
    private long animTime = 5000;
    private long lastTime;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sds);
        imageAnim = (ImageView) findViewById(R.id.image_anim);
        mContext = getApplicationContext();
        myHandler = new MyHandler();
        setOncClick();
    }

    private void setOncClick() {
        imageAnim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentTime = System.currentTimeMillis() / 1000;
                if ((currentTime - lastTime) <= 1) {
                    finish();
                } else {
                    lastTime = currentTime;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        InitAnimation();
        initReceiver();
    }

    private void initReceiver() {
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(MyConstants.ACTION_BIAOQING_ZHUANGTAI);
        mFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        mFilter.addAction(Intent.ACTION_BATTERY_LOW);
        mFilter.addAction(MyConstants.ACTION_FINISH_SDS_ACTIVITY);
        myReceiver = new MyReceiver();
        registerReceiver(myReceiver, mFilter);
    }

    private void InitAnimation() {
        Glide.with(mContext).asGif().load("file:///android_asset/happyAnim.gif").into(imageAnim);
        myHandler.sendEmptyMessageDelayed(animWhat, animTime);
    }

    private void startAnim(int index) {
        switch (index) {
            case happyAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/happyAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case chargeAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/chargeAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case shyAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/shyAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case cryAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/cryAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case coolAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/coolAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case lowBatteryAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/lowBatteryAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case angryAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/angryAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case naughtyAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/naughtyAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case wrongedAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/wrongedAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case confusedAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/confusedAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            case shockAnim:
                Glide.with(mContext).asGif().load("file:///android_asset/shockAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
            default:
                Glide.with(mContext).asGif().load("file:///android_asset/happyAnim.gif").into(imageAnim);
                myHandler.removeMessages(animWhat);
                myHandler.sendEmptyMessageDelayed(animWhat, animTime);
                break;
        }
    }

    class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(MyConstants.ACTION_BIAOQING_ZHUANGTAI)) {
                int sign = intent.getIntExtra(MyConstants.KEY_BIAOQING_SIGN, 0);
                startAnim(sign);
            } else if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                startAnim(chargeAnim);
            } else if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                startAnim(lowBatteryAnim);
            } else if (action.equals(MyConstants.ACTION_FINISH_SDS_ACTIVITY)) {
                finish();
            }
        }
    }

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case animWhat:
                    startAnim(happyAnim);
                    break;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        myHandler.removeMessages(animWhat);
        Glide.with(mContext).clear(imageAnim);
        if (myReceiver != null) {
            unregisterReceiver(myReceiver);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        myHandler.removeMessages(animWhat);
    }

    @Override
    protected void onDestroy() {
        sendBroadcast(new Intent(MyConstants.ACTION_SDS_ACTIVITY_FINISHED));
        super.onDestroy();
    }
}
