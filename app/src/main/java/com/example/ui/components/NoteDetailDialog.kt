package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CoupleProfile
import com.example.data.model.Note
import com.example.data.model.OwnerType
import com.example.ui.util.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteDetailDialog(
    note: Note,
    profile: CoupleProfile,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: (String, Boolean) -> Unit = { _, _ -> },
    onToggleChecklistItem: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onShare: (Note) -> Unit = {}
) {
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = AppStrings.get(lang, key, *args)
    val isDe = lang.equals("DE", ignoreCase = true)

    val dateFormat = remember(lang) {
        val loc = when (lang.uppercase()) {
            "DE" -> Locale.GERMANY
            "IT" -> Locale.ITALY
            "ES" -> Locale("es", "ES")
            "FR" -> Locale.FRANCE
            else -> Locale.ENGLISH
        }
        SimpleDateFormat("d. MMMM yyyy, HH:mm", loc)
    }

    val updatedDateStr = remember(note.updatedAt) {
        if (note.updatedAt > 0L) {
            dateFormat.format(Date(note.updatedAt))
        } else {
            ""
        }
    }

    val parsedCardColor = remember(note.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(note.colorHex))
        } catch (e: Exception) {
            Color(0xFFEDE9FE)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("dialog_note_detail")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Category Badge & Owner Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = parsedCardColor.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, parsedCardColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.category.iconEmoji,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = AppStrings.getNoteCategoryName(note.category, lang),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Owner Badge
                        val (ownerLabel, ownerColor) = when (note.ownerType) {
                            OwnerType.ME -> profile.myName to (runCatching { Color(android.graphics.Color.parseColor(profile.myColorHex)) }.getOrNull() ?: MaterialTheme.colorScheme.primary)
                            OwnerType.PARTNER -> profile.partnerName to (runCatching { Color(android.graphics.Color.parseColor(profile.partnerColorHex)) }.getOrNull() ?: MaterialTheme.colorScheme.secondary)
                            OwnerType.TOGETHER -> (if (isDe) "Gemeinsam" else "Together") to (runCatching { Color(android.graphics.Color.parseColor(profile.togetherColorHex)) }.getOrNull() ?: MaterialTheme.colorScheme.tertiary)
                        }
                        Surface(
                            shape = CircleShape,
                            color = ownerColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = ownerLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = ownerColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Action Icons: Pin, Share, Close
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onTogglePin(note.id, !note.isPinned) },
                            modifier = Modifier.size(36.dp).testTag("btn_detail_toggle_pin")
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (note.isPinned) "Unpin" else "Pin",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { onShare(note) },
                            modifier = Modifier.size(36.dp).testTag("btn_detail_share_note")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp).testTag("btn_close_note_detail")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Expanded Note Reading Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Note Title (large display)
                    Text(
                        text = if (note.title.isNotBlank()) note.title else (if (isDe) "Ohne Titel" else "Untitled Note"),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 28.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (updatedDateStr.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (isDe) "Zuletzt bearbeitet" else "Last edited"}: $updatedDateStr",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                        // Checklist items reading & interactive checking mode
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            note.checklistItems.forEachIndexed { index, item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            onToggleChecklistItem(note.id, index, !item.isChecked)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = item.isChecked,
                                            onCheckedChange = { isChecked ->
                                                onToggleChecklistItem(note.id, index, isChecked)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.text,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.5.sp
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Regular note text content
                        SelectionContainer {
                            Text(
                                text = if (note.content.isNotBlank()) note.content else (if (isDe) "Kein Inhalt vorhanden." else "No content added."),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = 15.5.sp,
                                    lineHeight = 23.sp,
                                    color = if (note.content.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Action Bar: Delete (Bin) on left, Close & Edit (Pencil) on right - icon-only without text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("btn_detail_delete_note")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Note",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedIconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_detail_dismiss_note")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FilledIconButton(
                            onClick = onEdit,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("btn_detail_edit_note")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Note",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
