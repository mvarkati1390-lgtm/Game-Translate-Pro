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
        "salamati" to "سلامتی", "khobish" to "خوبیش",
        "chetoriya" to "چطوری", "chetori?" to "چطوری؟",
        "khoobam" to "خوبم", "khoobi?" to "خوبی؟",
        "kojayi?" to "کجایی؟", "kojai" to "کجایی",
        "komakam" to "کمکم", "komak kon" to "کمک کن",
        "komak konid" to "کمک کنید", "komak mikoni" to "کمک می‌کنی",
        "bia" to "بیا", "biay" to "بیای",
        "hamishe" to "همیشه", "hanooz" to "هنوز",
        "fekr" to "فکر", "fek mikonam" to "فکر می‌کنم",
        "midonam" to "می‌دونم", "nemidonam" to "نمی‌دونم",
        "midooni" to "می‌دونی", "nemikhad" to "نمی‌خواد",
        "mitoni" to "می‌تونی", "mitunam" to "می‌تونم",
        "bayad" to "باید", "nabاید" to "نباید",
        "dorosteh" to "درسته", "eshkali" to "اشکالی",
        "bebakhshid" to "ببخشید", "bebakhsh" to "ببخش",
        "khodet" to "خودت", "khodam" to "خودم",
        "khodesh" to "خودش", "shoma" to "شما",
        "hamintor" to "همین‌طور", "hamin" to "همین",
        "alan koja" to "الان کجا", "chi shod" to "چی شد",
        "chi migi" to "چی می‌گی", "chi mikhay" to "چی می‌خوای",
        "bazi kon" to "بازی کن", "bazi konim" to "بازی کنیم",
        "team beshim" to "تیم بشیم", "join sho" to "جوین شو",
        "join besho" to "جوین بشو", "ready hasti" to "آماده‌ای",
        "amadeam" to "آماده‌ام", "attack kon" to "حمله کن",
        "poshtam" to "پشتم", "komakam kon" to "کمکم کن",
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
        if (latinWords.isEmpty()) return input
        val translatedCount = latinWords.count { it in words }
        if (translatedCount == 0) return input

        // Keep unknown words (for example player names) unchanged.
        return tokens.joinToString("") { token ->
            if (token.firstOrNull()?.isLetter() == true) words[token] ?: token else token
        }
    }
}
