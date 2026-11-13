package com.example

import android.app.Application
import android.util.Log
import com.example.core.system.AlarmScheduler
import com.example.core.system.InitialRestrictionConfig
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StrictBlockerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Seed defaults once, without replacing any user-created configuration.
        // Schedule alarms only after the initial database configuration is committed.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                InitialRestrictionConfig.seedIfNeeded(this@StrictBlockerApplication)
                AlarmScheduler.scheduleNextTransition(this@StrictBlockerApplication)
            } catch (t: Throwable) {
                Log.e("StrictBlockerApp", "Error restoring initial restriction configuration", t)
            }
        }
    }
}
