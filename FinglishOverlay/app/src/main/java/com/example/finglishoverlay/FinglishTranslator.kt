package com.example.finglishoverlay

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors

object FinglishTranslator {
    private val executor = Executors.newSingleThreadExecutor()
    private val phrases = mapOf(
        "salam" to "سلام", "salaam" to "سلام", "chetori" to "چطوری", "khubi" to "خوبی",
        "khobi" to "خوبی", "merci" to "مرسی", "mamnoon" to "ممنون", "khodahafez" to "خداحافظ",
        "bebakhshid" to "ببخشید", "lotfan" to "لطفاً", "are" to "آره", "na" to "نه",
        "bale" to "بله", "bia" to "بیا", "boro" to "برو", "berim" to "بریم",
        "sabr kon" to "صبر کن", "komak" to "کمک", "dost" to "دوست", "man" to "من",
        "to" to "تو", "shoma" to "شما", "chi" to "چی", "chera" to "چرا",
        "alan" to "الان", "emrooz" to "امروز", "farda" to "فردا", "bazi" to "بازی",
        "bazikon" to "بازیکن", "level" to "لول", "pool" to "پول", "bia inja" to "بیا اینجا"
    )
    private val digraphs = listOf("kh" to "خ", "gh" to "ق", "sh" to "ش", "ch" to "چ", "zh" to "ژ", "aa" to "ا", "ou" to "و", "ei" to "ی", "ey" to "ی")
    private val chars = mapOf('a' to "ا",'b' to "ب",'c' to "ک",'d' to "د",'e' to "ه",'f' to "ف",'g' to "گ",'h' to "ه",'i' to "ی",'j' to "ج",'k' to "ک",'l' to "ل",'m' to "م",'n' to "ن",'o' to "و",'p' to "پ",'q' to "ق",'r' to "ر",'s' to "س",'t' to "ت",'u' to "و",'v' to "و",'w' to "و",'x' to "کس",'y' to "ی",'z' to "ز")

    fun offline(input: String): String {
        val normalized = input.trim().lowercase(Locale.ROOT)
        phrases[normalized]?.let { return it }
        return normalized.split(Regex("(\\s+)")).joinToString("") { token ->
            if (token.isBlank()) token else {
                val punctuation = token.takeLastWhile { !it.isLetterOrDigit() }
                val word = token.dropLast(punctuation.length)
                (phrases[word] ?: transliterateWord(word)) + punctuation
            }
        }
    }

    private fun transliterateWord(word: String): String {
        var s = word
        for ((from, to) in digraphs) s = s.replace(from, to)
        return s.map { chars[it] ?: it.toString() }.joinToString("")
    }

    fun translateHybrid(input: String, callback: (String) -> Unit) {
        val fallback = offline(input)
        executor.execute { callback(tryOnline(input) ?: fallback) }
    }

    private fun tryOnline(input: String): String? = try {
        val encoded = URLEncoder.encode(input, "UTF-8")
        val connection = (URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=en|fa").openConnection() as HttpURLConnection).apply {
            connectTimeout = 3500; readTimeout = 3500; requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) {
                null
            } else {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                Regex("\"translatedText\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").find(body)?.groupValues?.get(1)
                    ?.replace("\\\"", "\"")?.replace("\\n", "\n")
                    ?.takeIf { it.isNotBlank() && !it.equals(input, true) }
            }
        } finally { connection.disconnect() }
    } catch (_: Exception) { null }
}
