//
// Created by lenovo on 2021/3/22.
//

#ifndef GRAPH3_0_UTILS_H
#define GRAPH3_0_UTILS_H
#include <android/log.h>
#include <jni.h>

#define LOG_TAG "debug"
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args)

//extern char *jstringToChar(JNIEnv *env, jstring jstr);


#endif //GRAPH3_0_UTILS_H
