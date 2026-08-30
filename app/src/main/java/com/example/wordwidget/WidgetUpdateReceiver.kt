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

            // Her widget için ayrı ayrı işlem yap
            for (appWidgetId in appWidgetIds) {
                try {
                    val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
                    val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                    
                    Log.d(TAG, "Widget ID: $appWidgetId, MinWidth: $minWidth, MinHeight: $minHeight, MaxWidth: $maxWidth, MaxHeight: $maxHeight")

                    // Widget boyutuna göre layout seç
                    val layoutResId = when {
                        minHeight <= 50 && minWidth <= 120 -> R.layout.widget_layout_2x1
                        minHeight <= 50 && minWidth <= 180 -> R.layout.widget_layout_3x1
                        minHeight <= 50 && minWidth > 180 -> R.layout.widget_layout_4x1
                        else -> R.layout.widget_layout
                    }

                    Log.d(TAG, "Seçilen layout: ${
                        when(layoutResId) {
                            R.layout.widget_layout_2x1 -> "2x1"
                            R.layout.widget_layout_3x1 -> "3x1"
                            R.layout.widget_layout_4x1 -> "4x1"
                            else -> "3x2 (ana)"
                        }
                    }")

                    val views = RemoteViews(context.packageName, layoutResId)
                    val now = Date()

                    // Kelime gösterimi - tüm layoutlar için ortak alanlar
                    if (word != null) {
                        // Yatay container varsa
                        try {
                            views.setViewVisibility(R.id.container_horizontal, android.view.View.VISIBLE)
                            views.setTextViewText(R.id.widget_english_h, word.english)
                            views.setTextViewText(R.id.widget_turkish_h, word.turkish)
                        } catch (e: Exception) {
                            // Dikey container dene
                            try {
                                views.setViewVisibility(R.id.container_vertical, android.view.View.VISIBLE)
                                views.setTextViewText(R.id.widget_english, word.english)
                                views.setTextViewText(R.id.widget_turkish, word.turkish)
                            } catch (e2: Exception) {
                                Log.w(TAG, "Kelime container'ları bulunamadı")
                            }
                        }
                    } else {
                        try {
                            views.setTextViewText(R.id.widget_english_h, "Kelime ekleyin")
                        } catch (e: Exception) {
                            try {
                                views.setTextViewText(R.id.widget_english, "Kelime ekleyin")
                            } catch (e2: Exception) {
                                Log.w(TAG, "Kelime alanları bulunamadı")
                            }
                        }
                    }

                    // Tarih gösterimi - tüm layoutlar için
                    try {
                        views.setTextViewText(R.id.widget_day_number, SimpleDateFormat("dd", Locale("tr", "TR")).format(now))
                        views.setTextViewText(R.id.widget_month, SimpleDateFormat("MMMM", Locale("tr", "TR")).format(now))
                    } catch (e: Exception) {
                        Log.w(TAG, "Tarih view'ları bulunamadı")
                    }

                    // Sadece ana layout'ta (3x2) olan alanlar
                    if (layoutResId == R.layout.widget_layout) {
                        try {
                            val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))
                            views.setTextViewText(R.id.widget_day, dayFormat.format(now))
                            views.setTextViewText(R.id.widget_year, SimpleDateFormat("yyyy", Locale("tr", "TR")).format(now))
                            views.setViewVisibility(R.id.widget_day, android.view.View.VISIBLE)
                            views.setViewVisibility(R.id.widget_year, android.view.View.VISIBLE)
                        } catch (e: Exception) {
                            Log.w(TAG, "Ana layout view'ları bulunamadı")
                        }
                    } else {
                        // Küçük widget'larda gün ismini göster (yıl yok)
                        try {
                            val dayFormat = SimpleDateFormat("EEE", Locale("tr", "TR"))
                            views.setTextViewText(R.id.widget_day, dayFormat.format(now))
                            views.setViewVisibility(R.id.widget_day, android.view.View.VISIBLE)
                        } catch (e: Exception) {
                            Log.w(TAG, "Gün view'sı bulunamadı")
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    Log.e(TAG, "Widget ID $appWidgetId güncellenirken hata: ${e.message}", e)
                }
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