/*******************************************************************************
 * Copyright 2014 AISpeech
 ******************************************************************************/
package com.yihengke.robotspeech.utils;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * 辅助生成ebnf语法文件类 <br>
 * 需要权限:
 * {@link android.Manifest.permission#WRITE_EXTERNAL_STORAGE
 * android.permission.WRITE_EXTERNAL_STORAGE}
 */
public class GrammarHelper {

    private Context mContext;

    public GrammarHelper(Context mContext) {
        this.mContext = mContext;
    }

    /**
     * 利用模板文件，生成ebnf格式grammar
     *
     * @param filename assets目录下模板文件名
     * @return
     */
    public String importAssets(String filename) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(mContext.getAssets().open(filename), Charset.forName("UTF-8")));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("error", "Template file " + filename + " not found, Pls fix this bug!");
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }
}
