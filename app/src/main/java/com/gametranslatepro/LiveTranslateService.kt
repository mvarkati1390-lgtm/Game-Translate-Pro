package com.gametranslatepro

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.nio.ByteBuffer

class LiveTranslateService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var windowManager: WindowManager? = null
    private var overlay: LinearLayout? = null
    private var output: TextView? = null
    @Volatile private var processing = false
    private var lastOcrAt = 0L
    private var lastEncodeAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var worker: HandlerThread? = null
    private var handler: Handler? = null

    private var encoder: MediaCodec? = null
    private var muxer: MediaMuxer? = null
    private var muxerStarted = false
    private var trackIndex = -1
    private var outputFile: File? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var yuvFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
    private val bufferInfo = MediaCodec.BufferInfo()

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
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            startCapture(resultCode, data)
        } catch (_: Exception) {
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        worker = HandlerThread("GameTranslateOCR").also { it.start() }
        handler = Handler(worker!!.looper)
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        val metrics = resources.displayMetrics
        videoWidth = (metrics.widthPixels / 2) * 2
        videoHeight = (metrics.heightPixels / 2) * 2
        startVideoEncoder()
        reader = ImageReader.newInstance(videoWidth, videoHeight, PixelFormat.RGBA_8888, 3)
        reader?.setOnImageAvailableListener({ source -> processLatestFrame(source) }, handler)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopCapture()
                stopSelf()
            }
        }, handler)
        display = projection?.createVirtualDisplay(
            "GameTranslateCombinedCapture", videoWidth, videoHeight, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader?.surface, null, handler
        )
        if (display == null) throw IllegalStateException("Virtual display unavailable")
        showOverlay("ترجمه و ضبط هم‌زمان فعال شد…")
    }

    private fun startVideoEncoder() {
        val folder = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "GameTranslatePro")
        if (!folder.exists()) folder.mkdirs()
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        outputFile = File(folder, "GameTranslate_$stamp.mp4")

        val codecName = MediaCodecListCompat.findAvcEncoder(yuvFormat)
        val codec = if (codecName != null) MediaCodec.createByCodecName(codecName) else MediaCodec.createEncoderByType("video/avc")
        val caps = codec.codecInfo.getCapabilitiesForType("video/avc")
        val formats = caps.colorFormats
        yuvFormat = when {
            formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            formats.contains(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
            else -> throw IllegalStateException("No supported byte-buffer YUV encoder")
        }
        val format = MediaFormat.createVideoFormat("video/avc", videoWidth, videoHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, yuvFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 15)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
        }
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        encoder = codec
        muxer = MediaMuxer(outputFile!!.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    private fun processLatestFrame(source: ImageReader) {
        val image = try { source.acquireLatestImage() } catch (_: Exception) { null } ?: return
        try {
            val now = SystemClock.elapsedRealtime()
            if (now - lastEncodeAt >= 66L) {
                encodeFrame(image)
                lastEncodeAt = now
            }
            if (!processing && now - lastOcrAt >= 1400L) {
                val bitmap = imageToBitmap(image)
                if (bitmap != null) {
                    processing = true
                    lastOcrAt = now
                    recognizer.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { text ->
                            val lines = text.textBlocks.flatMap { it.lines }.map { it.text.trim() }.filter { it.isNotEmpty() }
                            val translated = lines.map { FinglishTranslator.translate(it) }
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
            }
        } finally {
            image.close()
        }
        drainEncoder(false)
    }

    private fun encodeFrame(image: Image) {
        val codec = encoder ?: return
        val inputIndex = codec.dequeueInputBuffer(0)
        if (inputIndex < 0) return
        val input = codec.getInputBuffer(inputIndex) ?: return
        input.clear()
        if (!writeYuv(image, input)) {
            codec.queueInputBuffer(inputIndex, 0, 0, System.nanoTime() / 1000L, 0)
            return
        }
        val size = input.position()
        codec.queueInputBuffer(inputIndex, 0, size, System.nanoTime() / 1000L, 0)
    }

    private fun writeYuv(image: Image, out: ByteBuffer): Boolean {
        return try {
            val plane = image.planes[0]
            val src = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = image.width
            val height = image.height
            val ySize = width * height
            val chromaSize = ySize / 4
            if (out.remaining() < ySize + chromaSize * 2) return false
            val yStart = out.position()
            for (row in 0 until height) {
                for (col in 0 until width) {
                    val p = row * rowStride + col * pixelStride
                    val r = src.get(p).toInt() and 0xff
                    val g = src.get(p + 1).toInt() and 0xff
                    val b = src.get(p + 2).toInt() and 0xff
                    val y = (((66 * r + 129 * g + 25 * b + 128) shr 8) + 16).coerceIn(0, 255)
                    out.put(y.toByte())
                }
            }
            val chromaStart = yStart + ySize
            if (yuvFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                for (row in 0 until height step 2) {
                    for (col in 0 until width step 2) {
                        var uSum = 0
                        var vSum = 0
                        for (dy in 0..1) for (dx in 0..1) {
                            val p = (row + dy) * rowStride + (col + dx) * pixelStride
                            val r = src.get(p).toInt() and 0xff
                            val g = src.get(p + 1).toInt() and 0xff
                            val b = src.get(p + 2).toInt() and 0xff
                            uSum += (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                            vSum += (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                        }
                        out.put((uSum / 4).toByte())
                        out.put((vSum / 4).toByte())
                    }
                }
            } else {
                val uPos = chromaStart
                val vPos = chromaStart + chromaSize
                val base = out.position()
                val uValues = ByteArray(chromaSize)
                val vValues = ByteArray(chromaSize)
                var index = 0
                for (row in 0 until height step 2) {
                    for (col in 0 until width step 2) {
                        var uSum = 0
                        var vSum = 0
                        for (dy in 0..1) for (dx in 0..1) {
                            val p = (row + dy) * rowStride + (col + dx) * pixelStride
                            val r = src.get(p).toInt() and 0xff
                            val g = src.get(p + 1).toInt() and 0xff
                            val b = src.get(p + 2).toInt() and 0xff
                            uSum += (((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128).coerceIn(0, 255)
                            vSum += (((112 * r - 94 * g - 18 * b + 128) shr 8) + 128).coerceIn(0, 255)
                        }
                        uValues[index] = (uSum / 4).toByte()
                        vValues[index] = (vSum / 4).toByte()
                        index++
                    }
                }
                out.position(uPos)
                out.put(uValues)
                out.position(vPos)
                out.put(vValues)
                out.position(base + chromaSize * 2)
            }
            true
        } catch (_: Exception) { false }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        val codec = encoder ?: return
        while (true) {
            val index = codec.dequeueOutputBuffer(bufferInfo, 0)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> return
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) throw IllegalStateException("Encoder format changed twice")
                    trackIndex = muxer!!.addTrack(codec.outputFormat)
                    muxer!!.start()
                    muxerStarted = true
                }
                index >= 0 -> {
                    val encoded = codec.getOutputBuffer(index)
                    if (encoded != null && bufferInfo.size > 0 && muxerStarted &&
                        (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        encoded.position(bufferInfo.offset)
                        encoded.limit(bufferInfo.offset + bufferInfo.size)
                        muxer!!.writeSampleData(trackIndex, encoded, bufferInfo)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return
                }
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val plane = image.planes[0]
            val paddedWidth = plane.rowStride / plane.pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            val buffer = plane.buffer
            buffer.rewind()
            padded.copyPixelsFromBuffer(buffer)
            if (paddedWidth == image.width) padded else {
                val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
                padded.recycle()
                cropped
            }
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
        wm.addView(panel, params)
        overlay = panel
    }

    private fun buildNotification(): Notification {
        val channelId = "game_translate_live"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "ترجمه و ضبط زنده", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("Game Translate Pro")
            .setContentText("ترجمه و ضبط هم‌زمان صفحه فعال است")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
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
        try { encoder?.signalEndOfInputStream() } catch (_: Exception) {}
        try { drainEncoder(true) } catch (_: Exception) {}
        try { encoder?.stop(); encoder?.release() } catch (_: Exception) {}
        encoder = null
        try { if (muxerStarted) muxer?.stop() } catch (_: Exception) {}
        try { muxer?.release() } catch (_: Exception) {}
        muxer = null
        muxerStarted = false
        if (outputFile?.exists() == true && outputFile!!.length() < 2048L) outputFile?.delete()
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

private object MediaCodecListCompat {
    fun findAvcEncoder(preferredFormat: Int): String? {
        val list = MediaCodecListCompatInternal.codecs()
        return list.firstOrNull { info ->
            info.isEncoder && info.supportedTypes.any { it.equals("video/avc", true) } &&
                runCatching { info.getCapabilitiesForType("video/avc").colorFormats.contains(preferredFormat) }.getOrDefault(false)
        }?.name
    }
}

private object MediaCodecListCompatInternal {
    fun codecs(): Array<android.media.MediaCodecInfo> = android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos
}
