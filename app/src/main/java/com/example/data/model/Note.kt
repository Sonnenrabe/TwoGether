package com.example.data.model

import java.util.UUID

data class NoteCategory(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val iconEmoji: String = "📝",
    val defaultColorHex: String = "#EDE9FE",
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    // Compatibility property for code expecting .name or .defaultDisplayName
    val name: String get() = id
    val defaultDisplayName: String get() = displayName

    companion object {
        val GENERAL = NoteCategory("GENERAL", "General", "📝", "#EDE9FE")
        val DATE_IDEAS = NoteCategory("DATE_IDEAS", "Date Ideas", "🥂", "#FCE7F3")
        val SHOPPING = NoteCategory("SHOPPING", "Shopping", "🛒", "#E0F2FE")
        val LOVE_NOTE = NoteCategory("LOVE_NOTE", "Love Notes", "💕", "#FFE4E6")
        val TODO = NoteCategory("TODO", "To-Do", "✅", "#FEF3C7")
        val TRAVEL = NoteCategory("TRAVEL", "Travel", "✈️", "#DCFCE7")

        val DEFAULT_CATEGORIES: List<NoteCategory> = listOf(
            DATE_IDEAS,
            SHOPPING,
            TODO,
            TRAVEL,
            LOVE_NOTE,
            GENERAL
        )

        fun fromString(value: String, available: List<NoteCategory> = DEFAULT_CATEGORIES): NoteCategory {
            if (value.isBlank()) return GENERAL
            return available.firstOrNull {
                it.id.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true) ||
                it.name.equals(value, ignoreCase = true)
            } ?: NoteCategory(id = value, displayName = value, iconEmoji = "📝", defaultColorHex = "#EDE9FE", isCustom = true)
        }
    }
}

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val content: String = "",
    val category: NoteCategory = NoteCategory.GENERAL,
    val ownerType: OwnerType = OwnerType.TOGETHER,
    val createdByName: String = "You",
    val colorHex: String = "#EDE9FE",
    val isPinned: Boolean = false,
    val checklistItems: List<ChecklistItem> = emptyList(),
    val isChecklist: Boolean = false,
    val coupleId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    /**
     * Builds a clean text representation suitable for sharing to WhatsApp, Messages, etc.
     */
    fun toShareableText(): String {
        val sb = StringBuilder()
        if (title.isNotBlank()) {
            sb.appendLine("${category.iconEmoji} $title")
            sb.appendLine("-------------------")
        }
        if (isChecklist && checklistItems.isNotEmpty()) {
            checklistItems.forEach { item ->
                val mark = if (item.isChecked) "[✓]" else "[ ]"
                sb.appendLine("$mark ${item.text}")
            }
        } else if (content.isNotBlank()) {
            sb.appendLine(content)
        }
        sb.appendLine()
        sb.append("Shared with TwoGether 💕")
        return sb.toString()
    }
}
