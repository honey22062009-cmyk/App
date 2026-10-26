package com.example.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TrackerCalculatorTest {
    private val zone = ZoneId.of("UTC")

    private fun ms(year: Int, month: Int, day: Int, hour: Int = 0): Long =
        LocalDateTime.of(year, month, day, hour, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun noEventsUsesTrackerStart() {
        val start = ms(2026, 1, 1)
        val now = ms(2026, 1, 8)
        val stats = TrackerCalculator.stats(start, emptyList(), now, zone)
        assertEquals(7, stats.currentDays)
        assertEquals(7, stats.bestDays)
        assertEquals(0, stats.previousDays)
        assertEquals(0, stats.totalResets)
    }

    @Test
    fun oneResetBuildsPreviousAndCurrentStreak() {
        val start = ms(2026, 1, 1)
        val reset = TrackerResetRecord("one", ms(2026, 1, 4))
        val stats = TrackerCalculator.stats(start, listOf(reset), ms(2026, 1, 10), zone)
        assertEquals(6, stats.currentDays)
        assertEquals(3, stats.previousDays)
        assertEquals(6, stats.bestDays)
    }

    @Test
    fun multipleResetsWithinSameDayRemainValid() {
        val start = ms(2026, 1, 1)
        val events = listOf(
            TrackerResetRecord("one", ms(2026, 1, 2, 8)),
            TrackerResetRecord("two", ms(2026, 1, 2, 9))
        )
        val stats = TrackerCalculator.stats(start, events, ms(2026, 1, 3, 9), zone)
        assertEquals(1, stats.currentDays)
        assertEquals(0, stats.previousDays)
        assertEquals(2, stats.totalResets)
    }

    @Test
    fun leapDayAndYearBoundaryDoNotBreakGraph() {
        val start = ms(2024, 2, 27)
        val reset = TrackerResetRecord("leap", ms(2024, 2, 29))
        val points = TrackerCalculator.graphPoints(
            trackerStartedAt = start,
            resets = listOf(reset),
            startDate = java.time.LocalDate.of(2024, 2, 27),
            endDate = java.time.LocalDate.of(2024, 3, 2),
            zone = zone
        )
        assertEquals(5, points.size)
        assertTrue(points.any { it.date.dayOfMonth == 29 && it.reset })
    }

    @Test
    fun goalScaleCanUseLongCurrentStreakWithoutOverflow() {
        val start = ms(2016, 1, 1)
        val now = ms(2026, 1, 1)
        val stats = TrackerCalculator.stats(start, emptyList(), now, zone)
        assertTrue(stats.currentDays >= 3650)
        assertEquals(stats.currentDays, stats.bestDays)
    }
}
