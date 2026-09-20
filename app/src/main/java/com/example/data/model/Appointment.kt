package com.example.data.model

import java.util.UUID

enum class OwnerType(val displayName: String, val badge: String) {
    TOGETHER("Together", "💜"),
    ME("My Appointment", "💙"),
    PARTNER("Partner's", "💖");

    companion object {
        fun fromString(value: String): OwnerType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TOGETHER
        }
    }
}

data class AppointmentCategory(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    val iconEmoji: String = "📌",
    val defaultColorHex: String = "#8B5CF6",
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
) {
    val name: String get() = id
    val defaultDisplayName: String get() = displayName

    companion object {
        val DATE_NIGHT = AppointmentCategory("DATE_NIGHT", "Date Night", "🥂", "#E11D48", isCustom = false, updatedAt = 0L)
        val TOGETHER_PLAN = AppointmentCategory("TOGETHER_PLAN", "Together", "💜", "#8B5CF6", isCustom = false, updatedAt = 0L)
        val WORK = AppointmentCategory("WORK", "Work", "💼", "#0284C7", isCustom = false, updatedAt = 0L)
        val HEALTH = AppointmentCategory("HEALTH", "Health & Doctor", "🩺", "#059669", isCustom = false, updatedAt = 0L)
        val TRAVEL = AppointmentCategory("TRAVEL", "Travel & Trip", "✈️", "#D97706", isCustom = false, updatedAt = 0L)
        val HOME_CHORE = AppointmentCategory("HOME_CHORE", "Home & Errands", "🏠", "#EA580C", isCustom = false, updatedAt = 0L)
        val CELEBRATION = AppointmentCategory("CELEBRATION", "Celebration", "🎂", "#DB2777", isCustom = false, updatedAt = 0L)
        val PERSONAL = AppointmentCategory("PERSONAL", "Personal", "👤", "#4B5563", isCustom = false, updatedAt = 0L)
        val OTHER = AppointmentCategory("OTHER", "Other", "📌", "#6B7280", isCustom = false, updatedAt = 0L)

        val DEFAULT_CATEGORIES: List<AppointmentCategory> = listOf(
            DATE_NIGHT,
            TOGETHER_PLAN,
            WORK,
            HEALTH,
            TRAVEL,
            HOME_CHORE,
            CELEBRATION,
            PERSONAL,
            OTHER
        )

        val entries: List<AppointmentCategory> get() = DEFAULT_CATEGORIES

        fun fromString(value: String, available: List<AppointmentCategory> = DEFAULT_CATEGORIES): AppointmentCategory {
            if (value.isBlank()) return OTHER
            return available.firstOrNull {
                it.id.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true) ||
                it.name.equals(value, ignoreCase = true)
            } ?: AppointmentCategory(id = value, displayName = value, iconEmoji = "📌", defaultColorHex = "#8B5CF6", isCustom = true)
        }
    }
}

data class Appointment(
    val id: String,
    val title: String,
    val description: String = "",
    val location: String = "",
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val isAllDay: Boolean = false,
    val ownerType: OwnerType = OwnerType.TOGETHER,
    val createdByName: String = "Me",
    val category: AppointmentCategory = AppointmentCategory.DATE_NIGHT,
    val colorHex: String = "#8B5CF6",
    val coupleId: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 30
)

data class CoupleProfile(
    val coupleCode: String = "",
    val isPaired: Boolean = false,
    val myName: String = "You",
    val partnerName: String = "Partner",
    val myColorHex: String = "#3B82F6",
    val partnerColorHex: String = "#EC4899",
    val togetherColorHex: String = "#8B5CF6",
    val anniversaryMillis: Long? = null,
    val lastSyncMillis: Long = 0L,
    val autoSyncEnabled: Boolean = true,
    val appThemeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val widgetThemeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val appLanguage: String = "SYSTEM", // "SYSTEM", "EN", "DE"
    val googleAccountEmail: String? = null,
    val googleAccountName: String? = null,
    val googleAccountPhotoUrl: String? = null,
    val isGoogleLinked: Boolean = false,
    val autoSyncIntervalMinutes: Int = 15
)

data class PartnerLinkRequest(
    val coupleCode: String = "",
    val partnerName: String = "",
    val partnerDeviceId: String = "",
    val timestamp: Long = 0L
)

enum class SyncState {
    IDLE,
    SYNCING,
    SUCCESS,
    OFFLINE,
    ERROR
}
