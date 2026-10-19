package com.example.core.di

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.dao.AppDao
import com.example.data.db.dao.BlockEventDao
import com.example.data.db.dao.ManagedAppDao
import com.example.data.db.dao.ScheduleDao
import com.example.data.db.dao.ScheduleEntityDao
import com.example.data.preferences.SettingsPreferences
import com.example.data.repository.AppBlockerRepository

/**
 * Dependency Injection container for managing application-wide singletons
 * including Room Database, DAOs, DataStore Preferences, and Repositories.
 */
interface AppContainer {
    val database: AppDatabase
    val appDao: AppDao
    val scheduleEntityDao: ScheduleEntityDao
    val managedAppDao: ManagedAppDao
    val scheduleDao: ScheduleDao
    val blockEventDao: BlockEventDao
    val settingsPreferences: SettingsPreferences
    val repository: AppBlockerRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val appDao: AppDao by lazy {
        database.appDao()
    }

    override val scheduleEntityDao: ScheduleEntityDao by lazy {
        database.scheduleEntityDao()
    }

    override val managedAppDao: ManagedAppDao by lazy {
        database.managedAppDao()
    }

    override val scheduleDao: ScheduleDao by lazy {
        database.scheduleDao()
    }

    override val blockEventDao: BlockEventDao by lazy {
        database.blockEventDao()
    }

    override val settingsPreferences: SettingsPreferences by lazy {
        SettingsPreferences(context)
    }

    override val repository: AppBlockerRepository by lazy {
        AppBlockerRepository(
            context = context,
            database = database,
            managedAppDao = managedAppDao,
            scheduleDao = scheduleDao,
            blockEventDao = blockEventDao,
            settings = settingsPreferences,
            appDao = appDao,
            scheduleEntityDao = scheduleEntityDao
        )
    }
}
