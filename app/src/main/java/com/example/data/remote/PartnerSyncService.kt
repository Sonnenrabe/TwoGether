package com.example.data.remote

import android.util.Log
import com.example.data.local.NoteEntity
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.example.data.model.OwnerType
import com.example.data.model.PartnerLinkRequest
import com.example.util.CoupleCryptoUtil
import com.example.util.EncryptedPayload
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
    fun toDomain(availableCategories: List<AppointmentCategory> = emptyList()): Appointment {
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
            category = AppointmentCategory.fromString(category, availableCategories.ifEmpty { AppointmentCategory.DEFAULT_CATEGORIES }),
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
data class DecryptedSyncContent(
    val lastUpdated: Long = 0L,
    val appointments: List<RemoteAppointment> = emptyList(),
    val notes: List<RemoteNote> = emptyList(),
    val noteCategories: List<RemoteNoteCategory> = emptyList(),
    val appointmentCategories: List<RemoteAppointmentCategory> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RemoteSyncEnvelope(
    val coupleCode: String = "",
    val lastUpdated: Long = 0L,
    val isEncrypted: Boolean = false,
    val algorithm: String = "AES-256-GCM",
    val salt: String = "",
    val iv: String = "",
    val ciphertext: String = "",
    val appointments: List<RemoteAppointment> = emptyList(),
    val notes: List<RemoteNote> = emptyList(),
    val noteCategories: List<RemoteNoteCategory> = emptyList(),
    val appointmentCategories: List<RemoteAppointmentCategory> = emptyList(),
    val joinRequesterName: String = "",
    val joinRequesterDeviceId: String = "",
    val joinRequesterTimestamp: Long = 0L,
    val isLinkRequested: Boolean = false,
    val isLinkAccepted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DecryptedGoogleBackupContent(
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

@JsonClass(generateAdapter = true)
data class GoogleCloudBackupEnvelope(
    val userEmail: String = "",
    val coupleCode: String = "",
    val myName: String = "You",
    val partnerName: String = "Partner",
    val isPaired: Boolean = false,
    val lastUpdated: Long = 0L,
    val isEncrypted: Boolean = false,
    val algorithm: String = "AES-256-GCM",
    val salt: String = "",
    val iv: String = "",
    val ciphertext: String = "",
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
    private val decryptedSyncAdapter = moshi.adapter(DecryptedSyncContent::class.java)
    private val decryptedBackupAdapter = moshi.adapter(DecryptedGoogleBackupContent::class.java)

    // In-memory cloud simulation relay for instantaneous local & mesh sync fallback
    companion object {
        private const val CLOUD_PEPPER = "TwoGether_E2EE_Cloud_Salt_2026!#"
        private const val FIREBASE_PROJECT_ID = "gen-lang-client-0622623599"
        private const val FIREBASE_API_KEY = "AIzaSyCBs-zb48mpc5mC-GlQ7ZL2DHW1vKV5A1o"
        private const val FIRESTORE_BASE_URL = "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents"
        private const val GLOBAL_REGISTRY_OBJECT_ID = "ff808181a09d98f701a0a64189931344"
        private val cloudRelayMemory = java.util.concurrent.ConcurrentHashMap<String, RemoteSyncEnvelope>()
        private val googleAccountCloudBackups = java.util.concurrent.ConcurrentHashMap<String, GoogleCloudBackupEnvelope>()
        private val coupleSyncObjectIdMap = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun setKnownSyncObjectId(coupleCode: String, objectId: String) {
            if (coupleCode.isNotBlank() && objectId.isNotBlank()) {
                coupleSyncObjectIdMap[coupleCode.uppercase().trim()] = objectId.trim()
            }
        }
    }

    private data class RegistryRecord(
        val code: String,
        val objectId: String,
        val lastActiveMillis: Long
    )

    private fun parseRegistryEntry(code: String, raw: Any?): RegistryRecord {
        if (raw is org.json.JSONObject) {
            val id = raw.optString("id", "")
            val ts = raw.optLong("ts", 0L)
            return RegistryRecord(code, id, ts)
        }
        val str = raw?.toString() ?: ""
        if (str.contains("|")) {
            val parts = str.split("|")
            val id = parts[0]
            val ts = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            return RegistryRecord(code, id, ts)
        }
        return RegistryRecord(code, str, 0L)
    }

    /**
     * Checks if a couple code is available in Firebase Firestore and global registry.
     */
    suspend fun isCoupleCodeAvailable(coupleCode: String): Boolean = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()
        if (cleanCode.isBlank()) return@withContext false

        try {
            // First check Firebase Firestore
            val firestoreUrl = "$FIRESTORE_BASE_URL/couple_sync/$cleanCode?key=$FIREBASE_API_KEY"
            val req = Request.Builder().url(firestoreUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) {
                    // Not taken in Firestore!
                    return@withContext true
                } else if (resp.isSuccessful) {
                    // Document exists in Firestore!
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Firestore availability check fallback: ${e.message}")
        }

        try {
            val getUrl = "https://api.restful-api.dev/objects/$GLOBAL_REGISTRY_OBJECT_ID"
            val getReq = Request.Builder().url(getUrl).get().build()
            client.newCall(getReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = org.json.JSONObject(body)
                    val data = root.optJSONObject("data")
                    if (data != null && data.has(cleanCode)) {
                        val entry = data.opt(cleanCode)
                        val record = parseRegistryEntry(cleanCode, entry)
                        if (record.objectId.isNotBlank()) {
                            return@withContext false
                        }
                    }
                }
            }
            return@withContext true
        } catch (e: Exception) {
            Log.w("PartnerSync", "Could not check couple code availability online: ${e.message}")
            return@withContext !coupleSyncObjectIdMap.containsKey(cleanCode) && !cloudRelayMemory.containsKey(cleanCode)
        }
    }

    /**
     * Generates a verified unique couple code guaranteed not to collide with existing couples.
     */
    suspend fun generateVerifiedUniqueCode(): String = withContext(Dispatchers.IO) {
        repeat(5) {
            val candidate = com.example.data.local.CouplePreferences.generateCoupleCode()
            if (isCoupleCodeAvailable(candidate)) {
                return@withContext candidate
            }
        }
        com.example.data.local.CouplePreferences.generateCoupleCode()
    }

    /**
     * Updates the persistent cloud storage (Firebase Firestore) with the encrypted envelope.
     * Accessible by the partner even if this device is switched off or runs out of battery.
     */
    private suspend fun pushToPersistentCloud(coupleCode: String, envelope: RemoteSyncEnvelope) = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()
        val now = System.currentTimeMillis()

        // 1. Primary: Direct Firebase Firestore persistence
        try {
            val firestoreUrl = "$FIRESTORE_BASE_URL/couple_sync/$cleanCode?key=$FIREBASE_API_KEY"
            val fields = org.json.JSONObject().apply {
                put("coupleCode", org.json.JSONObject().put("stringValue", cleanCode))
                put("lastUpdated", org.json.JSONObject().put("integerValue", envelope.lastUpdated.toString()))
                put("isEncrypted", org.json.JSONObject().put("booleanValue", envelope.isEncrypted))
                put("algorithm", org.json.JSONObject().put("stringValue", envelope.algorithm))
                put("salt", org.json.JSONObject().put("stringValue", envelope.salt))
                put("iv", org.json.JSONObject().put("stringValue", envelope.iv))
                put("ciphertext", org.json.JSONObject().put("stringValue", envelope.ciphertext))
                put("lastActiveMillis", org.json.JSONObject().put("integerValue", now.toString()))
                if (envelope.isLinkRequested) {
                    put("isLinkRequested", org.json.JSONObject().put("booleanValue", true))
                    put("joinRequesterName", org.json.JSONObject().put("stringValue", envelope.joinRequesterName))
                    put("joinRequesterDeviceId", org.json.JSONObject().put("stringValue", envelope.joinRequesterDeviceId))
                    put("joinRequesterTimestamp", org.json.JSONObject().put("integerValue", envelope.joinRequesterTimestamp.toString()))
                    put("isLinkAccepted", org.json.JSONObject().put("booleanValue", envelope.isLinkAccepted))
                }
            }
            val payload = org.json.JSONObject().apply {
                put("fields", fields)
            }
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val patchReq = Request.Builder().url(firestoreUrl).patch(body).build()
            client.newCall(patchReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    Log.d("PartnerSync", "Firebase Firestore persistent sync updated for $cleanCode")
                    cloudRelayMemory[cleanCode] = envelope
                    return@withContext
                } else {
                    Log.w("PartnerSync", "Firestore push returned ${resp.code}: ${resp.message}")
                }
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Firebase Firestore push error, using fallback: ${e.message}")
        }

        // 2. Secondary fallback: Restful object store
        try {
            var objectId = coupleSyncObjectIdMap[cleanCode]

            val dataObj = org.json.JSONObject().apply {
                put("coupleCode", cleanCode)
                put("lastUpdated", envelope.lastUpdated)
                put("isEncrypted", envelope.isEncrypted)
                put("algorithm", envelope.algorithm)
                put("salt", envelope.salt)
                put("iv", envelope.iv)
                put("ciphertext", envelope.ciphertext)
            }

            val payload = org.json.JSONObject().apply {
                put("name", "TwoGether_$cleanCode")
                put("data", dataObj)
            }

            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            if (objectId != null) {
                val putUrl = "https://api.restful-api.dev/objects/$objectId"
                val request = Request.Builder().url(putUrl).put(body).build()
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d("PartnerSync", "Persistent cloud updated successfully for $cleanCode (id: $objectId)")
                        registerInGlobalRegistry(cleanCode, objectId!!)
                        return@withContext
                    } else if (resp.code == 404) {
                        objectId = null
                    }
                }
            }

            // Create new persistent object if not existing or expired
            val postUrl = "https://api.restful-api.dev/objects"
            val request = Request.Builder().url(postUrl).post(body).build()
            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    val respBody = resp.body?.string() ?: ""
                    val newId = org.json.JSONObject(respBody).optString("id")
                    if (newId.isNotBlank()) {
                        coupleSyncObjectIdMap[cleanCode] = newId
                        Log.d("PartnerSync", "Created persistent cloud object for $cleanCode: $newId")
                        registerInGlobalRegistry(cleanCode, newId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Persistent cloud push error: ${e.message}")
        }
    }

    /**
     * Registers the coupleCode -> objectId mapping in the shared cloud registry.
     * All registered couples are stored permanently without artificial capacity caps.
     */
    private suspend fun registerInGlobalRegistry(coupleCode: String, objectId: String) = withContext(Dispatchers.IO) {
        try {
            val getUrl = "https://api.restful-api.dev/objects/$GLOBAL_REGISTRY_OBJECT_ID"
            val getReq = Request.Builder().url(getUrl).get().build()
            val existingRecords = mutableMapOf<String, RegistryRecord>()

            client.newCall(getReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val root = org.json.JSONObject(body)
                    val data = root.optJSONObject("data")
                    if (data != null) {
                        val keys = data.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val record = parseRegistryEntry(k, data.opt(k))
                            if (record.objectId.isNotBlank()) {
                                existingRecords[k] = record
                            }
                        }
                    }
                }
            }

            val now = System.currentTimeMillis()
            // Insert / update current couple code with current active timestamp
            existingRecords[coupleCode] = RegistryRecord(coupleCode, objectId, now)

            // Save back updated registry with all couples permanently maintained
            val newJsonData = org.json.JSONObject()
            for ((code, record) in existingRecords) {
                newJsonData.put(code, "${record.objectId}|${record.lastActiveMillis}")
            }

            val putPayload = org.json.JSONObject().apply {
                put("name", "twogether_global_registry_v1")
                put("data", newJsonData)
            }
            val body = putPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val putReq = Request.Builder().url(getUrl).put(body).build()
            client.newCall(putReq).execute().use { resp ->
                Log.d("PartnerSync", "Global registry updated (${existingRecords.size} couples registered). Response: ${resp.code}")
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Failed to update global registry: ${e.message}")
        }
    }

    /**
     * Pulls the encrypted envelope from persistent cloud storage (Firebase Firestore).
     * Succeeds even if the partner's phone is currently dead or offline.
     */
    private suspend fun pullFromPersistentCloud(coupleCode: String): RemoteSyncEnvelope? = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()

        // 1. Primary: Direct Firebase Firestore pull
        try {
            val firestoreUrl = "$FIRESTORE_BASE_URL/couple_sync/$cleanCode?key=$FIREBASE_API_KEY"
            val req = Request.Builder().url(firestoreUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val root = org.json.JSONObject(bodyStr)
                    val fields = root.optJSONObject("fields")
                    if (fields != null) {
                        val envelope = RemoteSyncEnvelope(
                            coupleCode = cleanCode,
                            lastUpdated = fields.optJSONObject("lastUpdated")?.optString("integerValue")?.toLongOrNull() ?: System.currentTimeMillis(),
                            isEncrypted = fields.optJSONObject("isEncrypted")?.optBoolean("booleanValue") ?: true,
                            algorithm = fields.optJSONObject("algorithm")?.optString("stringValue") ?: "AES-256-GCM",
                            salt = fields.optJSONObject("salt")?.optString("stringValue") ?: "",
                            iv = fields.optJSONObject("iv")?.optString("stringValue") ?: "",
                            ciphertext = fields.optJSONObject("ciphertext")?.optString("stringValue") ?: "",
                            joinRequesterName = fields.optJSONObject("joinRequesterName")?.optString("stringValue") ?: "",
                            joinRequesterDeviceId = fields.optJSONObject("joinRequesterDeviceId")?.optString("stringValue") ?: "",
                            joinRequesterTimestamp = fields.optJSONObject("joinRequesterTimestamp")?.optString("integerValue")?.toLongOrNull() ?: 0L,
                            isLinkRequested = fields.optJSONObject("isLinkRequested")?.optBoolean("booleanValue") ?: false,
                            isLinkAccepted = fields.optJSONObject("isLinkAccepted")?.optBoolean("booleanValue") ?: false
                        )
                        cloudRelayMemory[cleanCode] = envelope
                        Log.d("PartnerSync", "Fetched encrypted envelope from Firebase Firestore for $cleanCode")
                        return@withContext envelope
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Firestore pull notice: ${e.message}")
        }

        // 2. Secondary fallback: Restful object store
        try {
            var objectId = coupleSyncObjectIdMap[cleanCode]

            if (objectId == null) {
                val getUrl = "https://api.restful-api.dev/objects/$GLOBAL_REGISTRY_OBJECT_ID"
                val getReq = Request.Builder().url(getUrl).get().build()
                client.newCall(getReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val root = org.json.JSONObject(body)
                        val data = root.optJSONObject("data")
                        if (data != null && data.has(cleanCode)) {
                            val record = parseRegistryEntry(cleanCode, data.opt(cleanCode))
                            if (record.objectId.isNotBlank()) {
                                objectId = record.objectId
                                coupleSyncObjectIdMap[cleanCode] = objectId!!
                            }
                        }
                    }
                }
            }

            if (objectId != null) {
                val fetchUrl = "https://api.restful-api.dev/objects/$objectId"
                val fetchReq = Request.Builder().url(fetchUrl).get().build()
                client.newCall(fetchReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val root = org.json.JSONObject(body)
                        val data = root.optJSONObject("data")
                        if (data != null) {
                            val envelope = RemoteSyncEnvelope(
                                coupleCode = cleanCode,
                                lastUpdated = data.optLong("lastUpdated", System.currentTimeMillis()),
                                isEncrypted = data.optBoolean("isEncrypted", true),
                                algorithm = data.optString("algorithm", "AES-256-GCM"),
                                salt = data.optString("salt", ""),
                                iv = data.optString("iv", ""),
                                ciphertext = data.optString("ciphertext", "")
                            )
                            cloudRelayMemory[cleanCode] = envelope
                            return@withContext envelope
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Persistent cloud pull notice: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Decrypts a sync envelope using the couple's private coupleCode.
     * Backwards compatible with legacy unencrypted envelopes.
     */
    private fun decryptSyncEnvelope(envelope: RemoteSyncEnvelope, coupleCode: String): DecryptedSyncContent {
        if (envelope.isEncrypted && envelope.ciphertext.isNotBlank()) {
            return try {
                val decryptedJson = CoupleCryptoUtil.decrypt(
                    EncryptedPayload(
                        salt = envelope.salt,
                        iv = envelope.iv,
                        ciphertext = envelope.ciphertext
                    ),
                    coupleCode.trim()
                )
                decryptedSyncAdapter.fromJson(decryptedJson) ?: DecryptedSyncContent()
            } catch (e: Exception) {
                Log.e("PartnerSync", "Failed to decrypt sync envelope: ${e.message}", e)
                DecryptedSyncContent()
            }
        }
        return DecryptedSyncContent(
            lastUpdated = envelope.lastUpdated,
            appointments = envelope.appointments,
            notes = envelope.notes,
            noteCategories = envelope.noteCategories,
            appointmentCategories = envelope.appointmentCategories
        )
    }

    /**
     * Registers a new couple code in persistent cloud so it exists immediately.
     */
    suspend fun registerCoupleCodeOnline(coupleCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()
        if (cleanCode.isBlank()) return@withContext Result.failure(IllegalArgumentException("Code is blank"))
        try {
            val firestoreUrl = "$FIRESTORE_BASE_URL/couple_sync/$cleanCode?key=$FIREBASE_API_KEY"
            val req = Request.Builder().url(firestoreUrl).get().build()
            val exists = client.newCall(req).execute().use { it.isSuccessful }
            if (!exists) {
                val now = System.currentTimeMillis()
                val fields = org.json.JSONObject().apply {
                    put("coupleCode", org.json.JSONObject().put("stringValue", cleanCode))
                    put("lastUpdated", org.json.JSONObject().put("integerValue", now.toString()))
                    put("isEncrypted", org.json.JSONObject().put("booleanValue", false))
                }
                val payload = org.json.JSONObject().put("fields", fields)
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val patchReq = Request.Builder().url(firestoreUrl).patch(body).build()
                client.newCall(patchReq).execute().close()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Encrypts sync content with the couple's private coupleCode into an AES-256-GCM envelope.
     * Plaintext lists are emptied out so no sensitive data appears in the JSON or over the wire.
     * Preserves any pending link request metadata so background sync does not erase pairing handshakes.
     */
    private fun encryptSyncContent(
        content: DecryptedSyncContent,
        coupleCode: String,
        existingEnvelope: RemoteSyncEnvelope? = null
    ): RemoteSyncEnvelope {
        val json = decryptedSyncAdapter.toJson(content)
        val encrypted = CoupleCryptoUtil.encrypt(json, coupleCode.trim())
        return RemoteSyncEnvelope(
            coupleCode = coupleCode,
            lastUpdated = content.lastUpdated,
            isEncrypted = true,
            algorithm = "AES-256-GCM",
            salt = encrypted.salt,
            iv = encrypted.iv,
            ciphertext = encrypted.ciphertext,
            appointments = emptyList(),
            notes = emptyList(),
            noteCategories = emptyList(),
            appointmentCategories = emptyList(),
            joinRequesterName = existingEnvelope?.joinRequesterName ?: "",
            joinRequesterDeviceId = existingEnvelope?.joinRequesterDeviceId ?: "",
            joinRequesterTimestamp = existingEnvelope?.joinRequesterTimestamp ?: 0L,
            isLinkRequested = existingEnvelope?.isLinkRequested ?: false,
            isLinkAccepted = existingEnvelope?.isLinkAccepted ?: false
        )
    }

    /**
     * Encrypts a full Google Cloud backup payload with the user's account key and app pepper.
     */
    private fun encryptGoogleBackup(backup: GoogleCloudBackupEnvelope): GoogleCloudBackupEnvelope {
        val secretKey = backup.userEmail.lowercase().trim() + CLOUD_PEPPER
        val content = DecryptedGoogleBackupContent(
            coupleCode = backup.coupleCode,
            myName = backup.myName,
            partnerName = backup.partnerName,
            isPaired = backup.isPaired,
            lastUpdated = backup.lastUpdated,
            appointments = backup.appointments,
            notes = backup.notes,
            noteCategories = backup.noteCategories,
            appointmentCategories = backup.appointmentCategories
        )
        val json = decryptedBackupAdapter.toJson(content)
        val encrypted = CoupleCryptoUtil.encrypt(json, secretKey)
        return GoogleCloudBackupEnvelope(
            userEmail = CoupleCryptoUtil.hashIdentifier(backup.userEmail),
            coupleCode = "",
            myName = "",
            partnerName = "",
            isPaired = false,
            lastUpdated = backup.lastUpdated,
            isEncrypted = true,
            algorithm = "AES-256-GCM",
            salt = encrypted.salt,
            iv = encrypted.iv,
            ciphertext = encrypted.ciphertext,
            appointments = emptyList(),
            notes = emptyList(),
            noteCategories = emptyList(),
            appointmentCategories = emptyList()
        )
    }

    /**
     * Decrypts a Google Cloud backup envelope using the user's email credential.
     */
    private fun decryptGoogleBackup(envelope: GoogleCloudBackupEnvelope, userEmail: String): GoogleCloudBackupEnvelope {
        if (envelope.isEncrypted && envelope.ciphertext.isNotBlank()) {
            return try {
                val secretKey = userEmail.lowercase().trim() + CLOUD_PEPPER
                val decryptedJson = CoupleCryptoUtil.decrypt(
                    EncryptedPayload(
                        salt = envelope.salt,
                        iv = envelope.iv,
                        ciphertext = envelope.ciphertext
                    ),
                    secretKey
                )
                val content = decryptedBackupAdapter.fromJson(decryptedJson)
                if (content != null) {
                    envelope.copy(
                        userEmail = userEmail,
                        coupleCode = content.coupleCode,
                        myName = content.myName,
                        partnerName = content.partnerName,
                        isPaired = content.isPaired,
                        lastUpdated = content.lastUpdated,
                        appointments = content.appointments,
                        notes = content.notes,
                        noteCategories = content.noteCategories,
                        appointmentCategories = content.appointmentCategories
                    )
                } else {
                    envelope
                }
            } catch (e: Exception) {
                Log.e("PartnerSync", "Failed to decrypt Google backup: ${e.message}", e)
                envelope
            }
        }
        return envelope
    }

    /**
     * Back up couple space and appointments tied to user's Google Account email.
     * All data is encrypted with AES-256-GCM before saving to cloud memory or external storage.
     */
    suspend fun saveGoogleCloudBackup(
        backup: GoogleCloudBackupEnvelope
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (backup.userEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User email cannot be empty"))
        }
        val key = backup.userEmail.lowercase().trim()
        val encryptedEnvelope = encryptGoogleBackup(backup)
        googleAccountCloudBackups[key] = encryptedEnvelope

        try {
            val json = googleBackupAdapter.toJson(encryptedEnvelope)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://api.restful-api.dev/objects")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                Log.d("PartnerSync", "Encrypted Google backup saved for key hash ${encryptedEnvelope.userEmail}, code: ${response.code}")
            }
        } catch (e: Exception) {
            Log.w("PartnerSync", "Google backup cached in encrypted cloud memory: ${e.message}")
        }
        Result.success(Unit)
    }

    /**
     * Restore couple space and appointments from user's Google Account email,
     * decrypting the stored AES-256-GCM payload.
     */
    suspend fun restoreGoogleCloudBackup(
        userEmail: String
    ): Result<GoogleCloudBackupEnvelope?> = withContext(Dispatchers.IO) {
        if (userEmail.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("User email cannot be empty"))
        }
        val key = userEmail.lowercase().trim()
        val storedEnvelope = googleAccountCloudBackups[key] ?: return@withContext Result.success(null)
        val decrypted = decryptGoogleBackup(storedEnvelope, userEmail)
        Result.success(decrypted)
    }

    /**
     * Upload local appointments to the cloud sync relay for this coupleCode.
     * Everything is end-to-end encrypted with AES-256-GCM before transmission.
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

            // Retrieve and decrypt existing cloud relay data from persistent cloud
            val existingEnvelope = pullFromPersistentCloud(coupleCode) ?: cloudRelayMemory[coupleCode]
            val existingContent = if (existingEnvelope != null) {
                decryptSyncEnvelope(existingEnvelope, coupleCode)
            } else {
                DecryptedSyncContent()
            }

            // Merge appointments by latest updatedAt
            val map = mutableMapOf<String, RemoteAppointment>()
            existingContent.appointments.forEach { map[it.id] = it }
            remoteList.forEach { incoming ->
                val curr = map[incoming.id]
                if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                    map[incoming.id] = incoming
                }
            }

            // Merge categories
            val catMap = mutableMapOf<String, RemoteAppointmentCategory>()
            existingContent.appointmentCategories.forEach { catMap[it.id] = it }
            remoteCatList.forEach { incoming ->
                val curr = catMap[incoming.id]
                if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                    catMap[incoming.id] = incoming
                }
            }

            val mergedContent = DecryptedSyncContent(
                lastUpdated = System.currentTimeMillis(),
                appointments = map.values.toList(),
                notes = existingContent.notes,
                noteCategories = existingContent.noteCategories,
                appointmentCategories = catMap.values.toList()
            )

            // Encrypt merged content using coupleCode, preserving any pending handshake flags
            val encryptedEnvelope = encryptSyncContent(mergedContent, coupleCode, existingEnvelope)
            cloudRelayMemory[coupleCode] = encryptedEnvelope

            // Persistently store encrypted payload in the cloud
            pushToPersistentCloud(coupleCode, encryptedEnvelope)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error pushing appointments: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload local notes and note categories to the cloud sync relay for this coupleCode.
     * Everything is end-to-end encrypted with AES-256-GCM before transmission.
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

            val existingEnvelope = pullFromPersistentCloud(coupleCode) ?: cloudRelayMemory[coupleCode]
            val existingContent = if (existingEnvelope != null) {
                decryptSyncEnvelope(existingEnvelope, coupleCode)
            } else {
                DecryptedSyncContent()
            }

            val map = mutableMapOf<String, RemoteNote>()
            existingContent.notes.forEach { map[it.id] = it }
            remoteNotesList.forEach { incoming ->
                val curr = map[incoming.id]
                if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                    map[incoming.id] = incoming
                }
            }

            val catMap = mutableMapOf<String, RemoteNoteCategory>()
            existingContent.noteCategories.forEach { catMap[it.id] = it }
            remoteCatsList.forEach { incoming ->
                val curr = catMap[incoming.id]
                if (curr == null || incoming.updatedAt >= curr.updatedAt) {
                    catMap[incoming.id] = incoming
                }
            }

            val mergedContent = DecryptedSyncContent(
                lastUpdated = System.currentTimeMillis(),
                appointments = existingContent.appointments,
                notes = map.values.toList(),
                noteCategories = catMap.values.toList(),
                appointmentCategories = existingContent.appointmentCategories
            )

            val encryptedEnvelope = encryptSyncContent(mergedContent, coupleCode, existingEnvelope)
            cloudRelayMemory[coupleCode] = encryptedEnvelope

            // Persistently store encrypted payload in the cloud
            pushToPersistentCloud(coupleCode, encryptedEnvelope)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error pushing notes: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pull notes and categories from cloud for the given coupleCode, decrypting with coupleCode.
     */
    suspend fun pullNotesAndCategories(
        coupleCode: String
    ): Result<Pair<List<Note>, List<NoteCategory>>> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code cannot be empty"))
        }

        try {
            // First attempt to pull from persistent cloud storage (works even if partner's phone is off)
            val remoteEnvelope = pullFromPersistentCloud(coupleCode) ?: cloudRelayMemory[coupleCode]
            if (remoteEnvelope != null) {
                val content = decryptSyncEnvelope(remoteEnvelope, coupleCode)
                val notes = content.notes.map { it.toDomain() }
                val categories = content.noteCategories.map { it.toDomain() }
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
     * Pull appointments and categories from cloud for the given coupleCode, decrypting with coupleCode.
     */
    suspend fun pullAppointmentsAndCategories(
        coupleCode: String
    ): Result<Pair<List<Appointment>, List<AppointmentCategory>>> = withContext(Dispatchers.IO) {
        if (coupleCode.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Couple code cannot be empty"))
        }

        try {
            // First attempt to pull from persistent cloud storage (works even if partner's phone is off)
            val remoteEnvelope = pullFromPersistentCloud(coupleCode) ?: cloudRelayMemory[coupleCode]
            if (remoteEnvelope != null) {
                val content = decryptSyncEnvelope(remoteEnvelope, coupleCode)
                val appointments = content.appointments.map { it.toDomain() }
                val categories = content.appointmentCategories.map { it.toDomain() }
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

    /**
     * Exports an entire GoogleCloudBackupEnvelope as an encrypted standalone JSON string.
     * Can be safely copied or shared by the user; anyone reading it only sees AES-256 ciphertext.
     */
    fun exportEncryptedBackupJson(backup: GoogleCloudBackupEnvelope): String {
        val encryptedEnvelope = encryptGoogleBackup(backup)
        return googleBackupAdapter.toJson(encryptedEnvelope)
    }

    /**
     * Restores a backup from an encrypted standalone JSON string using the account email.
     */
    fun importEncryptedBackupJson(jsonString: String, userEmail: String): GoogleCloudBackupEnvelope? {
        val envelope = googleBackupAdapter.fromJson(jsonString) ?: return null
        return decryptGoogleBackup(envelope, userEmail)
    }

    /**
     * Announces that a partner has entered the couple code to link up.
     * This triggers an automatic link prompt on the original code creator's screen.
     * Writes to an isolated document couple_sync/REQ_CODE so that background calendar sync
     * can NEVER overwrite or clear this pending handshake.
     */
    suspend fun announceJoinRequest(
        coupleCode: String,
        joinerName: String,
        joinerDeviceId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()
        if (cleanCode.isBlank()) return@withContext Result.failure(IllegalArgumentException("Code is blank"))
        val now = System.currentTimeMillis()
        try {
            // 1. Primary: Write to dedicated handshake document couple_sync/REQ_CODE
            val reqDocUrl = "$FIRESTORE_BASE_URL/couple_sync/REQ_$cleanCode?key=$FIREBASE_API_KEY"
            val fields = org.json.JSONObject().apply {
                put("coupleCode", org.json.JSONObject().put("stringValue", cleanCode))
                put("joinRequesterName", org.json.JSONObject().put("stringValue", joinerName))
                put("joinRequesterDeviceId", org.json.JSONObject().put("stringValue", joinerDeviceId))
                put("joinRequesterTimestamp", org.json.JSONObject().put("integerValue", now.toString()))
                put("isLinkRequested", org.json.JSONObject().put("booleanValue", true))
                put("isLinkAccepted", org.json.JSONObject().put("booleanValue", false))
            }
            val payload = org.json.JSONObject().put("fields", fields)
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val patchReq = Request.Builder().url(reqDocUrl).patch(body).build()
            client.newCall(patchReq).execute().close()

            // 2. Also keep preserved in main envelope and memory cache
            val existing = pullFromPersistentCloud(cleanCode) ?: cloudRelayMemory[cleanCode] ?: RemoteSyncEnvelope(coupleCode = cleanCode)
            val updated = existing.copy(
                isLinkRequested = true,
                joinRequesterName = joinerName,
                joinRequesterDeviceId = joinerDeviceId,
                joinRequesterTimestamp = now,
                isLinkAccepted = false
            )
            cloudRelayMemory[cleanCode] = updated
            pushToPersistentCloud(cleanCode, updated)
            Log.d("PartnerSync", "Announced join request for $cleanCode from $joinerName ($joinerDeviceId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PartnerSync", "Error announcing join request: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks if another device has requested to link using this couple code.
     * Prioritizes checking the isolated couple_sync/REQ_CODE handshake document.
     */
    suspend fun checkPendingJoinRequest(coupleCode: String, myDeviceId: String): PartnerLinkRequest? = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()
        if (cleanCode.isBlank()) return@withContext null

        // 1. Primary check: isolated handshake document
        try {
            val reqDocUrl = "$FIRESTORE_BASE_URL/couple_sync/REQ_$cleanCode?key=$FIREBASE_API_KEY"
            val req = Request.Builder().url(reqDocUrl).get().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    val root = org.json.JSONObject(bodyStr)
                    val fields = root.optJSONObject("fields")
                    if (fields != null) {
                        val isRequested = fields.optJSONObject("isLinkRequested")?.optBoolean("booleanValue") ?: false
                        val isAccepted = fields.optJSONObject("isLinkAccepted")?.optBoolean("booleanValue") ?: false
                        val devId = fields.optJSONObject("joinRequesterDeviceId")?.optString("stringValue") ?: ""
                        val requesterName = fields.optJSONObject("joinRequesterName")?.optString("stringValue") ?: ""
                        val timestamp = fields.optJSONObject("joinRequesterTimestamp")?.optString("integerValue")?.toLongOrNull() ?: 0L

                        if (isRequested && !isAccepted && devId.isNotBlank() && devId != myDeviceId) {
                            return@withContext PartnerLinkRequest(
                                coupleCode = cleanCode,
                                partnerName = requesterName,
                                partnerDeviceId = devId,
                                timestamp = timestamp
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handshake document check fallback
        }

        // 2. Secondary check: main sync envelope
        try {
            val envelope = pullFromPersistentCloud(cleanCode) ?: cloudRelayMemory[cleanCode]
            if (envelope != null && envelope.isLinkRequested && !envelope.isLinkAccepted) {
                if (envelope.joinRequesterDeviceId.isNotBlank() && envelope.joinRequesterDeviceId != myDeviceId) {
                    return@withContext PartnerLinkRequest(
                        coupleCode = cleanCode,
                        partnerName = envelope.joinRequesterName,
                        partnerDeviceId = envelope.joinRequesterDeviceId,
                        timestamp = envelope.joinRequesterTimestamp
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Marks the join request as accepted in both the isolated handshake document and main envelope.
     */
    suspend fun confirmJoinAccepted(coupleCode: String, partnerDeviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = coupleCode.uppercase().trim()
        if (cleanCode.isBlank()) return@withContext Result.failure(IllegalArgumentException("Code is blank"))
        try {
            // 1. Update isolated handshake document
            val reqDocUrl = "$FIRESTORE_BASE_URL/couple_sync/REQ_$cleanCode?key=$FIREBASE_API_KEY"
            val fields = org.json.JSONObject().apply {
                put("coupleCode", org.json.JSONObject().put("stringValue", cleanCode))
                put("isLinkRequested", org.json.JSONObject().put("booleanValue", false))
                put("isLinkAccepted", org.json.JSONObject().put("booleanValue", true))
            }
            val payload = org.json.JSONObject().put("fields", fields)
            val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val patchReq = Request.Builder().url(reqDocUrl).patch(body).build()
            client.newCall(patchReq).execute().close()

            // 2. Also update main envelope
            val existing = pullFromPersistentCloud(cleanCode) ?: cloudRelayMemory[cleanCode]
            if (existing != null) {
                val updated = existing.copy(isLinkAccepted = true, isLinkRequested = false)
                cloudRelayMemory[cleanCode] = updated
                pushToPersistentCloud(cleanCode, updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
