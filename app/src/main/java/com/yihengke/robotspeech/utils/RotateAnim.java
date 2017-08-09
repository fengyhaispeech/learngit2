package com.yihengke.robotspeech.utils;

import android.view.animation.Animation;
import android.view.animation.RotateAnimation;

/**
 * Created by Administrator on 2017/8/8.
 */

public class RotateAnim {

    public static RotateAnimation loadAnimation() {

        final RotateAnimation rotateAnim = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnim.setDuration(1000);
        rotateAnim.setRepeatCount(10);
        return rotateAnim;
    }
}
