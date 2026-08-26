package com.example.wordwidget

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var db: WordDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var wordAdapter: WordAdapter
    private lateinit var tvListTitle: TextView
    private var words: List<WordPair> = emptyList()

    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton

    companion object {
        private const val WORD_WIDGET_FOLDER = "WordWidget"
    }

    // ══════════════════════════════════════════════════════════
    // EXPORT LAUNCHER (Dosya kaydetme - izin gerektirmez)
    // ══════════════════════════════════════════════════════════
    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            try {
                val csvContent = db.exportWordsToCsv()
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "Kelimeler başarıyla kaydedildi!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // IMPORT LAUNCHER (Dosya açma - tüm dosya tipleri)
    // ══════════════════════════════════════════════════════════
    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = contentResolver.openInputStream(it)
                val csvString = inputStream?.bufferedReader(Charsets.UTF_8)?.readText() ?: ""
                inputStream?.close()

                if (csvString.isNotEmpty()) {
                    db.importWordsFromCsv(csvString)
                    loadLast10Words() // Import sonrası her zaman son 10'a dön
                    updateWidget()
                    val wordCount = db.getAllWords().size
                    Toast.makeText(this, "$wordCount kelime başarıyla içe aktarıldı!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Dosya boş veya okunamadı", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
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

        etSearch = findViewById(R.id.et_search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        tvListTitle = findViewById(R.id.tv_list_title)
        recyclerView = findViewById(R.id.recycler_words)

        // Arama kutusu text rengini beyaz yap (mavi arka plan için)
        etSearch.setTextColor(Color.WHITE)
        etSearch.setHintTextColor(Color.parseColor("#B3FFFFFF"))

        btnAddWord.setOnClickListener {
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnExport.setOnClickListener {
            exportToFile()
        }

        btnImport.setOnClickListener {
            importLauncher.launch("*/*")
        }

        // ═════════════════════════════════════════════════════
        // ANLIK ARAMA MANTIĞI
        // ══════════════════════════════════════════════════════
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()

                // Çarpı butonunu göster/gizle
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.isBlank()) {
                    loadLast10Words() // Başlık otomatik "Son Eklenen 10 Kelime" olur
                } else {
                    val searchResults = db.searchWords(query)
                    wordAdapter.updateWords(searchResults)

                    // Başlığı "Arama Sonuçları" yap
                    tvListTitle.text = "Arama Sonuçları (${searchResults.size})"
                }
            }
        })

        // ══════════════════════════════════════════════════════
        // ÇARPI BUTONUNA BASINCA TEMİZLE
        // ══════════════════════════════════════════════════════
        btnClearSearch.setOnClickListener {
            etSearch.text.clear() // Bu, afterTextChanged'i tetikler ve listeyi son 10'a döndürür
        }

        setupRecyclerView()
        loadLast10Words() // Başlangıçta son 10 kelimeyi yükle
    }

    override fun onResume() {
        super.onResume()
        // Sayfaya dönüldüğünde arama kutusu boşsa listeyi yenile
        if (etSearch.text.isBlank()) {
            loadLast10Words()
        }
    }

    // ══════════════════════════════════════════════════════════
    // EXPORT FONKSİYONU
    // Dosya adı: KelimeWidget_YYYYMMDD_HHMM.csv
    // ═════════════════════════════════════════════════════════
    private fun exportToFile() {
        val wordsList = db.getAllWords()
        if (wordsList.isEmpty()) {
            Toast.makeText(this, "Dışa aktarılacak kelime yok!", Toast.LENGTH_SHORT).show()
            return
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val fileName = "KelimeWidget_${dateFormat.format(Date())}.csv"
        exportLauncher.launch(fileName)
    }

    // ══════════════════════════════════════════════════════════
    // SON 10 KELİMEYİ YÜKLE
    // ══════════════════════════════════════════════════════════
    private fun loadLast10Words() {
        val allWords = db.getAllWords()
        words = allWords.takeLast(10).reversed()
        wordAdapter.updateWords(words)

        // Başlığı güncelle
        tvListTitle.text = "Son Eklenen 10 Kelime"
    }

    // ══════════════════════════════════════════════════════════
    // KELİME DÜZENLEME DİALOGU (Liste öğesine tıklayınca açılır)
    // ══════════════════════════════════════════════════════════
    private fun showEditWordDialog(word: WordPair) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val englishInput = EditText(this).apply {
            hint = "İngilizce"
            setText(word.english)
            setPadding(0, 20, 0, 20)
        }

        val turkishInput = EditText(this).apply {
            hint = "Türkçe"
            setText(word.turkish)
            setPadding(0, 20, 0, 20)
        }

        layout.addView(englishInput)
        layout.addView(turkishInput)

        AlertDialog.Builder(this)
            .setTitle("✏️ Kelimeyi Düzenle")
            .setView(layout)
            .setPositiveButton("Kaydet") { dialog, _ ->
                val newEnglish = englishInput.text.toString()
                val newTurkish = turkishInput.text.toString()

                if (newEnglish.isNotBlank() && newTurkish.isNotBlank()) {
                    db.updateWord(word.id, newEnglish, newTurkish)

                    // Arama modundaysak aramayı yenile, değilse son 10'u yenile
                    if (etSearch.text.isNotBlank()) {
                        wordAdapter.updateWords(db.searchWords(etSearch.text.toString()))
                    } else {
                        loadLast10Words()
                    }
                    updateWidget()
                    Toast.makeText(this, "Kelime güncellendi", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Boş bırakılamaz", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // ══════════════════════════════════════════════════════════
    // RECYCLER VIEW KURULUMU
    // ══════════════════════════════════════════════════════════
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        wordAdapter = WordAdapter(
            words = words,
            onDeleteClick = { word ->
                db.deleteWord(word.id)
                // Silme işleminden sonra mevcut görünümü koru
                if (etSearch.text.isNotBlank()) {
                    wordAdapter.updateWords(db.searchWords(etSearch.text.toString()))
                } else {
                    loadLast10Words()
                }
                updateWidget()
                Toast.makeText(this, "Kelime silindi", Toast.LENGTH_SHORT).show()
            },
            onItemClick = { word ->
                // Kelimeye tıklayınca düzenleme ekranı açılır
                showEditWordDialog(word)
            }
        )
        recyclerView.adapter = wordAdapter
    }

    // ══════════════════════════════════════════════════════════
    // WIDGET GÜNCELLEME
    // ══════════════════════════════════════════════════════════
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

    // ══════════════════════════════════════════════════════════
    // RECYCLER VIEW ADAPTER
    // ══════════════════════════════════════════════════════════
    class WordAdapter(
        private var words: List<WordPair>,
        private val onDeleteClick: (WordPair) -> Unit,
        private val onItemClick: ((WordPair) -> Unit)? = null
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

            // Tüm satıra tıklayınca düzenleme açılır
            holder.itemView.setOnClickListener {
                onItemClick?.invoke(word)
            }
        }

        override fun getItemCount() = words.size

        class WordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvEnglish: TextView = itemView.findViewById(R.id.tv_english)
            val tvTurkish: TextView = itemView.findViewById(R.id.tv_turkish)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
        }
    }
}