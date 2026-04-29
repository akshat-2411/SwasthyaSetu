package com.example.swasthyasetu.repository

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.swasthyasetu.receiver.ReminderBroadcastReceiver
import kotlin.jvm.java

class ReminderRepository {
    fun scheduleAlarm(context: Context, timeInMillis: Long, medicineName: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        if (alarmManager == null) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(ReminderBroadcastReceiver.EXTRA_MEDICINE_NAME, medicineName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        } catch (e: SecurityException) {
            Log.e("ReminderRepository", "Missing permissions.", e)
        }
    }

    fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}