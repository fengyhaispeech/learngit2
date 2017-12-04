package com.yihengke.robotspeech.utils;

import android.content.Context;
import android.graphics.Typeface;

/**
 * Created by yu on 2017/2/24.
 */

public class TypefaceUtil {

    private static Typeface mZhanku;

    public static Typeface getZhanku(Context context) {
        if (null == mZhanku) {
            mZhanku = Typeface.createFromAsset(context.getAssets(), "fonts/zhanku.TTF");
        }

        return mZhanku;
    }
}
