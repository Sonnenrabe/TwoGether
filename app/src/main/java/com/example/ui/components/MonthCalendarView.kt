package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Appointment
import com.example.data.model.CoupleProfile
import com.example.data.model.OwnerType
import com.example.ui.viewmodel.CalendarViewMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthCalendarView(
    displayMonthMillis: Long,
    selectedDateMillis: Long,
    appointments: List<Appointment>,
    profile: CoupleProfile,
    viewMode: CalendarViewMode,
    onDateSelected: (Long) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onViewModeChanged: (CalendarViewMode) -> Unit,
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
    val monthCal = remember(displayMonthMillis, profile.appLanguage) {
        Calendar.getInstance(locale).apply { timeInMillis = displayMonthMillis }
    }
    val monthFormat = SimpleDateFormat("MMMM yyyy", locale)
    val monthTitle = monthFormat.format(monthCal.time)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("month_calendar_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
            // Top Navigation & Month Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Clickable Month Title with dropdown indicator to jump year/month
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onMonthClick() }
                        .testTag("btn_select_month_year")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Change Month & Year",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Controls: Prev, Today, Next & ViewMode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onPrevMonth,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    TextButton(
                        onClick = onTodayClick,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_today")
                    ) {
                        Text(
                            text = com.example.ui.util.AppStrings.get(profile.appLanguage, "today"),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday Headers (Localized according to selected language)
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
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid with Swipe Detection
            var dragDistance = 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dragDistance > 60) {
                                    onPrevMonth()
                                } else if (dragDistance < -60) {
                                    onNextMonth()
                                }
                                dragDistance = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragDistance += dragAmount
                            }
                        )
                    }
            ) {
                MonthGrid(
                    monthCal = monthCal,
                    selectedDateMillis = selectedDateMillis,
                    appointments = appointments,
                    profile = profile,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    monthCal: Calendar,
    selectedDateMillis: Long,
    appointments: List<Appointment>,
    profile: CoupleProfile,
    onDateSelected: (Long) -> Unit
) {
    val tempCal = (monthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
    // Convert so Monday is 0, Sunday is 6
    val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - Calendar.MONDAY
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val todayCal = Calendar.getInstance()
    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

    val rows = 6
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (r in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val cellCal = (monthCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, dayNumber)
                        }
                        val isToday = isSameDay(cellCal, todayCal)
                        val isSelected = isSameDay(cellCal, selectedCal)

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

                        val dayAppts = appointments.filter { it.startEpochMillis < cellEnd && it.endEpochMillis >= cellStart }

                        DayCell(
                            dayNumber = dayNumber,
                            isToday = isToday,
                            isSelected = isSelected,
                            dayAppointments = dayAppts,
                            profile = profile,
                            onClick = { onDateSelected(cellCal.timeInMillis) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Empty spacer cell
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    isToday: Boolean,
    isSelected: Boolean,
    dayAppointments: List<Appointment>,
    profile: CoupleProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val togetherColor = try {
        Color(android.graphics.Color.parseColor(profile.togetherColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.secondary
    }
    val myColor = try {
        Color(android.graphics.Color.parseColor(profile.myColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val partnerColor = try {
        Color(android.graphics.Color.parseColor(profile.partnerColorHex))
    } catch (e: Exception) {
        Color(0xFFEC4899)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isSelected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                } else if (isToday) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                } else {
                    Modifier
                }
            )
            .clickable { onClick() }
            .testTag("day_cell_$dayNumber"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            )

            // Appointment indicator dots
            if (dayAppointments.isNotEmpty()) {
                val hasTogether = dayAppointments.any { it.ownerType == OwnerType.TOGETHER }
                val hasMe = dayAppointments.any { it.ownerType == OwnerType.ME }
                val hasPartner = dayAppointments.any { it.ownerType == OwnerType.PARTNER }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasTogether) {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(togetherColor)
                        )
                    }
                    if (hasMe) {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(myColor)
                        )
                    }
                    if (hasPartner) {
                        Box(
                            modifier = Modifier
                                .size(4.5.dp)
                                .clip(CircleShape)
                                .background(partnerColor)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
