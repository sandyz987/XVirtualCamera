package com.sandyz.virtualcam.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.DrawableRes

/**
 * 测试用
 */
object BitmapLoader {
    fun decodeBitmapFromResource(
        res: Resources?,
        @DrawableRes resId: Int,
        decodeWidth: Int,
        decodeHeight: Int
    ): Bitmap? {
        if (resId == 0) {
            return null
        }
        if (decodeHeight <= 0 || decodeWidth <= 0) {
            return null
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true //预加载
        BitmapFactory.decodeResource(res, resId, options)
        val imgWidth = options.outWidth //要加载的图片的宽
        val imgHeight = options.outHeight //要加载的图片的高
        var inSampleSize = 1
        if (imgWidth > decodeWidth || imgHeight > decodeHeight) {
            val halfWidth = imgWidth / 2
            val halfHeight = imgHeight / 2
            while (halfWidth / inSampleSize >= decodeWidth &&
                halfHeight / inSampleSize >= decodeHeight
            ) {
                inSampleSize *= 2
            }
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeResource(res, resId, options) ?: return null
        val matrix = Matrix().apply {
            postScale((decodeWidth / bitmap.width.toFloat()), (decodeHeight / bitmap.height.toFloat()))
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun decodeBitmapFromResourceByWidth(
        res: Resources?,
        @DrawableRes resId: Int,
        decodeWidth: Int
    ): Bitmap? {
        if (resId == 0) {
            return null
        }
        if (decodeWidth <= 0) {
            return null
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true //预加载
        BitmapFactory.decodeResource(res, resId, options)
        val imgWidth = options.outWidth //要加载的图片的宽
        val imgHeight = options.outHeight //要加载的图片的高
        val heightDivWidth = imgHeight / imgWidth.toFloat()
        val decodeHeight = decodeWidth * heightDivWidth
        var inSampleSize = 1
        if (imgWidth > decodeWidth || imgHeight > decodeHeight) {
            val halfWidth = imgWidth / 2
            val halfHeight = imgHeight / 2
            while (halfWidth / inSampleSize >= decodeWidth &&
                halfHeight / inSampleSize >= decodeHeight
            ) {
                inSampleSize *= 2
            }
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeResource(res, resId, options) ?: return null
        val matrix = Matrix().apply {
            postScale((decodeWidth.toFloat() / bitmap.width), (decodeHeight / bitmap.height))
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun decodeBitmapFromResourceByHeight(
        res: Resources?,
        @DrawableRes resId: Int,
        decodeHeight: Int
    ): Bitmap? {
        if (resId == 0) {
            return null
        }
        if (decodeHeight <= 0) {
            return null
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true //预加载
        BitmapFactory.decodeResource(res, resId, options)
        val imgWidth = options.outWidth //要加载的图片的宽
        val imgHeight = options.outHeight //要加载的图片的高
        val widthDivHeight = imgWidth / imgHeight.toFloat()
        val decodeWidth = decodeHeight * widthDivHeight
        var inSampleSize = 1
        if (imgWidth > decodeWidth || imgHeight > decodeHeight) {
            val halfWidth = imgWidth / 2
            val halfHeight = imgHeight / 2
            while (halfWidth / inSampleSize >= decodeWidth &&
                halfHeight / inSampleSize >= decodeHeight
            ) {
                inSampleSize *= 2
            }
        }
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        val bitmap = BitmapFactory.decodeResource(res, resId, options) ?: return null
        val matrix = Matrix().apply {
            postScale((decodeWidth / bitmap.width), (decodeHeight / bitmap.height.toFloat()))
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun resizeBitmap(bitmapSrc: Bitmap?, targetWidth: Int, targetHeight: Int): Bitmap? {
        val bitmap = bitmapSrc ?: return null
        val matrix = Matrix().apply {
            postScale((targetWidth / bitmap.width.toFloat()), (targetHeight / bitmap.height.toFloat()))
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}