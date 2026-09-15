package com.example.data.remote

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.CoupleProfile
import com.example.data.model.Note
import com.example.data.model.NoteCategory
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GoogleDriveFileMetadata(
    val id: String = "",
    val name: String = "",
    val modifiedTime: String = "",
    val sizeBytes: Long = 0L
)

@JsonClass(generateAdapter = true)
data class GoogleDriveBackupPayload(
    val appName: String = "TwoGether Partner Calendar",
    val formatVersion: Int = 1,
    val backupDateIso: String = "",
    val backupEpochMillis: Long = 0L,
    val userEmail: String = "",
    val coupleCode: String = "",
    val myName: String = "You",
    val partnerName: String = "Partner",
    val isPaired: Boolean = false,
    val appointmentsCount: Int = 0,
    val notesCount: Int = 0,
    val appointments: List<RemoteAppointment> = emptyList(),
    val notes: List<RemoteNote> = emptyList(),
    val appointmentCategories: List<RemoteAppointmentCategory> = emptyList(),
    val noteCategories: List<RemoteNoteCategory> = emptyList()
)

class GoogleDriveSyncService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(GoogleDriveBackupPayload::class.java).indent("  ")

    companion object {
        const val DRIVE_BACKUP_FILENAME = "TwoGether_Calendar_Backup.json"
        const val OAUTH_SCOPE_DRIVE_FILE = "oauth2:https://www.googleapis.com/auth/drive.file"
    }

    /**
     * Attempts to acquire an OAuth 2.0 access token for Google Drive API using Android's AccountManager.
     */
    suspend fun getDriveAuthToken(userEmail: String, activity: Activity? = null): String? = withContext(Dispatchers.IO) {
        try {
            val am = AccountManager.get(context)
            val accounts = am.getAccountsByType("com.google")
            val targetAccount = accounts.firstOrNull { it.name.equals(userEmail.trim(), ignoreCase = true) }
                ?: accounts.firstOrNull()

            if (targetAccount == null) {
                Log.w("GoogleDrive", "No Google account found on device matching '$userEmail'")
                return@withContext null
            }

            val bundle = am.getAuthToken(
                targetAccount,
                OAUTH_SCOPE_DRIVE_FILE,
                null,
                activity,
                null,
                null
            ).result

            val token = bundle.getString(AccountManager.KEY_AUTHTOKEN)
            if (token != null) {
                Log.d("GoogleDrive", "Successfully acquired Drive OAuth token for ${targetAccount.name}")
            }
            token
        } catch (e: Exception) {
            Log.w("GoogleDrive", "Could not get Drive OAuth token via AccountManager: ${e.message}")
            null
        }
    }

    /**
     * Builds the GoogleDriveBackupPayload structure from local models.
     */
    fun buildPayload(
        profile: CoupleProfile,
        appointments: List<Appointment>,
        notes: List<Note>,
        appointmentCategories: List<AppointmentCategory>,
        noteCategories: List<NoteCategory>
    ): GoogleDriveBackupPayload {
        val now = System.currentTimeMillis()
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(now))
        return GoogleDriveBackupPayload(
            appName = "TwoGether Partner Calendar",
            formatVersion = 1,
            backupDateIso = isoDate,
            backupEpochMillis = now,
            userEmail = profile.googleAccountEmail ?: "",
            coupleCode = profile.coupleCode,
            myName = profile.myName,
            partnerName = profile.partnerName,
            isPaired = profile.isPaired,
            appointmentsCount = appointments.size,
            notesCount = notes.size,
            appointments = appointments.map { RemoteAppointment.fromDomain(it) },
            notes = notes.map { RemoteNote.fromDomain(it) },
            appointmentCategories = appointmentCategories.map { RemoteAppointmentCategory.fromDomain(it) },
            noteCategories = noteCategories.map { RemoteNoteCategory.fromDomain(it) }
        )
    }

    fun serializePayload(payload: GoogleDriveBackupPayload): String {
        return payloadAdapter.toJson(payload)
    }

    fun deserializePayload(jsonString: String): GoogleDriveBackupPayload? {
        return try {
            payloadAdapter.fromJson(jsonString)
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error deserializing backup payload: ${e.message}", e)
            null
        }
    }

    /**
     * Search for existing TwoGether_Calendar_Backup.json in the user's Google Drive.
     */
    suspend fun findExistingDriveBackup(authToken: String): GoogleDriveFileMetadata? = withContext(Dispatchers.IO) {
        try {
            val query = "name = '$DRIVE_BACKUP_FILENAME' and trashed = false"
            val url = "https://www.googleapis.com/drive/v3/files?spaces=drive&q=${Uri.encode(query)}&fields=files(id,name,modifiedTime,size)"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w("GoogleDrive", "Search Drive failed: ${response.code}")
                    return@withContext null
                }
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val files = json.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    val first = files.getJSONObject(0)
                    return@withContext GoogleDriveFileMetadata(
                        id = first.optString("id"),
                        name = first.optString("name"),
                        modifiedTime = first.optString("modifiedTime"),
                        sizeBytes = first.optLong("size", 0L)
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error searching Drive backup: ${e.message}", e)
            null
        }
    }

    /**
     * Upload or update the backup JSON file directly in Google Drive.
     * Appears right in Google Drive as 'TwoGether_Calendar_Backup.json'.
     */
    suspend fun uploadToGoogleDrive(
        authToken: String,
        payload: GoogleDriveBackupPayload
    ): Result<GoogleDriveFileMetadata> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = serializePayload(payload)
            val existing = findExistingDriveBackup(authToken)

            if (existing != null && existing.id.isNotBlank()) {
                // Update existing file content
                val updateUrl = "https://www.googleapis.com/upload/drive/v3/files/${existing.id}?uploadType=media"
                val body = jsonContent.toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(updateUrl)
                    .addHeader("Authorization", "Bearer $authToken")
                    .patch(body)
                    .build()

                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        Log.d("GoogleDrive", "Updated existing Drive backup ${existing.id}")
                        return@withContext Result.success(existing.copy(modifiedTime = "Just now"))
                    } else {
                        return@withContext Result.failure(Exception("Drive update failed: ${resp.code} ${resp.message}"))
                    }
                }
            } else {
                // Create new file using multipart upload
                val createUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                val boundary = "==TwoGetherDriveBoundary=="
                val metadataPart = """
                    {"name":"$DRIVE_BACKUP_FILENAME","mimeType":"application/json","description":"TwoGether Partner Calendar & Notes Shared Backup"}
                """.trimIndent()

                val multipartBodyStr = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadataPart)
                    append("\r\n--$boundary\r\n")
                    append("Content-Type: application/json\r\n\r\n")
                    append(jsonContent)
                    append("\r\n--$boundary--\r\n")
                }

                val body = multipartBodyStr.toRequestBody("multipart/related; boundary=$boundary".toMediaType())
                val request = Request.Builder()
                    .url(createUrl)
                    .addHeader("Authorization", "Bearer $authToken")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { resp ->
                    val respStr = resp.body?.string() ?: ""
                    if (resp.isSuccessful) {
                        val respJson = JSONObject(respStr)
                        val newId = respJson.optString("id")
                        Log.d("GoogleDrive", "Created new Drive backup file $newId")
                        return@withContext Result.success(
                            GoogleDriveFileMetadata(
                                id = newId,
                                name = DRIVE_BACKUP_FILENAME,
                                modifiedTime = "Just now",
                                sizeBytes = jsonContent.length.toLong()
                            )
                        )
                    } else {
                        return@withContext Result.failure(Exception("Drive upload failed: ${resp.code} $respStr"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error in uploadToGoogleDrive: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads the TwoGether_Calendar_Backup.json from Google Drive.
     */
    suspend fun downloadFromGoogleDrive(
        authToken: String
    ): Result<GoogleDriveBackupPayload> = withContext(Dispatchers.IO) {
        try {
            val existing = findExistingDriveBackup(authToken)
                ?: return@withContext Result.failure(Exception("No '$DRIVE_BACKUP_FILENAME' found in your Google Drive."))

            val downloadUrl = "https://www.googleapis.com/drive/v3/files/${existing.id}?alt=media"
            val request = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Drive download error: ${resp.code}"))
                }
                val jsonString = resp.body?.string() ?: ""
                val payload = deserializePayload(jsonString)
                    ?: return@withContext Result.failure(Exception("Failed to parse Google Drive backup file JSON."))
                Result.success(payload)
            }
        } catch (e: Exception) {
            Log.e("GoogleDrive", "Error in downloadFromGoogleDrive: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Storage Access Framework (SAF) exporter.
     * Lets the user pick Google Drive (or any folder) using Android's native system file picker.
     */
    suspend fun exportToSafUri(uri: Uri, payload: GoogleDriveBackupPayload): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = serializePayload(payload)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(jsonString)
                    writer.flush()
                }
            } ?: return@withContext Result.failure(Exception("Cannot open stream for $uri"))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("GoogleDrive", "SAF export failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Storage Access Framework (SAF) importer.
     * Lets the user select the JSON file from Google Drive via Android's file picker.
     */
    suspend fun importFromSafUri(uri: Uri): Result<GoogleDriveBackupPayload> = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            } ?: return@withContext Result.failure(Exception("Cannot open stream for $uri"))

            val payload = deserializePayload(stringBuilder.toString())
                ?: return@withContext Result.failure(Exception("Selected file is not a valid TwoGether backup JSON."))
            Result.success(payload)
        } catch (e: Exception) {
            Log.e("GoogleDrive", "SAF import failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
