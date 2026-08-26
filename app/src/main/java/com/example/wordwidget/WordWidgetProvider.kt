package com.example.wordwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

class WordWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Widget ilk eklendiğinde servisi güvenli şekilde başlatır
        val serviceIntent = Intent(context, WidgetUpdateReceiver::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    override fun onDisabled(context: Context) {
        // Widget kaldırıldığında servisi durdurur
        val serviceIntent = Intent(context, WidgetUpdateReceiver::class.java)
        context.stopService(serviceIntent)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WORD) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, WordWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val db = WordDatabase(context)
        val word = db.getRandomWord()

        if (word != null) {
            views.setTextViewText(
                R.id.widget_word,
                "${word.english}: ${word.turkish}"
            )
        } else {
            views.setTextViewText(R.id.widget_word, "Kelime ekleyin!")
        }

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
        val currentDate = dateFormat.format(Date())
        views.setTextViewText(R.id.widget_date, currentDate)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        const val ACTION_UPDATE_WORD = "com.example.wordwidget.UPDATE_WORD"
    }
}
