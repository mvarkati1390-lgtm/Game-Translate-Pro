package com.gametranslatepro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
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
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class LiveTranslateService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var windowManager: WindowManager? = null
    private var overlay: LinearLayout? = null
    private var output: TextView? = null
    private var processing = false
    private var lastFrameAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var worker: HandlerThread? = null
    private var handler: Handler? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START) return START_NOT_STICKY
        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
        val data = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(EXTRA_DATA)
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTIFICATION_ID, buildNotification())
        startCapture(resultCode, data)
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        try {
            worker = HandlerThread("GameTranslateOCR").also { it.start() }
            handler = Handler(worker!!.looper)
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader?.setOnImageAvailableListener({ source -> processLatestFrame(source) }, handler)
            display = projection?.createVirtualDisplay(
                "GameTranslateLiveOCR", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface, null, handler
            )
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopCapture(); stopSelf() }
            }, handler)
            showOverlay("ترجمه زنده آماده است؛ متن انگلیسی روی صفحه را می‌خوانم…")
        } catch (_: Exception) {
            stopCapture()
            stopSelf()
        }
    }

    private fun processLatestFrame(source: ImageReader) {
        val now = SystemClock.elapsedRealtime()
        if (processing || now - lastFrameAt < 1400) {
            val skipped = try { source.acquireLatestImage() } catch (_: Exception) { null }
            skipped?.close()
            return
        }
        val image = try { source.acquireLatestImage() } catch (_: Exception) { null } ?: return
        val bitmap = imageToBitmap(image)
        image.close()
        if (bitmap == null) return
        processing = true
        lastFrameAt = now
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text ->
                val lines = text.textBlocks.flatMap { it.lines }.map { it.text.trim() }.filter { it.isNotEmpty() }
                val translated = lines.map { line -> FinglishTranslator.translate(line) }
                val displayText = if (lines.isEmpty()) "متن انگلیسی پیدا نشد." else
                    lines.zip(translated).joinToString("\n") { (original, fa) -> "$original\n← $fa" }
                handler?.post { output?.text = displayText }
            }
            .addOnFailureListener { handler?.post { output?.text = "خواندن متن ناموفق بود؛ دوباره تلاش می‌کنم." } }
            .addOnCompleteListener {
                bitmap.recycle()
                processing = false
            }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val paddedWidth = rowStride / pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            padded.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
            padded.recycle()
            cropped
        } catch (_: Exception) { null }
    }

    private fun showOverlay(message: String) {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = wm
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 16, 22, 16)
            setBackgroundColor(0xEE142238.toInt())
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "GAME TRANSLATE • زنده"
            setTextColor(0xFF65D6FF.toInt())
            textSize = 15f
        }, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(Button(this).apply {
            text = "بستن"
            setOnClickListener { stopCapture(); stopSelf() }
        })
        panel.addView(header)
        val scroll = ScrollView(this)
        output = TextView(this).apply {
            text = message
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            setPadding(4, 12, 4, 8)
        }
        scroll.addView(output)
        panel.addView(scroll, LinearLayout.LayoutParams(-1, 240))
        val params = WindowManager.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 70
        }
        try { wm.addView(panel, params); overlay = panel } catch (_: Exception) { stopCapture(); stopSelf() }
    }

    private fun buildNotification(): Notification {
        val channelId = "game_translate_live"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "ترجمه زنده بازی", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("Game Translate Pro")
            .setContentText("ترجمه زنده صفحه فعال است")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setOngoing(true).build()
    }

    private fun stopCapture() {
        try { display?.release() } catch (_: Exception) {}
        display = null
        try { reader?.setOnImageAvailableListener(null, null); reader?.close() } catch (_: Exception) {}
        reader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        try { overlay?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        overlay = null
        output = null
        try { recognizer.close() } catch (_: Exception) {}
        try { worker?.quitSafely() } catch (_: Exception) {}
        worker = null
        handler = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() { stopCapture(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.gametranslatepro.LIVE_START"
        const val ACTION_STOP = "com.gametranslatepro.LIVE_STOP"
        const val EXTRA_RESULT_CODE = "live_result_code"
        const val EXTRA_DATA = "live_permission_data"
        private const val NOTIFICATION_ID = 72
    }
}
