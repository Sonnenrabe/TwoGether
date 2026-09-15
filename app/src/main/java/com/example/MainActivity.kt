package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.CalendarHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CalendarViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleWidgetIntent(intent)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.coupleProfile.appThemeMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemInDark
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalendarHomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        val dateMillis = intent.getLongExtra("EXTRA_DATE_MILLIS", -1L)
        val shouldOpenAdd = intent.getBooleanExtra("EXTRA_OPEN_ADD", false)
        val appointmentId = intent.getStringExtra("EXTRA_APPOINTMENT_ID")

        if (dateMillis > 0) {
            viewModel.selectDate(dateMillis)
        }
        if (!appointmentId.isNullOrBlank()) {
            viewModel.openAppointmentById(appointmentId)
        } else if (shouldOpenAdd) {
            viewModel.openAddDialog(if (dateMillis > 0) dateMillis else null)
        }
    }
}
