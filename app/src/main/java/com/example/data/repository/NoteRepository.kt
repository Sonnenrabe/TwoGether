package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.CouplePreferences
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import com.example.data.remote.PartnerSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteRepository(
    private val context: Context,
    private val noteDao: NoteDao,
    private val couplePreferences: CouplePreferences,
    private val syncService: PartnerSyncService = PartnerSyncService()
) {

    val allActiveNotes: Flow<List<Note>> =
        combine(
            noteDao.getAllActiveNotes(),
            couplePreferences.noteCategories
        ) { list, cats ->
            list.map { it.toDomain(cats) }
        }

    val noteCategories: StateFlow<List<NoteCategory>> = couplePreferences.noteCategories

    suspend fun addCategory(category: NoteCategory) = withContext(Dispatchers.IO) {
        couplePreferences.addOrUpdateNoteCategory(category)
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncNotesWithPartner(profile.coupleCode)
        }
    }

    suspend fun updateCategory(category: NoteCategory) = withContext(Dispatchers.IO) {
        couplePreferences.addOrUpdateNoteCategory(category)
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncNotesWithPartner(profile.coupleCode)
        }
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        couplePreferences.deleteNoteCategory(categoryId)
        // If notes had this deleted category, fallback to GENERAL
        val allNotes = noteDao.getAllActiveNotesList()
        allNotes.forEach { entity ->
            if (entity.category.equals(categoryId, ignoreCase = true)) {
                noteDao.insertOrUpdate(entity.copy(category = NoteCategory.GENERAL.id, updatedAt = System.currentTimeMillis()))
            }
        }
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncNotesWithPartner(profile.coupleCode)
        }
    }

    suspend fun purgePlaceholderNotes() = withContext(Dispatchers.IO) {
        val placeholderTitles = listOf(
            "Weekend Wishlist & Date Ideas",
            "Couple Grocery & Pantry List",
            "Weekend Date Ideas",
            "Grocery List for Tonight",
            "Just wanted to say"
        )
        val allNotes = noteDao.getAllActiveNotesList()
        allNotes.forEach { entity ->
            if (placeholderTitles.any { entity.title.contains(it, ignoreCase = true) }) {
                noteDao.deletePermanently(entity.id)
            }
        }
    }

    suspend fun getNoteById(id: String): Note? {
        val cats = couplePreferences.noteCategories.value
        return noteDao.getNoteById(id)?.toDomain(cats)
    }

    suspend fun saveNote(note: Note, autoSync: Boolean = true) = withContext(Dispatchers.IO) {
        val updated = note.copy(
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertOrUpdate(NoteEntity.fromDomain(updated))

        if (autoSync) {
            val profile = couplePreferences.coupleProfile.value
            if (profile.coupleCode.isNotBlank()) {
                syncNotesWithPartner(profile.coupleCode)
            }
        }
    }

    suspend fun togglePin(id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        noteDao.updatePinned(id, isPinned)
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncNotesWithPartner(profile.coupleCode)
        }
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        noteDao.markDeleted(id)
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncNotesWithPartner(profile.coupleCode)
        }
    }

    suspend fun deletePartnerNotes() = withContext(Dispatchers.IO) {
        noteDao.deletePartnerNotes()
    }

    suspend fun syncNotesWithPartner(coupleCode: String): Result<Int> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code is blank"))
        }

        try {
            // 1. Pull remote notes & categories
            val pullResult = syncService.pullNotesAndCategories(coupleCode)
            val (remoteNotes, remoteCats) = pullResult.getOrDefault(Pair(emptyList(), emptyList()))

            // Merge remote categories
            if (remoteCats.isNotEmpty()) {
                couplePreferences.mergeRemoteNoteCategories(remoteCats)
            }

            // 2. Fetch local notes
            val localEntities = noteDao.getAllForSync()
            val localMap = localEntities.map { it.toDomain() }.associateBy { it.id }.toMutableMap()

            var mergedCount = 0

            // 3. Merge remote notes into local
            for (remote in remoteNotes) {
                val local = localMap[remote.id]
                if (local == null) {
                    noteDao.insertOrUpdate(NoteEntity.fromDomain(remote))
                    localMap[remote.id] = remote
                    mergedCount++
                } else if (remote.updatedAt > local.updatedAt) {
                    noteDao.insertOrUpdate(NoteEntity.fromDomain(remote))
                    localMap[remote.id] = remote
                    mergedCount++
                }
            }

            // 4. Push combined state (notes and categories including tombstones) back to remote
            val syncCategories = couplePreferences.getAllNoteCategoriesForSync()
            syncService.pushNotes(coupleCode, localMap.values.toList(), syncCategories)

            Result.success(mergedCount)
        } catch (e: Exception) {
            Log.e("NoteRepo", "Sync notes error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a sweet shared sample note from partner for instant testing & delight
     */
    suspend fun simulatePartnerNote(partnerName: String, partnerColorHex: String) = withContext(Dispatchers.IO) {
        val sampleNotes = listOf(
            Note(
                id = UUID.randomUUID().toString(),
                title = "Weekend Date Ideas 🥂",
                content = "1. Sunset picnic by the river 🌅\n2. Italian dinner with candlelight 🍝\n3. Movie night & popcorn 🍿",
                category = NoteCategory.DATE_IDEAS,
                ownerType = OwnerType.PARTNER,
                createdByName = partnerName,
                colorHex = "#FCE7F3",
                isPinned = true,
                isChecklist = false
            ),
            Note(
                id = UUID.randomUUID().toString(),
                title = "Grocery List for Tonight 🛒",
                content = "",
                category = NoteCategory.SHOPPING,
                ownerType = OwnerType.TOGETHER,
                createdByName = partnerName,
                colorHex = "#E0F2FE",
                isPinned = false,
                isChecklist = true,
                checklistItems = listOf(
                    ChecklistItem(text = "Fresh pasta 🍝", isChecked = false),
                    ChecklistItem(text = "Parmesan cheese 🧀", isChecked = true),
                    ChecklistItem(text = "Basil & tomatoes 🍅", isChecked = false),
                    ChecklistItem(text = "Bottle of red wine 🍷", isChecked = false)
                )
            ),
            Note(
                id = UUID.randomUUID().toString(),
                title = "Just wanted to say... 💕",
                content = "Thinking of you today! Can't wait for our evening together. Love you so much! ❤️",
                category = NoteCategory.LOVE_NOTE,
                ownerType = OwnerType.PARTNER,
                createdByName = partnerName,
                colorHex = "#FFE4E6",
                isPinned = true,
                isChecklist = false
            )
        )

        val noteToAdd = sampleNotes.random()
        saveNote(noteToAdd, autoSync = true)
    }

    /**
     * Opens system Share Sheet to send note content via WhatsApp, SMS, etc.
     */
    fun shareNote(note: Note) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, note.title)
                putExtra(Intent.EXTRA_TEXT, note.toShareableText())
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Note with Partner").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Log.e("NoteRepo", "Failed to share note: ${e.message}")
        }
    }

    suspend fun getAllNotesForBackup(): List<Note> = withContext(Dispatchers.IO) {
        noteDao.getAllForSync().map { it.toDomain() }
    }

    suspend fun restoreNotesFromList(notes: List<Note>): Int = withContext(Dispatchers.IO) {
        val entities = notes.map { NoteEntity.fromDomain(it) }
        var count = 0
        entities.forEach {
            noteDao.insertOrUpdate(it)
            count++
        }
        count
    }
}
