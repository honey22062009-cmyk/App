package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.system.AlarmScheduler
import com.example.core.system.TrackerReminderScheduler
import com.example.data.repository.AppBlockerRepository
import com.example.service.BlockerMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Repairs schedules and reminder alarms after reboot or app updates.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                BlockerMonitoringService.start(context)
                AlarmScheduler.scheduleNextTransition(context)
                TrackerReminderScheduler.refresh(context)

                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        TrackerReminderScheduler.refreshNow(appContext)

                    } catch (e: Exception) {
                        Log.w("BootReceiver", "Could not refresh reminders", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
