package com.example.wordwidget

import android.content.Context
import android.content.SharedPreferences

data class WordPair(val id: Long, val english: String, val turkish: String)

class WordDatabase(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("word_db", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WORDS = "words"
        private const val KEY_LAST_SHOWN_ID = "last_shown_id"
        private const val KEY_INTERVAL = "update_interval"
    }

    fun addWord(english: String, turkish: String) {
        val words = getAllWords().toMutableList()
        words.add(WordPair(System.currentTimeMillis(), english.trim(), turkish.trim()))
        saveWords(words)
    }

    fun deleteWord(id: Long) {
        val words = getAllWords().filter { it.id != id }
        saveWords(words)
    }

    fun getAllWords(): List<WordPair> {
        val json = prefs.getString(KEY_WORDS, "[]") ?: "[]"
        return parseWords(json)
    }

    // --- YENİ: Aynı kelimenin üst üste gelmesini önleyen fonksiyon ---
    fun getRandomWord(): WordPair? {
        val words = getAllWords()
        if (words.isEmpty()) return null
        if (words.size == 1) return words[0] // Sadece 1 kelime varsa mecbur onu gösterecek

        val lastShownId = prefs.getLong(KEY_LAST_SHOWN_ID, -1L)

        // Son gösterilen kelime hariç diğerlerini filtrele
        val availableWords = words.filter { it.id != lastShownId }

        // Yeni rastgele kelimeyi seç
        val randomWord = availableWords.random()

        // Yeni gösterilen kelimenin ID'sini kaydet
        prefs.edit().putLong(KEY_LAST_SHOWN_ID, randomWord.id).apply()

        return randomWord
    }

    fun getUpdateInterval(): Int = prefs.getInt(KEY_INTERVAL, 30) // Varsayılan 30 dk
    fun setUpdateInterval(minutes: Int) = prefs.edit().putInt(KEY_INTERVAL, minutes).apply()

    private fun saveWords(words: List<WordPair>) {
        val json = buildString {
            append("[")
            words.forEachIndexed { index, word ->
                if (index > 0) append(",")
                append("{\"id\":${word.id},\"english\":\"${escapeJson(word.english)}\",\"turkish\":\"${escapeJson(word.turkish)}\"}")
            }
            append("]")
        }
        prefs.edit().putString(KEY_WORDS, json).apply()
    }

    private fun parseWords(json: String): List<WordPair> {
        val words = mutableListOf<WordPair>()
        val regex = Regex("\\{\"id\":(\\d+),\"english\":\"([^\"]*)\",\"turkish\":\"([^\"]*)\"\\}")
        regex.findAll(json).forEach { match ->
            words.add(WordPair(match.groupValues[1].toLong(), match.groupValues[2], match.groupValues[3]))
        }
        return words
    }

    private fun escapeJson(text: String): String {
        return text.replace("\"", "\\\"").replace("\n", "\\n")
    }

    // Tüm kelimeleri JSON string olarak döndür (Export için)
    fun exportWordsToJson(): String {
        return prefs.getString(KEY_WORDS, "[]") ?: "[]"
    }

    // JSON string'i alıp veritabanına kaydet (Import için)
    fun importWordsFromJson(jsonString: String) {
        // Basit doğrulama
        if (jsonString.trim().startsWith("[")) {
            prefs.edit().putString(KEY_WORDS, jsonString).apply()
        }
    }
    // Saat formatı (true = 24 saat, false = 12 saat)
    fun getIs24HourFormat(): Boolean {
        return prefs.getBoolean("is_24_hour_format", true) // Varsayılan 24 saat
    }

    fun setIs24HourFormat(is24Hour: Boolean) {
        prefs.edit().putBoolean("is_24_hour_format", is24Hour).apply()
    }


}