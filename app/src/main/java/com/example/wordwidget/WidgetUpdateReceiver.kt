package com.example.wordwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetUpdateReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UPDATE = "com.example.wordwidget.ACTION_UPDATE"
        private const val TAG = "WidgetDebug"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "🔔 onReceive çağrıldı. Action: ${intent.action}")
        try {
            if (intent.action == ACTION_UPDATE || intent.action == Intent.ACTION_BOOT_COMPLETED) {
                updateWidget(context)
                scheduleNextUpdate(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ onReceive içinde kritik hata: ${e.message}", e)
        }
    }

    private fun updateWidget(context: Context) {
        Log.d(TAG, "🔄 updateWidget işlemi başlatıldı")
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WordWidgetProvider::class.java)
            )

            Log.d(TAG, "📦 Ekranda bulunan widget sayısı: ${appWidgetIds.size}")
            if (appWidgetIds.isEmpty()) {
                Log.d(TAG, "⚠️ Hiç widget bulunamadı, işlem iptal edildi.")
                return
            }

            val db = WordDatabase(context)
            val word = db.getRandomWord()
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val now = Date()

            // Gün İsmi
            val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))
            views.setTextViewText(R.id.widget_day, dayFormat.format(now))

            // Kelime Düzeni
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

            // Tarih
            views.setTextViewText(R.id.widget_day_number, SimpleDateFormat("dd", Locale("tr", "TR")).format(now))
            views.setTextViewText(R.id.widget_month, SimpleDateFormat("MMMM", Locale("tr", "TR")).format(now))
            views.setTextViewText(R.id.widget_year, SimpleDateFormat("yyyy", Locale("tr", "TR")).format(now))

            for (id in appWidgetIds) {
                appWidgetManager.updateAppWidget(id, views)
            }
            Log.d(TAG, "✅ Widget başarıyla güncellendi!")

        } catch (e: Exception) {
            Log.e(TAG, "❌ updateWidget içinde hata: ${e.message}", e)
        }
    }

    private fun scheduleNextUpdate(context: Context) {
        try {
            val db = WordDatabase(context)
            val intervalMinutes = db.getUpdateInterval()
            val intervalMillis = intervalMinutes * 60 * 1000L

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = ACTION_UPDATE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + intervalMillis
            Log.d(TAG, "⏰ Alarm kuruluyor. Aralık: $intervalMinutes dk, Tetikleme zamanı: $triggerTime")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    Log.d(TAG, "✅ Exact Alarm başarıyla kuruldu.")
                } else {
                    Log.d(TAG, "❌ Exact Alarm izni YOK! Normal alarm deneniyor (ertelenebilir).")
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Alarm kurma sırasında hata: ${e.message}", e)
        }
    }
}