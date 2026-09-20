package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val ownerType: String,
    val createdByName: String,
    val colorHex: String,
    val isPinned: Boolean,
    val isChecklist: Boolean,
    val checklistJson: String,
    val coupleId: String,
    val updatedAt: Long,
    val isDeleted: Boolean
) {
    fun toDomain(availableCategories: List<NoteCategory> = emptyList()): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            category = NoteCategory.fromString(category, availableCategories.ifEmpty { NoteCategory.DEFAULT_CATEGORIES }),
            ownerType = OwnerType.fromString(ownerType),
            createdByName = createdByName,
            colorHex = colorHex,
            isPinned = isPinned,
            isChecklist = isChecklist,
            checklistItems = parseChecklistJson(checklistJson),
            coupleId = coupleId,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                title = note.title,
                content = note.content,
                category = note.category.name,
                ownerType = note.ownerType.name,
                createdByName = note.createdByName,
                colorHex = note.colorHex,
                isPinned = note.isPinned,
                isChecklist = note.isChecklist,
                checklistJson = encodeChecklistJson(note.checklistItems),
                coupleId = note.coupleId,
                updatedAt = note.updatedAt,
                isDeleted = note.isDeleted
            )
        }

        fun encodeChecklistJson(items: List<ChecklistItem>): String {
            val jsonArray = JSONArray()
            items.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("text", item.text)
                obj.put("isChecked", item.isChecked)
                jsonArray.put(obj)
            }
            return jsonArray.toString()
        }

        fun parseChecklistJson(json: String): List<ChecklistItem> {
            if (json.isBlank()) return emptyList()
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<ChecklistItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ChecklistItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            text = obj.optString("text", ""),
                            isChecked = obj.optBoolean("isChecked", false)
                        )
                    )
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
