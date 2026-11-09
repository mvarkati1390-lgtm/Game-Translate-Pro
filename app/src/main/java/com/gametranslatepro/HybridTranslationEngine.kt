package com.gametranslatepro

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Hybrid translation path:
 * 1) Preserve the existing curated Finglish dictionary/transliteration offline.
 * 2) If offline rules do not recognize the text, try online English -> Persian.
 * 3) If the request fails, times out, or returns no useful result, return the offline text.
 *
 * Note: general-purpose translation APIs do not reliably understand romanized Persian.
 * Known/recognizable Finglish should be handled by FinglishTranslator first.
 */
object HybridTranslationEngine {
    private val executor = Executors.newFixedThreadPool(2)
    private val cache = ConcurrentHashMap<String, String>()

    fun translateAsync(input: String, callback: (String) -> Unit) {
        val source = input.trim()
        if (source.isEmpty()) {
            callback(input)
            return
        }

        val offlineResult = FinglishTranslator.translate(source)
        if (offlineResult != source || !looksLikeEnglish(source)) {
            callback(offlineResult)
            return
        }

        cache[source.lowercase()]?.let {
            callback(it)
            return
        }

        executor.execute {
            val result = requestEnglishToPersian(source) ?: offlineResult
            if (result.isNotBlank()) cache[source.lowercase()] = result
            callback(result)
        }
    }

    private fun looksLikeEnglish(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        if (letters.isEmpty() || letters.any { it !in 'A'..'Z' && it !in 'a'..'z' }) return false

        // These combinations are common in Persian Finglish. Leave unknown romanized
        // Persian for offline transliteration instead of sending it as English.
        val lower = text.lowercase()
        val persianSignals = listOf("kh", "gh", "sh", "ch", "zh", "salam", "chet", "khob", "mikham")
        return persianSignals.none { lower.contains(it) }
    }

    private fun requestEnglishToPersian(text: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val query = URLEncoder.encode(text, Charsets.UTF_8.name())
            val endpoint = URL("https://api.mymemory.translated.net/get?q=$query&langpair=en%7Cfa")
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode !in 200..299) return null

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val translated = JSONObject(body)
                .optJSONObject("responseData")
                ?.optString("translatedText")
                ?.takeIf { it.isNotBlank() }
                ?: return null

            translated
                .replace("&#39;", "'")
                .replace("&quot;", "\"")
                .takeIf { it.isNotBlank() && !it.equals(text, ignoreCase = true) }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}
