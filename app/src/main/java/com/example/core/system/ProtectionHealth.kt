package com.example.core.system

import android.content.Context
import com.example.service.AdultFilterVpnService

data class ProtectionHealthSnapshot(
    val accessibilityEnabled: Boolean,
    val usageAccessGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val exactAlarmsAllowed: Boolean,
    val notificationsAllowed: Boolean,
    val vpnConfigured: Boolean
) {
    val coreReady: Boolean
        get() = accessibilityEnabled && usageAccessGranted

    val reliabilityScore: Int
        get() {
            val checks = listOf(
                accessibilityEnabled,
                usageAccessGranted,
                batteryOptimizationIgnored,
                exactAlarmsAllowed,
                notificationsAllowed
            )
            return (checks.count { it } * 100) / checks.size
        }

    val degradedReasons: List<String>
        get() = buildList {
            if (!accessibilityEnabled) add("Accessibility interception is disabled.")
            if (!usageAccessGranted) add("Usage Access fallback detection is unavailable.")
            if (!batteryOptimizationIgnored) add("Android may restrict background monitoring.")
            if (!exactAlarmsAllowed) add("Schedule boundaries use an inexact fallback.")
            if (!notificationsAllowed) add("Protection service notifications may be hidden.")
        }
}

object ProtectionHealth {
    fun snapshot(context: Context): ProtectionHealthSnapshot {
        val app = context.applicationContext
        return ProtectionHealthSnapshot(
            accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(app),
            usageAccessGranted = PermissionHelper.isUsageAccessGranted(app),
            batteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(app),
            exactAlarmsAllowed = PermissionHelper.canScheduleExactAlarms(app),
            notificationsAllowed = PermissionHelper.isNotificationPermissionGranted(app),
            vpnConfigured = AdultFilterVpnService.isVpnConfigured(app)
        )
    }
}
