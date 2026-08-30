package com.example.wordwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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
        try {
            if (intent.action == ACTION_UPDATE || intent.action == Intent.ACTION_BOOT_COMPLETED) {
                updateWidget(context)
                scheduleNextUpdate(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onReceive hatası: ${e.message}", e)
        }
    }

    private fun updateWidget(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WordWidgetProvider::class.java)
            )
            if (appWidgetIds.isEmpty()) return

            val db = WordDatabase(context)
            val word = db.getRandomWord()

            // Geçmişi kaydet
            if (word != null) {
                db.logShownWord(word)
            }

            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val now = Date()

            // Her widget için aynı layout'u kullan ama boyuta göre içeriği ayarla
            for (appWidgetId in appWidgetIds) {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                
                Log.d(TAG, "Widget ID: $appWidgetId, MinWidth: $minWidth, MinHeight: $minHeight")

                // Widget boyutuna göre bazı view'ları gizle/göster
                val isSmallWidget = minHeight <= 50
                
                // Küçük widget'larda saat ve gün ismi gösterme
                if (isSmallWidget) {
                    try {
                        views.setViewVisibility(R.id.widget_day, android.view.View.GONE)
                        views.setViewVisibility(R.id.widget_year, android.view.View.GONE)
                    } catch (e: Exception) {
                        Log.w(TAG, "Küçük widget view'ları bulunamadı")
                    }
                } else {
                    try {
                        val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))
                        views.setTextViewText(R.id.widget_day, dayFormat.format(now))
                        views.setTextViewText(R.id.widget_year, SimpleDateFormat("yyyy", Locale("tr", "TR")).format(now))
                        views.setViewVisibility(R.id.widget_day, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.widget_year, android.view.View.VISIBLE)
                    } catch (e: Exception) {
                        Log.w(TAG, "Ana layout view'ları bulunamadı")
                    }
                }

                // Kelime gösterimi
                if (word != null) {
                    val totalLength = word.english.length + word.turkish.length
                    if (totalLength <= 10 && !isSmallWidget) {
                        // Yatay düzen (sadece büyük widgetlarda)
                        try {
                            views.setViewVisibility(R.id.container_horizontal, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.container_vertical, android.view.View.GONE)
                            views.setTextViewText(R.id.widget_english_h, word.english)
                            views.setTextViewText(R.id.widget_turkish_h, word.turkish)
                        } catch (e: Exception) {
                            try {
                                views.setViewVisibility(R.id.container_horizontal, android.view.View.GONE)
                                views.setViewVisibility(R.id.container_vertical, android.view.View.VISIBLE)
                                views.setTextViewText(R.id.widget_english, word.english)
                                views.setTextViewText(R.id.widget_turkish, word.turkish)
                            } catch (e2: Exception) {
                                Log.w(TAG, "Kelime container'ları bulunamadı")
                            }
                        }
                    } else {
                        // Dikey düzen (küçük widgetlar veya uzun kelimeler)
                        try {
                            views.setViewVisibility(R.id.container_horizontal, android.view.View.GONE)
                            views.setViewVisibility(R.id.container_vertical, android.view.View.VISIBLE)
                            views.setTextViewText(R.id.widget_english, word.english)
                            views.setTextViewText(R.id.widget_turkish, word.turkish)
                        } catch (e: Exception) {
                            try {
                                views.setViewVisibility(R.id.container_horizontal, android.view.View.VISIBLE)
                                views.setViewVisibility(R.id.container_vertical, android.view.View.GONE)
                                views.setTextViewText(R.id.widget_english_h, word.english)
                                views.setTextViewText(R.id.widget_turkish_h, word.turkish)
                            } catch (e2: Exception) {
                                Log.w(TAG, "Kelime container'ları bulunamadı")
                            }
                        }
                    }
                } else {
                    try {
                        views.setViewVisibility(R.id.container_horizontal, android.view.View.VISIBLE)
                        views.setViewVisibility(R.id.container_vertical, android.view.View.GONE)
                        views.setTextViewText(R.id.widget_english_h, "Kelime ekleyin")
                        views.setTextViewText(R.id.widget_turkish_h, "")
                    } catch (e: Exception) {
                        Log.w(TAG, "Varsayılan kelime container'ları bulunamadı")
                    }
                }

                // Tarih gösterimi - tüm widget'lar için
                try {
                    views.setTextViewText(R.id.widget_day_number, SimpleDateFormat("dd", Locale("tr", "TR")).format(now))
                    views.setTextViewText(R.id.widget_month, SimpleDateFormat("MMMM", Locale("tr", "TR")).format(now))
                } catch (e: Exception) {
                    Log.w(TAG, "Tarih view'ları bulunamadı")
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateWidget hatası: ${e.message}", e)
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Alarm hatası: ${e.message}", e)
        }
    }
}