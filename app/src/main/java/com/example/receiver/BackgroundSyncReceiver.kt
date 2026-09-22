package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.CouplePreferences
import com.example.data.repository.AppointmentRepository
import com.example.data.repository.NoteRepository
import com.example.util.AppointmentReminderScheduler
import com.example.util.BackgroundSyncScheduler
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BackgroundSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = CouplePreferences(context)
                val profile = prefs.coupleProfile.value
                val intervalMinutes = profile.autoSyncIntervalMinutes
                val coupleCode = profile.coupleCode

                if (intervalMinutes > 0 && coupleCode.isNotBlank()) {
                    val db = AppDatabase.getInstance(context)
                    val apptRepo = AppointmentRepository(context, db.appointmentDao(), prefs)
                    val noteRepo = NoteRepository(context, db.noteDao(), prefs)

                    // 1. Sync appointments & categories
                    try {
                        apptRepo.syncWithPartner(coupleCode)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 2. Sync notes
                    try {
                        noteRepo.syncNotesWithPartner(coupleCode)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 3. Reschedule reminders for any new appointments from partner
                    try {
                        val allActive = apptRepo.getAllActiveAppointments()
                        AppointmentReminderScheduler(context).rescheduleAllReminders(allActive)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // 4. Force widget refresh so home screen widget immediately shows partner's updates
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Schedule next alarm for next interval
                BackgroundSyncScheduler.scheduleNextSync(context)
                pendingResult.finish()
            }
        }
    }
}
