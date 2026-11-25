package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.system.AlarmScheduler
import com.example.service.BlockerMonitoringService

/** Restores the scheduled-app monitor and alarm boundaries after reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                BlockerMonitoringService.start(context)
                AlarmScheduler.scheduleNextTransition(context)
            }
        }
    }
}
