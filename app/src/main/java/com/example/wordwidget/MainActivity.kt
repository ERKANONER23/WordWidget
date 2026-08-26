package com.example.wordwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.util.Log

class MainActivity : AppCompatActivity() {

    private lateinit var db: WordDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var wordAdapter: WordAdapter
    private var words: List<WordPair> = emptyList()

    companion object {
        private const val WORD_WIDGET_FOLDER = "WordWidget"
        private const val BACKUP_FILE_NAME = "words_backup.json"
    }

    // İzin isteme launcher'ı (Android 6-10)
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            exportToFile()
        } else {
            Toast.makeText(this, "Dosya kaydetme için izin gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    // Tüm dosyalara erişim izni launcher'ı (Android 11+)
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                exportToFile()
            } else {
                Toast.makeText(this, "Dosya yönetimi izni gerekli", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = WordDatabase(this)

        val btnAddWord = findViewById<Button>(R.id.btn_add_word)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val btnExport = findViewById<Button>(R.id.btn_export)
        val btnImport = findViewById<Button>(R.id.btn_import)
        recyclerView = findViewById(R.id.recycler_words)

        val btnTestUpdate = findViewById<Button>(R.id.btn_test_update)
        btnTestUpdate.setOnClickListener {
            try {
                Log.d("WidgetDebug", "🚀 Manuel güncelleme butonuna basıldı.")

                // ComponentName ile doğrudan receiver'a gönder
                val intent = Intent(this, WidgetUpdateReceiver::class.java).apply {
                    action = WidgetUpdateReceiver.ACTION_UPDATE
                    setComponent(android.content.ComponentName(
                        "com.example.wordwidget",
                        "com.example.wordwidget.WidgetUpdateReceiver"
                    ))
                }
                sendBroadcast(intent)
                Toast.makeText(this, "Güncelleme komutu gönderildi!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("WidgetDebug", "❌ Broadcast gönderilirken hata: ${e.message}", e)
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnAddWord.setOnClickListener {
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnExport.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnImport.setOnClickListener {
            importFromFile()
        }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadWords()
    }

    private fun checkAndRequestPermissions() {
        when {
            // Android 11+ (API 30+)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (Environment.isExternalStorageManager()) {
                    exportToFile()
                } else {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    manageStorageLauncher.launch(intent)
                }
            }
            // Android 6-10 (API 23-29)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                val permissions = arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )

                val allGranted = permissions.all {
                    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }

                if (allGranted) {
                    exportToFile()
                } else {
                    permissionLauncher.launch(permissions)
                }
            }
            // Android 5 ve altı
            else -> {
                exportToFile()
            }
        }
    }

    private fun exportToFile() {
        val wordsList = db.getAllWords()
        if (wordsList.isEmpty()) {
            Toast.makeText(this, "Dışa aktarılacak kelime yok!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val wordWidgetDir = File(downloadsDir, WORD_WIDGET_FOLDER)

            if (!wordWidgetDir.exists()) {
                wordWidgetDir.mkdirs()
            }

            val backupFile = File(wordWidgetDir, BACKUP_FILE_NAME)
            val jsonContent = db.exportWordsToJson()

            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(jsonContent.toByteArray())
            }

            Toast.makeText(
                this,
                "Kelimeler kaydedildi:\n${backupFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromFile() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val wordWidgetDir = File(downloadsDir, WORD_WIDGET_FOLDER)
            val backupFile = File(wordWidgetDir, BACKUP_FILE_NAME)

            if (!backupFile.exists()) {
                Toast.makeText(
                    this,
                    "Yedek dosyası bulunamadı:\n${backupFile.absolutePath}\n\nÖnce dışa aktarım yapın.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val jsonString = FileInputStream(backupFile).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }

            db.importWordsFromJson(jsonString)
            loadWords()
            updateWidget()

            val wordCount = db.getAllWords().size
            Toast.makeText(
                this,
                "$wordCount kelime başarıyla içe aktarıldı!",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        wordAdapter = WordAdapter(
            words = words,
            onDeleteClick = { word ->
                db.deleteWord(word.id)
                loadWords()
                updateWidget()
                Toast.makeText(this, "Kelime silindi", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = wordAdapter
    }

    private fun loadWords() {
        words = db.getAllWords()
        wordAdapter.updateWords(words)
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

    // --- RecyclerView Adapter ---
    class WordAdapter(
        private var words: List<WordPair>,
        private val onDeleteClick: (WordPair) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

        fun updateWords(newWords: List<WordPair>) {
            words = newWords
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_word, parent, false)
            return WordViewHolder(view)
        }

        override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
            val word = words[position]
            holder.tvEnglish.text = word.english
            holder.tvTurkish.text = word.turkish
            holder.btnDelete.setOnClickListener { onDeleteClick(word) }
        }

        override fun getItemCount() = words.size

        class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvEnglish: TextView = itemView.findViewById(R.id.tv_english)
            val tvTurkish: TextView = itemView.findViewById(R.id.tv_turkish)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        }
    }
}