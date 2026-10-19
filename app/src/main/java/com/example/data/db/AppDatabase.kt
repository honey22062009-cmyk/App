package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.db.dao.AppDao
import com.example.data.db.dao.BlockEventDao
import com.example.data.db.dao.ManagedAppDao
import com.example.data.db.dao.ScheduleDao
import com.example.data.db.dao.ScheduleEntityDao
import com.example.data.db.entity.AppEntity
import com.example.data.db.entity.BlockEventEntity
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.db.entity.ScheduleEntity
import com.example.data.db.entity.SchedulePeriodEntity

@Database(
    entities = [
        AppEntity::class,
        ScheduleEntity::class,
        ManagedAppEntity::class,
        SchedulePeriodEntity::class,
        BlockEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao
    abstract fun scheduleEntityDao(): ScheduleEntityDao
    abstract fun managedAppDao(): ManagedAppDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun blockEventDao(): BlockEventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "strict_blocker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
