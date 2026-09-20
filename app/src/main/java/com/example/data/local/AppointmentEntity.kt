package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.OwnerType

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val isAllDay: Boolean,
    val ownerType: String,
    val createdByName: String,
    val category: String,
    val colorHex: String,
    val coupleId: String,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val hasReminder: Boolean,
    val reminderMinutesBefore: Int
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
        fun fromDomain(appointment: Appointment): AppointmentEntity {
            return AppointmentEntity(
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
