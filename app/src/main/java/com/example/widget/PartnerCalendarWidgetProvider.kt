package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PartnerCalendarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_appointment_list)
                    WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = context.getSharedPreferences("widget_prefs_$appWidgetId", Context.MODE_PRIVATE)

                    when (intent.action) {
                        ACTION_PREV_MONTH -> {
                            val current = prefs.getInt("month_offset", 0)
                            prefs.edit().putInt("month_offset", current - 1).apply()
                            WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                        ACTION_NEXT_MONTH -> {
                            val current = prefs.getInt("month_offset", 0)
                            prefs.edit().putInt("month_offset", current + 1).apply()
                            WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                        ACTION_TODAY -> {
                            prefs.edit()
                                .putInt("month_offset", 0)
                                .putLong("selected_millis", System.currentTimeMillis())
                                .apply()
                            WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                        ACTION_SELECT_DAY -> {
                            val millis = intent.getLongExtra(EXTRA_SELECTED_DATE_MILLIS, System.currentTimeMillis())
                            prefs.edit().putLong("selected_millis", millis).apply()
                            WidgetUpdateHelper.updateAppWidget(context, appWidgetManager, appWidgetId)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_PREV_MONTH = "com.example.widget.ACTION_PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.example.widget.ACTION_NEXT_MONTH"
        const val ACTION_TODAY = "com.example.widget.ACTION_TODAY"
        const val ACTION_SELECT_DAY = "com.example.widget.ACTION_SELECT_DAY"
        const val EXTRA_SELECTED_DATE_MILLIS = "extra_selected_date_millis"
    }
}
