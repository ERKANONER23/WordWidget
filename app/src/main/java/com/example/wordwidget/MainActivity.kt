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

class MainActivity : AppCompatActivity() {

    private lateinit var db: WordDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var wordAdapter: WordAdapter
    private var words: List<WordPair> = emptyList()

    companion object {
        private const val WORD_WIDGET_FOLDER = "WordWidget"
        private const val BACKUP_FILE_NAME = "words.csv"
    }

    // 1. IMPORT LAUNCHER (Sınıf seviyesinde, onCreate dışı)
    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val csvString = inputStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
                inputStream?.close()

                db.importWordsFromCsv(csvString)
                loadWords()
                updateWidget()
                Toast.makeText(this, "Kelimeler başarıyla içe aktarıldı!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Hata: Dosya okunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        btnAddWord.setOnClickListener { startActivity(Intent(this, AddWordActivity::class.java)) }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }

        btnExport.setOnClickListener { checkAndRequestPermissions() }
        btnImport.setOnClickListener { importLauncher.launch("text/csv") }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadWords()
    }

    private fun checkAndRequestPermissions() {
        when {
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
            else -> { exportToFile() }
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
            if (!wordWidgetDir.exists()) wordWidgetDir.mkdirs()

            val backupFile = File(wordWidgetDir, BACKUP_FILE_NAME)
            val csvContent = db.exportWordsToCsv()

            FileOutputStream(backupFile).use { outputStream ->
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            Toast.makeText(this, "Kelimeler CSV olarak kaydedildi:\n${backupFile.absolutePath}", Toast.LENGTH_LONG).show()
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
        val allWords = db.getAllWords()
        // Son 10 kelimeyi göster (en son eklenenler)
        words = allWords.takeLast(10).reversed() // En yeni en üstte
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