package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllAppsFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isManaged = 1")
    suspend fun getActiveApps(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE isManaged = 1")
    fun getActiveAppsFlow(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE packageName = :pkg LIMIT 1")
    suspend fun getByPackage(pkg: String): AppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Update
    suspend fun update(app: AppEntity)

    @Delete
    suspend fun delete(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName = :pkg")
    suspend fun deleteByPackage(pkg: String)

    @Query("UPDATE apps SET isManaged = :managed WHERE packageName = :pkg")
    suspend fun setManagedStatus(pkg: String, managed: Boolean)

    @Query("DELETE FROM apps")
    suspend fun deleteAll()
}
