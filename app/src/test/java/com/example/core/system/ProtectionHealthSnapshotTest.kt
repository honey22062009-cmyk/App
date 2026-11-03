package com.example.core.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionHealthSnapshotTest {

    @Test
    fun optionalSettingsDoNotMakeCoreProtectionUnready() {
        val health = ProtectionHealthSnapshot(
            accessibilityEnabled = true,
            usageAccessGranted = true,
            batteryOptimizationIgnored = false,
            exactAlarmsAllowed = false,
            notificationsAllowed = false
        )

        assertTrue(health.coreReady)
        assertEquals(3, health.recommendedIssues.size)
        assertTrue(health.coreIssues.isEmpty())
        assertEquals(40, health.reliabilityScore)
    }

    @Test
    fun missingRequiredAccessIsReportedSeparatelyFromRecommendations() {
        val health = ProtectionHealthSnapshot(
            accessibilityEnabled = false,
            usageAccessGranted = true,
            batteryOptimizationIgnored = true,
            exactAlarmsAllowed = true,
            notificationsAllowed = true
        )

        assertFalse(health.coreReady)
        assertEquals(
            listOf("Accessibility interception is disabled."),
            health.coreIssues
        )
        assertTrue(health.recommendedIssues.isEmpty())
    }

    @Test
    fun fullyConfiguredProtectionHasNoOutstandingSetupItems() {
        val health = ProtectionHealthSnapshot(
            accessibilityEnabled = true,
            usageAccessGranted = true,
            batteryOptimizationIgnored = true,
            exactAlarmsAllowed = true,
            notificationsAllowed = true
        )

        assertTrue(health.coreReady)
        assertTrue(health.recommendedIssues.isEmpty())
        assertTrue(health.degradedReasons.isEmpty())
        assertEquals(100, health.reliabilityScore)
    }
}
