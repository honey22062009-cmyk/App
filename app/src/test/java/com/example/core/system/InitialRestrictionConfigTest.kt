package com.example.core.system

import com.example.core.evaluator.ScheduleEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class InitialRestrictionConfigTest {
    private val periods = InitialRestrictionConfig.defaultAllowedPeriods("test.target")

    private fun isBlocked(dateTime: LocalDateTime): Boolean =
        ScheduleEvaluator.evaluate(periods, dateTime).isBlocked

    @Test
    fun mondayToSaturdayRestrictionWindowsAreBlocked() {
        val monday = LocalDate.of(2026, 11, 16)
        assertTrue(isBlocked(monday.atTime(9, 0)))
        assertTrue(isBlocked(monday.atTime(14, 59)))
        assertFalse(isBlocked(monday.atTime(15, 0)))
        assertFalse(isBlocked(monday.atTime(21, 59)))
        assertTrue(isBlocked(monday.atTime(22, 0)))
        assertTrue(isBlocked(monday.atTime(23, 30)))

        val tuesday = monday.plusDays(1)
        assertTrue(isBlocked(tuesday.atTime(0, 30)))
        assertFalse(isBlocked(tuesday.atTime(1, 0)))
        assertTrue(isBlocked(tuesday.atTime(9, 0)))
        assertFalse(isBlocked(tuesday.atTime(15, 0)))
    }

    @Test
    fun sundayUsesTheShorterRestrictionWindowsAndCorrectSaturdayCarryOver() {
        val sunday = LocalDate.of(2026, 11, 22)
        assertTrue(isBlocked(sunday.atTime(0, 30))) // Saturday 22:00-01:00
        assertFalse(isBlocked(sunday.atTime(1, 0)))
        assertTrue(isBlocked(sunday.atTime(11, 0)))
        assertTrue(isBlocked(sunday.atTime(12, 59)))
        assertFalse(isBlocked(sunday.atTime(13, 0)))
        assertTrue(isBlocked(sunday.atTime(22, 0)))
        assertFalse(isBlocked(sunday.atTime(23, 0)))
    }

    @Test
    fun sundayLateAccessCarriesIntoMondayWithoutAnAccidentalLock() {
        val sunday = LocalDate.of(2026, 11, 22)
        assertFalse(isBlocked(sunday.atTime(23, 30)))
        assertFalse(isBlocked(sunday.plusDays(1).atTime(0, 30)))
        assertTrue(isBlocked(sunday.plusDays(1).atTime(9, 0)))
    }

    @Test
    fun FiveInstalledAppTargetsAndSeparateWebsiteRuleAreConfigured() {
        assertEquals(5, InitialRestrictionConfig.targetApps.size)
        assertEquals(5, InitialRestrictionConfig.targetApps.map { it.packageName }.toSet().size)
        assertTrue(InitialRestrictionConfig.targetApps.any { it.packageName == "com.android.vending" })
        assertTrue(InitialRestrictionConfig.targetDomains.contains("anifam.net"))
        assertFalse(InitialRestrictionConfig.targetApps.any { it.name == "AniFam" })
    }

    @Test
    fun weekdayWindowDefinitionsRemainExact() {
        assertEquals(
            listOf(
                InitialRestrictionConfig.TimeWindow(0, 0, 9, 0),
                InitialRestrictionConfig.TimeWindow(15, 0, 22, 0)
            ),
            InitialRestrictionConfig.allowedWindowsFor(DayOfWeek.MONDAY)
        )
        assertEquals(
            listOf(
                InitialRestrictionConfig.TimeWindow(1, 0, 11, 0),
                InitialRestrictionConfig.TimeWindow(13, 0, 22, 0),
                InitialRestrictionConfig.TimeWindow(23, 0, 0, 0)
            ),
            InitialRestrictionConfig.allowedWindowsFor(DayOfWeek.SUNDAY)
        )
    }
}
