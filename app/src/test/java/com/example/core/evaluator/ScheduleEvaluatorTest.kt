package com.example.core.evaluator

import com.example.core.model.SchedulePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleEvaluatorTest {

    private val samplePkg = "com.google.android.youtube"

    @Test
    fun testNormalAllowedPeriod_insideWindow_isAllowed() {
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 18,
                startMinute = 0,
                endHour = 19,
                endMinute = 0,
                isEnabled = true
            )
        )

        // Monday 18:30 (inside window)
        val now = LocalDate.of(2026, 10, 19).atTime(18, 30) // 2026-10-19 is a Monday
        assertEquals(DayOfWeek.MONDAY, now.dayOfWeek)

        val result = ScheduleEvaluator.evaluate(periods, now)
        assertFalse(result.isBlocked)
        assertNotNull(result.activePeriod)
    }

    @Test
    fun testNormalAllowedPeriod_outsideWindow_isBlocked() {
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 18,
                startMinute = 0,
                endHour = 19,
                endMinute = 0,
                isEnabled = true
            )
        )

        // Monday 19:05 (outside window)
        val now = LocalDate.of(2026, 10, 19).atTime(19, 5)
        val result = ScheduleEvaluator.evaluate(periods, now)
        assertTrue(result.isBlocked)
    }

    @Test
    fun testMidnightCrossingPeriod_fridayNight_isAllowed() {
        // Friday 22:00 to 01:00 (Saturday morning)
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.FRIDAY,
                startHour = 22,
                startMinute = 0,
                endHour = 1,
                endMinute = 0,
                isEnabled = true
            )
        )

        assertTrue(periods[0].crossesMidnight)

        // Friday 23:30 (inside window)
        val fridayNight = LocalDate.of(2026, 10, 23).atTime(23, 30) // 2026-10-23 is Friday
        assertEquals(DayOfWeek.FRIDAY, fridayNight.dayOfWeek)

        val result = ScheduleEvaluator.evaluate(periods, fridayNight)
        assertFalse(result.isBlocked)
    }

    @Test
    fun testMidnightCrossingPeriod_saturdayEarlyMorning_isAllowed() {
        // Friday 22:00 to 01:00 (Saturday morning)
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.FRIDAY,
                startHour = 22,
                startMinute = 0,
                endHour = 1,
                endMinute = 0,
                isEnabled = true
            )
        )

        // Saturday 00:30 (inside overnight window that started Friday)
        val saturdayMorning = LocalDate.of(2026, 10, 24).atTime(0, 30) // Saturday
        assertEquals(DayOfWeek.SATURDAY, saturdayMorning.dayOfWeek)

        val result = ScheduleEvaluator.evaluate(periods, saturdayMorning)
        assertFalse(result.isBlocked)
        assertNotNull(result.activePeriod)
    }

    @Test
    fun testMidnightCrossingPeriod_saturdayAfterEnd_isBlocked() {
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.FRIDAY,
                startHour = 22,
                startMinute = 0,
                endHour = 1,
                endMinute = 0,
                isEnabled = true
            )
        )

        // Saturday 01:15 (past 01:00 cutoff)
        val saturdayLater = LocalDate.of(2026, 10, 24).atTime(1, 15)
        val result = ScheduleEvaluator.evaluate(periods, saturdayLater)
        assertTrue(result.isBlocked)
    }

    @Test
    fun testMultiplePeriodsInSameDay() {
        // Monday: 07:00-08:00 and 18:00-19:30
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 7,
                startMinute = 0,
                endHour = 8,
                endMinute = 0
            ),
            SchedulePeriod(
                id = 2,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 18,
                startMinute = 0,
                endHour = 19,
                endMinute = 30
            )
        )

        val mondayMorning = LocalDate.of(2026, 10, 19).atTime(7, 30)
        assertFalse(ScheduleEvaluator.evaluate(periods, mondayMorning).isBlocked)

        val mondayMidday = LocalDate.of(2026, 10, 19).atTime(12, 0)
        assertTrue(ScheduleEvaluator.evaluate(periods, mondayMidday).isBlocked)

        val mondayEvening = LocalDate.of(2026, 10, 19).atTime(18, 45)
        assertFalse(ScheduleEvaluator.evaluate(periods, mondayEvening).isBlocked)
    }

    @Test
    fun testFindNextAllowedTime() {
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 18,
                startMinute = 0,
                endHour = 19,
                endMinute = 0
            ),
            SchedulePeriod(
                id = 2,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.TUESDAY,
                startHour = 14,
                startMinute = 0,
                endHour = 15,
                endMinute = 0
            )
        )

        // Current time: Monday 10:00 AM -> next allowed is today at 18:00
        val now = LocalDate.of(2026, 10, 19).atTime(10, 0)
        val nextTime = ScheduleEvaluator.findNextAllowedTime(periods, now)
        assertNotNull(nextTime)
        assertEquals(18, nextTime?.hour)
        assertEquals(0, nextTime?.minute)
        assertEquals(LocalDate.of(2026, 10, 19), nextTime?.toLocalDate())
    }

    @Test
    fun testDetectOverlapsAndMerge() {
        val periods = listOf(
            SchedulePeriod(
                id = 1,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startHour = 14,
                startMinute = 0,
                endHour = 16,
                endMinute = 0
            ),
            SchedulePeriod(
                id = 2,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.WEDNESDAY,
                startHour = 15,
                startMinute = 30,
                endHour = 17,
                endMinute = 0
            )
        )

        val warnings = ScheduleEvaluator.detectOverlaps(periods)
        assertEquals(1, warnings.size)

        val merged = ScheduleEvaluator.mergeOverlapping(periods)
        assertEquals(1, merged.size)
        assertEquals(14, merged[0].startHour)
        assertEquals(17, merged[0].endHour)
    }

    @Test
    fun mergeOverlapping_overnightMergeKeepsTheLatestEndTime() {
        val periods = listOf(
            SchedulePeriod(
                id = 10,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.FRIDAY,
                startHour = 22,
                startMinute = 0,
                endHour = 1,
                endMinute = 0
            ),
            SchedulePeriod(
                id = 11,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.FRIDAY,
                startHour = 23,
                startMinute = 0,
                endHour = 2,
                endMinute = 0
            )
        )

        val merged = ScheduleEvaluator.mergeOverlapping(periods)
        assertEquals(1, merged.size)
        assertEquals(22, merged.single().startHour)
        assertEquals(2, merged.single().endHour)
        assertEquals(0, merged.single().endMinute)
        assertTrue(merged.single().crossesMidnight)
    }

    @Test
    fun mergeOverlapping_doesNotMergeEnabledAndDisabledRules() {
        val periods = listOf(
            SchedulePeriod(
                id = 20,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 18,
                startMinute = 0,
                endHour = 20,
                endMinute = 0,
                isEnabled = true
            ),
            SchedulePeriod(
                id = 21,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 19,
                startMinute = 0,
                endHour = 21,
                endMinute = 0,
                isEnabled = false
            )
        )

        val merged = ScheduleEvaluator.mergeOverlapping(periods)
        assertEquals(2, merged.size)
        assertEquals(setOf(true, false), merged.map { it.isEnabled }.toSet())
    }

    @Test
    fun findNextTransition_insideWindowReturnsItsClosingBoundary() {
        val period = SchedulePeriod(
            id = 30,
            appPackage = samplePkg,
            dayOfWeek = DayOfWeek.MONDAY,
            startHour = 18,
            startMinute = 0,
            endHour = 19,
            endMinute = 0
        )
        val now = LocalDate.of(2026, 10, 19).atTime(18, 30)

        val transition = ScheduleEvaluator.findNextTransition(listOf(period), now)

        assertNotNull(transition)
        assertEquals(LocalDate.of(2026, 10, 19).atTime(19, 0), transition?.dateTime)
        assertEquals(false, transition?.isOpening)
    }

    @Test
    fun findNextTransition_beforeWindowReturnsItsOpeningBoundary() {
        val period = SchedulePeriod(
            id = 31,
            appPackage = samplePkg,
            dayOfWeek = DayOfWeek.MONDAY,
            startHour = 18,
            startMinute = 0,
            endHour = 19,
            endMinute = 0
        )
        val now = LocalDate.of(2026, 10, 19).atTime(17, 0)

        val transition = ScheduleEvaluator.findNextTransition(listOf(period), now)

        assertNotNull(transition)
        assertEquals(LocalDate.of(2026, 10, 19).atTime(18, 0), transition?.dateTime)
        assertEquals(true, transition?.isOpening)
    }
    @Test
    fun findNextTransition_overnightWindowReturnsTodaysClosingBoundary() {
        val period = SchedulePeriod(
            id = 32,
            appPackage = samplePkg,
            dayOfWeek = DayOfWeek.MONDAY,
            startHour = 22,
            startMinute = 0,
            endHour = 1,
            endMinute = 0
        )
        val now = LocalDate.of(2026, 10, 20).atTime(0, 30)

        val transition = ScheduleEvaluator.findNextTransition(listOf(period), now)

        assertNotNull(transition)
        assertEquals(LocalDate.of(2026, 10, 20).atTime(1, 0), transition?.dateTime)
        assertEquals(false, transition?.isOpening)
    }
    @Test
    fun mergeOverlapping_doesNotTurnFullDayCoverageIntoEmptyMidnightWindow() {
        val periods = listOf(
            SchedulePeriod(
                id = 40,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 0,
                startMinute = 0,
                endHour = 12,
                endMinute = 0
            ),
            SchedulePeriod(
                id = 41,
                appPackage = samplePkg,
                dayOfWeek = DayOfWeek.MONDAY,
                startHour = 12,
                startMinute = 0,
                endHour = 0,
                endMinute = 0
            )
        )

        val merged = ScheduleEvaluator.mergeOverlapping(periods)

        // A 00:00-00:00 period is interpreted as empty, not as 24 hours.
        assertEquals(2, merged.size)
        assertTrue(merged.all { it.startHour != 0 || it.startMinute != 0 || it.endHour != 0 || it.endMinute != 0 })
        assertFalse(ScheduleEvaluator.evaluate(merged, LocalDate.of(2026, 10, 19).atTime(16, 0)).isBlocked)
    }
}
