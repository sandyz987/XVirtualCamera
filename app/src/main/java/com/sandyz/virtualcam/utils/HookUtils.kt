package com.sandyz.virtualcam.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.children
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.sandyz.virtualcam.hooks.IHook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import java.lang.ref.WeakReference
import kotlin.coroutines.CoroutineContext


/**
 *@author sandyz987
 *@date 2023/11/18
 *@description
 */

@SuppressLint("StaticFieldLeak")
object HookUtils {
    var app: Context? = null

    // 获取当前Activity用
    private val activityTop = mutableListOf<WeakReference<Activity>>()
    fun getActivities(): List<Activity> {
        val activities = mutableListOf<Activity>()
        val iterator = activityTop.iterator()
        while (iterator.hasNext()) {
            val activity = iterator.next().get()
            if (activity != null && !activity.isFinishing) {
                activities.add(activity)
            } else {
                iterator.remove()
            }
        }
        return activities
    }

    fun getTopActivity(): Activity? {
        val activities = getActivities()
        return if (activities.isEmpty()) {
            null
        } else {
            activities[0]
        }
    }

    fun getLifecycle(): Lifecycle? {
        // 反射获取lifecycle提高成功率
        val activity = getTopActivity()
        mutableListOf(
            "androidx.lifecycle.LifecycleOwner",
            "android.arch.lifecycle.LifecycleOwner",
            "android.support.v4.app.FragmentActivity",
            "android.support.v4.app.SupportActivity",
            "androidx.fragment.app.FragmentActivity",
            "androidx.appcompat.app.AppCompatActivity",
            "androidx.activity.ComponentActivity",
            "androidx.core.app.ComponentActivity",
        ).forEach {
            try {
                val clazz = try {
                    XposedHelpers.findClass(it, activity?.classLoader)
                } catch (t: Throwable) {
                    Class.forName(it)
                }
                val activityCast = clazz?.cast(activity)
                val function = clazz?.getDeclaredMethod("getLifecycle")
                function?.isAccessible = true
                val lifecycle = function?.invoke(activityCast) as? Lifecycle
                if (lifecycle != null) {
                    return lifecycle
                } else {
                    xLog("lifecycle is null")
                }
            } catch (t: Throwable) {
                xLog(t.toString())
            }
        }
        return null
    }


    private val coroutineScopeMap = HashMap<Activity, CoroutineScope>()

    fun coroutineScope(): CoroutineScope = if (coroutineScopeMap[getTopActivity()] != null) {
        coroutineScopeMap[getTopActivity()]!!
    } else {
        MyCoroutineScope().also {
            xLog("activity: ${getTopActivity()}")
            xLog("lifecycle2: ${getLifecycle()}")
            val activity = getTopActivity()?: return@also
            val activityLifecycle = getLifecycle()?: return@also
            val lifecycleEventObserver = object :LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        it.cancel()
                        activityLifecycle.removeObserver(this)
                        coroutineScopeMap.remove(activity)
                    }
                }
            }
            activityLifecycle.addObserver(lifecycleEventObserver)
            coroutineScopeMap[activity] = it
        }
    }

    fun getView(): View? = getTopActivity()?.window?.decorView

    fun getContentView(): ViewGroup? = getView()?.findViewById(android.R.id.content) as? ViewGroup

    fun dumpView(v: View?, depth: Int) {
        v ?: return
        xLog("${"  ".repeat(depth)}${v.javaClass.name}")
        if (v is ViewGroup) {
            v.children.forEach {
                dumpView(it, depth + 1)
            }
        }
    }

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        val instrumentation = XposedHelpers.findClass(
            "android.app.Instrumentation", lpparam.classLoader
        )
        XposedBridge.hookAllMethods(instrumentation, "callApplicationOnCreate", object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                app = param.args[0] as Context
            }
        })

        val activity = XposedHelpers.findClass(
            "android.app.Activity", lpparam.classLoader
        )
        XposedBridge.hookAllConstructors(activity, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                if (!getActivities().contains(param.thisObject)) {
                    activityTop.add(0, WeakReference(param.thisObject as Activity))
                }
            }
        })
    }

}

fun IHook.xLog(msg: String?) {
    XposedBridge.log("[${this::class.java.simpleName} ${Thread.currentThread().id}] $msg")
}

fun xLog(msg: String?) {
    XposedBridge.log("[${Thread.currentThread().id}] $msg")
}

fun xLog(param: XC_MethodHook.MethodHookParam?, msg: String?, depth: Int = 15) {
    xLog(msg)
    if (param == null) {
        return
    }
    val stackTrace = Thread.currentThread().stackTrace as Array<StackTraceElement>
    stackTrace.forEachIndexed { index, stackTraceElement ->
        if (stackTraceElement.className.equals("LSPHooker_")) {
            for (i in index + 1..index + depth) {
                if (i < stackTrace.size) {
                    xLog("          ${stackTrace[i].className}.${stackTrace[i].methodName}")
                }
            }
        }
    }
}

fun xLogTrace(param: XC_MethodHook.MethodHookParam?, msg: String?) {
    if (param == null) {
        xLog(msg)
        return
    }
    xLog(msg)
    val stackTrace = Thread.currentThread().stackTrace as Array<StackTraceElement>
    stackTrace.forEach {
        xLog("          ${it.className}.${it.methodName}")

    }
}

fun toast(context: Context?, text: CharSequence, duration: Int) {
    try {
        context?.let {
            Toast.makeText(it, text, duration).show()
        }
    } catch (e: Throwable) {
        xLog("toast: $text")
    }
}

class MyCoroutineScope: CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.IO +
            job +
            CoroutineName("MyCoroutineScope") +
            CoroutineExceptionHandler{ coroutineContext, throwable ->
                xLog("coroutineException in $coroutineContext: $throwable")
            }
}