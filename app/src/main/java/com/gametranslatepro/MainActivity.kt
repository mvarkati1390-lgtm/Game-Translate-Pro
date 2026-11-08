package com.gametranslatepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

private val Page = Color(0xFF0B1020)
private val Panel = Color(0xFF17243A)
private val Accent = Color(0xFF65D6FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Page,
                    surface = Panel,
                    primary = Accent
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Page) {
                    GameTranslateHome()
                }
            }
        }
    }
}

@Composable
private fun GameTranslateHome() {
    var source by remember { mutableStateOf("salam chetori") }
    var result by remember { mutableStateOf(FinglishTranslator.translate("salam chetori")) }
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("GAME TRANSLATE PRO", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("مترجم بازی", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("ترجمه پیام‌های فینگلیش و متن‌های بازی به فارسی", color = Color(0xFFB8C4D9), fontSize = 14.sp)

        TabRow(selectedTabIndex = selectedTab, containerColor = Page, contentColor = Accent) {
            listOf("ترجمه", "ضبط", "دانلودها").forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
            }
        }

        when (selectedTab) {
            0 -> {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("فینگلیش را وارد کن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedTextField(
                            value = source,
                            onValueChange = { source = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            placeholder = { Text("مثلاً: bia team beshim") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Color(0xFF52627A),
                                cursorColor = Accent
                            )
                        )
                        Button(onClick = { result = FinglishTranslator.translate(source) }, modifier = Modifier.fillMaxWidth()) {
                            Text("ترجمه به فارسی")
                        }
                        Text("نتیجه", color = Accent, fontWeight = FontWeight.Bold)
                        Text(result.ifBlank { "ترجمه اینجا نمایش داده می‌شود" }, color = Color.White, fontSize = 22.sp)
                        OutlinedButton(onClick = { source = ""; result = "" }, modifier = Modifier.fillMaxWidth()) {
                            Text("پاک کردن")
                        }
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
                Text("این نسخه فعلاً از واژه‌نامه آفلاین اولیه استفاده می‌کند؛ عبارت‌های ناشناخته ممکن است ترجمه نشوند.", color = Color(0xFFFFC77A), fontSize = 12.sp)
            }
            1 -> PlaceholderCard("ضبط صفحه", "بخش ضبط با مجوز رسمی اندروید در مرحله بعد اضافه می‌شود.")
            else -> PlaceholderCard("دانلودها", "ویدیوهای ضبط‌شده پس از پیاده‌سازی قابلیت ضبط، اینجا نمایش داده می‌شوند.")
        }
        Text("نسخه آزمایشی 0.2.0", color = Color(0xFF71819A), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
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

@Composable
private fun PlaceholderCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(message, color = Color(0xFFB8C4D9))
        }
    }
}
