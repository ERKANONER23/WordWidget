package com.example.wordwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetUpdateReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UPDATE = "com.example.wordwidget.ACTION_UPDATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_UPDATE, Intent.ACTION_BOOT_COMPLETED -> {
                updateWidget(context)
                scheduleNextUpdate(context)
            }
        }
    }

    private fun updateWidget(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, WordWidgetProvider::class.java)
        )
        if (appWidgetIds.isEmpty()) return

        val db = WordDatabase(context)
        val word = db.getRandomWord()
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        if (word != null) {
            views.setTextViewText(R.id.widget_word, "${word.english}: ${word.turkish}")
        } else {
            views.setTextViewText(R.id.widget_word, "Kelime ekleyin!")
        }

        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr", "TR"))
        views.setTextViewText(R.id.widget_date, dateFormat.format(Date()))

        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    private fun scheduleNextUpdate(context: Context) {
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

        // Android 12+ için tam alarm izni kontrolü
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                // İzin yoksa yaklaşık alarm kullan (pil optimizasyonuna takılabilir)
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}