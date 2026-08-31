package com.example.wordwidget

import com.example.wordwidget.R
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddWordActivity : AppCompatActivity() {

    private lateinit var db: WordDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_word)

        db = WordDatabase(this)

        val etEnglish = findViewById<EditText>(R.id.et_english)
        val etTurkish = findViewById<EditText>(R.id.et_turkish)
        val btnSave = findViewById<Button>(R.id.btn_save)

        btnSave.setOnClickListener {
            val english = etEnglish.text.toString().trim()
            val turkish = etTurkish.text.toString().trim()

            if (english.isEmpty() || turkish.isEmpty()) {
                Toast.makeText(this, "Lütfen her iki alanı da doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isAdded = db.addWord(english, turkish)
            if (isAdded) {
                Toast.makeText(this, "Kelime başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                // Başarılıysa ekranı kapat veya listeyi yenile
                finish()
            } else {
                // Kelime zaten varsa kullanıcıyı uyar
                Toast.makeText(this, "Bu kelime zaten listenizde var!", Toast.LENGTH_LONG).show()
            }
            // Widget'ı güncelle
            updateWidget()

            etEnglish.text.clear()
            etTurkish.text.clear()
            etEnglish.requestFocus()
        }
    }

    private fun updateWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(this, WordWidgetProvider::class.java)
        )
        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(this, WordWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            sendBroadcast(intent)
        }
    }
}
