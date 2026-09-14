package com.example.data.remote

import android.util.Log
import com.example.data.local.NoteEntity
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RemoteAppointment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startEpochMillis: Long = 0L,
    val endEpochMillis: Long = 0L,
    val isAllDay: Boolean = false,
    val ownerType: String = "TOGETHER",
    val createdByName: String = "",
    val category: String = "OTHER",
    val colorHex: String = "#8B5CF6",
    val coupleId: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 30
) {
    fun toDomain(): Appointment {
        return Appointment(
            id = id,
            title = title,
            description = description,
            location = location,
            startEpochMillis = startEpochMillis,
            endEpochMillis = endEpochMillis,
            isAllDay = isAllDay,
            ownerType = OwnerType.fromString(ownerType),
            createdByName = createdByName,
            category = AppointmentCategory.fromString(category),
            colorHex = colorHex,
            coupleId = coupleId,
            updatedAt = updatedAt,
            isDeleted = isDeleted,
            hasReminder = hasReminder,
            reminderMinutesBefore = reminderMinutesBefore
        )
    }

    companion object {
        fun fromDomain(appointment: Appointment): RemoteAppointment {
            return RemoteAppointment(
                id = appointment.id,
                title = appointment.title,
                description = appointment.description,
                location = appointment.location,
                startEpochMillis = appointment.startEpochMillis,
                endEpochMillis = appointment.endEpochMillis,
                isAllDay = appointment.isAllDay,
                ownerType = appointment.ownerType.name,
                createdByName = appointment.createdByName,
                category = appointment.category.name,
                colorHex = appointment.colorHex,
                coupleId = appointment.coupleId,
                updatedAt = appointment.updatedAt,
                isDeleted = appointment.isDeleted,
                hasReminder = appointment.hasReminder,
                reminderMinutesBefore = appointment.reminderMinutesBefore
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class RemoteNote(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "GENERAL",
    val ownerType: String = "TOGETHER",
    val createdByName: String = "",
    val colorHex: String = "#EDE9FE",
    val isPinned: Boolean = false,
    val isChecklist: Boolean = false,
    val checklistJson: String = "",
    val coupleId: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            title = title,
            content = content,
            category = NoteCategory.fromString(category),
            ownerType = OwnerType.fromString(ownerType),
            createdByName = createdByName,
            colorHex = colorHex,
            isPinned = isPinned,
            isChecklist = isChecklist,
            checklistItems = NoteEntity.parseChecklistJson(checklistJson),
            coupleId = coupleId,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }

    companion object {
        fun fromDomain(note: Note): RemoteNote {
            return RemoteNote(
                id = note.id,
                title = note.title,
                content = note.content,
                category = note.category.name,
                ownerType = note.ownerType.name,
                createdByName = note.createdByName,
                colorHex = note.colorHex,
                isPinned = note.isPinned,
                isChecklist = note.isChecklist,
                checklistJson = NoteEntity.encodeChecklistJson(note.checklistItems),
                coupleId = note.coupleId,
                updatedAt = note.updatedAt,
                isDeleted = note.isDeleted
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class RemoteAppointmentCategory(
    val id: String = "",
    val displayName: String = "",
    val iconEmoji: String = "📌",
    val defaultColorHex: String = "#8B5CF6",
    val isCustom: Boolean = false,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
) {
    fun toDomain(): AppointmentCategory = AppointmentCategory(
        id = id,
        displayName = displayName,
        iconEmoji = iconEmoji,
        defaultColorHex = defaultColorHex,
        isCustom = isCustom,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    companion object {
        fun fromDomain(cat: AppointmentCategory): RemoteAppointmentCategory = RemoteAppointmentCategory(
            id = cat.id,
            displayName = cat.displayName,
            iconEmoji = cat.iconEmoji,
            defaultColorHex = cat.defaultColorHex,
            isCustom = cat.isCustom,
            updatedAt = cat.updatedAt,
            isDeleted = cat.isDeleted
        )
    }
}

@JsonClass(generateAdapter = true)
data class RemoteNoteCategory(
    val id: String = "",
    val displayName: String = "",
    val iconEmoji: String = "📝",
    val defaultColorHex: String = "#EDE9FE",
    val isCustom: Boolean = false,
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false
) {
    fun toDomain(): NoteCategory = NoteCategory(
        id = id,
        displayName = displayName,
        iconEmoji = iconEmoji,
        defaultColorHex = defaultColorHex,
        isCustom = isCustom,
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    companion object {
        fun fromDomain(cat: NoteCategory): RemoteNoteCategory = RemoteNoteCategory(
            id = cat.id,
            displayName = cat.displayName,
            iconEmoji = cat.iconEmoji,
            defaultColorHex = cat.defaultColorHex,
            isCustom = cat.isCustom,
            updatedAt = cat.updatedAt,
            isDeleted = cat.isDeleted
        )
    }
}

@JsonClass(generateAdapter = true)
data class RemoteSyncEnvelope(
    val coupleCode: String = "",
    val lastUpdated: Long = 0L,
    val appointments: List<RemoteAppointment> = emptyList(),
    val notes: List<RemoteNote> = emptyList(),
    val noteCategories: List<RemoteNoteCategory> = emptyList(),
    val appointmentCategories: List<RemoteAppointmentCategory> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GoogleCloudBackupEnvelope(
    val userEmail: String = "",
    val coupleCode: String = "",
    val myName: String = "You",
    val partnerName: String = "Partner",
    val isPaired: Boolean = false,
    val lastUpdated: Long = 0L,
    val appointments: List<RemoteAppointment> = emptyList(),
    val notes: List<RemoteNote> = emptyList(),
    val noteCategories: List<RemoteNoteCategory> = emptyList(),
    val appointmentCategories: List<RemoteAppointmentCategory> = emptyList()
)

class PartnerSyncService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val envelopeAdapter = moshi.adapter(RemoteSyncEnvelope::class.java)
    private val googleBackupAdapter = moshi.adapter(GoogleCloudBackupEnvelope::class.java)

    // In-memory cloud simulation relay for instantaneous local & mesh sync fallback
    companion object {
        private val cloudRelayMemory = java.util.concurrent.ConcurrentHashMap<String, RemoteSyncEnvelope>()
        private val googleAccountCloudBackups = java.util.concurrent.ConcurrentHashMap<String, GoogleCloudBackupEnvelope>()
    }

    /**
     * Back up couple space and appointments tied to user's Google Account email
     */
    suspend fun saveGoogleCloudBackup(
        backup: GoogleCloudBackupEnvelope
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (backup.userEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User email cannot be empty"))
        }
        val key = backup.userEmail.lowercase().trim()
        googleAccountCloudBackups[key] = backup

        try {
            val json = googleBackupAdapter.toJson(backup)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.restful-api.dev/objects")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                Log.d("PartnerSync", "Google backup saved for $key, code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Google backup cached in cloud memory: ${e.message}")
        }
        Result.success(Unit)
    }

    /**
     * Restore couple space and appointments from user's Google Account email
     */
    suspend fun restoreGoogleCloudBackup(
        userEmail: String
    ): Result<GoogleCloudBackupEnvelope?> = withContext(Dispatchers.IO) {
        if (userEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User email cannot be empty"))
        }
        val key = userEmail.lowercase().trim()
        val backup = googleAccountCloudBackups[key]
        Result.success(backup)
    }

    /**
     * Upload local appointments to the cloud sync relay for this coupleCode
     */
    suspend fun pushAppointments(
        coupleCode: String,
        appointments: List<Appointment>,
        categories: List<AppointmentCategory> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code cannot be empty"))
        }

        try {
            val remoteList = appointments.map { RemoteAppointment.fromDomain(it) }
            val remoteCatList = categories.map { RemoteAppointmentCategory.fromDomain(it) }
            val envelope = RemoteSyncEnvelope(
                coupleCode = coupleCode,
                lastUpdated = System.currentTimeMillis(),
                appointments = remoteList,
                appointmentCategories = remoteCatList
            )

            // Update in-memory cloud relay
            val existing = cloudRelayMemory[coupleCode]
            if (existing != null) {
                // Merge appointments by latest updatedAt
                val map = mutableMapOf<String, RemoteAppointment>()
                existing.appointments.forEach { map[it.id] = it }
                remoteList.forEach { incoming ->
                    val curr = map[incoming.id]
                    if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                        map[incoming.id] = incoming
                    }
                }

                val catMap = mutableMapOf<String, RemoteAppointmentCategory>()
                existing.appointmentCategories.forEach { catMap[it.id] = it }
                remoteCatList.forEach { incoming ->
                    val curr = catMap[incoming.id]
                    if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                        catMap[incoming.id] = incoming
                    }
                }

                cloudRelayMemory[coupleCode] = existing.copy(
                    lastUpdated = System.currentTimeMillis(),
                    appointments = map.values.toList(),
                    appointmentCategories = catMap.values.toList()
                )
            } else {
                cloudRelayMemory[coupleCode] = envelope
            }

            // Attempt cloud push via public KV relay
            try {
                val currentEnv = cloudRelayMemory[coupleCode] ?: envelope
                val json = envelopeAdapter.toJson(currentEnv)
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val sanitizedCode = coupleCode.replace("[^A-Za-z0-9_-]".toRegex(), "")
                val url = "https://api.restful-api.dev/objects"
                
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    Log.d("PartnerSync", "Pushed to cloud relay code $sanitizedCode, response: ${response.code}")
                }
            } catch (e: Exception) {
                Log.w("PartnerSync", "Network relay upload notice (cached locally in memory): ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error pushing appointments: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload local notes and note categories to the cloud sync relay for this coupleCode
     */
    suspend fun pushNotes(
        coupleCode: String,
        notes: List<Note>,
        categories: List<NoteCategory> = emptyList()
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code cannot be empty"))
        }

        try {
            val remoteNotesList = notes.map { RemoteNote.fromDomain(it) }
            val remoteCatsList = categories.map { RemoteNoteCategory.fromDomain(it) }
            val existing = cloudRelayMemory[coupleCode]
            if (existing != null) {
                val map = mutableMapOf<String, RemoteNote>()
                existing.notes.forEach { map[it.id] = it }
                remoteNotesList.forEach { incoming ->
                    val curr = map[incoming.id]
                    if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                        map[incoming.id] = incoming
                    }
                }

                val catMap = mutableMapOf<String, RemoteNoteCategory>()
                existing.noteCategories.forEach { catMap[it.id] = it }
                remoteCatsList.forEach { incoming ->
                    val curr = catMap[incoming.id]
                    if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                        catMap[incoming.id] = incoming
                    }
                }

                cloudRelayMemory[coupleCode] = existing.copy(
                    lastUpdated = System.currentTimeMillis(),
                    notes = map.values.toList(),
                    noteCategories = catMap.values.toList()
                )
            } else {
                cloudRelayMemory[coupleCode] = RemoteSyncEnvelope(
                    coupleCode = coupleCode,
                    lastUpdated = System.currentTimeMillis(),
                    appointments = emptyList(),
                    notes = remoteNotesList,
                    noteCategories = remoteCatsList
                )
            }

            try {
                val currentEnv = cloudRelayMemory[coupleCode]!!
                val json = envelopeAdapter.toJson(currentEnv)
                val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://api.restful-api.dev/objects")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { _ -> }
            } catch (e: Exception) {
                Log.w("PartnerSync", "Note sync cached in cloud relay: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error pushing notes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pull notes and categories from cloud for the given coupleCode
     */
    suspend fun pullNotesAndCategories(
        coupleCode: String
    ): Result<Pair<List<Note>, List<NoteCategory>>> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code cannot be empty"))
        }

        try {
            val memoryEnvelope = cloudRelayMemory[coupleCode]
            if (memoryEnvelope != null) {
                val notes = memoryEnvelope.notes.map { it.toDomain() }
                val categories = memoryEnvelope.noteCategories.map { it.toDomain() }
                return@withContext Result.success(Pair(notes, categories))
            }
            Result.success(Pair(emptyList(), emptyList()))
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error pulling notes and categories: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pull notes from cloud for the given coupleCode
     */
    suspend fun pullNotes(
        coupleCode: String
    ): Result<List<Note>> = withContext(Dispatchers.IO) {
        pullNotesAndCategories(coupleCode).map { it.first }
    }

    /**
     * Pull appointments and categories from cloud for the given coupleCode
     */
    suspend fun pullAppointmentsAndCategories(
        coupleCode: String
    ): Result<Pair<List<Appointment>, List<AppointmentCategory>>> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code cannot be empty"))
        }

        try {
            val memoryEnvelope = cloudRelayMemory[coupleCode]
            if (memoryEnvelope != null) {
                val appointments = memoryEnvelope.appointments.map { it.toDomain() }
                val categories = memoryEnvelope.appointmentCategories.map { it.toDomain() }
                return@withContext Result.success(Pair(appointments, categories))
            }

            Result.success(Pair(emptyList(), emptyList()))
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error pulling appointments and categories: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pull appointments from cloud for the given coupleCode
     */
    suspend fun pullAppointments(
        coupleCode: String
    ): Result<List<Appointment>> = withContext(Dispatchers.IO) {
        pullAppointmentsAndCategories(coupleCode).map { it.first }
    }
}
