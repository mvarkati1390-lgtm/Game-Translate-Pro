package com.gametranslatepro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
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
import java.io.File

private val Page = Color(0xFF0B1020)
private val Panel = Color(0xFF17243A)
private val Accent = Color(0xFF65D6FF)

class MainActivity : ComponentActivity() {
    private var recording by mutableStateOf(false)
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenRecordService::class.java).apply {
                action = ScreenRecordService.ACTION_START
                putExtra(ScreenRecordService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenRecordService.EXTRA_DATA, result.data)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            recording = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Page, surface = Panel, primary = Accent)) {
                Surface(Modifier.fillMaxSize(), color = Page) {
                    GameTranslateHome(recording, { requestRecording() }, {
                        startService(Intent(this, ScreenRecordService::class.java).setAction(ScreenRecordService.ACTION_STOP))
                        recording = false
                    }, { recordings() })
                }
            }
        }
    }

    private fun requestRecording() {
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun recordings(): List<File> {
        val folder = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "GameTranslatePro")
        return folder.listFiles()?.filter { it.isFile && it.extension.equals("mp4", true) }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

@Composable
private fun GameTranslateHome(recording: Boolean, onStartRecording: () -> Unit, onStopRecording: () -> Unit, getRecordings: () -> List<File>) {
    var source by remember { mutableStateOf("salam chetori") }
    var result by remember { mutableStateOf(FinglishTranslator.translate("salam chetori")) }
    var selectedTab by remember { mutableStateOf(0) }
    var refreshDownloads by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("GAME TRANSLATE PRO", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("مترجم بازی", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("ترجمه پیام‌های فینگلیش و متن‌های بازی به فارسی", color = Color(0xFFB8C4D9), fontSize = 14.sp)
        TabRow(selectedTabIndex = selectedTab, containerColor = Page, contentColor = Accent) {
            listOf("ترجمه", "ضبط", "دانلودها").forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index; refreshDownloads++ }, text = { Text(label) })
            }
        }
        when (selectedTab) {
            0 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("فینگلیش را وارد کن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(value = source, onValueChange = { source = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("مثلاً: bia team beshim") })
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
                    }
                }
                Text("نسخه فعلی از واژه‌نامه آفلاین اولیه استفاده می‌کند.", color = Color(0xFFFFC77A), fontSize = 12.sp)
            }
            1 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("ضبط صفحه و بازی", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("قبل از ضبط، اندروید اجازه می‌گیرد و هنگام ضبط اعلان دائمی نشان می‌دهد.", color = Color(0xFFB8C4D9))
                        if (recording) {
                            Text("● ضبط در حال انجام است", color = Color(0xFFFF7777), fontWeight = FontWeight.Bold)
                            Button(onClick = onStopRecording, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB83A4B))) { Text("توقف ضبط") }
                        } else {
                            Button(onClick = onStartRecording, modifier = Modifier.fillMaxWidth()) { Text("شروع ضبط صفحه") }
                        }
                        Text("ویدیو در پوشه اختصاصی برنامه ذخیره می‌شود. بعضی بازی‌ها ممکن است ضبط را مسدود کنند.", color = Color(0xFFFFC77A), fontSize = 12.sp)
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
                            Text(item.length().div(1024L * 1024L).toString() + " MB", color = Color(0xFFB8C4D9), fontSize = 11.sp)
                        }
                        OutlinedButton(onClick = { refreshDownloads++ }, modifier = Modifier.fillMaxWidth()) { Text("به‌روزرسانی فهرست") }
                        Text("با حذف برنامه، این ویدیوها هم پاک می‌شوند.", color = Color(0xFFFFC77A), fontSize = 12.sp)
                    }
                }
            }
        }
        Text("نسخه آزمایشی 0.3.0", color = Color(0xFF71819A), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
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
