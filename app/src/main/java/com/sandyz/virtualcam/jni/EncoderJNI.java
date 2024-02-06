package com.sandyz.virtualcam.jni;

import android.annotation.SuppressLint;

import com.sandyz.virtualcam.HookMain;

import kotlin.jvm.Synchronized;

/**
 * @author zhangzhe
 * @date 2021/3/22
 * @description
 */

@SuppressLint("UnsafeDynamicallyLoadedCode")
public class EncoderJNI {

    static {
        System.load(HookMain.Companion.getModulePath() +"/lib/arm64/libencoder.so");
    }

    @Synchronized
    public static native byte[] encodeYUV420SP(int[] argb, int width, int height);
}
