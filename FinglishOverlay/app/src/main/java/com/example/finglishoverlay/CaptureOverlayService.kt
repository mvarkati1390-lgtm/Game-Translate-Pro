package com.example.finglishoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class CaptureOverlayService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL_ID = "finglish_capture"
        private const val NOTIFICATION_ID = 77
    }
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var overlay: TextView? = null
    private var windowManager: WindowManager? = null
    private var workerThread: HandlerThread? = null
    private var worker: Handler? = null
    private val processing = AtomicBoolean(false)
    private var lastText = ""
    private var screenWidth = 0
    private var screenHeight = 0
    private var density = 1
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        workerThread = HandlerThread("finglish-ocr").also { it.start() }
        worker = Handler(workerThread!!.looper)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()
        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels; screenHeight = metrics.heightPixels; density = metrics.densityDpi
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java) else {
            @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultData == null) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, resultData)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, worker)
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                if (!processing.compareAndSet(false, true)) { image.close(); return@setOnImageAvailableListener }
                worker?.post {
                    try { runOcr(imageToBitmap(image)) } catch (_: Exception) { processing.set(false) }
                    finally { image.close() }
                }
            }, worker)
            virtualDisplay = projection?.createVirtualDisplay("FinglishOverlayCapture", screenWidth, screenHeight, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader?.surface, null, worker)
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]; val buffer = plane.buffer
        val paddedWidth = plane.rowStride / plane.pixelStride
        val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        buffer.rewind(); padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        if (cropped !== padded) padded.recycle()
        return cropped
    }

    private fun runOcr(bitmap: Bitmap) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result ->
                val text = result.textBlocks.joinToString("\n") { block -> block.lines.joinToString(" ") { it.text } }.trim()
                if (text.isNotBlank() && text != lastText) {
                    lastText = text
                    FinglishTranslator.translateHybrid(text) { translated -> worker?.post { updateOverlay(text, translated) } }
                }
            }
            .addOnCompleteListener { if (!bitmap.isRecycled) bitmap.recycle(); processing.set(false) }
    }

    private fun createOverlay() {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        overlay = TextView(this).apply {
            text = "مترجم فعال است؛ منتظر نوشته‌های بازی..."
            textSize = 15f; setTextColor(0xFFFFFFFF.toInt()); setBackgroundColor(0xCC101010.toInt())
            setPadding(18, 12, 18, 12); textDirection = TextView.TEXT_DIRECTION_RTL
        }
        val params = WindowManager.LayoutParams((resources.displayMetrics.widthPixels * 0.88f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (resources.displayMetrics.density * 48).toInt()
        }
        try { windowManager?.addView(overlay, params) } catch (_: Exception) { stopSelf() }
    }

    private fun updateOverlay(original: String, translated: String) { overlay?.text = "$translated\n\n──────────\nاصل: $original" }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "مترجم شناور", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("مترجم شناور فعال است").setContentText("برای توقف، برنامه مترجم را باز کنید.")
        .setSmallIcon(android.R.drawable.ic_menu_search).setOngoing(true).build()

    override fun onDestroy() {
        try {
            virtualDisplay?.release(); imageReader?.close(); projection?.stop()
            overlay?.let { windowManager?.removeView(it) }; recognizer.close(); workerThread?.quitSafely()
        } catch (_: Exception) {}
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
