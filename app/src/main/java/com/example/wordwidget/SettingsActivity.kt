package com.example.wordwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var db: WordDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        db = WordDatabase(this)

        val seekBar = findViewById<SeekBar>(R.id.seekbar_interval)
        val tvInterval = findViewById<TextView>(R.id.tv_interval_value)
        val btnEnableExactAlarm = findViewById<Button>(R.id.btn_enable_exact_alarm)

        // Mevcut süreyi yükle
        val currentInterval = db.getUpdateInterval()
        seekBar.max = 119
        seekBar.progress = currentInterval - 1
        tvInterval.text = "$currentInterval dakika"

        // Seekbar hareket ettiğinde
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress + 1
                tvInterval.text = "$minutes dakika"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val minutes = seekBar!!.progress + 1
                db.setUpdateInterval(minutes)
                Toast.makeText(this@SettingsActivity, "Aralık $minutes dakika olarak ayarlandı", Toast.LENGTH_SHORT).show()
                resetAlarm(this@SettingsActivity)
            }
        })

        // Android 12+ Tam Alarm İzni Butonu
        btnEnableExactAlarm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "İzin zaten verilmiş durumda.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bu ayar sadece Android 12 ve üzeri için gereklidir.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        fun resetAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = WidgetUpdateReceiver.ACTION_UPDATE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            context.sendBroadcast(intent)
        }
    }
}