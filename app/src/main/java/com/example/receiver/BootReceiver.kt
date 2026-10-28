package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.system.AlarmScheduler
import com.example.core.system.TrackerReminderScheduler
import com.example.data.repository.AppBlockerRepository
import com.example.service.BlockerMonitoringService
import com.example.service.AdultFilterVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores monitoring after reboot/update and restores the optional DNS filter only
 * when the user has left that protection enabled.
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
                        val settings = AppBlockerRepository(appContext).settings
                        val adultProtectionEnabled = settings.isAdultProtectionEnabledFlow.first()
                        val dnsFilterEnabled = settings.isDnsFilterEnabledFlow.first()
                        if (adultProtectionEnabled &&
                            dnsFilterEnabled &&
                            AdultFilterVpnService.isVpnConfigured(appContext)
                        ) {
                            AdultFilterVpnService.start(appContext)
                        }
                    } catch (e: Exception) {
                        Log.w("BootReceiver", "Could not restore optional DNS filtering", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
