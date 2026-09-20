package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.model.Appointment
import com.example.receiver.AppointmentAlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppointmentReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleReminder(appointment: Appointment) {
        if (!appointment.hasReminder) {
            cancelReminder(appointment.id)
            return
        }

        val reminderOffsetMillis = appointment.reminderMinutesBefore * 60 * 1000L
        val reminderTime = appointment.startEpochMillis - reminderOffsetMillis
        val now = System.currentTimeMillis()

        // Don't schedule reminders in the past
        if (reminderTime <= now) {
            Log.d("ReminderScheduler", "Skipping past reminder for ${appointment.title}")
            return
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTimeStr = timeFormat.format(Date(appointment.startEpochMillis))

        val message = when {
            appointment.reminderMinutesBefore <= 0 -> "Termin beginnt jetzt ($startTimeStr)"
            appointment.reminderMinutesBefore < 60 -> "In ${appointment.reminderMinutesBefore} Minuten ($startTimeStr)"
            appointment.reminderMinutesBefore == 60 -> "In 1 Stunde ($startTimeStr)"
            appointment.reminderMinutesBefore == 120 -> "In 2 Stunden ($startTimeStr)"
            appointment.reminderMinutesBefore == 1440 -> "Morgen um $startTimeStr"
            appointment.reminderMinutesBefore == 2880 -> "In 2 Tagen um $startTimeStr"
            else -> "In ${appointment.reminderMinutesBefore / 60} Std ($startTimeStr)"
        } + if (appointment.location.isNotBlank()) " • 📍 ${appointment.location}" else ""

        val intent = Intent(context, AppointmentAlarmReceiver::class.java).apply {
            putExtra("EXTRA_ID", appointment.id)
            putExtra("EXTRA_TITLE", "Termin: ${appointment.title}")
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_START_TIME", appointment.startEpochMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
            Log.d("ReminderScheduler", "Scheduled reminder for ${appointment.title} at $reminderTime")
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "SecurityException scheduling alarm: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } catch (ex: Exception) {
                Log.e("ReminderScheduler", "Fallback alarm failed: ${ex.message}")
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule alarm: ${e.message}")
        }
    }

    fun cancelReminder(appointmentId: String) {
        val intent = Intent(context, AppointmentAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderScheduler", "Cancelled reminder for $appointmentId")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel alarm: ${e.message}")
        }
    }

    fun rescheduleAllReminders(appointments: List<Appointment>) {
        val now = System.currentTimeMillis()
        appointments.filter { it.hasReminder && it.startEpochMillis > now }.forEach {
            scheduleReminder(it)
        }
    }
}
