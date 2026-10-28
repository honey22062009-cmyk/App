package com.example

import android.app.AlarmManager
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.core.di.AppContainer
import com.example.core.di.DefaultAppContainer
import com.example.core.system.AlarmScheduler
import com.example.core.system.TrackerReminderScheduler
import com.example.data.repository.AppBlockerRepository
import com.example.service.AdultFilterVpnService
import com.example.service.BlockerMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StrictBlockerApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val exactAlarmPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                intent?.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            ) {
                AlarmScheduler.scheduleNextTransition(context)
                TrackerReminderScheduler.refresh(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)

        // Every process start is a repair opportunity. Re-schedule boundaries and
        // re-establish monitoring if the user has protection enabled.
        AlarmScheduler.scheduleNextTransition(this)
        repairProtectionServices()
        TrackerReminderScheduler.refresh(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val filter = IntentFilter(
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(exactAlarmPermissionReceiver, filter, RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("DEPRECATION")
                    registerReceiver(exactAlarmPermissionReceiver, filter)
                }
            } catch (t: Throwable) {
                Log.w("StrictBlockerApp", "Could not register exact-alarm state receiver", t)
            }
        }
    }

    private fun repairProtectionServices() {
        appScope.launch {
            try {
                val repository = AppBlockerRepository(applicationContext)
                if (repository.settings.isProtectionEnabledFlow.first()) {
                    BlockerMonitoringService.start(applicationContext)
                }

                val adultEnabled = repository.settings.isAdultProtectionEnabledFlow.first()
                val dnsEnabled = repository.settings.isDnsFilterEnabledFlow.first()
                if (
                    adultEnabled &&
                    dnsEnabled &&
                    AdultFilterVpnService.isVpnConfigured(applicationContext)
                ) {
                    AdultFilterVpnService.start(applicationContext)
                }
            } catch (t: Throwable) {
                Log.w("StrictBlockerApp", "Protection repair pass failed", t)
            }
        }
    }

    override fun onTerminate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { unregisterReceiver(exactAlarmPermissionReceiver) }
        }
        super.onTerminate()
    }
}
