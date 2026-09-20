package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChecklistItem
import com.example.data.model.CoupleProfile
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import com.example.ui.util.AppStrings
import java.util.UUID

val NOTE_PASTEL_COLORS = listOf(
    "#EDE9FE", // Soft Lilac
    "#FCE7F3", // Soft Rose
    "#E0F2FE", // Soft Sky Blue
    "#FEF3C7", // Warm Butter Amber
    "#DCFCE7", // Mint Sage
    "#FFE4E6", // Romantic Blush
    "#F3F4F6"  // Minimalist Off-White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteDialog(
    profile: CoupleProfile,
    editingNote: Note? = null,
    categories: List<NoteCategory> = NoteCategory.DEFAULT_CATEGORIES,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    onShare: ((Note) -> Unit)? = null
) {
    val context = LocalContext.current
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = AppStrings.get(lang, key, *args)

    var title by remember { mutableStateOf(editingNote?.title ?: "") }
    var content by remember { mutableStateOf(editingNote?.content ?: "") }
    var category by remember { mutableStateOf(editingNote?.category ?: categories.firstOrNull() ?: NoteCategory.GENERAL) }
    var ownerType by remember { mutableStateOf(editingNote?.ownerType ?: OwnerType.TOGETHER) }
    var colorHex by remember { mutableStateOf(editingNote?.colorHex ?: NOTE_PASTEL_COLORS.first()) }
    var isPinned by remember { mutableStateOf(editingNote?.isPinned ?: false) }
    var isChecklist by remember { mutableStateOf(editingNote?.isChecklist ?: false) }
    var checklistItems by remember {
        mutableStateOf(editingNote?.checklistItems ?: emptyList())
    }
    var newItemText by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val togetherColor = try { Color(android.graphics.Color.parseColor(profile.togetherColorHex)) } catch (e: Exception) { Color(0xFF8B5CF6) }
    val myColor = try { Color(android.graphics.Color.parseColor(profile.myColorHex)) } catch (e: Exception) { Color(0xFF3B82F6) }
    val partnerColor = try { Color(android.graphics.Color.parseColor(profile.partnerColorHex)) } catch (e: Exception) { Color(0xFFEC4899) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("dialog_add_edit_note"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (editingNote == null) t("add_note") else t("edit_note"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isPinned = !isPinned },
                            modifier = Modifier.testTag("btn_toggle_pin")
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) t("unpin_note") else t("pin_note"),
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (editingNote != null && onShare != null) {
                            IconButton(
                                onClick = {
                                    val currentDraft = Note(
                                        id = editingNote.id,
                                        title = title,
                                        content = content,
                                        category = category,
                                        ownerType = ownerType,
                                        createdByName = if (ownerType == OwnerType.ME) profile.myName else if (ownerType == OwnerType.PARTNER) profile.partnerName else "Together",
                                        colorHex = colorHex,
                                        isPinned = isPinned,
                                        isChecklist = isChecklist,
                                        checklistItems = checklistItems
                                    )
                                    onShare(currentDraft)
                                },
                                modifier = Modifier.testTag("btn_share_note_draft")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = t("share_note"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = t("cancel"))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Body Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text(t("note_title_label")) },
                        placeholder = { Text(t("note_title_placeholder")) },
                        isError = titleError,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_note_title")
                    )

                    // Note Type Selector: Text Note vs Checklist
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isChecklist) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (!isChecklist) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isChecklist = false }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_type_text")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📝 " + t("note_type_text"),
                                        fontWeight = if (!isChecklist) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecklist) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (isChecklist) 2.dp else 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isChecklist = true }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_type_checklist")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "☑️ " + t("note_type_checklist"),
                                        fontWeight = if (isChecklist) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }

                    // Note Category Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = t("category"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { cat ->
                                val isSelected = category.id.equals(cat.id, ignoreCase = true)
                                val catLabel = AppStrings.getNoteCategoryName(cat, profile.appLanguage)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        category = cat
                                        if (editingNote == null && !isSelected) {
                                            colorHex = cat.defaultColorHex
                                        }
                                    },
                                    label = { Text("${cat.iconEmoji} $catLabel") },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Who is this note for (Together, Mine, Partner)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = t("who_is_this_for"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(
                                Triple(OwnerType.TOGETHER, t("filter_together"), togetherColor),
                                Triple(OwnerType.ME, "${t("filter_mine")} (${profile.myName})", myColor),
                                Triple(OwnerType.PARTNER, profile.partnerName, partnerColor)
                            )
                            options.forEach { (type, label, col) ->
                                val isSel = ownerType == type
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSel) col.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = if (isSel) BorderStroke(1.5.dp, col) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { ownerType = type }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) col else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card Color Palette
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = t("appearance_tab"),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NOTE_PASTEL_COLORS.forEach { hex ->
                                val swatch = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
                                val isSelected = colorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(swatch)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                        .clickable { colorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = Color.DarkGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main Content Editor: Either Text or Checklist
                    if (!isChecklist) {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text(t("notes")) },
                            placeholder = { Text(t("note_content_placeholder")) },
                            shape = RoundedCornerShape(14.dp),
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_note_content")
                        )
                    } else {
                        // Checklist items management
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "☑️ " + t("note_type_checklist"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Existing items
                            checklistItems.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { checked ->
                                            checklistItems = checklistItems.toMutableList().apply {
                                                this[index] = item.copy(isChecked = checked)
                                            }
                                        }
                                    )
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                checklistItems = checklistItems.toMutableList().apply {
                                                    this[index] = item.copy(isChecked = !item.isChecked)
                                                }
                                            }
                                    )
                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems.toMutableList().apply {
                                                removeAt(index)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Delete item",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Add new item input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = newItemText,
                                    onValueChange = { newItemText = it },
                                    placeholder = { Text(t("add_checklist_item")) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        if (newItemText.isNotBlank()) {
                                            checklistItems = checklistItems + ChecklistItem(text = newItemText.trim())
                                            newItemText = ""
                                        }
                                    }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_add_checklist_item")
                                )
                                IconButton(
                                    onClick = {
                                        if (newItemText.isNotBlank()) {
                                            checklistItems = checklistItems + ChecklistItem(text = newItemText.trim())
                                            newItemText = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .size(42.dp)
                                        .testTag("btn_add_checklist_item")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions: Delete (Bin), Cancel (X), Save (Check)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (editingNote != null && onDelete != null) {
                        FilledTonalIconButton(
                            onClick = { onDelete(editingNote.id) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_delete_note")
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
                                .testTag("btn_cancel_note")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = t("cancel"),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = {
                                if (isSaving) return@FilledIconButton
                                if (title.isBlank()) {
                                    titleError = true
                                    return@FilledIconButton
                                }
                                isSaving = true
                                val createdBy = when (ownerType) {
                                    OwnerType.ME -> profile.myName
                                    OwnerType.PARTNER -> profile.partnerName
                                    OwnerType.TOGETHER -> "Together"
                                }
                                val noteToSave = (editingNote ?: Note()).copy(
                                    title = title.trim(),
                                    content = content.trim(),
                                    category = category,
                                    ownerType = ownerType,
                                    createdByName = createdBy,
                                    colorHex = colorHex,
                                    isPinned = isPinned,
                                    isChecklist = isChecklist,
                                    checklistItems = checklistItems,
                                    coupleId = profile.coupleCode
                                )
                                onSave(noteToSave)
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_save_note")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = t("save"),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
