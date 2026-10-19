package com.example

import android.app.Application
import android.util.Log
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer
import com.example.core.system.AlarmScheduler

class StrictBlockerApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Safely schedule transition alarms if any exist
        try {
            AlarmScheduler.scheduleNextTransition(this)
        } catch (t: Throwable) {
            Log.e("StrictBlockerApp", "Error scheduling initial transition", t)
        }
    }
}
