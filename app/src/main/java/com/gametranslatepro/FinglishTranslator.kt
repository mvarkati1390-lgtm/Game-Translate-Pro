package com.gametranslatepro

import java.util.Locale

/**
 * Persian Finglish transliterator. Known words use a curated dictionary;
 * unknown words can be phonetic-transliterated when the line looks like Finglish.
 */
object FinglishTranslator {
    private val phrases = mapOf(
        "salam chetori" to "سلام چطوری؟",
        "salam khubi" to "سلام خوبی؟",
        "bia team beshim" to "بیا تیم بشیم",
        "bia ba ham bazi konim" to "بیا با هم بازی کنیم",
        "man komak mikham" to "من کمک می‌خوام",
        "koja hasti" to "کجا هستی؟",
        "chetori hasti" to "چطوری هستی؟",
        "khodet ro moarefi kon" to "خودت رو معرفی کن",
        "merci az komaket" to "مرسی از کمکت",
        "boro jelotar" to "برو جلوتر",
        "sabr kon" to "صبر کن",
        "berim bazi" to "بریم بازی",
        "bia inja" to "بیا اینجا",
        "che khabar" to "چه خبر؟",
        "khobam mamnoon" to "خوبم ممنون"
    )

    private val words = mapOf(
        "salam" to "سلام", "salaam" to "سلام",
        "chetori" to "چطوری", "chetoury" to "چطوری", "chetoriya" to "چطوری",
        "khubi" to "خوبی", "khoobi" to "خوبی", "khobam" to "خوبم", "khob" to "خوب",
        "man" to "من", "to" to "تو", "ma" to "ما", "shoma" to "شما",
        "oon" to "اون", "un" to "اون", "in" to "این", "oonja" to "اونجا",
        "inja" to "اینجا", "kojayi" to "کجایی", "koja" to "کجا", "hasti" to "هستی",
        "hastam" to "هستم", "hast" to "هست", "nist" to "نیست", "nisti" to "نیستی",
        "bia" to "بیا", "biya" to "بیا", "berim" to "بریم", "boro" to "برو",
        "beroo" to "برو", "bashi" to "باشی", "bash" to "باش", "beshi" to "بشی",
        "besho" to "بشو", "kon" to "کن", "konim" to "کنیم", "mikham" to "می‌خوام",
        "mikhay" to "می‌خوای", "mikoni" to "می‌کنی", "mishe" to "میشه",
        "nemishe" to "نمیشه", "nashod" to "نشد", "shod" to "شد", "hame" to "همه",
        "ham" to "هم", "ba" to "با", "az" to "از", "be" to "به", "ro" to "رو",
        "ra" to "را", "baraye" to "برای", "chera" to "چرا", "chi" to "چی",
        "che" to "چه", "khabar" to "خبر", "komak" to "کمک", "lotfan" to "لطفاً",
        "merci" to "مرسی", "mamnoon" to "ممنون", "khodahafez" to "خداحافظ",
        "khoda" to "خدا", "kojayi" to "کجایی", "midonam" to "می‌دونم",
        "nemidonam" to "نمی‌دونم", "midooni" to "می‌دونی", "nemikhad" to "نمی‌خواد",
        "mitoni" to "می‌تونی", "mitunam" to "می‌تونم", "bayad" to "باید",
        "dorosteh" to "درسته", "bebakhshid" to "ببخشید", "bebakhsh" to "ببخش",
        "khodet" to "خودت", "khodam" to "خودم", "khodesh" to "خودش",
        "hamintor" to "همین‌طور", "hamin" to "همین", "alan" to "الان",
        "emrooz" to "امروز", "emroz" to "امروز", "farda" to "فردا",
        "shab" to "شب", "rooz" to "روز", "sari" to "سریع", "zood" to "زود",
        "dir" to "دیر", "vaysa" to "وایسا", "sabr" to "صبر", "montazer" to "منتظر",
        "hamechi" to "همه‌چی", "eshtebah" to "اشتباه", "dorost" to "درست",
        "khaste" to "خسته", "khafan" to "خفن", "kheyli" to "خیلی",
        "khoob" to "خوب", "bad" to "بد", "garm" to "گرم", "sard" to "سرد",
        "manam" to "منم", "toam" to "تو هم", "bacheha" to "بچه‌ها",
        "doost" to "دوست", "baba" to "بابا", "dadash" to "داداش",
        "abji" to "آبجی", "khahar" to "خواهر", "baradar" to "برادر",
        "eshgh" to "عشق", "daram" to "دارم", "nadaram" to "ندارم",
        "bazi" to "بازی", "team" to "تیم", "tim" to "تیم",
        "yeki" to "یکی", "do" to "دو", "se" to "سه", "yek" to "یک",
        "na" to "نه", "are" to "آره", "bale" to "بله", "ok" to "اوکی",
        "dige" to "دیگه", "khodet" to "خودت", "fekr" to "فکر",
        "fekram" to "فکرم", "fek mikonam" to "فکر می‌کنم",
        "join" to "جوین", "player" to "پلیر", "attack" to "اتک",
        "ready" to "ردی", "amadeam" to "آماده‌ام", "poshtam" to "پشتم",
        "komakam" to "کمکم", "komak" to "کمک", "shoro" to "شروع",
        "berid" to "برید", "biay" to "بیای", "begu" to "بگو",
        "begoo" to "بگو", "goftam" to "گفتم", "migam" to "میگم",
        "migi" to "میگی", "raftam" to "رفتم", "miam" to "میام",
        "biaim" to "بیایم", "boroim" to "برویم", "khastam" to "خواستم",
        "khast" to "خواست", "cheghadr" to "چقدر", "cheragh" to "چراغ",
        "vaghti" to "وقتی", "chon" to "چون", "chonke" to "چونکه",
        "vali" to "ولی", "agar" to "اگر", "age" to "اگه", "pas" to "پس",
        "hamin" to "همین", "hich" to "هیچ", "hichi" to "هیچی",
        "har" to "هر", "harja" to "هرجا", "hamishe" to "همیشه",
        "hanooz" to "هنوز", "alan" to "الان", "badam" to "بعدم",
        "bad" to "بعد", "ghablan" to "قبلاً", "emshab" to "امشب",
        "sobh" to "صبح", "zohr" to "ظهر", "shoma" to "شما",
        "khub" to "خوب", "khoob" to "خوب", "nafar" to "نفر",
        "nafare" to "نفره", "dost" to "دوست", "doostam" to "دوستم",
        "omidvaram" to "امیدوارم", "movafagh" to "موفق", "mamnoonam" to "ممنونم"
    )

    // Longest sound combinations must be checked before single letters.
    private val sounds = listOf(
        "kh" to "خ", "gh" to "غ", "sh" to "ش", "ch" to "چ", "zh" to "ژ",
        "oo" to "و", "ou" to "و", "aa" to "ا", "ee" to "ی",
        "a" to "ا", "b" to "ب", "p" to "پ", "t" to "ت", "s" to "س",
        "j" to "ج", "چ" to "چ", "h" to "ه", "x" to "خ", "d" to "د",
        "r" to "ر", "z" to "ز", "f" to "ف", "q" to "ق", "k" to "ک",
        "g" to "گ", "l" to "ل", "m" to "م", "n" to "ن", "v" to "و",
        "w" to "و", "u" to "و", "i" to "ی", "y" to "ی", "e" to "",
        "o" to "ُ"
    )

    private fun phoneticWord(word: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < word.length) {
            val match = sounds.firstOrNull { (latin, _) ->
                word.regionMatches(i, latin, 0, latin.length, ignoreCase = true)
            }
            if (match != null) {
                result.append(match.second)
                i += match.first.length
            } else {
                result.append(word[i])
                i++
            }
        }
        return result.toString().replace("اُ", "ا").replace("ُ", "")
    }

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

        val knownCount = latinWords.count { it in words }
        val hasPersianSound = latinWords.any { w ->
            listOf("kh", "gh", "sh", "ch", "zh").any { w.contains(it) }
        }
        // Avoid turning ordinary English game UI into Persian gibberish.
        if (knownCount == 0 && !hasPersianSound) return input

        return tokens.joinToString("") { token ->
            if (token.firstOrNull()?.isLetter() != true) token
            else words[token] ?: phoneticWord(token)
        }
    }
}
