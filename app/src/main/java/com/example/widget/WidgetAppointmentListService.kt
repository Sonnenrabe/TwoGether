package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.CouplePreferences
import com.example.data.model.Appointment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WidgetAppointmentListService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetAppointmentListFactory(applicationContext, intent)
    }
}

class WidgetAppointmentListFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var appointments = listOf<Appointment>()
    private var primaryTextColor = Color.BLACK
    private var secondaryTextColor = Color.GRAY
    private var eventBg = R.drawable.widget_event_item_bg

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            val prefs = context.getSharedPreferences("widget_prefs_$appWidgetId", Context.MODE_PRIVATE)
            val selectedMillis = prefs.getLong("selected_millis", System.currentTimeMillis())

            val couplePrefs = CouplePreferences(context)
            val profile = couplePrefs.coupleProfile.value
            val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val isWidgetDark = when (profile.widgetThemeMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }
            primaryTextColor = if (isWidgetDark) Color.parseColor("#F1F5F9") else Color.parseColor("#1F1F24")
            secondaryTextColor = if (isWidgetDark) Color.parseColor("#94A3B8") else Color.parseColor("#6B7280")
            eventBg = if (isWidgetDark) R.drawable.widget_event_item_bg_dark else R.drawable.widget_event_item_bg

            val selCal = Calendar.getInstance().apply { timeInMillis = selectedMillis }
            val dayStart = (selCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = (selCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val db = AppDatabase.getInstance(context)
            appointments = runBlocking(Dispatchers.IO) {
                db.appointmentDao().getAppointmentsForDaySync(dayStart, dayEnd).map { it.toDomain() }
            }.sortedBy { it.startEpochMillis }
        } catch (e: Exception) {
            android.util.Log.e("WidgetListFactory", "Error loading data for widget $appWidgetId: ${e.message}", e)
            appointments = emptyList()
        }
    }

    override fun onDestroy() {
        appointments = emptyList()
    }

    override fun getCount(): Int = appointments.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_appointment_item)
        if (position >= appointments.size) return views

        val appt = appointments[position]
        views.setInt(R.id.widget_item_container, "setBackgroundResource", eventBg)
        views.setTextViewText(R.id.widget_item_badge, appt.ownerType.badge)
        views.setTextViewText(R.id.widget_item_time, formatEventTime(appt))
        views.setTextViewText(R.id.widget_item_title, appt.title)
        views.setTextColor(R.id.widget_item_title, primaryTextColor)
        views.setTextColor(R.id.widget_item_time, secondaryTextColor)

        // Fill-in Intent: merges with pending intent template to launch MainActivity
        val fillInIntent = Intent().apply {
            putExtra("EXTRA_DATE_MILLIS", appt.startEpochMillis)
            putExtra("EXTRA_APPOINTMENT_ID", appt.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun formatEventTime(appointment: Appointment): String {
        if (appointment.isAllDay) return "All day"
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(appointment.startEpochMillis)
    }
}
