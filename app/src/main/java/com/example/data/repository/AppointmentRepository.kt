package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppointmentDao
import com.example.data.local.AppointmentEntity
import com.example.data.local.CouplePreferences
import com.example.data.model.Appointment
import com.example.data.model.AppointmentCategory
import com.example.data.model.OwnerType
import com.example.data.remote.PartnerSyncService
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class AppointmentRepository(
    private val context: Context,
    private val appointmentDao: AppointmentDao,
    private val couplePreferences: CouplePreferences,
    private val syncService: PartnerSyncService = PartnerSyncService()
) {

    val appointmentCategories: StateFlow<List<AppointmentCategory>> =
        couplePreferences.appointmentCategories

    val allActiveAppointments: Flow<List<Appointment>> =
        appointmentDao.getAllActiveAppointments().map { list ->
            list.map { it.toDomain() }
        }

    fun getAppointmentsForDay(dayStartMillis: Long, dayEndMillis: Long): Flow<List<Appointment>> {
        return appointmentDao.getAppointmentsForDay(dayStartMillis, dayEndMillis).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAppointmentsForDaySync(dayStartMillis: Long, dayEndMillis: Long): List<Appointment> {
        return appointmentDao.getAppointmentsForDaySync(dayStartMillis, dayEndMillis).map { it.toDomain() }
    }

    suspend fun getAppointmentById(id: String): Appointment? {
        return appointmentDao.getAppointmentById(id)?.toDomain()
    }

    suspend fun addCategory(category: AppointmentCategory) = withContext(Dispatchers.IO) {
        couplePreferences.addOrUpdateAppointmentCategory(category)
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncWithPartner(profile.coupleCode)
        }
    }

    suspend fun updateCategory(category: AppointmentCategory) = withContext(Dispatchers.IO) {
        couplePreferences.addOrUpdateAppointmentCategory(category)
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncWithPartner(profile.coupleCode)
        }
    }

    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        couplePreferences.deleteAppointmentCategory(categoryId)
        val all = appointmentDao.getAllForSync()
        all.filter { it.category == categoryId }.forEach { entity ->
            appointmentDao.insertOrUpdate(entity.copy(category = AppointmentCategory.OTHER.id, updatedAt = System.currentTimeMillis()))
        }
        val profile = couplePreferences.coupleProfile.value
        if (profile.coupleCode.isNotBlank()) {
            syncWithPartner(profile.coupleCode)
        }
    }

    suspend fun saveAppointment(appointment: Appointment, autoSync: Boolean = true) = withContext(Dispatchers.IO) {
        val updated = appointment.copy(
            updatedAt = System.currentTimeMillis()
        )
        appointmentDao.insertOrUpdate(AppointmentEntity.fromDomain(updated))
        
        // Notify widget of change
        WidgetUpdateHelper.updateAllWidgets(context)

        if (autoSync) {
            val profile = couplePreferences.coupleProfile.value
            if (profile.coupleCode.isNotBlank()) {
                syncWithPartner(profile.coupleCode)
            }
        }
    }

    suspend fun deleteAppointment(id: String, autoSync: Boolean = true) = withContext(Dispatchers.IO) {
        appointmentDao.markDeleted(id, System.currentTimeMillis())
        WidgetUpdateHelper.updateAllWidgets(context)

        if (autoSync) {
            val profile = couplePreferences.coupleProfile.value
            if (profile.coupleCode.isNotBlank()) {
                syncWithPartner(profile.coupleCode)
            }
        }
    }

    suspend fun syncWithPartner(coupleCode: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Get all local appointments including deleted markers
            val localEntities = appointmentDao.getAllForSync()
            val localList = localEntities.map { it.toDomain() }

            // 2. Push local state to cloud relay
            val currentCats = couplePreferences.appointmentCategories.value
            syncService.pushAppointments(coupleCode, localList, currentCats)

            // 3. Pull remote state from cloud relay
            val pullResult = syncService.pullAppointmentsAndCategories(coupleCode)
            var mergedCount = 0
            if (pullResult.isSuccess) {
                val (remoteList, remoteCats) = pullResult.getOrDefault(Pair(emptyList(), emptyList()))
                if (remoteCats.isNotEmpty()) {
                    couplePreferences.mergeRemoteAppointmentCategories(remoteCats)
                }

                val localMap = localEntities.associateBy { it.id }.toMutableMap()
                val toUpsert = mutableListOf<AppointmentEntity>()
                for (remote in remoteList) {
                    val local = localMap[remote.id]
                    if (local == null) {
                        toUpsert.add(AppointmentEntity.fromDomain(remote))
                        mergedCount++
                    } else if (remote.updatedAt > local.updatedAt) {
                        toUpsert.add(AppointmentEntity.fromDomain(remote))
                        mergedCount++
                    }
                }

                if (toUpsert.isNotEmpty()) {
                    appointmentDao.insertAll(toUpsert)
                }
            }

            // Update last sync time
            couplePreferences.updateProfile(lastSyncMillis = System.currentTimeMillis())
            WidgetUpdateHelper.updateAllWidgets(context)

            // Also back up to Google account if linked
            val currentProfile = couplePreferences.coupleProfile.value
            val googleEmail = currentProfile.googleAccountEmail
            if (!googleEmail.isNullOrBlank()) {
                val allEntities = appointmentDao.getAllForSync()
                syncService.saveGoogleCloudBackup(
                    com.example.data.remote.GoogleCloudBackupEnvelope(
                        userEmail = googleEmail,
                        coupleCode = coupleCode,
                        myName = currentProfile.myName,
                        partnerName = currentProfile.partnerName,
                        isPaired = currentProfile.isPaired,
                        lastUpdated = System.currentTimeMillis(),
                        appointments = allEntities.map { com.example.data.remote.RemoteAppointment.fromDomain(it.toDomain()) },
                        appointmentCategories = couplePreferences.appointmentCategories.value.map { com.example.data.remote.RemoteAppointmentCategory.fromDomain(it) }
                    )
                )
            }

            Result.success(mergedCount)
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Sync failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unlinks the current partner so friends or users can connect with a new partner.
     * @param keepOwnEvents if true, keeps the user's personal appointments and removes partner's appointments.
     *                      if false, completely resets the calendar to a fresh blank canvas.
     */
    suspend fun unlinkPartner(keepOwnEvents: Boolean) = withContext(Dispatchers.IO) {
        if (keepOwnEvents) {
            // Delete partner appointments, keep ME appointments and convert TOGETHER to ME
            appointmentDao.deletePartnerAppointments()
        } else {
            // Clear all appointments for a completely clean slate
            appointmentDao.clearAll()
        }

        val newCoupleCode = syncService.generateVerifiedUniqueCode()
        couplePreferences.updateProfile(
            coupleCode = newCoupleCode,
            isPaired = false,
            partnerName = "Partner"
        )

        WidgetUpdateHelper.updateAllWidgets(context)
    }

    /**
     * Generates and assigns a new verified unique couple code.
     */
    suspend fun generateNewVerifiedCoupleCode(): String {
        val newCode = syncService.generateVerifiedUniqueCode()
        couplePreferences.updateProfile(
            coupleCode = newCode,
            isPaired = false
        )
        return newCode
    }

    /**
     * Restores all appointments and couple settings tied to the user's Google Account
     */
    suspend fun restoreFromGoogleAccount(email: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val result = syncService.restoreGoogleCloudBackup(email)
            val envelope = result.getOrNull()
            if (envelope != null && (envelope.appointments.isNotEmpty() || envelope.appointmentCategories.isNotEmpty())) {
                var restoredCount = 0
                if (envelope.appointments.isNotEmpty()) {
                    val entities = envelope.appointments.map { AppointmentEntity.fromDomain(it.toDomain()) }
                    appointmentDao.insertAll(entities)
                    restoredCount = entities.size
                }
                if (envelope.appointmentCategories.isNotEmpty()) {
                    couplePreferences.mergeRemoteAppointmentCategories(envelope.appointmentCategories.map { it.toDomain() })
                }

                couplePreferences.updateProfile(
                    coupleCode = envelope.coupleCode.ifBlank { CouplePreferences.generateCoupleCode() },
                    isPaired = envelope.isPaired,
                    partnerName = envelope.partnerName,
                    lastSyncMillis = System.currentTimeMillis()
                )

                WidgetUpdateHelper.updateAllWidgets(context)
                Result.success(restoredCount)
            } else {
                Result.success(0)
            }
        } catch (e: Exception) {
            Log.e("AppointmentRepository", "Google restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Placeholder data removed per user request for a completely clean slate.
     */
    suspend fun seedInitialDataIfEmpty(myName: String = "You", partnerName: String = "Alex") = withContext(Dispatchers.IO) {
        // No-op
    }

    suspend fun purgePlaceholderAppointments() = withContext(Dispatchers.IO) {
        val placeholders = listOf(
            "Romantic Dinner at Osteria",
            "Dentist Checkup",
            "Weekend Botanical Garden Walk",
            "Surprise Movie & Popcorn Night",
            "Couples Cooking Workshop",
            "Weekend Getaway Cabin Booking"
        )
        val all = appointmentDao.getAllActiveAppointmentsList()
        var purged = false
        all.forEach { entity ->
            if (placeholders.any { entity.title.contains(it, ignoreCase = true) }) {
                appointmentDao.deletePermanently(entity.id)
                purged = true
            }
        }
        if (purged) {
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }

    /**
     * Helper to simulate a partner adding an appointment over the internet to test synchronization
     */
    suspend fun simulatePartnerAction(
        partnerName: String,
        coupleCode: String
    ): Appointment = withContext(Dispatchers.IO) {
        val ideas = listOf(
            Triple("Surprise Movie & Popcorn Night 🎬", "I got two tickets for the new release!", AppointmentCategory.DATE_NIGHT),
            Triple("$partnerName's Team Presentation 💻", "Important client pitch meeting", AppointmentCategory.WORK),
            Triple("Couples Cooking Workshop 👨‍🍳👩‍🍳", "Making fresh homemade gnocchi", AppointmentCategory.TOGETHER_PLAN),
            Triple("Weekend Getaway Cabin Booking 🌲", "Cozy cabin in the woods", AppointmentCategory.TRAVEL),
            Triple("$partnerName's Gym Session 🏋️", "Leg day training", AppointmentCategory.PERSONAL)
        )
        val selected = ideas.random()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, (1..5).random())
        cal.set(Calendar.HOUR_OF_DAY, (10..20).random())
        cal.set(Calendar.MINUTE, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, 2)
        val end = cal.timeInMillis

        val isTogether = selected.third == AppointmentCategory.DATE_NIGHT || selected.third == AppointmentCategory.TOGETHER_PLAN || selected.third == AppointmentCategory.TRAVEL
        val newAppt = Appointment(
            id = UUID.randomUUID().toString(),
            title = selected.first,
            description = selected.second,
            location = "City Center",
            startEpochMillis = start,
            endEpochMillis = end,
            ownerType = if (isTogether) OwnerType.TOGETHER else OwnerType.PARTNER,
            createdByName = partnerName,
            category = selected.third,
            colorHex = if (isTogether) "#8B5CF6" else "#EC4899",
            coupleId = coupleCode,
            updatedAt = System.currentTimeMillis()
        )

        appointmentDao.insertOrUpdate(AppointmentEntity.fromDomain(newAppt))
        syncService.pushAppointments(coupleCode, listOf(newAppt))
        WidgetUpdateHelper.updateAllWidgets(context)
        newAppt
    }

    suspend fun getAllAppointmentsForBackup(): List<Appointment> = withContext(Dispatchers.IO) {
        appointmentDao.getAllForSync().map { it.toDomain() }
    }

    suspend fun restoreAppointmentsFromList(appointments: List<Appointment>): Int = withContext(Dispatchers.IO) {
        val entities = appointments.map { AppointmentEntity.fromDomain(it) }
        if (entities.isNotEmpty()) {
            appointmentDao.insertAll(entities)
            WidgetUpdateHelper.updateAllWidgets(context)
        }
        entities.size
    }
}
