package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistItem
import com.example.data.model.CoupleProfile
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import com.example.ui.util.AppStrings
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    profile: CoupleProfile,
    notes: List<Note>,
    categories: List<NoteCategory> = NoteCategory.DEFAULT_CATEGORIES,
    onOpenManageCategories: () -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: NoteCategory?,
    onSelectCategory: (NoteCategory?) -> Unit,
    selectedOwnerType: OwnerType?,
    onSelectOwnerType: (OwnerType?) -> Unit,
    onNoteClick: (Note) -> Unit,
    onTogglePin: (String, Boolean) -> Unit,
    onToggleChecklistItem: (noteId: String, itemIndex: Int, isChecked: Boolean) -> Unit,
    onShareNote: (Note) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSimulatePartnerNote: (() -> Unit)? = null,
    onSyncNotes: () -> Unit,
    onAddNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = AppStrings.get(lang, key, *args)
    val isDark = isSystemInDarkTheme()

    val togetherColor = try { Color(android.graphics.Color.parseColor(profile.togetherColorHex)) } catch (e: Exception) { Color(0xFF8B5CF6) }
    val myColor = try { Color(android.graphics.Color.parseColor(profile.myColorHex)) } catch (e: Exception) { Color(0xFF3B82F6) }
    val partnerColor = try { Color(android.graphics.Color.parseColor(profile.partnerColorHex)) } catch (e: Exception) { Color(0xFFEC4899) }

    // Filter notes based on search query, category, and owner
    val filteredNotes = remember(notes, searchQuery, selectedCategory, selectedOwnerType) {
        notes.filter { note ->
            val matchesQuery = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true) ||
                    note.checklistItems.any { it.text.contains(searchQuery, ignoreCase = true) }
            val matchesCategory = selectedCategory == null || note.category.id.equals(selectedCategory.id, ignoreCase = true)
            val matchesOwner = selectedOwnerType == null || note.ownerType == selectedOwnerType
            matchesQuery && matchesCategory && matchesOwner
        }
    }

    val (pinnedNotes, otherNotes) = remember(filteredNotes) {
        filteredNotes.partition { it.isPinned }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("screen_notebook")
    ) {
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text(t("search_notes_placeholder")) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = t("search_clear"))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_notes")
                )

                // Category Chips Scroll Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("✨ " + t("all_notes")) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    categories.forEach { cat ->
                        val isSel = selectedCategory?.id == cat.id
                        val catLabel = when (cat.id) {
                            NoteCategory.GENERAL.id -> t("filter_cat_general")
                            NoteCategory.DATE_IDEAS.id -> t("filter_cat_date_ideas")
                            NoteCategory.SHOPPING.id -> t("filter_cat_shopping")
                            NoteCategory.LOVE_NOTE.id -> t("filter_cat_love_notes")
                            NoteCategory.TODO.id -> t("filter_cat_todo")
                            NoteCategory.TRAVEL.id -> t("filter_cat_travel")
                            else -> cat.displayName
                        }
                        FilterChip(
                            selected = isSel,
                            onClick = { onSelectCategory(if (isSel) null else cat) },
                            label = { Text("${cat.iconEmoji} $catLabel") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Manage Categories Chip
                    AssistChip(
                        onClick = onOpenManageCategories,
                        label = { Text("⚙️ " + t("manage_categories")) },
                        shape = RoundedCornerShape(12.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("btn_manage_categories")
                    )
                }

                // Owner Filters & Quick Action Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Together filter chip
                    SuggestionChip(
                        onClick = {
                            onSelectOwnerType(if (selectedOwnerType == OwnerType.TOGETHER) null else OwnerType.TOGETHER)
                        },
                        label = { Text(t("filter_together")) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedOwnerType == OwnerType.TOGETHER) togetherColor.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedOwnerType == OwnerType.TOGETHER) togetherColor else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Mine filter chip
                    SuggestionChip(
                        onClick = {
                            onSelectOwnerType(if (selectedOwnerType == OwnerType.ME) null else OwnerType.ME)
                        },
                        label = { Text("${t("filter_mine")} (${profile.myName})") },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedOwnerType == OwnerType.ME) myColor.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedOwnerType == OwnerType.ME) myColor else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Partner filter chip
                    SuggestionChip(
                        onClick = {
                            onSelectOwnerType(if (selectedOwnerType == OwnerType.PARTNER) null else OwnerType.PARTNER)
                        },
                        label = { Text(profile.partnerName) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (selectedOwnerType == OwnerType.PARTNER) partnerColor.copy(alpha = 0.2f) else Color.Transparent
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedOwnerType == OwnerType.PARTNER) partnerColor else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Sync action
                    IconButton(
                        onClick = onSyncNotes,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = t("sync_notes"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Main List of Notes
        if (filteredNotes.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📓", fontSize = 38.sp)
                    }

                    Text(
                        text = t("empty_notes_title"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = t("empty_notes_desc", profile.partnerName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Button(
                        onClick = onAddNewNote,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("btn_empty_create_note")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(t("add_note"))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pinned Notes
                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = t("pin_note").uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            profile = profile,
                            isDark = isDark,
                            onClick = { onNoteClick(note) },
                            onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                            onToggleChecklistItem = { idx, checked ->
                                onToggleChecklistItem(note.id, idx, checked)
                            },
                            onShare = { onShareNote(note) },
                            onDelete = { onDeleteNote(note.id) }
                        )
                    }

                    if (otherNotes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = t("all_notes").uppercase(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }

                // Regular Notes
                items(otherNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        profile = profile,
                        isDark = isDark,
                        onClick = { onNoteClick(note) },
                        onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                        onToggleChecklistItem = { idx, checked ->
                            onToggleChecklistItem(note.id, idx, checked)
                        },
                        onShare = { onShareNote(note) },
                        onDelete = { onDeleteNote(note.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    profile: CoupleProfile,
    isDark: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleChecklistItem: (Int, Boolean) -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.appLanguage
    fun t(key: String, vararg args: Any): String = AppStrings.get(lang, key, *args)

    val noteColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val cardBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
        noteColor.copy(alpha = 0.4f)
    }

    val ownerColor = when (note.ownerType) {
        OwnerType.TOGETHER -> try { Color(android.graphics.Color.parseColor(profile.togetherColorHex)) } catch (e: Exception) { Color(0xFF8B5CF6) }
        OwnerType.ME -> try { Color(android.graphics.Color.parseColor(profile.myColorHex)) } catch (e: Exception) { Color(0xFF3B82F6) }
        OwnerType.PARTNER -> try { Color(android.graphics.Color.parseColor(profile.partnerColorHex)) } catch (e: Exception) { Color(0xFFEC4899) }
    }

    val ownerLabel = when (note.ownerType) {
        OwnerType.TOGETHER -> "Together 💕"
        OwnerType.ME -> profile.myName
        OwnerType.PARTNER -> profile.partnerName
    }

    val dateFormatted = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            width = if (note.isPinned) 2.dp else 1.dp,
            color = if (note.isPinned) ownerColor else if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else noteColor.copy(alpha = 0.8f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_note_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Category tag, Owner badge, Pin action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    ) {
                        Text(
                            text = "${note.category.iconEmoji} ${note.category.defaultDisplayName}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Owner Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ownerColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, ownerColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = ownerLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = ownerColor
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) t("unpin_note") else t("pin_note"),
                            tint = if (note.isPinned) ownerColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = t("share_note"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Note Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Note Body: Checklist or Plain text
            if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                val completedCount = note.checklistItems.count { it.isChecked }
                val totalCount = note.checklistItems.size

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LinearProgressIndicator(
                            progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ownerColor,
                            trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "$completedCount/$totalCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Checklist items preview (up to 5 items)
                    note.checklistItems.take(5).forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleChecklistItem(idx, !item.isChecked) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked -> onToggleChecklistItem(idx, checked) },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (note.checklistItems.size > 5) {
                        Text(
                            text = "+ ${note.checklistItems.size - 5} more items...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                        )
                    }
                }
            } else if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bottom Footer: Date & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = t("delete"),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
