package com.gametranslatepro

import java.util.Locale

/** Offline phrase and vocabulary translator for common game chat and UI text. */
object FinglishTranslator {
    private val phrases = mapOf(
        "salam chetori" to "سلام چطوری؟", "salam chetori?" to "سلام چطوری؟",
        "bia team beshim" to "بیا تیم بشیم", "where are you" to "کجا هستی؟",
        "where r u" to "کجایی؟", "what are you doing" to "چه کار می‌کنی؟",
        "good game" to "بازی خوبی بود", "good luck" to "موفق باشی",
        "i need help" to "کمک لازم دارم", "follow me" to "دنبالم بیا",
        "wait for me" to "منتظرم بمان", "lets go" to "بزن بریم",
        "let's go" to "بزن بریم", "thank you" to "ممنون",
        "nice to meet you" to "از آشنایی با تو خوشحالم",
        "dark knight" to "شوالیه تاریکی", "iron man" to "مرد آهنی",
        "game over" to "پایان بازی", "mission complete" to "مأموریت کامل شد",
        "new mission" to "مأموریت جدید", "level up" to "ارتقای مرحله",
        "chetori" to "چطوری؟", "khubi" to "خوبی؟", "salam" to "سلام",
        "bia" to "بیا", "berim" to "بریم", "kojayi" to "کجایی؟",
        "koja hasti" to "کجا هستی؟", "komak" to "کمک", "merci" to "مرسی",
        "khodahafez" to "خداحافظ", "man" to "من", "to" to "تو",
        "team" to "تیم", "player" to "بازیکن", "enemy" to "دشمن",
        "friend" to "دوست", "attack" to "حمله", "defend" to "دفاع کن",
        "wait" to "صبر کن", "go" to "برو", "stop" to "ایست",
        "yes" to "بله", "no" to "نه", "thanks" to "ممنون",
        "hello" to "سلام", "hi" to "سلام", "bye" to "خداحافظ",
        "start" to "شروع", "exit" to "خروج", "settings" to "تنظیمات",
        "continue" to "ادامه", "play" to "بازی", "loading" to "در حال بارگذاری",
        "player one" to "بازیکن یک", "player two" to "بازیکن دو",
        "victory" to "پیروزی", "defeat" to "شکست", "health" to "سلامتی",
        "ammo" to "مهمات", "weapon" to "سلاح", "map" to "نقشه",
        "join" to "پیوستن", "leave" to "ترک کردن", "invite" to "دعوت",
        "accept" to "قبول", "decline" to "رد", "searching" to "در حال جست‌وجو",
        "connecting" to "در حال اتصال", "reconnect" to "اتصال دوباره"
    )

    fun translate(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val normalized = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("[’‘]"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()
        phrases[normalized]?.let { return it }
        val tokens = Regex("[a-zA-Z']+|[^a-zA-Z']+").findAll(normalized).map { it.value }
        return tokens.joinToString("") { token ->
            if (token.firstOrNull()?.isLetter() == true) phrases[token] ?: token else token
        }.let { result ->
            if (result == normalized) input else result
        }
    }
}
