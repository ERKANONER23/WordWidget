package com.example.wordwidget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var db: WordDatabase
    private lateinit var recyclerView: RecyclerView
    private lateinit var wordAdapter: WordAdapter
    private var words: List<WordPair> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = WordDatabase(this)

        val btnAddWord = findViewById<Button>(R.id.btn_add_word)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        recyclerView = findViewById(R.id.recycler_words)

        btnAddWord.setOnClickListener {
            startActivity(Intent(this, AddWordActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadWords()
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

    // RecyclerView Adapter
    class WordAdapter(
        private var words: List<WordPair>,
        private val onDeleteClick: (WordPair) -> Unit
    ) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

        fun updateWords(newWords: List<WordPair>) {
            words = newWords
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_word, parent, false)
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