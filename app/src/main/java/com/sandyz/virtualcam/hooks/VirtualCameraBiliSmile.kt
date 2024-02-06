package com.sandyz.virtualcam.hooks

import android.content.res.XModuleResources
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.os.Build
import android.os.Handler
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import com.sandyz.virtualcam.jni.EncoderJNI
import com.sandyz.virtualcam.utils.HookUtils
import com.sandyz.virtualcam.utils.PlayIjk
import com.sandyz.virtualcam.utils.xLog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import kotlin.math.min

/**
 *@author sandyz987
 *@date 2023/11/18
 *@description b站、快手、微信视频号能用的虚拟摄像头
 */

class VirtualCameraBiliSmile : IHook {
    override fun getName(): String = "b站、快手能用的虚拟摄像头"
    override fun getSupportedPackages() = listOf(
        "tv.danmaku.bili",
        "com.smile.gifmaker",
        "com.tencent.mm",
    )

    override fun init(cl: ClassLoader?) {
    }

    override fun registerRes(moduleRes: XModuleResources?) {
    }

    private val fps = 30

    private var lastDrawTimestamp = 0L

    private var previewCallbackClazz: Class<*>? = null

    private var virtualSurfaceView: SurfaceView? = null

    private var camera: Camera? = null

    @Volatile
    private var width = 0

    @Volatile
    private var height = 0

    @Volatile
    private var yuvByteArray: ByteArray? = null

    private var drawJob: Job? = null

    private var bitmap: Bitmap? = null

    private var ijkMediaPlayer: IjkMediaPlayer? = null

    override fun hook(lpparam: LoadPackageParam?) {

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam?.classLoader, "startPreview", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                xLog("应用程序开始预览startPreview    topActivity:${HookUtils.getTopActivity()}")
                stopPreview()
                startPreview()
                HookUtils.dumpView(HookUtils.getContentView(), 0)
            }
        })

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam?.classLoader, "stopPreview", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                xLog("应用程序停止预览stopPreview   ")
                stopPreview()
            }
        })

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam?.classLoader, "setPreviewCallbackWithBuffer", PreviewCallback::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (previewCallbackClazz == null) {
                    previewCallbackClazz = param.args[0].javaClass
                    XposedHelpers.findAndHookMethod(previewCallbackClazz, "onPreviewFrame", ByteArray::class.java, Camera::class.java, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam?) {
                            if (drawJob?.isActive != true) return
                            camera = param?.args?.get(1) as Camera
                            width = camera?.parameters?.previewSize?.width ?: 0
                            height = camera?.parameters?.previewSize?.height ?: 0
                            xLog("onPreviewFrameWithBuffer             package:${lpparam?.packageName}          process:${lpparam?.processName}          bytearray:${param.args?.get(0)}")
                            if (yuvByteArray != null) {
                                val byteArray = param.args?.get(0) as ByteArray
                                // copy the yuvByteArray to byteArray
                                yuvByteArray?.let {
                                    System.arraycopy(it, 0, byteArray, 0, min(byteArray.size, it.size))
                                }
                            }
                        }
                    })
                }

            }
        })

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam?.classLoader, "setPreviewCallback", PreviewCallback::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                if (previewCallbackClazz == null) {
                    previewCallbackClazz = param.args[0].javaClass
                    XposedHelpers.findAndHookMethod(previewCallbackClazz, "onPreviewFrame", ByteArray::class.java, Camera::class.java, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam?) {
                            xLog("onPreviewFrame             package:${lpparam?.packageName}          process:${lpparam?.processName}          bytearray:${param?.args?.get(0)}")
                            // clear the bytearray
                            val byteArray = param?.args?.get(0) as ByteArray
                            byteArray.forEachIndexed { index, _ ->
                                byteArray[index] = 0
                            }
                        }
                    })
                }
            }
        })
    }

    private suspend fun drawer() {
        while (true) {
            try {
                if (width == 0 || height == 0) {
                    delay(1000L / fps.toLong())
                    continue
                }
                lastDrawTimestamp = System.currentTimeMillis()
                /**
                 * 获取ijkMediaPlayer的图像
                 */
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                virtualSurfaceView?.let {
                    getBitmapByView(it)
                }
                bitmap = getRotateBitmap(bitmap, -90f, width, height)
                xLog("rotatedbitmap:$bitmap, width:${bitmap?.width}, height:${bitmap?.height}")
                yuvByteArray = bitmap?.let { bitmapToYuv((it), width, height) }

                // 根据帧率计算休眠时间
                xLog("复制Surface内容耗时: ${System.currentTimeMillis() - lastDrawTimestamp} bitmap:$bitmap, width:$width, height:$height")
                if (System.currentTimeMillis() < lastDrawTimestamp + 1000L / fps.toLong())
                    delay(lastDrawTimestamp + 1000L / fps.toLong() - System.currentTimeMillis())
            } catch (e: IllegalArgumentException) {
                xLog("exception:${e}")
                e.printStackTrace()
                stopPreview()
            }
        }
    }


    private fun getRotateBitmap(bitmap: Bitmap?, rotateDegree: Float, width: Int, height: Int): Bitmap? {
        bitmap ?: return null
        val matrix = Matrix()
        matrix.postRotate(rotateDegree)
        matrix.postScale(width / height.toFloat(), height / width.toFloat())
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width,
            bitmap.height, matrix, false
        )
    }


    private fun newIjkMediaPlayer(): IjkMediaPlayer = IjkMediaPlayer {} // 已经加载库了就不加载了

    private fun resetIjkMediaPlayer() {
        if (ijkMediaPlayer?.isPlaying == true) {
            ijkMediaPlayer?.stop()
        }
        ijkMediaPlayer?.release()
        ijkMediaPlayer = newIjkMediaPlayer()
    }

    private fun startPreview() {
        xLog("开启预览线程1")
        drawJob = HookUtils.coroutineScope().launch {
            drawer()
        }
        if (virtualSurfaceView == null) {
            virtualSurfaceView = SurfaceView(HookUtils.getTopActivity())
            HookUtils.getTopActivity()?.runOnUiThread {
                virtualSurfaceView ?: return@runOnUiThread
                HookUtils.getContentView()?.addView(virtualSurfaceView)
                HookUtils.getContentView()?.getChildAt(0)?.bringToFront()
                virtualSurfaceView?.layoutParams = FrameLayout.LayoutParams(2, 2)
            }
            virtualSurfaceView?.visibility = View.VISIBLE
            virtualSurfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    resetIjkMediaPlayer()
                    PlayIjk.play(holder.surface, ijkMediaPlayer)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {

                }
            })
        } else {
            resetIjkMediaPlayer()
            PlayIjk.play(virtualSurfaceView?.holder?.surface, ijkMediaPlayer)
        }
    }

    private fun stopPreview() {
        drawJob?.cancel()
        resetIjkMediaPlayer()
        if (virtualSurfaceView != null) {
            HookUtils.getTopActivity()?.runOnUiThread {
                HookUtils.getContentView()?.removeView(virtualSurfaceView)
            }
            virtualSurfaceView = null
        }
    }

    private fun bitmapToYuv(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val intArray = IntArray(width * height)
        bitmap.getPixels(intArray, 0, width, 0, 0, width, height)
//        val yuvByteArray = ByteArray(width * height * 3 / 2)
//        encodeYUV420SP(yuvByteArray, intArray, width, height)
        val yuvByteArray = EncoderJNI.encodeYUV420SP(intArray, width, height)
        if (yuvByteArray == null) xLog("为空")
        return yuvByteArray
    }

    // https://stackoverflow.com/questions/5960247/convert-bitmap-array-to-yuv-ycbcr-nv21
    // ps: Why does copilot know that I copied this code from stackoverflow?
    // 改native性能应该可以优化一点，现在有点卡
    private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var a: Int
        var R: Int
        var G: Int
        var B: Int
        var Y: Int
        var U: Int
        var V: Int
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                a = argb[index] and -0x1000000 shr 24 // a is not used obviously
                R = argb[index] and 0xff0000 shr 16
                G = argb[index] and 0xff00 shr 8
                B = argb[index] and 0xff shr 0

                // well known RGB to YUV algorithm
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (if (Y < 0) 0 else if (Y > 255) 255 else Y).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (if (V < 0) 0 else if (V > 255) 255 else V).toByte()
                    yuv420sp[uvIndex++] = (if (U < 0) 0 else if (U > 255) 255 else U).toByte()
                }
                index++
            }
        }
    }

    private fun getBitmapByView(surfaceView: SurfaceView) {
        val bitmap = bitmap ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 同步或者异步，不同设备可能不同
            PixelCopy.request(surfaceView, bitmap, { copyResult ->
                if (copyResult == PixelCopy.SUCCESS) {
                    xLog("get bitmap success")
                } else {
                    xLog("get bitmap failed")
                }
            }, Handler(HookUtils.getTopActivity()?.mainLooper!!))
        }
    }

}