package com.gametranslatepro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

private val Page = Color(0xFF0B1020)
private val Panel = Color(0xFF17243A)
private val Accent = Color(0xFF65D6FF)

class MainActivity : ComponentActivity() {
    private var recording by mutableStateOf(false)
    private var liveRunning by mutableStateOf(false)
    private var overlayAllowed by mutableStateOf(false)

    private val recordLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_START
                putExtra(ScreenRecordService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecordService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, intent)
            recording = true
        }
    }

    private val liveLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(this, LiveTranslateService::class.java).apply {
                action = LiveTranslateService.ACTION_START
                putExtra(LiveTranslateService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(LiveTranslateService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, intent)
            liveRunning = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayAllowed = Settings.canDrawOverlays(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Page, surface = Panel, primary = Accent)) {
                Surface(Modifier.fillMaxSize(), color = Page) {
                    GameTranslateHome(
                        recording, liveRunning, overlayAllowed,
                        { requestRecording() },
                        {
                            startService(Intent(this, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_STOP))
                            recording = false
                        },
                        { requestLiveTranslation() },
                        {
                            startService(Intent(this, LiveTranslateService::class.java).setAction(LiveTranslateService.ACTION_STOP))
                            liveRunning = false
                        },
                        { recordings() },
                        { playVideo(it) }, { shareVideo(it) }, { deleteVideo(it) }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayAllowed = Settings.canDrawOverlays(this)
    }

    private fun requestRecording() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        recordLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun requestLiveTranslation() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        liveLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun recordings(): List<File> {
        val folder = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "GameTranslatePro")
        return folder.listFiles()?.filter { it.isFile && it.extension.equals("mp4", true) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    private fun playVideo(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        runCatching { startActivity(intent) }
    }

    private fun shareVideo(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).setType("video/mp4")
            .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "اشتراک‌گذاری ویدیو"))
    }

    private fun deleteVideo(file: File) {
        file.delete()
    }
}

@Composable
private fun GameTranslateHome(
    recording: Boolean, liveRunning: Boolean, overlayAllowed: Boolean,
    onStartRecording: () -> Unit, onStopRecording: () -> Unit,
    onStartLive: () -> Unit, onStopLive: () -> Unit,
    getRecordings: () -> List<File>, onPlay: (File) -> Unit,
    onShare: (File) -> Unit, onDelete: (File) -> Unit
) {
    var source by remember { mutableStateOf("salam chetori") }
    var result by remember { mutableStateOf(FinglishTranslator.translate("salam chetori")) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var refreshDownloads by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("GAME TRANSLATE PRO", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("مترجم بازی", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("ترجمه فینگلیش و متن انگلیسی بازی روی صفحه", color = Color(0xFFB8C4D9), fontSize = 14.sp)
        TabRow(selectedTabIndex = selectedTab, containerColor = Page, contentColor = Accent) {
            listOf("ترجمه", "ترجمه زنده", "ضبط", "دانلودها").forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index; refreshDownloads++ }, text = { Text(label) })
            }
        }
        when (selectedTab) {
            0 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("متن فینگلیش یا انگلیسی", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(value = source, onValueChange = { source = it; result = FinglishTranslator.translate(it) }, modifier = Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("مثلاً: bia team beshim") })
                        Button(onClick = { result = FinglishTranslator.translate(source) }, modifier = Modifier.fillMaxWidth()) { Text("ترجمه به فارسی") }
                        Text("نتیجه", color = Accent, fontWeight = FontWeight.Bold)
                        Text(result.ifBlank { "ترجمه اینجا نمایش داده می‌شود" }, color = Color.White, fontSize = 22.sp)
                        OutlinedButton(onClick = { source = ""; result = "" }, modifier = Modifier.fillMaxWidth()) { Text("پاک کردن") }
                    }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("نمونه‌های آماده", color = Color.White, fontWeight = FontWeight.Bold)
                        ExampleRow("salam", "سلام") { source = it; result = FinglishTranslator.translate(it) }
                        ExampleRow("chetori?", "چطوری؟") { source = it; result = FinglishTranslator.translate(it) }
                        ExampleRow("bia team beshim", "بیا تیم بشیم") { source = it; result = FinglishTranslator.translate(it) }
                        ExampleRow("where are you", "کجا هستی؟") { source = it; result = FinglishTranslator.translate(it) }
                        ExampleRow("dark knight", "شوالیه تاریکی") { source = it; result = FinglishTranslator.translate(it) }
                    }
                }
            }
            1 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("ترجمه زنده روی بازی", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("با اجازه ضبط صفحه، متن انگلیسی قابل مشاهده روی بازی خوانده و در پنجره شناور به فارسی نمایش داده می‌شود.", color = Color(0xFFB8C4D9))
                        if (!overlayAllowed) {
                            Text("ابتدا اجازه نمایش روی برنامه‌های دیگر را فعال کن.", color = Color(0xFFFFC77A))
                            Button(onClick = onStartLive, modifier = Modifier.fillMaxWidth()) { Text("فعال‌کردن اجازه و ادامه") }
                        } else if (liveRunning) {
                            Text("ترجمه زنده فعال است. پنجره شناور را روی بازی می‌بینی.", color = Color(0xFF78E0A3))
                            Button(onClick = onStopLive, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB83A4B))) { Text("توقف ترجمه زنده") }
                        } else {
                            Button(onClick = onStartLive, modifier = Modifier.fillMaxWidth()) { Text("شروع ترجمه زنده") }
                        }
                        Text("اندروید هر بار اجازه ضبط صفحه می‌خواهد. صفحه‌های امن یا بعضی بازی‌ها ممکن است تصویر را مخفی کنند.", color = Color(0xFFFFC77A), fontSize = 12.sp)
                    }
                }
            }
            2 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("ضبط صفحه و بازی", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (recording) {
                            Text("● ضبط در حال انجام است", color = Color(0xFFFF7777), fontWeight = FontWeight.Bold)
                            Button(onClick = onStopRecording, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB83A4B))) { Text("توقف ضبط") }
                        } else {
                            Button(onClick = onStartRecording, modifier = Modifier.fillMaxWidth()) { Text("شروع ضبط صفحه") }
                        }
                        Text("ضبط با مجوز رسمی اندروید انجام می‌شود و اعلان دائمی دارد.", color = Color(0xFFB8C4D9))
                    }
                }
            }
            else -> {
                val files = remember(refreshDownloads) { getRecordings() }
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ویدیوهای ضبط‌شده", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        if (files.isEmpty()) Text("هنوز ویدیویی ذخیره نشده است.", color = Color(0xFFB8C4D9))
                        files.forEach { item ->
                            Text(item.name, color = Color.White, fontSize = 13.sp)
                            Text(String.format("%.1f MB", item.length() / (1024.0 * 1024.0)), color = Color(0xFFB8C4D9), fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onPlay(item) }) { Text("پخش") }
                                OutlinedButton(onClick = { onShare(item) }) { Text("اشتراک") }
                                OutlinedButton(onClick = { onDelete(item); refreshDownloads++ }) { Text("حذف") }
                            }
                            Divider(color = Color(0xFF30415B))
                        }
                        OutlinedButton(onClick = { refreshDownloads++ }, modifier = Modifier.fillMaxWidth()) { Text("به‌روزرسانی فهرست") }
                    }
                }
            }
        }
        Text("نسخه آزمایشی 0.4.0", color = Color(0xFF71819A), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ExampleRow(english: String, persian: String, onClick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(english, color = Color(0xFFB8C4D9))
        Text(persian, color = Color.White, fontWeight = FontWeight.Medium)
        TextButton(onClick = { onClick(english) }) { Text("انتخاب") }
    }
}
