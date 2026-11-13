package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.core.system.AlarmScheduler
import com.example.core.system.InitialRestrictionConfig
import com.example.data.repository.AppBlockerRepository
import com.example.service.BlockerMonitoringService
import com.example.service.AdultFilterVpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-seeds only the versioned defaults (never overwriting user schedules), then
 * recalculates the actual current time state and restores eligible background work.
 *
 * Credential-protected Room/DataStore data is restored after Android has completed
 * boot and the user has unlocked the device. This receiver is not Direct-Boot aware.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        InitialRestrictionConfig.seedIfNeeded(appContext)

                        val repository = AppBlockerRepository(appContext)
                        val protectionEnabled = repository.settings.isProtectionEnabledFlow.first()

                        if (protectionEnabled) {
                            // Android may still reject a background service start on some
                            // builds/OEM policies. The scheduled alarms remain a separate path.
                            BlockerMonitoringService.start(appContext)
                        }

                        AlarmScheduler.scheduleNextTransition(appContext)

                        val adultProtectionEnabled = repository.settings.isAdultProtectionEnabledFlow.first()
                        val dnsFilterEnabled = repository.settings.isDnsFilterEnabledFlow.first()
                        if (adultProtectionEnabled &&
                            dnsFilterEnabled &&
                            AdultFilterVpnService.isVpnConfigured(appContext)
                        ) {
                            AdultFilterVpnService.start(appContext)
                        }
                    } catch (e: Exception) {
                        Log.w("BootReceiver", "Could not fully restore protection after boot/update", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
