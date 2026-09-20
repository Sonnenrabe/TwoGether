package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.CouplePreferences
import com.example.data.model.Appointment
import com.example.data.model.OwnerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WidgetUpdateHelper {

    private val DAY_CELL_IDS = intArrayOf(
        R.id.widget_day_cell_0, R.id.widget_day_cell_1, R.id.widget_day_cell_2, R.id.widget_day_cell_3, R.id.widget_day_cell_4, R.id.widget_day_cell_5, R.id.widget_day_cell_6,
        R.id.widget_day_cell_7, R.id.widget_day_cell_8, R.id.widget_day_cell_9, R.id.widget_day_cell_10, R.id.widget_day_cell_11, R.id.widget_day_cell_12, R.id.widget_day_cell_13,
        R.id.widget_day_cell_14, R.id.widget_day_cell_15, R.id.widget_day_cell_16, R.id.widget_day_cell_17, R.id.widget_day_cell_18, R.id.widget_day_cell_19, R.id.widget_day_cell_20,
        R.id.widget_day_cell_21, R.id.widget_day_cell_22, R.id.widget_day_cell_23, R.id.widget_day_cell_24, R.id.widget_day_cell_25, R.id.widget_day_cell_26, R.id.widget_day_cell_27,
        R.id.widget_day_cell_28, R.id.widget_day_cell_29, R.id.widget_day_cell_30, R.id.widget_day_cell_31, R.id.widget_day_cell_32, R.id.widget_day_cell_33, R.id.widget_day_cell_34,
        R.id.widget_day_cell_35, R.id.widget_day_cell_36, R.id.widget_day_cell_37, R.id.widget_day_cell_38, R.id.widget_day_cell_39, R.id.widget_day_cell_40, R.id.widget_day_cell_41
    )

    private val DAY_NUM_IDS = intArrayOf(
        R.id.widget_day_num_0, R.id.widget_day_num_1, R.id.widget_day_num_2, R.id.widget_day_num_3, R.id.widget_day_num_4, R.id.widget_day_num_5, R.id.widget_day_num_6,
        R.id.widget_day_num_7, R.id.widget_day_num_8, R.id.widget_day_num_9, R.id.widget_day_num_10, R.id.widget_day_num_11, R.id.widget_day_num_12, R.id.widget_day_num_13,
        R.id.widget_day_num_14, R.id.widget_day_num_15, R.id.widget_day_num_16, R.id.widget_day_num_17, R.id.widget_day_num_18, R.id.widget_day_num_19, R.id.widget_day_num_20,
        R.id.widget_day_num_21, R.id.widget_day_num_22, R.id.widget_day_num_23, R.id.widget_day_num_24, R.id.widget_day_num_25, R.id.widget_day_num_26, R.id.widget_day_num_27,
        R.id.widget_day_num_28, R.id.widget_day_num_29, R.id.widget_day_num_30, R.id.widget_day_num_31, R.id.widget_day_num_32, R.id.widget_day_num_33, R.id.widget_day_num_34,
        R.id.widget_day_num_35, R.id.widget_day_num_36, R.id.widget_day_num_37, R.id.widget_day_num_38, R.id.widget_day_num_39, R.id.widget_day_num_40, R.id.widget_day_num_41
    )

    private val DAY_DOTS_IDS = intArrayOf(
        R.id.widget_day_dots_0, R.id.widget_day_dots_1, R.id.widget_day_dots_2, R.id.widget_day_dots_3, R.id.widget_day_dots_4, R.id.widget_day_dots_5, R.id.widget_day_dots_6,
        R.id.widget_day_dots_7, R.id.widget_day_dots_8, R.id.widget_day_dots_9, R.id.widget_day_dots_10, R.id.widget_day_dots_11, R.id.widget_day_dots_12, R.id.widget_day_dots_13,
        R.id.widget_day_dots_14, R.id.widget_day_dots_15, R.id.widget_day_dots_16, R.id.widget_day_dots_17, R.id.widget_day_dots_18, R.id.widget_day_dots_19, R.id.widget_day_dots_20,
        R.id.widget_day_dots_21, R.id.widget_day_dots_22, R.id.widget_day_dots_23, R.id.widget_day_dots_24, R.id.widget_day_dots_25, R.id.widget_day_dots_26, R.id.widget_day_dots_27,
        R.id.widget_day_dots_28, R.id.widget_day_dots_29, R.id.widget_day_dots_30, R.id.widget_day_dots_31, R.id.widget_day_dots_32, R.id.widget_day_dots_33, R.id.widget_day_dots_34,
        R.id.widget_day_dots_35, R.id.widget_day_dots_36, R.id.widget_day_dots_37, R.id.widget_day_dots_38, R.id.widget_day_dots_39, R.id.widget_day_dots_40, R.id.widget_day_dots_41
    )

    private val WEEKDAY_IDS = intArrayOf(
        R.id.widget_weekday_0, R.id.widget_weekday_1, R.id.widget_weekday_2,
        R.id.widget_weekday_3, R.id.widget_weekday_4, R.id.widget_weekday_5, R.id.widget_weekday_6
    )

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, PartnerCalendarWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                for (id in appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_appointment_list)
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    suspend fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val prefs = context.getSharedPreferences("widget_prefs_$appWidgetId", Context.MODE_PRIVATE)
            val monthOffset = prefs.getInt("month_offset", 0)
            var selectedMillis = prefs.getLong("selected_millis", -1L)
            if (selectedMillis <= 0) {
                selectedMillis = System.currentTimeMillis()
                prefs.edit().putLong("selected_millis", selectedMillis).apply()
            }

            val calendar = Calendar.getInstance()
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.MONTH, monthOffset)

            val displayMonthCal = calendar.clone() as Calendar

            // Fetch appointments from DB
            val db = AppDatabase.getInstance(context)
            val allAppointments = try {
                db.appointmentDao().getAllActiveAppointmentsList().map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }

            val couplePrefs = CouplePreferences(context)
            val profile = couplePrefs.coupleProfile.value

            // Determine widget theme (System, Light, Dark)
            val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val isWidgetDark = when (profile.widgetThemeMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }

            // Determine widget locale
            val widgetLocale = when (profile.appLanguage.uppercase()) {
                "DE" -> Locale.GERMAN
                "EN" -> Locale.ENGLISH
                else -> Locale.getDefault()
            }
            val isDe = widgetLocale.language.equals("de", ignoreCase = true)

            val views = RemoteViews(context.packageName, R.layout.widget_partner_calendar_4x4)

            // Apply theme colors & drawables to RemoteViews
            val rootBg = if (isWidgetDark) R.drawable.widget_card_background_dark else R.drawable.widget_card_background
            val buttonBg = if (isWidgetDark) R.drawable.widget_button_bg_dark else R.drawable.widget_button_bg
            val eventBg = if (isWidgetDark) R.drawable.widget_event_item_bg_dark else R.drawable.widget_event_item_bg
            val primaryTextColor = if (isWidgetDark) Color.parseColor("#F1F5F9") else Color.parseColor("#1F1F24")
            val secondaryTextColor = if (isWidgetDark) Color.parseColor("#94A3B8") else Color.parseColor("#6B7280")
            val emptyStateBg = if (isWidgetDark) R.drawable.widget_empty_state_bg_dark else R.drawable.widget_empty_state_bg
            val emptyStateTextColor = if (isWidgetDark) Color.parseColor("#A1A1AA") else Color.parseColor("#64748B")

            views.setInt(R.id.widget_root, "setBackgroundResource", rootBg)
            views.setInt(R.id.widget_btn_prev, "setBackgroundResource", buttonBg)
            views.setInt(R.id.widget_btn_today, "setBackgroundResource", buttonBg)
            views.setInt(R.id.widget_btn_next, "setBackgroundResource", buttonBg)

            val syncButtonBg = if (isWidgetDark) R.drawable.widget_button_bg_dark else R.drawable.widget_sync_button_bg
            views.setInt(R.id.widget_btn_sync, "setBackgroundResource", syncButtonBg)
            views.setImageViewResource(R.id.widget_btn_sync, R.drawable.ic_widget_sync)
            views.setInt(R.id.widget_btn_sync, "setColorFilter", Color.WHITE)

            val navArrowColor = if (isWidgetDark) Color.WHITE else primaryTextColor
            views.setTextColor(R.id.widget_month_title, primaryTextColor)
            views.setTextColor(R.id.widget_btn_prev, navArrowColor)
            views.setTextColor(R.id.widget_btn_next, navArrowColor)
            val todayTextColor = if (isWidgetDark) Color.parseColor("#F472B6") else Color.parseColor("#DB2777")
            views.setTextColor(R.id.widget_btn_today, todayTextColor)
            views.setTextColor(R.id.widget_selected_date_label, primaryTextColor)
            views.setTextColor(R.id.widget_selected_count_badge, secondaryTextColor)

            // Empty state styling: Darkened bar to blend smoothly with UI
            views.setInt(R.id.widget_empty_state_text, "setBackgroundResource", emptyStateBg)
            views.setTextColor(R.id.widget_empty_state_text, emptyStateTextColor)

            views.setTextViewText(R.id.widget_btn_today, if (isDe) "Heute" else "Today")

            // 1. Header Month Title
            val monthFormat = SimpleDateFormat("MMMM yyyy", widgetLocale)
            val monthTitle = monthFormat.format(displayMonthCal.time)
            views.setTextViewText(R.id.widget_month_title, monthTitle)

            // 2. Weekday Header Labels
            val weekdayLabels = if (isDe) {
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            } else {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
            }
            for (w in 0..6) {
                views.setTextViewText(WEEKDAY_IDS[w], weekdayLabels[w])
                val isWeekend = w >= 5
                val wColor = if (isWeekend) Color.parseColor("#E11D48") else secondaryTextColor
                views.setTextColor(WEEKDAY_IDS[w], wColor)
            }

            // 3. Populate Interactive Month Day Grid
            val firstDayCal = displayMonthCal.clone() as Calendar
            firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
            val dayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1, MONDAY=2, ...
            val daysBeforeMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY

            val gridStartCal = (firstDayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -daysBeforeMonday)
            }

            val todayCal = Calendar.getInstance()
            val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedMillis }

            // Check if week 5 row has any days of the current month
            val week5HasCurrentMonth = (35..41).any { idx ->
                val c = (gridStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, idx) }
                c.get(Calendar.MONTH) == displayMonthCal.get(Calendar.MONTH)
            }
            views.setViewVisibility(R.id.widget_week_row_5, if (week5HasCurrentMonth) View.VISIBLE else View.GONE)

            for (i in 0..41) {
                val cellCal = (gridStartCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, i)
                }

                val isCurrentMonth = cellCal.get(Calendar.MONTH) == displayMonthCal.get(Calendar.MONTH)
                val isSelected = isSameDay(cellCal, selectedCal)
                val isToday = isSameDay(cellCal, todayCal)
                val dayNum = cellCal.get(Calendar.DAY_OF_MONTH).toString()

                views.setTextViewText(DAY_NUM_IDS[i], dayNum)

                // Day Cell Background & Text Color
                if (isSelected) {
                    views.setInt(DAY_CELL_IDS[i], "setBackgroundResource", R.drawable.widget_day_selected_bg)
                    views.setTextColor(DAY_NUM_IDS[i], Color.WHITE)
                    views.setTextColor(DAY_DOTS_IDS[i], Color.WHITE)
                } else if (isToday) {
                    views.setInt(DAY_CELL_IDS[i], "setBackgroundResource", if (isWidgetDark) R.drawable.widget_day_today_bg_dark else R.drawable.widget_day_today_bg)
                    views.setTextColor(DAY_NUM_IDS[i], Color.parseColor("#E11D48"))
                    views.setTextColor(DAY_DOTS_IDS[i], secondaryTextColor)
                } else {
                    views.setInt(DAY_CELL_IDS[i], "setBackgroundResource", 0)
                    val numColor = if (!isCurrentMonth) {
                        if (isWidgetDark) Color.parseColor("#4B5563") else Color.parseColor("#9CA3AF")
                    } else {
                        val dow = cellCal.get(Calendar.DAY_OF_WEEK)
                        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) Color.parseColor("#E11D48") else primaryTextColor
                    }
                    views.setTextColor(DAY_NUM_IDS[i], numColor)
                    views.setTextColor(DAY_DOTS_IDS[i], secondaryTextColor)
                }

                // Check appointments on this day
                val cellStart = (cellCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val cellEnd = (cellCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis

                val cellAppts = allAppointments.filter { it.startEpochMillis < cellEnd && it.endEpochMillis >= cellStart }
                if (cellAppts.isNotEmpty()) {
                    val dotsText = if (isSelected) {
                        when {
                            cellAppts.size >= 3 -> "•••"
                            cellAppts.size == 2 -> "••"
                            else -> "•"
                        }
                    } else {
                        val hasTogether = cellAppts.any { it.ownerType == OwnerType.TOGETHER }
                        val hasMe = cellAppts.any { it.ownerType == OwnerType.ME }
                        val hasPartner = cellAppts.any { it.ownerType == OwnerType.PARTNER }
                        val sb = StringBuilder()
                        if (hasTogether) sb.append("💜")
                        if (hasMe) sb.append("💙")
                        if (hasPartner) sb.append("💖")
                        if (sb.isEmpty()) sb.append("●")
                        sb.toString()
                    }
                    views.setTextViewText(DAY_DOTS_IDS[i], dotsText)
                    views.setViewVisibility(DAY_DOTS_IDS[i], View.VISIBLE)
                } else {
                    views.setTextViewText(DAY_DOTS_IDS[i], "")
                    views.setViewVisibility(DAY_DOTS_IDS[i], View.INVISIBLE)
                }

                // PendingIntent on each day cell: selecting updates the widget without opening the app!
                val selectDayIntent = Intent(context, PartnerCalendarWidgetProvider::class.java).apply {
                    action = PartnerCalendarWidgetProvider.ACTION_SELECT_DAY
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(PartnerCalendarWidgetProvider.EXTRA_SELECTED_DATE_MILLIS, cellCal.timeInMillis)
                }
                val selectDayPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 100 + i,
                    selectDayIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(DAY_CELL_IDS[i], selectDayPendingIntent)
            }

            // 4. Selected Day Overview & Appointment List
            val dayStart = (selectedCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = (selectedCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val isSelectedToday = isSameDay(selectedCal, todayCal)
            val dayLabelFormat = SimpleDateFormat("EEE, d. MMM", widgetLocale)
            val dayPrefix = if (isSelectedToday) {
                "📅 ${if (isDe) "Heute" else "Today"} (${dayLabelFormat.format(selectedCal.time)})"
            } else {
                "📅 ${dayLabelFormat.format(selectedCal.time)}"
            }
            views.setTextViewText(R.id.widget_selected_date_label, dayPrefix)

            val dayAppointments = allAppointments.filter { appt ->
                appt.startEpochMillis < dayEnd && appt.endEpochMillis >= dayStart
            }.sortedBy { it.startEpochMillis }

            val plansWord = if (isDe) "Termine" else "plans"
            views.setTextViewText(R.id.widget_selected_count_badge, "${dayAppointments.size} $plansWord")

            // Empty state placeholder text & background
            val emptyMessage = if (isSelectedToday) {
                if (isDe) "Keine Termine heute ☕" else "No appointments today ☕"
            } else {
                if (isDe) "Keine Termine an diesem Tag ☕" else "No appointments for this day ☕"
            }
            views.setTextViewText(R.id.widget_empty_state_text, emptyMessage)
            views.setInt(R.id.widget_empty_state_text, "setBackgroundResource", emptyStateBg)
            views.setTextColor(R.id.widget_empty_state_text, emptyStateTextColor)

            // Setup Dedicated Add Appointment button for the currently selected date
            views.setTextViewText(R.id.widget_btn_add_selected_day, if (isDe) "＋ Termin" else "＋ Add")
            views.setInt(R.id.widget_btn_add_selected_day, "setBackgroundResource", if (isWidgetDark) R.drawable.widget_button_bg_dark else R.drawable.widget_button_bg)

            val addForSelectedDayIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_OPEN_ADD", true)
                putExtra("EXTRA_DATE_MILLIS", selectedMillis)
            }
            val addForSelectedDayPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 1000 + 8,
                addForSelectedDayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add_selected_day, addForSelectedDayPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_empty_state_text, addForSelectedDayPendingIntent)

            // Bind Scrollable ListView via RemoteViewsService
            val serviceIntent = Intent(context, WidgetAppointmentListService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_appointment_list, serviceIntent)
            views.setEmptyView(R.id.widget_appointment_list, R.id.widget_empty_state_text)

            // Template PendingIntent for ListView items: when clicked, launches MainActivity with item details
            val listClickIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val listClickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 1000 + 9,
                listClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_appointment_list, listClickPendingIntent)

            // 5. Pending Intents for Navigation & App Actions
            // Prev month button
            val prevIntent = Intent(context, PartnerCalendarWidgetProvider::class.java).apply {
                action = PartnerCalendarWidgetProvider.ACTION_PREV_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                PendingIntent.getBroadcast(context, appWidgetId * 10 + 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Next month button
            val nextIntent = Intent(context, PartnerCalendarWidgetProvider::class.java).apply {
                action = PartnerCalendarWidgetProvider.ACTION_NEXT_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Today button: Reset to current month & today's date
            val todayIntent = Intent(context, PartnerCalendarWidgetProvider::class.java).apply {
                action = PartnerCalendarWidgetProvider.ACTION_TODAY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_today,
                PendingIntent.getBroadcast(context, appWidgetId * 10 + 3, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Force Sync button in header: Directly forces data synchronization from the widget
            val syncIntent = Intent(context, PartnerCalendarWidgetProvider::class.java).apply {
                action = PartnerCalendarWidgetProvider.ACTION_FORCE_SYNC
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_sync,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 4,
                    syncIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Tapping date label launches the app on that selected date
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_DATE_MILLIS", selectedMillis)
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 5,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_selected_date_label, openAppPendingIntent)

            // Tapping month title opens the app to that month
            views.setOnClickPendingIntent(R.id.widget_month_title, openAppPendingIntent)

            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_appointment_list)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateHelper", "Error updating widget $appWidgetId: ${e.message}", e)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatEventTime(appointment: Appointment): String {
        if (appointment.isAllDay) return "All day"
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return timeFormat.format(appointment.startEpochMillis)
    }
}
