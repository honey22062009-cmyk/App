package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.ManagedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ManagedAppDao {

    @Query("SELECT * FROM managed_apps ORDER BY appName ASC")
    fun getAllManagedAppsFlow(): Flow<List<ManagedAppEntity>>

    @Query("SELECT * FROM managed_apps WHERE isManaged = 1")
    suspend fun getActiveManagedApps(): List<ManagedAppEntity>

    @Query("SELECT * FROM managed_apps WHERE isManaged = 1")
    fun getActiveManagedAppsFlow(): Flow<List<ManagedAppEntity>>

    @Query("SELECT * FROM managed_apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): ManagedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: ManagedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<ManagedAppEntity>)

    @Update
    suspend fun update(app: ManagedAppEntity)

    @Delete
    suspend fun delete(app: ManagedAppEntity)

    @Query("DELETE FROM managed_apps WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("UPDATE managed_apps SET isManaged = :managed WHERE packageName = :pkg")
    suspend fun setManagedStatus(pkg: String, managed: Boolean)

    @Query("DELETE FROM managed_apps")
    suspend fun deleteAll()
}
