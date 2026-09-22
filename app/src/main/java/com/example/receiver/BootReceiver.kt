package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.util.AppointmentReminderScheduler
import com.example.util.BackgroundSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val appointments = db.appointmentDao().getAllActiveAppointmentsList().map { it.toDomain() }
                    val scheduler = AppointmentReminderScheduler(context)
                    scheduler.rescheduleAllReminders(appointments)

                    // Reschedule background sync (even when app is not open)
                    BackgroundSyncScheduler.scheduleNextSync(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
