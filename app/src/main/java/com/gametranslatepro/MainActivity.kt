package com.gametranslatepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0B1220)) { GameTranslateHome() }
            }
        }
    }
}

@Composable
private fun GameTranslateHome() {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("GAME TRANSLATE PRO", color = Color(0xFF65D6FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("بازی کن، ترجمه را ببین", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("مترجم متن بازی و چت بازیکنان", color = Color(0xFFB8C4D6), fontSize = 14.sp)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
            Column(Modifier.padding(18.dp)) {
                Text("ترجمه روی صفحه", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("نوشته‌های قابل مشاهده بازی شناسایی و برای ترجمه آماده می‌شوند.", color = Color(0xFFB8C4D6), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("فعال‌سازی مترجم شناور") }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
            Column(Modifier.padding(18.dp)) {
                Text("مترجم فینگلیش چت", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("chetori?  ←  چطوری؟", color = Color(0xFFB8C4D6), fontSize = 14.sp)
                Text("bia team beshim  ←  بیا تیم بشیم", color = Color(0xFFB8C4D6), fontSize = 14.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureCard("ضبط ویدیو", "در حال آماده‌سازی", Modifier.weight(1f))
            FeatureCard("دانلودها", "ویدیوهای ذخیره‌شده", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureCard("تنظیمات", "زبان و کیفیت", Modifier.weight(1f))
            FeatureCard("راهنما", "مجوزهای اندروید", Modifier.weight(1f))
        }
        Text("نسخه آزمایشی 0.1.0", color = Color(0xFF718096), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier.height(105.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17243A))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = Color(0xFF9EADC2), fontSize = 11.sp)
        }
    }
}
