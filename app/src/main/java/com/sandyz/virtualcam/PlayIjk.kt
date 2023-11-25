package com.sandyz.virtualcam

import android.view.Surface
import android.widget.Toast
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 *@author sandyz987
 *@date 2023/11/27
 *@description
 */

object PlayIjk {
    /**
     * 播放视频总逻辑
     * vSurface: 要播放虚拟视频的surface
     * ijkMP: 播放器
     */
    fun play(vSurface: Surface?, ijkMP: IjkMediaPlayer?) {
        if (vSurface == null) {
            xLog("播放失败，virtualSurface为空！")
            toast(HookUtils.app, "播放失败！", Toast.LENGTH_SHORT)
            return
        } else if (ijkMP == null) {
            xLog("播放失败，ijkMediaPlayer为空！")
            toast(HookUtils.app, "播放失败！", Toast.LENGTH_SHORT)
            return
        }
        val filePath = HookUtils.app?.externalCacheDir?.path?.toString() + "/stream.txt"
        var urlStr = ""
        try {
            File(filePath).let {
                if (!it.exists()) {
                    it.createNewFile()
                }
                val reader = BufferedReader(FileReader(it))
                urlStr = reader.readLine() ?: ""
                reader.close()
            }
        } catch (e: Exception) {
            toast(HookUtils.app, "读取失败！", Toast.LENGTH_SHORT)
        }
        if (urlStr.isBlank()) {
            urlStr = HookUtils.app?.externalCacheDir?.path?.toString() + "/virtual.mp4"
            if (File(urlStr).exists()) {
                toast(HookUtils.app, "播放本地视频：$urlStr", Toast.LENGTH_LONG)
            } else {
                toast(HookUtils.app, "请前往${filePath}输入视频地址！或者查看插件使用说明！", Toast.LENGTH_LONG)
                return
            }
        } else {
            urlStr = urlStr.replace("https", "http")
        }
        vSurface.let {
            ijkMP.setSurface(it)
            ijkMP.isLooping = true
            ijkMP.dataSource = urlStr
            ijkMP.prepareAsync()
            ijkMP.setOnPreparedListener {
                ijkMP.start()
            }
        }
        toast(HookUtils.app, "开始播放，ijk:$ijkMP，surface:$vSurface url:$urlStr", Toast.LENGTH_SHORT)
        xLog("currentActivity: ${HookUtils.getActivities()}, currentTopActivity: ${HookUtils.getTopActivity()}")
    }
}