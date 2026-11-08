package com.gametranslatepro

import java.util.Locale

/**
 * Converts Persian written with Latin letters (Finglish) into Persian script.
 * Ordinary English game text is intentionally left unchanged.
 */
object FinglishTranslator {
    private val phrases = mapOf(
        "salam chetori" to "سلام چطوری؟",
        "salam chetori?" to "سلام چطوری؟",
        "salam khubi" to "سلام خوبی؟",
        "bia team beshim" to "بیا تیم بشیم",
        "bia ba ham bazi konim" to "بیا با هم بازی کنیم",
        "man komak mikham" to "من کمک می‌خوام",
        "man komak mikham!" to "من کمک می‌خوام!",
        "koja hasti" to "کجا هستی؟",
        "chetori hasti" to "چطوری هستی؟",
        "khodet ro moarefi kon" to "خودت رو معرفی کن",
        "merci az komaket" to "مرسی از کمکت",
        "doset daram" to "دوستت دارم",
        "boro jelotar" to "برو جلوتر",
        "sabr kon" to "صبر کن",
        "berim bazi" to "بریم بازی",
        "bia inja" to "بیا اینجا",
        "man omad am" to "من اومدم",
        "che khabar" to "چه خبر؟",
        "khobam mamnoon" to "خوبم ممنون"
    )

    private val words = mapOf(
        "salam" to "سلام", "salaam" to "سلام",
        "chetori" to "چطوری", "chetoury" to "چطوری",
        "khubi" to "خوبی", "khoobi" to "خوبی",
        "khobam" to "خوبم", "khob" to "خوب",
        "man" to "من", "to" to "تو", "tո" to "تو",
        "ma" to "ما", "shoma" to "شما", "oon" to "اون",
        "in" to "این", "un" to "اون", "oonja" to "اونجا",
        "inja" to "اینجا", "kojayi" to "کجایی",
        "koja" to "کجا", "hasti" to "هستی",
        "hastam" to "هستم", "hast" to "هست",
        "bia" to "بیا", "biya" to "بیا",
        "berim" to "بریم", "boro" to "برو",
        "beroo" to "برو", "bashi" to "باشی",
        "bash" to "باش", "beshi" to "بشی",
        "besho" to "بشو", "kon" to "کن",
        "mikham" to "می‌خوام", "mikhay" to "می‌خوای",
        "mikoni" to "می‌کنی", "mishe" to "میشه",
        "nemishe" to "نمیشه", "nashod" to "نشد",
        "shod" to "شد", "hame" to "همه",
        "ham" to "هم", "ba" to "با",
        "az" to "از", "be" to "به",
        "ro" to "رو", "ra" to "را",
        "baraye" to "برای", "chera" to "چرا",
        "chera?" to "چرا؟", "chi" to "چی",
        "che" to "چه", "khabar" to "خبر",
        "komak" to "کمک", "lotfan" to "لطفاً",
        "merci" to "مرسی", "mamnoon" to "ممنون",
        "khodahafez" to "خداحافظ", "khoda" to "خدا",
        "doset" to "دوستت", "daram" to "دارم",
        "nadaram" to "ندارم", "bazi" to "بازی",
        "konim" to "کنیم", "team" to "تیم",
        "tim" to "تیم", "player" to "پلیر",
        "yeki" to "یکی", "do" to "دو",
        "se" to "سه", "yek" to "یک",
        "na" to "نه", "are" to "آره",
        "bale" to "بله", "ok" to "اوکی",
        "oke" to "اوکی", "dige" to "دیگه",
        "alan" to "الان", "emrooz" to "امروز",
        "farda" to "فردا", "shab" to "شب",
        "rooz" to "روز", "sari" to "سریع",
        "zood" to "زود", "dir" to "دیر",
        "boro" to "برو", "vaysa" to "وایسا",
        "sabr" to "صبر", "montazer" to "منتظر",
        "hamechi" to "همه‌چی", "eshtebah" to "اشتباه",
        "dorost" to "درست", "khaste" to "خسته",
        "khafan" to "خفن", "kheyli" to "خیلی",
        "khoob" to "خوب", "bad" to "بد",
        "garm" to "گرم", "sard" to "سرد",
        "manam" to "منم", "toam" to "تو هم",
        "bacheha" to "بچه‌ها", "doost" to "دوست",
        "baba" to "بابا", "dadash" to "داداش",
        "abji" to "آبجی", "khahar" to "خواهر",
        "baradar" to "برادر", "eshgh" to "عشق"
    )

    fun translate(input: String): String {
        val original = input.trim()
        if (original.isEmpty() || !original.any { it in 'A'..'Z' || it in 'a'..'z' }) return input

        val normalized = original.lowercase(Locale.ROOT)
            .replace(Regex("[’‘]"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()

        phrases[normalized]?.let { return it }

        val tokens = Regex("[a-zA-Z]+|[^a-zA-Z]+").findAll(normalized).map { it.value }.toList()
        val latinWords = tokens.filter { it.firstOrNull()?.isLetter() == true }
        if (latinWords.isEmpty() || latinWords.any { it !in words }) return input

        return tokens.joinToString("") { token ->
            if (token.firstOrNull()?.isLetter() == true) words[token] ?: token else token
        }
    }
}
