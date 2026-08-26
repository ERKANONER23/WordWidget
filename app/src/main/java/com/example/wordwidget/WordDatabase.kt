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

    fun getRandomWord(): WordPair? {
        val words = getAllWords()
        if (words.isEmpty()) return null
        if (words.size == 1) return words[0]

        val lastShownId = prefs.getLong(KEY_LAST_SHOWN_ID, -1L)
        val availableWords = words.filter { it.id != lastShownId }
        val randomWord = availableWords.random()

        prefs.edit().putLong(KEY_LAST_SHOWN_ID, randomWord.id).apply()
        return randomWord
    }

    fun getUpdateInterval(): Int = prefs.getInt(KEY_INTERVAL, 30)
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

    // --- CSV FONKSİYONLARI ---
    fun exportWordsToCsv(): String {
        val words = getAllWords()
        if (words.isEmpty()) return ""

        val csv = StringBuilder()
        csv.appendLine("english,turkish")
        for (word in words) {
            val english = escapeCsvField(word.english)
            val turkish = escapeCsvField(word.turkish)
            csv.appendLine("$english,$turkish")
        }
        return csv.toString()
    }

    fun importWordsFromCsv(csvString: String) {
        val lines = csvString.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val dataLines = if (lines.first().lowercase().contains("english")) {
            lines.drop(1)
        } else {
            lines
        }

        for (line in dataLines) {
            val fields = parseCsvLine(line)
            if (fields.size >= 2) {
                val english = fields[0].trim()
                val turkish = fields[1].trim()
                if (english.isNotEmpty() && turkish.isNotEmpty()) {
                    addWord(english, turkish)
                }
            }
        }
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }

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
                        i++
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