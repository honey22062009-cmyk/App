package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleEntityDao {

    @Query("SELECT * FROM schedules ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllSchedulesFlow(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE appPackage = :pkg ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getSchedulesForAppFlow(pkg: String): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE appPackage = :pkg ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    suspend fun getSchedulesForApp(pkg: String): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE isEnabled = 1")
    suspend fun getAllEnabledSchedules(): List<ScheduleEntity>

    @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduleEntity>)

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM schedules WHERE appPackage = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("UPDATE schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setScheduleEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedules")
    suspend fun deleteAll()
}
