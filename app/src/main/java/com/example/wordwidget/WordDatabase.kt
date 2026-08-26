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

    // ══════════════════════════════════════════════════════════
    // TEMEL KELİME İŞLEMLERİ
    // ══════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════
    // RASTGELE KELİME SEÇİMİ (Son gösterilen hariç)
    // ═════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════
    // GÜNCELLEME ARALIĞI AYARLARI
    // ══════════════════════════════════════════════════════════

    fun getUpdateInterval(): Int = prefs.getInt(KEY_INTERVAL, 30) // Varsayılan 30 dk
    fun setUpdateInterval(minutes: Int) = prefs.edit().putInt(KEY_INTERVAL, minutes).apply()

    // ══════════════════════════════════════════════════════════
    // KELİME GÜNCELLEME VE ARAMA (YENİ)
    // ══════════════════════════════════════════════════════════

    // Kelime güncelleme fonksiyonu
    fun updateWord(id: Long, newEnglish: String, newTurkish: String) {
        val words = getAllWords().toMutableList()
        val index = words.indexOfFirst { it.id == id }
        if (index != -1) {
            words[index] = WordPair(id, newEnglish.trim(), newTurkish.trim())
            saveWords(words)
        }
    }

    // Arama fonksiyonu (İngilizce ve Türkçe'de arama yapar)
    fun searchWords(query: String): List<WordPair> {
        if (query.isBlank()) return getAllWords()
        val lowerQuery = query.lowercase()
        return getAllWords().filter {
            it.english.lowercase().contains(lowerQuery) ||
                    it.turkish.lowercase().contains(lowerQuery)
        }
    }

    // ══════════════════════════════════════════════════════════
    // CSV EXPORT / IMPORT
    // ══════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════
    // CSV EXPORT / IMPORT (İndis Numarası ile)
    // ══════════════════════════════════════════════════════════

    // CSV olarak dışa aktar (İndis numarası ile)
    fun exportWordsToCsv(): String {
        val words = getAllWords()
        if (words.isEmpty()) return ""

        val csv = StringBuilder()
        // Başlık satırı (İndis eklendi)
        csv.appendLine("index,english,turkish")

        // Her kelime için bir satır (İndis numarası ile)
        words.forEachIndexed { index, word ->
            val csvIndex = index + 1 // 1'den başlat
            val english = escapeCsvField(word.english)
            val turkish = escapeCsvField(word.turkish)
            csv.appendLine("$csvIndex,$english,$turkish")
        }

        return csv.toString()
    }

    // CSV'den içe aktar (İndis numarasını atlayarak)
    fun importWordsFromCsv(csvString: String) {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        // Başlık satırını atla (ilk satır)
        val dataLines = if (lines.first().lowercase().contains("index") ||
            lines.first().lowercase().contains("english")) {
            lines.drop(1)
        } else {
            lines
        }

        val existingWords = getAllWords()
        var importCount = 0
        var duplicateCount = 0

        for (line in dataLines) {
            val fields = parseCsvLine(line)

            // 3 kolon bekliyoruz: index,english,turkish
            // Eğer eski format ise (2 kolon), yine de çalışsın
            if (fields.size >= 2) {
                val english: String
                val turkish: String

                if (fields.size >= 3) {
                    // Yeni format: index,english,turkish
                    english = fields[1].trim()
                    turkish = fields[2].trim()
                } else {
                    // Eski format: english,turkish (geriye dönük uyumluluk)
                    english = fields[0].trim()
                    turkish = fields[1].trim()
                }

                if (english.isNotEmpty() && turkish.isNotEmpty()) {
                    // Duplicate kontrolü (büyük/küçük harf duyarsız)
                    val isDuplicate = existingWords.any {
                        it.english.equals(english, ignoreCase = true)
                    }

                    if (!isDuplicate) {
                        addWord(english, turkish)
                        importCount++
                    } else {
                        duplicateCount++
                    }
                }
            }
        }

        android.util.Log.d("ImportDebug", "İçe aktarılan: $importCount, Atlanan (duplicate): $duplicateCount")
    }

    // ══════════════════════════════════════════════════════════
    // YARDIMCI FONKSİYONLAR (JSON ve CSV işleme)
    // ══════════════════════════════════════════════════════════

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

    // CSV alanını düzgün formatla (virgül veya tırnak varsa tırnak içine al)
    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

    // CSV satırını parse et (tırnak içindeki virgülleri doğru işle)
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        currentField.append('"')
                        i++ // İki tırnağı atla
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                else -> currentField.append(c)
            }
            i++
        }
        fields.add(currentField.toString())
        return fields
    }
}