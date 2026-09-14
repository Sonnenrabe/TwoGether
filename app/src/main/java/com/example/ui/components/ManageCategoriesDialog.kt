package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppointmentCategory
import com.example.data.model.NoteCategory
import com.example.ui.util.AppStrings
import java.util.UUID

private val SUGGESTED_EMOJIS = listOf(
    "🥂", "🎬", "🍽️", "🏖️", "✈️", "🎁", "☕", "🐾",
    "📚", "🏋️", "🎵", "🛒", "💡", "📝", "💌", "✅", "🏡", "🍿",
    "💍", "🩺", "🚗", "🌟", "🎂", "🎉"
)

private val CATEGORY_PASTEL_COLORS = listOf(
    "#EDE9FE", // Soft Lilac
    "#FCE7F3", // Soft Rose
    "#E0F2FE", // Soft Sky Blue
    "#FEF3C7", // Warm Butter Amber
    "#DCFCE7", // Mint Sage
    "#FFE4E6", // Romantic Blush
    "#F3F4F6", // Neutral Light
    "#FDE68A"  // Sunny Yellow
)

private data class UnifiedCategoryItem(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val colorHex: String,
    val isCustom: Boolean,
    val isAppointment: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesDialog(
    initialTab: Int = 0,
    appointmentCategories: List<AppointmentCategory>,
    noteCategories: List<NoteCategory>,
    appLanguage: String,
    onAddAppointmentCategory: (AppointmentCategory) -> Unit,
    onUpdateAppointmentCategory: (AppointmentCategory) -> Unit,
    onDeleteAppointmentCategory: (String) -> Unit,
    onAddNoteCategory: (NoteCategory) -> Unit,
    onUpdateNoteCategory: (NoteCategory) -> Unit,
    onDeleteNoteCategory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    fun t(key: String, vararg args: Any): String = AppStrings.get(appLanguage, key, *args)

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 1)) }
    var editingItem by remember { mutableStateOf<UnifiedCategoryItem?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<UnifiedCategoryItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentItems: List<UnifiedCategoryItem> = if (selectedTab == 0) {
        appointmentCategories.map { apptCat ->
            UnifiedCategoryItem(
                id = apptCat.id,
                displayName = AppStrings.getCategoryName(apptCat, appLanguage),
                iconEmoji = apptCat.iconEmoji,
                colorHex = apptCat.defaultColorHex,
                isCustom = apptCat.isCustom,
                isAppointment = true
            )
        }
    } else {
        noteCategories.map { noteCat ->
            val localizedName = when (noteCat.id) {
                NoteCategory.GENERAL.id -> t("filter_cat_general")
                NoteCategory.DATE_IDEAS.id -> t("filter_cat_date_ideas")
                NoteCategory.SHOPPING.id -> t("filter_cat_shopping")
                NoteCategory.LOVE_NOTE.id -> t("filter_cat_love_notes")
                NoteCategory.TODO.id -> t("filter_cat_todo")
                NoteCategory.TRAVEL.id -> t("filter_cat_travel")
                else -> noteCat.displayName
            }
            UnifiedCategoryItem(
                id = noteCat.id,
                displayName = localizedName,
                iconEmoji = noteCat.iconEmoji,
                colorHex = noteCat.defaultColorHex,
                isCustom = noteCat.isCustom,
                isAppointment = false
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("dialog_manage_categories"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = t("manage_categories"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedIconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = t("cancel"), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Appointments vs Notes
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "📅 ${t("tab_appointments")}",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = {
                                Text(
                                    text = "📝 ${t("tab_notes")}",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Add Category Button
                Button(
                    onClick = {
                        isAddingNew = true
                        errorMessage = null
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_add_category")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(t("add_category"), fontWeight = FontWeight.Bold)
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List of Categories
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentItems, key = { it.id }) { item ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(item.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.surfaceVariant
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Emoji badge
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(catColor, CircleShape)
                                        .border(1.dp, Color.Black.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = item.iconEmoji, fontSize = 20.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Display Name
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }

                                // Edit button (Pencil)
                                IconButton(
                                    onClick = { editingItem = item },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = t("edit_category"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete button (Bin)
                                IconButton(
                                    onClick = {
                                        if (currentItems.size <= 1) {
                                            errorMessage = t("cannot_delete_last_category")
                                        } else {
                                            itemToDelete = item
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = t("delete"),
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(t("done") ?: "Fertig")
                }
            }
        }
    }

    // Sub-dialog: Add or Edit Category
    if (isAddingNew || editingItem != null) {
        val target = editingItem
        val isAppointmentCategory = if (target != null) target.isAppointment else (selectedTab == 0)
        var name by remember(target) { mutableStateOf(target?.displayName ?: "") }
        var emoji by remember(target) { mutableStateOf(target?.iconEmoji ?: (if (isAppointmentCategory) "🥂" else "💡")) }
        var colorHex by remember(target) { mutableStateOf(target?.colorHex ?: CATEGORY_PASTEL_COLORS.first()) }
        var nameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                isAddingNew = false
                editingItem = null
            },
            title = {
                Text(
                    text = if (target == null) t("add_category") else t("edit_category"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text(t("category_name")) },
                        placeholder = { Text(t("category_name_placeholder")) },
                        isError = nameError,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Emoji Selector
                    Text(
                        text = t("category_emoji"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SUGGESTED_EMOJIS.forEach { em ->
                            val isSel = emoji == em
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                                    .border(
                                        if (isSel) 2.dp else 1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { emoji = em },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(em, fontSize = 18.sp)
                            }
                        }
                    }

                    // Color Swatches
                    Text(
                        text = t("category_color"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CATEGORY_PASTEL_COLORS.forEach { hex ->
                            val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                            val isSel = colorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(parsed, CircleShape)
                                    .border(
                                        if (isSel) 2.5.dp else 1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .clickable { colorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                FilledIconButton(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@FilledIconButton
                        }
                        if (isAppointmentCategory) {
                            if (target == null) {
                                val newCat = AppointmentCategory(
                                    id = UUID.randomUUID().toString(),
                                    displayName = name.trim(),
                                    iconEmoji = emoji,
                                    defaultColorHex = colorHex,
                                    isCustom = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onAddAppointmentCategory(newCat)
                            } else {
                                val updatedCat = AppointmentCategory(
                                    id = target.id,
                                    displayName = name.trim(),
                                    iconEmoji = emoji,
                                    defaultColorHex = colorHex,
                                    isCustom = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onUpdateAppointmentCategory(updatedCat)
                            }
                        } else {
                            if (target == null) {
                                val newCat = NoteCategory(
                                    id = UUID.randomUUID().toString(),
                                    displayName = name.trim(),
                                    iconEmoji = emoji,
                                    defaultColorHex = colorHex,
                                    isCustom = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onAddNoteCategory(newCat)
                            } else {
                                val updatedCat = NoteCategory(
                                    id = target.id,
                                    displayName = name.trim(),
                                    iconEmoji = emoji,
                                    defaultColorHex = colorHex,
                                    isCustom = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onUpdateNoteCategory(updatedCat)
                            }
                        }
                        isAddingNew = false
                        editingItem = null
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = t("save"))
                }
            },
            dismissButton = {
                OutlinedIconButton(
                    onClick = {
                        isAddingNew = false
                        editingItem = null
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = t("cancel"))
                }
            }
        )
    }

    // Confirmation Delete Dialog
    if (itemToDelete != null) {
        val item = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(t("delete")) },
            text = {
                val confirmMessage = if (item.isAppointment) {
                    t("delete_appointment_category_confirm", item.displayName)
                } else {
                    t("delete_category_confirm", item.displayName)
                }
                Text(confirmMessage)
            },
            confirmButton = {
                FilledTonalIconButton(
                    onClick = {
                        if (item.isAppointment) {
                            onDeleteAppointmentCategory(item.id)
                        } else {
                            onDeleteNoteCategory(item.id)
                        }
                        itemToDelete = null
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = t("delete"))
                }
            },
            dismissButton = {
                OutlinedIconButton(
                    onClick = { itemToDelete = null },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = t("cancel"))
                }
            }
        )
    }
}
