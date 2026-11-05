package com.example.core.system

import android.content.Context

data class ProtectionHealthSnapshot(
    val accessibilityEnabled: Boolean,
    val usageAccessGranted: Boolean,
    val batteryOptimizationIgnored: Boolean,
    val exactAlarmsAllowed: Boolean,
    val notificationsAllowed: Boolean
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

    /** Required access that determines whether core blocking can operate. */
    val coreIssues: List<String>
        get() = buildList {
            if (!accessibilityEnabled) add("Accessibility interception is disabled.")
            if (!usageAccessGranted) add("Usage Access fallback detection is unavailable.")
        }

    /** Optional Android settings that improve background reliability but do not block setup. */
    val recommendedIssues: List<String>
        get() = buildList {
            if (!batteryOptimizationIgnored) add("Android may restrict background monitoring.")
            if (!exactAlarmsAllowed) add("Schedule boundaries use an inexact fallback.")
            if (!notificationsAllowed) add("Protection service notifications may be hidden.")
        }

    val degradedReasons: List<String>
        get() = coreIssues + recommendedIssues
}

object ProtectionHealth {
    fun snapshot(context: Context): ProtectionHealthSnapshot {
        val app = context.applicationContext
        return ProtectionHealthSnapshot(
            accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(app),
            usageAccessGranted = PermissionHelper.isUsageAccessGranted(app),
            batteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(app),
            exactAlarmsAllowed = PermissionHelper.canScheduleExactAlarms(app),
            notificationsAllowed = PermissionHelper.isNotificationPermissionGranted(app)
        )
    }
}
