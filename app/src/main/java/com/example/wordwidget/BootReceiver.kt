package com.example.wordwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            // Doğrudan servisi başlatan güvenli Android kodu
            val serviceIntent = Intent(context, WidgetUpdateReceiver::class.java) // Servisinizin tam adı neyse onu yazın (Örn: WidgetUpdateService veya WidgetUpdateReceiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Widget'ları güncelle
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WordWidgetProvider::class.java)
            )

            if (appWidgetIds.isNotEmpty()) {
                val updateIntent = Intent(context, WordWidgetProvider::class.java)
                updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                context.sendBroadcast(updateIntent)
            }
        }
    }
}
