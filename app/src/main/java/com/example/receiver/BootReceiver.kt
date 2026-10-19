package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.system.AlarmScheduler
import com.example.service.BlockerMonitoringService

/**
 * Ensures strict blocking enforcement and exact schedule alarms resume immediately after reboot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Restart monitoring service
                BlockerMonitoringService.start(context)

                // Reschedule all exact schedule transition alarms
                AlarmScheduler.scheduleNextTransition(context)

                // Resume adult content DNS filtering if configured
                try {
                    if (com.example.service.AdultFilterVpnService.isVpnConfigured(context)) {
                        com.example.service.AdultFilterVpnService.start(context)
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
