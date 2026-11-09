package com.example.finglishoverlay

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCapture = 4102
    private val requestOverlay = 4103

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(0xFF101010.toInt())
        }
        val title = TextView(this).apply {
            text = "مترجم شناور فینگلیش"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            textDirection = TextView.TEXT_DIRECTION_RTL
        }
        val info = TextView(this).apply {
            text = "برای خواندن نوشته‌های بازی، اجازه ضبط صفحه و نمایش روی برنامه‌های دیگر لازم است. تصویر صفحه فقط برای OCR روی گوشی پردازش می‌شود."
            textSize = 15f
            setTextColor(0xFFE0E0E0.toInt())
            setPadding(0, 18, 0, 22)
            textDirection = TextView.TEXT_DIRECTION_RTL
        }
        val start = Button(this).apply {
            text = "فعال‌کردن مترجم شناور"
            setOnClickListener { beginSetup() }
        }
        val stop = Button(this).apply {
            text = "خاموش‌کردن مترجم"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureOverlayService::class.java))
                Toast.makeText(this@MainActivity, "مترجم خاموش شد", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(title); root.addView(info); root.addView(start); root.addView(stop)
        setContentView(root)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 501)
    }

    private fun beginSetup() {
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), requestOverlay)
            return
        }
        requestScreenCapture()
    }

    private fun requestScreenCapture() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(manager.createScreenCaptureIntent(), requestCapture)
    }

    @Deprecated("Activity result API kept minimal for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestOverlay) {
            if (Settings.canDrawOverlays(this)) requestScreenCapture()
            else Toast.makeText(this, "مجوز نمایش روی برنامه‌ها لازم است", Toast.LENGTH_LONG).show()
        } else if (requestCode == requestCapture) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, CaptureOverlayService::class.java).apply {
                    putExtra(CaptureOverlayService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(CaptureOverlayService.EXTRA_RESULT_DATA, data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
                Toast.makeText(this, "مترجم فعال شد؛ به بازی برگردید", Toast.LENGTH_LONG).show()
                moveTaskToBack(true)
            } else Toast.makeText(this, "اجازه ضبط صفحه داده نشد", Toast.LENGTH_LONG).show()
        }
    }
}
