package com.sandyz.virtualcam.hooks

import android.content.res.XModuleResources
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.InputConfiguration
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Handler
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.sandyz.virtualcam.utils.HookUtils
import com.sandyz.virtualcam.utils.PlayIjk
import com.sandyz.virtualcam.utils.xLog
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 *@author sandyz987
 *@date 2023/11/18
 *@description 抖音和小米相机可用的虚拟摄像头模块
 */

class VirtualCameraWs : IHook {
    override fun getName(): String = "WhatsApp可用的虚拟摄像头模块"
    override fun getSupportedPackages() = listOf(
        "com.whatsapp",
        )

    override fun init(cl: ClassLoader?) {
    }

    override fun registerRes(moduleRes: XModuleResources?) {
    }

    private var nullSurface: Surface? = null
    private var nullSurfaceTex: SurfaceTexture? = null
    private var ijkMediaPlayer: IjkMediaPlayer? = null
    private var virtualSurface: Surface? = null

    // 要用class来hook接口，因为接口不能直接hook
    // 这些callback类设置为全局变量的原因是，如果为null就执行，确保只执行一次（防止多次hook同一个方法）
//    private var deviceStateCallbackClazz: Class<*>? = null
    private var sessionStateCallbackClazz: Class<*>? = null


    private fun newIjkMediaPlayer(): IjkMediaPlayer = IjkMediaPlayer {} // 已经加载库了就不加载了


    private fun resetIjkMediaPlayer() {
        if (ijkMediaPlayer?.isPlaying == true) {
            ijkMediaPlayer?.stop()
        }
        ijkMediaPlayer?.release()
        ijkMediaPlayer = newIjkMediaPlayer()
    }

    private fun resetSurface() {
        xLog("重置surface")
        virtualSurface = null
        nullSurfaceTex?.release()
        nullSurface?.release()
        nullSurfaceTex = SurfaceTexture(15)
        nullSurface = Surface(nullSurfaceTex)
    }


    override fun hook(lpparam: LoadPackageParam?) {
        XposedHelpers.findAndHookMethod(
            "android.media.ImageReader", lpparam!!.classLoader, "newInstance",
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    xLog("创建图片读取器ImageReader.newInstance   宽：${param?.args?.get(0)}，高：${param?.args?.get(1)}，格式：${param?.args?.get(2)}")
                }
            })

        // 标准库，要android9以上，根据系统不同可能走的createCaptureSession不同
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.impl.CameraDeviceImpl", lpparam.classLoader, "createCaptureSession", List::class.java, CameraCaptureSession.StateCallback::class.java, Handler::class.java, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    xLog("whatsapp hook createCaptureSession")

                    val surfaceList = param.args[0] as List<Surface>

                    xLog("whatsapp hook createCaptureSession应用程序创建相机管道?   ")

                    val newSurface = mutableListOf<Surface?>().apply { add(nullSurface) }

                    xLog("应用程序想要添加surfaces: $surfaceList，拦截后只添加：$nullSurface")
                    param.args[0] = newSurface

//                    hookSessionStateCallback(sessionConfiguration.stateCallback.javaClass)

                }
            })
        }


        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "addTarget", Surface::class.java, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                xLog("应用程序向相机添加输出目标addTarget          surface: ${param?.args?.get(0)}")
                xLog("找到屏幕上的surface          surface: ${param?.args?.get(0)}")
                if (virtualSurface == null) { // 如果还不知道屏幕上的surface是哪个的话，说明还没有hook到相机
                    resetSurface()
                    // 应用向相机添加输出目标，说明这个surface就是屏幕上显示的那个view
                    // 所以记录这个surface，播放器播放的内容就往这里面输出就行
                    virtualSurface = param?.args?.get(0) as Surface?
                    resetIjkMediaPlayer()
//                    HookUtils.dumpView(HookUtils.getContentView(), 0)
                    PlayIjk.play(virtualSurface, ijkMediaPlayer)
                }
                // 把相机向应用程序输出的内容定向为虚无
                // 因为createCaptureSession只添加了一个nullSurface，所以addTarget必须只能向nullSurface输出
                param?.args?.set(0, nullSurface)
            }
        })

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "startPreview", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                xLog("应用程序开始预览startPreview         屏幕surface: $virtualSurface")
            }
        })

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "build", object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam?) {
                xLog("应用程序相机请求创建CaptureRequest.Build         屏幕surface: $virtualSurface       ijkm: $ijkMediaPlayer")
                val builder = (param?.thisObject as? CaptureRequest.Builder)
                // 反射获取里面的surfaceset
                val request = XposedHelpers.getObjectField(builder, "mRequest") as CaptureRequest
                val surfaceSet = XposedHelpers.getObjectField(request, "mSurfaceSet") as MutableSet<*>
                xLog("surfaceSet: $surfaceSet")
            }
        })

        val clazz = XposedHelpers.findClass("android.view.TextureView", lpparam.classLoader)
        xLog("TextureView: $clazz")
        XposedBridge.hookAllConstructors(clazz, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam?) {
                xLog("TextureView: ${param?.thisObject}")
            }
        })

    }




}