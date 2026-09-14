package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppointmentCategory
import com.example.data.model.CoupleProfile
import com.example.data.model.NoteCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CouplePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("couple_prefs", Context.MODE_PRIVATE)

    private val _coupleProfile = MutableStateFlow(loadProfile())
    val coupleProfile: StateFlow<CoupleProfile> = _coupleProfile.asStateFlow()

    private val _noteCategories = MutableStateFlow<List<NoteCategory>>(loadNoteCategories())
    val noteCategories: StateFlow<List<NoteCategory>> = _noteCategories.asStateFlow()

    private val _appointmentCategories = MutableStateFlow<List<AppointmentCategory>>(loadAppointmentCategories())
    val appointmentCategories: StateFlow<List<AppointmentCategory>> = _appointmentCategories.asStateFlow()

    private fun loadProfile(): CoupleProfile {
        var code = prefs.getString("couple_code", null)
        if (code.isNullOrBlank()) {
            code = generateCoupleCode()
            prefs.edit().putString("couple_code", code).apply()
        }
        return CoupleProfile(
            coupleCode = code,
            isPaired = prefs.getBoolean("is_paired", false),
            myName = prefs.getString("my_name", "You") ?: "You",
            partnerName = prefs.getString("partner_name", "Partner") ?: "Partner",
            myColorHex = prefs.getString("my_color_hex", "#3B82F6") ?: "#3B82F6",
            partnerColorHex = prefs.getString("partner_color_hex", "#EC4899") ?: "#EC4899",
            togetherColorHex = prefs.getString("together_color_hex", "#8B5CF6") ?: "#8B5CF6",
            anniversaryMillis = if (prefs.contains("anniversary_millis")) prefs.getLong("anniversary_millis", 0L) else null,
            lastSyncMillis = prefs.getLong("last_sync_millis", 0L),
            autoSyncEnabled = prefs.getBoolean("auto_sync_enabled", true),
            appThemeMode = prefs.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM",
            widgetThemeMode = prefs.getString("widget_theme_mode", "SYSTEM") ?: "SYSTEM",
            appLanguage = prefs.getString("app_language", "SYSTEM") ?: "SYSTEM",
            googleAccountEmail = prefs.getString("google_account_email", null),
            googleAccountName = prefs.getString("google_account_name", null),
            googleAccountPhotoUrl = prefs.getString("google_account_photo", null),
            isGoogleLinked = prefs.getBoolean("is_google_linked", false)
        )
    }

    fun updateProfile(
        myName: String? = null,
        partnerName: String? = null,
        myColorHex: String? = null,
        partnerColorHex: String? = null,
        togetherColorHex: String? = null,
        anniversaryMillis: Long? = null,
        isPaired: Boolean? = null,
        coupleCode: String? = null,
        lastSyncMillis: Long? = null,
        appThemeMode: String? = null,
        widgetThemeMode: String? = null,
        appLanguage: String? = null,
        googleAccountEmail: String? = null,
        googleAccountName: String? = null,
        googleAccountPhotoUrl: String? = null,
        isGoogleLinked: Boolean? = null
    ) {
        val editor = prefs.edit()
        val current = _coupleProfile.value

        val newMyName = myName ?: current.myName
        val newPartnerName = partnerName ?: current.partnerName
        val newMyColor = myColorHex ?: current.myColorHex
        val newPartnerColor = partnerColorHex ?: current.partnerColorHex
        val newTogetherColor = togetherColorHex ?: current.togetherColorHex
        val newCoupleCode = coupleCode ?: current.coupleCode
        val newIsPaired = isPaired ?: current.isPaired
        val newLastSync = lastSyncMillis ?: current.lastSyncMillis
        val newAnniversary = if (anniversaryMillis != null) anniversaryMillis else current.anniversaryMillis
        val newAppTheme = appThemeMode ?: current.appThemeMode
        val newWidgetTheme = widgetThemeMode ?: current.widgetThemeMode
        val newLanguage = appLanguage ?: current.appLanguage
        val newGoogleEmail = if (googleAccountEmail != null) (if (googleAccountEmail.isBlank()) null else googleAccountEmail) else current.googleAccountEmail
        val newGoogleName = if (googleAccountName != null) (if (googleAccountName.isBlank()) null else googleAccountName) else current.googleAccountName
        val newGooglePhoto = googleAccountPhotoUrl ?: current.googleAccountPhotoUrl
        val newGoogleLinked = isGoogleLinked ?: current.isGoogleLinked

        editor.putString("my_name", newMyName)
        editor.putString("partner_name", newPartnerName)
        editor.putString("my_color_hex", newMyColor)
        editor.putString("partner_color_hex", newPartnerColor)
        editor.putString("together_color_hex", newTogetherColor)
        editor.putString("couple_code", newCoupleCode)
        editor.putBoolean("is_paired", newIsPaired)
        editor.putLong("last_sync_millis", newLastSync)
        editor.putString("app_theme_mode", newAppTheme)
        editor.putString("widget_theme_mode", newWidgetTheme)
        editor.putString("app_language", newLanguage)
        if (newGoogleEmail != null) editor.putString("google_account_email", newGoogleEmail) else editor.remove("google_account_email")
        if (newGoogleName != null) editor.putString("google_account_name", newGoogleName) else editor.remove("google_account_name")
        if (newGooglePhoto != null) editor.putString("google_account_photo", newGooglePhoto) else editor.remove("google_account_photo")
        editor.putBoolean("is_google_linked", newGoogleLinked)

        if (newAnniversary != null) {
            editor.putLong("anniversary_millis", newAnniversary)
        }
        editor.apply()

        _coupleProfile.value = current.copy(
            myName = newMyName,
            partnerName = newPartnerName,
            myColorHex = newMyColor,
            partnerColorHex = newPartnerColor,
            togetherColorHex = newTogetherColor,
            coupleCode = newCoupleCode,
            isPaired = newIsPaired,
            lastSyncMillis = newLastSync,
            anniversaryMillis = newAnniversary,
            appThemeMode = newAppTheme,
            widgetThemeMode = newWidgetTheme,
            appLanguage = newLanguage,
            googleAccountEmail = newGoogleEmail,
            googleAccountName = newGoogleName,
            googleAccountPhotoUrl = newGooglePhoto,
            isGoogleLinked = newGoogleLinked
        )
    }

    fun setGoogleAccount(email: String, displayName: String, photoUrl: String?) {
        updateProfile(
            googleAccountEmail = email,
            googleAccountName = displayName,
            googleAccountPhotoUrl = photoUrl,
            isGoogleLinked = true,
            myName = if (_coupleProfile.value.myName == "You" || _coupleProfile.value.myName.isBlank()) displayName else _coupleProfile.value.myName
        )
    }

    fun disconnectGoogleAccount() {
        val editor = prefs.edit()
        editor.remove("google_account_email")
        editor.remove("google_account_name")
        editor.remove("google_account_photo")
        editor.putBoolean("is_google_linked", false)
        editor.apply()

        _coupleProfile.value = _coupleProfile.value.copy(
            googleAccountEmail = null,
            googleAccountName = null,
            googleAccountPhotoUrl = null,
            isGoogleLinked = false
        )
    }

    fun joinCoupleCode(code: String, partnerName: String = "Partner") {
        updateProfile(
            coupleCode = code.uppercase().trim(),
            partnerName = partnerName,
            isPaired = true
        )
    }

    fun disconnectPairing() {
        val newCode = generateCoupleCode()
        updateProfile(
            coupleCode = newCode,
            isPaired = false,
            partnerName = "Partner"
        )
    }

    private fun loadNoteCategories(): List<NoteCategory> {
        val json = prefs.getString("note_categories_json", null)
        if (json.isNullOrBlank()) {
            return NoteCategory.DEFAULT_CATEGORIES
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<NoteCategory>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isDeleted = obj.optBoolean("isDeleted", false)
                if (!isDeleted) {
                    list.add(
                        NoteCategory(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            displayName = obj.optString("displayName", "General"),
                            iconEmoji = obj.optString("iconEmoji", "📝"),
                            defaultColorHex = obj.optString("defaultColorHex", "#EDE9FE"),
                            isCustom = obj.optBoolean("isCustom", false),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = false
                        )
                    )
                }
            }
            if (list.isEmpty()) NoteCategory.DEFAULT_CATEGORIES else list
        } catch (e: Exception) {
            NoteCategory.DEFAULT_CATEGORIES
        }
    }

    fun saveNoteCategories(categories: List<NoteCategory>) {
        _noteCategories.value = categories
        try {
            val array = JSONArray()
            categories.forEach { cat ->
                val obj = JSONObject()
                obj.put("id", cat.id)
                obj.put("displayName", cat.displayName)
                obj.put("iconEmoji", cat.iconEmoji)
                obj.put("defaultColorHex", cat.defaultColorHex)
                obj.put("isCustom", cat.isCustom)
                obj.put("updatedAt", cat.updatedAt)
                obj.put("isDeleted", cat.isDeleted)
                array.put(obj)
            }
            prefs.edit().putString("note_categories_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateNoteCategory(category: NoteCategory) {
        val current = _noteCategories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(category.copy(updatedAt = System.currentTimeMillis()))
        }
        saveNoteCategories(current)
    }

    fun deleteNoteCategory(categoryId: String) {
        val current = _noteCategories.value.filter { it.id != categoryId }
        // Ensure at least General category remains
        val finalCategories = if (current.isEmpty()) listOf(NoteCategory.GENERAL) else current
        saveNoteCategories(finalCategories)
    }

    fun mergeRemoteNoteCategories(remoteCats: List<NoteCategory>) {
        if (remoteCats.isEmpty()) return
        val current = _noteCategories.value.toMutableList()
        var changed = false

        for (remote in remoteCats) {
            val index = current.indexOfFirst { it.id == remote.id }
            if (index >= 0) {
                val existing = current[index]
                if (remote.isDeleted) {
                    current.removeAt(index)
                    changed = true
                } else if (remote.updatedAt > existing.updatedAt) {
                    current[index] = remote
                    changed = true
                }
            } else if (!remote.isDeleted) {
                current.add(remote)
                changed = true
            }
        }

        if (changed) {
            saveNoteCategories(current)
        }
    }

    private fun loadAppointmentCategories(): List<AppointmentCategory> {
        val json = prefs.getString("appointment_categories_json", null)
        if (json.isNullOrBlank()) {
            return AppointmentCategory.DEFAULT_CATEGORIES
        }
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<AppointmentCategory>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val isDeleted = obj.optBoolean("isDeleted", false)
                if (!isDeleted) {
                    list.add(
                        AppointmentCategory(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            displayName = obj.optString("displayName", "Other"),
                            iconEmoji = obj.optString("iconEmoji", "📌"),
                            defaultColorHex = obj.optString("defaultColorHex", "#8B5CF6"),
                            isCustom = obj.optBoolean("isCustom", false),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isDeleted = false
                        )
                    )
                }
            }
            if (list.isEmpty()) AppointmentCategory.DEFAULT_CATEGORIES else list
        } catch (e: Exception) {
            AppointmentCategory.DEFAULT_CATEGORIES
        }
    }

    fun saveAppointmentCategories(categories: List<AppointmentCategory>) {
        _appointmentCategories.value = categories
        try {
            val array = JSONArray()
            categories.forEach { cat ->
                val obj = JSONObject()
                obj.put("id", cat.id)
                obj.put("displayName", cat.displayName)
                obj.put("iconEmoji", cat.iconEmoji)
                obj.put("defaultColorHex", cat.defaultColorHex)
                obj.put("isCustom", cat.isCustom)
                obj.put("updatedAt", cat.updatedAt)
                obj.put("isDeleted", cat.isDeleted)
                array.put(obj)
            }
            prefs.edit().putString("appointment_categories_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addOrUpdateAppointmentCategory(category: AppointmentCategory) {
        val current = _appointmentCategories.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category.copy(updatedAt = System.currentTimeMillis())
        } else {
            current.add(category.copy(updatedAt = System.currentTimeMillis()))
        }
        saveAppointmentCategories(current)
    }

    fun deleteAppointmentCategory(categoryId: String) {
        val current = _appointmentCategories.value.filter { it.id != categoryId }
        val finalCategories = if (current.isEmpty()) listOf(AppointmentCategory.OTHER) else current
        saveAppointmentCategories(finalCategories)
    }

    fun mergeRemoteAppointmentCategories(remoteCats: List<AppointmentCategory>) {
        if (remoteCats.isEmpty()) return
        val current = _appointmentCategories.value.toMutableList()
        var changed = false

        for (remote in remoteCats) {
            val index = current.indexOfFirst { it.id == remote.id }
            if (index >= 0) {
                val existing = current[index]
                if (remote.isDeleted) {
                    current.removeAt(index)
                    changed = true
                } else if (remote.updatedAt > existing.updatedAt) {
                    current[index] = remote
                    changed = true
                }
            } else if (!remote.isDeleted) {
                current.add(remote)
                changed = true
            }
        }

        if (changed) {
            saveAppointmentCategories(current)
        }
    }

    companion object {
        fun generateCoupleCode(): String {
            val words = listOf("LOVE", "HEART", "PAIR", "BOND", "SOUL", "DATE", "SWEET", "COUPLE", "NEST")
            val prefix = words.random()
            val number = (100..999).random()
            return "$prefix-$number"
        }
    }
}
