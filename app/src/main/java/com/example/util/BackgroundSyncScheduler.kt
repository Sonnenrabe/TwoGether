package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.CouplePreferences
import com.example.receiver.BackgroundSyncReceiver

object BackgroundSyncScheduler {

    const val ACTION_BACKGROUND_SYNC = "com.example.action.BACKGROUND_SYNC"
    private const val REQUEST_CODE_BACKGROUND_SYNC = 88221

    fun scheduleNextSync(context: Context) {
        val prefs = CouplePreferences(context)
        val profile = prefs.coupleProfile.value
        val intervalMinutes = profile.autoSyncIntervalMinutes
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, BackgroundSyncReceiver::class.java).apply {
            action = ACTION_BACKGROUND_SYNC
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BACKGROUND_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // If interval is 0 (Manual only) or no couple code, cancel background alarms
        if (intervalMinutes <= 0 || profile.coupleCode.isBlank()) {
            try {
                alarmManager.cancel(pendingIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        // Calculate next trigger time: interval in milliseconds (at least 1 minute)
        val intervalMillis = intervalMinutes.coerceAtLeast(1) * 60 * 1000L
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelSync(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, BackgroundSyncReceiver::class.java).apply {
            action = ACTION_BACKGROUND_SYNC
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BACKGROUND_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
