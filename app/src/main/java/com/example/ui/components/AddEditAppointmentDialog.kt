package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.CoupleProfile
import com.example.data.model.OwnerType
import com.example.ui.util.AppStrings
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@Composable
fun AddEditAppointmentDialog(
    initialDateMillis: Long,
    editingAppointment: Appointment?,
    profile: CoupleProfile,
    categories: List<AppointmentCategory> = AppointmentCategory.DEFAULT_CATEGORIES,
    onManageCategories: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = AppStrings.get(lang, key, *args)

    val isEdit = editingAppointment != null

    var title by remember { mutableStateOf(editingAppointment?.title ?: "") }
    var description by remember { mutableStateOf(editingAppointment?.description ?: "") }
    var location by remember { mutableStateOf(editingAppointment?.location ?: "") }
    var ownerType by remember { mutableStateOf(editingAppointment?.ownerType ?: OwnerType.TOGETHER) }
    var category by remember(editingAppointment, categories) {
        mutableStateOf(
            editingAppointment?.category?.let { apptCat ->
                categories.find { it.id.equals(apptCat.id, ignoreCase = true) } ?: apptCat
            } ?: categories.firstOrNull() ?: AppointmentCategory.DATE_NIGHT
        )
    }
    var isAllDay by remember { mutableStateOf(editingAppointment?.isAllDay ?: false) }
    var hasReminder by remember { mutableStateOf(editingAppointment?.hasReminder ?: true) }
    var reminderMinutes by remember { mutableIntStateOf(editingAppointment?.reminderMinutesBefore ?: 30) }

    // Start & End date millis
    val defaultStart = remember(initialDateMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = editingAppointment?.startEpochMillis ?: initialDateMillis
            if (editingAppointment == null) {
                set(Calendar.HOUR_OF_DAY, 19)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        }
        cal.timeInMillis
    }
    var startEpochMillis by remember { mutableLongStateOf(defaultStart) }

    val defaultEnd = remember(startEpochMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = editingAppointment?.endEpochMillis ?: (startEpochMillis + 2 * 3600 * 1000)
        }
        cal.timeInMillis
    }
    var endEpochMillis by remember { mutableLongStateOf(defaultEnd) }

    val selectedLocale = remember(lang) {
        when (lang.uppercase()) {
            "DE" -> Locale.GERMAN
            "IT" -> Locale.ITALIAN
            "ES" -> Locale("es", "ES")
            "FR" -> Locale.FRENCH
            "EN" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }
    }

    val dateFormat = remember(selectedLocale) { SimpleDateFormat("EEE, d. MMM yyyy", selectedLocale) }
    val timeFormat = remember(selectedLocale) { SimpleDateFormat("HH:mm", selectedLocale) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .imePadding()
                .navigationBarsPadding()
                .clip(RoundedCornerShape(28.dp))
                .testTag("dialog_add_edit_appointment"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEdit) t("edit_appointment") else t("add_appointment"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_dialog_close")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = t("cancel"))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(t("title_event_name")) },
                    placeholder = { Text(t("title_placeholder")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_appointment_title"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Owner Selection (Who is this for?)
                Text(
                    text = t("who_is_this_for"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                    val owners = listOf(
                        Triple(OwnerType.TOGETHER, t("filter_together"), togetherColor),
                        Triple(OwnerType.ME, profile.myName, myColor),
                        Triple(OwnerType.PARTNER, profile.partnerName, partnerColor)
                    )

                    owners.forEach { (type, label, color) ->
                        val isSelected = ownerType == type
                        Surface(
                            onClick = { ownerType = type },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else color.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("owner_chip_${type.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Header & Management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = t("category"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    if (onManageCategories != null) {
                        TextButton(
                            onClick = onManageCategories,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(t("manage_categories"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category.id.equals(cat.id, ignoreCase = true)
                        val localizedCatName = AppStrings.getCategoryName(cat, lang)
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = cat },
                            label = {
                                Text(
                                    text = "${cat.iconEmoji} $localizedCatName",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date Picker Row
                Surface(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = startEpochMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val updatedStart = Calendar.getInstance().apply {
                                    timeInMillis = startEpochMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                val updatedEnd = Calendar.getInstance().apply {
                                    timeInMillis = endEpochMillis
                                    set(Calendar.YEAR, y)
                                    set(Calendar.MONTH, m)
                                    set(Calendar.DAY_OF_MONTH, d)
                                }
                                startEpochMillis = updatedStart.timeInMillis
                                endEpochMillis = updatedEnd.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_select_date")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = t("date"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = dateFormat.format(startEpochMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // All Day Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = t("all_day_event"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Switch(
                        checked = isAllDay,
                        onCheckedChange = { isAllDay = it },
                        modifier = Modifier.testTag("switch_all_day")
                    )
                }

                if (!isAllDay) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Start Time & End Time Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Time
                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = startEpochMillis }
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val updated = (cal.clone() as Calendar).apply {
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, m)
                                        }
                                        startEpochMillis = updated.timeInMillis
                                        if (endEpochMillis <= startEpochMillis) {
                                            endEpochMillis = startEpochMillis + 3600 * 1000
                                        }
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_select_start_time")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = t("starts"),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = timeFormat.format(startEpochMillis),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        // End Time
                        Surface(
                            onClick = {
                                val cal = Calendar.getInstance().apply { timeInMillis = endEpochMillis }
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        val updated = (cal.clone() as Calendar).apply {
                                            set(Calendar.HOUR_OF_DAY, h)
                                            set(Calendar.MINUTE, m)
                                        }
                                        endEpochMillis = updated.timeInMillis
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_select_end_time")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = t("ends"),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                    Text(
                                        text = timeFormat.format(endEpochMillis),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Location Field
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text(t("location_optional")) },
                    placeholder = { Text(t("location_placeholder")) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.LocationOn, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_appointment_location"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description / Notes Field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(t("notes_optional")) },
                    placeholder = { Text(t("notes_placeholder")) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Description, contentDescription = null)
                    },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_appointment_notes"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = t("reminder_notification"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it },
                        modifier = Modifier.testTag("switch_reminder")
                    )
                }

                if (hasReminder) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val reminderOptions = listOf(
                            15 to t("mins_before_15"),
                            30 to t("mins_before_30"),
                            60 to t("hour_before_1"),
                            1440 to t("day_before_1")
                        )
                        reminderOptions.forEach { (mins, label) ->
                            val isSelected = reminderMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = { reminderMinutes = mins },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons: Delete (Bin), Cancel (X), Save (Check)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEdit && onDelete != null && editingAppointment != null) {
                        FilledTonalIconButton(
                            onClick = { onDelete(editingAppointment.id) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_delete_appointment")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = t("delete"),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_cancel_appointment")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = t("cancel"),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (title.isBlank()) return@FilledIconButton
                                val colorHex = when (ownerType) {
                                    OwnerType.TOGETHER -> profile.togetherColorHex
                                    OwnerType.ME -> profile.myColorHex
                                    OwnerType.PARTNER -> profile.partnerColorHex
                                }
                                val creator = if (ownerType == OwnerType.PARTNER) profile.partnerName else profile.myName
                                val toSave = Appointment(
                                    id = editingAppointment?.id ?: UUID.randomUUID().toString(),
                                    title = title.trim(),
                                    description = description.trim(),
                                    location = location.trim(),
                                    startEpochMillis = startEpochMillis,
                                    endEpochMillis = endEpochMillis,
                                    isAllDay = isAllDay,
                                    ownerType = ownerType,
                                    createdByName = creator,
                                    category = category,
                                    colorHex = colorHex,
                                    coupleId = profile.coupleCode,
                                    hasReminder = hasReminder,
                                    reminderMinutesBefore = reminderMinutes
                                )
                                onSave(toSave)
                            },
                            enabled = title.isNotBlank(),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_save_appointment")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = t("save"),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Generous bottom spacer so nothing gets cut off when scrolling
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
