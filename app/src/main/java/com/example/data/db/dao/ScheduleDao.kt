package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.SchedulePeriodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Query("SELECT * FROM schedule_periods ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getAllPeriodsFlow(): Flow<List<SchedulePeriodEntity>>

    @Query("SELECT * FROM schedule_periods WHERE appPackage = :pkg ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    fun getPeriodsForAppFlow(pkg: String): Flow<List<SchedulePeriodEntity>>

    @Query("SELECT * FROM schedule_periods WHERE appPackage = :pkg ORDER BY dayOfWeek ASC, startHour ASC, startMinute ASC")
    suspend fun getPeriodsForApp(pkg: String): List<SchedulePeriodEntity>

    @Query("SELECT * FROM schedule_periods WHERE isEnabled = 1")
    suspend fun getAllEnabledPeriods(): List<SchedulePeriodEntity>

    @Query("SELECT * FROM schedule_periods WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SchedulePeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(period: SchedulePeriodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(periods: List<SchedulePeriodEntity>)

    @Update
    suspend fun update(period: SchedulePeriodEntity)

    @Delete
    suspend fun delete(period: SchedulePeriodEntity)

    @Query("DELETE FROM schedule_periods WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM schedule_periods WHERE appPackage = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("UPDATE schedule_periods SET isEnabled = :enabled WHERE id = :id")
    suspend fun setPeriodEnabled(id: Long, enabled: Boolean)

    @Query("DELETE FROM schedule_periods")
    suspend fun deleteAll()
}
