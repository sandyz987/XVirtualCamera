package com.sandyz.virtualcam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException

/**
 * 逻辑全都被注释了，现在这个Activity还没什么用
 * 因为之前设置url的方式有问题（权限）
 *
 * 待设计一个方便配置各个app虚拟摄像头视频的ui界面
 */
class MainActivity : AppCompatActivity() {


    companion object {
        var URI_PATH_ROOT = ""
        const val URI_FILE_NAME = "stream.txt"
        var uriString = ""

        fun getUri(): String {
            var uri = ""
            try {
                File(URI_PATH_ROOT).let {
                    if (!it.exists())
                        it.mkdirs()
                }
                File(URI_PATH_ROOT + URI_FILE_NAME).let {
                    if (!it.exists()) {
                        it.createNewFile()
                    }
                    val reader = BufferedReader(FileReader(it))
                    uri = reader.readLine() ?: ""
                    reader.close()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return e.toString()
            }
            return uri.also { uriString = it }
        }
    }

    private var afterRequestPermissionCallback: (() -> Unit)? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val btnLocal = findViewById<Button>(R.id.btn_local)
        val btnSav = findViewById<Button>(R.id.btn_sav)
        val etUri = findViewById<EditText>(R.id.et_uri)

        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                holder.lockCanvas()?.let {
                    it.drawColor(0xffff0000.toInt())
                    it.drawText("Hello World", 100f, 100f, TextView(this@MainActivity).paint)
                    holder.unlockCanvasAndPost(it)
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })


//        doIfPermission {
//            etUri.setText(getUri())
//        }

        btnSav.setOnClickListener {
            doIfPermission {
                val dir = File(URI_PATH_ROOT)
                if (!dir.exists()) {
                    val mkd = dir.mkdirs()
                    Toast.makeText(this@MainActivity, "创建目录：$mkd", Toast.LENGTH_SHORT).show()
                }
                etUri.setText(etUri.text.toString().replace("https", "http").replace("\n", ""))
                val uri = etUri.text.toString()
                uriString = uri

                val file = File(URI_PATH_ROOT + URI_FILE_NAME)

                try {
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                    val fos = FileOutputStream(file)
                    fos.write(uri.toByteArray())
                    fos.close()
                    Toast.makeText(this@MainActivity, "设置成功，内容保存在：$URI_PATH_ROOT$URI_FILE_NAME", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "设置失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private fun doIfPermission(callback: (() -> Unit)?) {
        if (!hasPermission()) {
            requestPermission()
            afterRequestPermissionCallback = callback
        } else {
            callback?.invoke()
        }
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
            ) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }
    }

    private fun hasPermission(): Boolean {
        val bool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED
                    && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED)
        } else true
        if (!bool) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
        return bool
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && requestCode == 1) {
            if (grantResults[0] != PackageManager.PERMISSION_DENIED) {
                if (afterRequestPermissionCallback != null) {
                    afterRequestPermissionCallback?.invoke()
                    afterRequestPermissionCallback = null
                } else {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


}