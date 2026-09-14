package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Appointment
import com.example.data.model.CoupleProfile
import com.example.data.model.OwnerType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WeekCalendarView(
    selectedDateMillis: Long,
    appointments: List<Appointment>,
    profile: CoupleProfile,
    onDateSelected: (Long) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onTodayClick: () -> Unit,
    onMonthClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val locale = remember(profile.appLanguage) {
        when (profile.appLanguage.uppercase()) {
            "DE" -> Locale.GERMAN
            "IT" -> Locale.ITALIAN
            "ES" -> Locale("es", "ES")
            "FR" -> Locale.FRENCH
            "EN" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }
    val selectedCal = remember(selectedDateMillis, profile.appLanguage) {
        Calendar.getInstance(locale).apply { timeInMillis = selectedDateMillis }
    }
    val todayCal = Calendar.getInstance(locale)

    // Calculate start of this week (Monday)
    val weekStartCal = (selectedCal.clone() as Calendar).apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }

    val weekTitleFormat = SimpleDateFormat("MMM yyyy", locale)
    val weekTitle = weekTitleFormat.format(weekStartCal.time)
    fun t(key: String, vararg args: Any): String = com.example.ui.util.AppStrings.get(profile.appLanguage, key, *args)

    val togetherColor = try {
        Color(android.graphics.Color.parseColor(profile.togetherColorHex))
    } catch (e: Exception) {
        Color(0xFF8B5CF6)
    }
    val myColor = try {
        Color(android.graphics.Color.parseColor(profile.myColorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }
    val partnerColor = try {
        Color(android.graphics.Color.parseColor(profile.partnerColorHex))
    } catch (e: Exception) {
        Color(0xFFEC4899)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("week_calendar_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onMonthClick() }
                        .testTag("btn_select_week_month_year")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${t("tab_week")} • $weekTitle",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Change Month & Year",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPrevWeek,
                        modifier = Modifier.size(32.dp).testTag("btn_prev_week")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Week",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    TextButton(
                        onClick = onTodayClick,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = t("today"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    IconButton(
                        onClick = onNextWeek,
                        modifier = Modifier.size(32.dp).testTag("btn_next_week")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Week",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7 Days Strip (Localized)
            val weekdays = when (profile.appLanguage.uppercase()) {
                "DE" -> listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
                "IT" -> listOf("L", "M", "M", "G", "V", "S", "D")
                "ES" -> listOf("L", "M", "X", "J", "V", "S", "D")
                "FR" -> listOf("L", "M", "M", "J", "V", "S", "D")
                else -> listOf("M", "T", "W", "T", "F", "S", "S")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (i in 0..6) {
                    val dayCal = (weekStartCal.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_YEAR, i)
                    }
                    val isToday = isSameDay(dayCal, todayCal)
                    val isSelected = isSameDay(dayCal, selectedCal)

                    val cellStart = (dayCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val cellEnd = (dayCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis

                    val dayAppts = appointments.filter { it.startEpochMillis < cellEnd && it.endEpochMillis >= cellStart }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                } else if (isToday) {
                                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onDateSelected(dayCal.timeInMillis) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = weekdays[i],
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected || isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Indicator Dots
                        if (dayAppts.isNotEmpty()) {
                            val hasTogether = dayAppts.any { it.ownerType == OwnerType.TOGETHER }
                            val hasMe = dayAppts.any { it.ownerType == OwnerType.ME }
                            val hasPartner = dayAppts.any { it.ownerType == OwnerType.PARTNER }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (hasTogether) {
                                    Box(
                                        modifier = Modifier.size(4.dp).clip(CircleShape).background(togetherColor)
                                    )
                                }
                                if (hasMe) {
                                    Box(
                                        modifier = Modifier.size(4.dp).clip(CircleShape).background(myColor)
                                    )
                                }
                                if (hasPartner) {
                                    Box(
                                        modifier = Modifier.size(4.dp).clip(CircleShape).background(partnerColor)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaCalendarView(
    appointments: List<Appointment>,
    profile: CoupleProfile,
    onAppointmentClick: (Appointment) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = com.example.ui.util.AppStrings.get(lang, key, *args)

    val locale = remember(lang) {
        when (lang.uppercase()) {
            "DE" -> Locale.GERMAN
            "IT" -> Locale.ITALIAN
            "ES" -> Locale("es", "ES")
            "FR" -> Locale.FRENCH
            "EN" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }

    val grouped = remember(appointments, locale) {
        val format = SimpleDateFormat("EEEE, MMMM d, yyyy", locale)
        appointments.sortedBy { it.startEpochMillis }.groupBy { format.format(it.startEpochMillis) }
    }

    if (appointments.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📅", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = t("no_appointments_day"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = t("empty_day_prompt", profile.partnerName),
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (dateHeader, list) ->
                item(key = dateHeader) {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(list, key = { it.id }) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        profile = profile,
                        onClick = { onAppointmentClick(appointment) }
                    )
                }
            }
        }
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
