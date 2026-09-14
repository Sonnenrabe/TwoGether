package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Query("SELECT * FROM appointments WHERE isDeleted = 0 ORDER BY startEpochMillis ASC")
    fun getAllActiveAppointments(): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE isDeleted = 0 ORDER BY startEpochMillis ASC")
    suspend fun getAllActiveAppointmentsList(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments ORDER BY updatedAt DESC")
    suspend fun getAllForSync(): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentById(id: String): AppointmentEntity?

    @Query("""
        SELECT * FROM appointments 
        WHERE isDeleted = 0 
          AND startEpochMillis < :dayEnd 
          AND endEpochMillis >= :dayStart 
        ORDER BY startEpochMillis ASC
    """)
    fun getAppointmentsForDay(dayStart: Long, dayEnd: Long): Flow<List<AppointmentEntity>>

    @Query("""
        SELECT * FROM appointments 
        WHERE isDeleted = 0 
          AND startEpochMillis < :dayEnd 
          AND endEpochMillis >= :dayStart 
        ORDER BY startEpochMillis ASC
    """)
    suspend fun getAppointmentsForDaySync(dayStart: Long, dayEnd: Long): List<AppointmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appointment: AppointmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(appointments: List<AppointmentEntity>)

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("DELETE FROM appointments WHERE ownerType = 'PARTNER'")
    suspend fun deletePartnerAppointments()

    @Query("DELETE FROM appointments")
    suspend fun clearAll()
}
