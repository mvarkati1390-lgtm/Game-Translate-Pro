package com.gametranslatepro

import java.util.Locale

/**
 * Offline starter Finglish-to-Persian phrase mapper.
 * It intentionally leaves unknown words unchanged instead of guessing.
 */
object FinglishTranslator {
    private val phrases = mapOf(
        "chetori" to "چطوری",
        "chetori?" to "چطوری؟",
        "khubi" to "خوبی",
        "salam" to "سلام",
        "salam chetori" to "سلام چطوری",
        "bia team beshim" to "بیا تیم بشیم",
        "bia" to "بیا",
        "berim" to "بریم",
        "kojayi" to "کجایی",
        "koja hasti" to "کجا هستی",
        "komak" to "کمک",
        "merci" to "مرسی",
        "khodahafez" to "خداحافظ",
        "man" to "من",
        "to" to "تو",
        "team" to "تیم",
        "player" to "بازیکن",
        "enemy" to "دشمن",
        "friend" to "دوست",
        "attack" to "حمله",
        "defend" to "دفاع کن",
        "wait" to "صبر کن",
        "go" to "برو",
        "stop" to "ایست",
        "yes" to "بله",
        "no" to "نه",
        "thanks" to "ممنون",
        "hello" to "سلام",
        "where are you" to "کجا هستی؟"
    )

    fun translate(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val normalized = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
            .removeSuffix("!")
            .removeSuffix(".")
        phrases[normalized]?.let { return it }
        val words = normalized.split(" ")
        if (words.size == 1) return phrases[words[0]] ?: input
        return words.joinToString(" ") { word -> phrases[word] ?: word }
    }
}
