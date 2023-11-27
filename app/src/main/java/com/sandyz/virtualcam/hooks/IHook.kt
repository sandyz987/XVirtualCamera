package com.sandyz.virtualcam.hooks

import android.content.res.XModuleResources
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam

/**
 *@author sandyz987
 *@date 2023/11/18
 *@description
 */

interface IHook {
    fun getSupportedPackages(): List<String>
    fun getName(): String
    fun hook(lpparam: LoadPackageParam?)
    fun init(cl: ClassLoader?)
    fun registerRes(moduleRes: XModuleResources?)
}