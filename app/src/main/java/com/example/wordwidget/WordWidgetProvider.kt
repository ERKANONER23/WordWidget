package com.example.wordwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        super.onEnabled(context)
        // İlk widget eklendiğinde güncelleme ve alarm döngüsünü başlat
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = WidgetUpdateReceiver.ACTION_UPDATE
        }
        context.sendBroadcast(intent)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Son widget kaldırıldığında gereksiz alarmı iptal et (Pil tasarrufu için)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = WidgetUpdateReceiver.ACTION_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val db = WordDatabase(context)
        val word = db.getRandomWord()
        val now = Date()

        // Gün İsmi
        val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))
        views.setTextViewText(R.id.widget_day, dayFormat.format(now))

        // Kelime Düzeni (Kısa/Uzun kontrolü)
        if (word != null) {
            val totalLength = word.english.length + word.turkish.length
            if (totalLength <= 18) {
                views.setViewVisibility(R.id.container_horizontal, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.container_vertical, android.view.View.GONE)
                views.setTextViewText(R.id.widget_english_h, word.english)
                views.setTextViewText(R.id.widget_turkish_h, word.turkish)
            } else {
                views.setViewVisibility(R.id.container_horizontal, android.view.View.GONE)
                views.setViewVisibility(R.id.container_vertical, android.view.View.VISIBLE)
                views.setTextViewText(R.id.widget_english, word.english)
                views.setTextViewText(R.id.widget_turkish, word.turkish)
            }
        } else {
            views.setViewVisibility(R.id.container_horizontal, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.container_vertical, android.view.View.GONE)
            views.setTextViewText(R.id.widget_english_h, "Kelime ekleyin")
            views.setTextViewText(R.id.widget_turkish_h, "")
        }

        // Tarih Bilgileri
        views.setTextViewText(R.id.widget_day_number, SimpleDateFormat("dd", Locale("tr", "TR")).format(now))
        views.setTextViewText(R.id.widget_month, SimpleDateFormat("MMMM", Locale("tr", "TR")).format(now))
        views.setTextViewText(R.id.widget_year, SimpleDateFormat("yyyy", Locale("tr", "TR")).format(now))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}