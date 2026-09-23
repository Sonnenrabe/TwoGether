package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Appointment
import com.example.data.model.Note
import com.example.ui.util.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PartnerNotificationHelper {

    const val CHANNEL_PARTNER_UPDATES = "partner_calendar_updates_v2"
    const val CHANNEL_APPOINTMENT_REMINDERS = "partner_calendar_reminders_high"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Partner Updates Channel (When partner adds/updates appointments or notes)
            val updatesChannel = NotificationChannel(
                CHANNEL_PARTNER_UPDATES,
                "Partner Updates & Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when your partner adds or updates appointments and notes"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 250)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(updatesChannel)

            // 2. Appointment Reminders Channel
            val remindersChannel = NotificationChannel(
                CHANNEL_APPOINTMENT_REMINDERS,
                "Termine & Erinnerungen",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Erinnerungen für gemeinsame und persönliche Termine"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 200, 350)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    /**
     * Show notification when partner creates or updates an appointment
     */
    fun notifyPartnerAppointment(
        context: Context,
        partnerName: String,
        appointment: Appointment,
        lang: String = "SYSTEM"
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isDe = isGerman(lang)
        val name = if (partnerName.isNotBlank() && partnerName != "Partner") partnerName else if (isDe) "Dein Partner" else "Your Partner"

        val title = if (isDe) {
            "💕 $name hat einen Termin geplant"
        } else {
            "💕 $name planned an appointment"
        }

        val timeStr = if (appointment.isAllDay) {
            if (isDe) "Ganztägig" else "All day"
        } else {
            val sdf = SimpleDateFormat("EEE, d. MMM · HH:mm", Locale.getDefault())
            sdf.format(Date(appointment.startEpochMillis))
        }

        val categoryEmoji = appointment.category.iconEmoji
        val contentText = "$categoryEmoji ${appointment.title} ($timeStr)"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_APPOINTMENT_ID", appointment.id)
            putExtra("EXTRA_DATE_MILLIS", appointment.startEpochMillis)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            appointment.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PARTNER_UPDATES)
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        if (appointment.location.isNotBlank()) {
                            "$contentText\n📍 ${appointment.location}"
                        } else {
                            contentText
                        }
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(appointment.id.hashCode(), notification)
    }

    /**
     * Show notification when partner adds a note or checklist
     */
    fun notifyPartnerNote(
        context: Context,
        partnerName: String,
        note: Note,
        lang: String = "SYSTEM"
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isDe = isGerman(lang)
        val name = if (partnerName.isNotBlank() && partnerName != "Partner") partnerName else if (isDe) "Dein Partner" else "Your Partner"

        val title = if (isDe) {
            "💌 Neue Notiz von $name"
        } else {
            "💌 New note from $name"
        }

        val emoji = note.category.iconEmoji
        val content = if (note.content.isNotBlank()) {
            "$emoji ${note.title}: ${note.content.take(80)}"
        } else {
            "$emoji ${note.title}"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_NOTE_ID", note.id)
            putExtra("EXTRA_OPEN_NOTEBOOK", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            note.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PARTNER_UPDATES)
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(note.id.hashCode(), notification)
    }

    /**
     * Show notification when partner enters our code and requests pairing
     */
    fun notifyPartnerLinkRequest(
        context: Context,
        partnerName: String,
        lang: String = "SYSTEM"
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isDe = isGerman(lang)
        val name = if (partnerName.isNotBlank() && partnerName != "Partner") partnerName else if (isDe) "Dein Partner" else "Your partner"

        val title = if (isDe) "💖 Kopplungsanfrage von $name!" else "💖 Partner link request from $name!"
        val content = if (isDe) {
            "$name möchte eure Kalender verbinden. Tippe hier zum Bestätigen."
        } else {
            "$name wants to link your calendars. Tap here to confirm."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_OPEN_PAIRING", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            "partner_link_req".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PARTNER_UPDATES)
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(9909, notification)
    }

    private fun isGerman(lang: String): Boolean {
        if (lang.equals("DE", ignoreCase = true)) return true
        if (lang.equals("SYSTEM", ignoreCase = true)) {
            return Locale.getDefault().language.lowercase().startsWith("de")
        }
        return false
    }
}
