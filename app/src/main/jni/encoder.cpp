#include <cstdlib>
#include <cstdio>
#include <cmath>
#include <cstring>
#include <jni.h>
#include <map>
#include "utils.h"

jbyte yuv420spRaw[4096 * 2048];

extern "C" jbyteArray
Java_com_sandyz_virtualcam_jni_EncoderJNI_encodeYUV420SP(JNIEnv *env, jclass jobj,
                                                         jintArray argb, jint width, jint height) {
    int frameSize = width * height;
    int yIndex = 0;
    int uvIndex = frameSize;
    int a;
    int R;
    int G;
    int B;
    int Y;
    int U;
    int V;
    int index = 0;
    jint *argbArray = env->GetIntArrayElements(argb, 0);
    for (int i = 0; i < height; ++i) {
        for (int j = 0; j < width; ++j) {
            a = argbArray[index] & -0x1000000 >> 24;
            R = argbArray[index] & 0xff0000 >> 16;
            G = argbArray[index] & 0xff00 >> 8;
            B = argbArray[index] & 0xff;
            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
            yuv420spRaw[yIndex++] = (jbyte) (Y < 0 ? 0 : (Y > 255 ? 255 : Y));
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420spRaw[uvIndex++] = (jbyte) (V < 0 ? 0 : (V > 255 ? 255 : V));
                yuv420spRaw[uvIndex++] = (jbyte) (U < 0 ? 0 : (U > 255 ? 255 : U));
            }
            index++;
        }
    }
    jbyteArray yuv420sp = env->NewByteArray(frameSize * 3 / 2);
    env->SetByteArrayRegion(yuv420sp, 0, frameSize * 3 / 2, yuv420spRaw);
    return yuv420sp;
}