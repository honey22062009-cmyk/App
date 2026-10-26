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


    @Test
    fun outOfOrderEventsAreSortedBeforeCalculating() {
        val start = ms(2026, 1, 1)
        val events = listOf(
            TrackerResetRecord("later", ms(2026, 1, 8)),
            TrackerResetRecord("earlier", ms(2026, 1, 4))
        )
        val stats = TrackerCalculator.stats(start, events, ms(2026, 1, 10), zone)
        assertEquals(2, stats.currentDays)
        assertEquals(4, stats.previousDays)
        assertEquals(4, stats.bestDays)
        assertEquals(2, stats.totalResets)
    }

    @Test
    fun futureEventsAreIgnored() {
        val start = ms(2026, 1, 1)
        val now = ms(2026, 1, 10)
        val events = listOf(
            TrackerResetRecord("valid", ms(2026, 1, 4)),
            TrackerResetRecord("future", ms(2026, 1, 12))
        )
        val stats = TrackerCalculator.stats(start, events, now, zone)
        assertEquals(6, stats.currentDays)
        assertEquals(3, stats.previousDays)
        assertEquals(1, stats.totalResets)
    }

    @Test
    fun graphSupportsSingleDayWithoutNaNStyleInputs() {
        val date = java.time.LocalDate.of(2026, 1, 1)
        val points = TrackerCalculator.graphPoints(
            trackerStartedAt = ms(2026, 1, 1),
            resets = emptyList(),
            startDate = date,
            endDate = date,
            zone = zone
        )
        assertEquals(1, points.size)
        assertEquals(0, points.single().streakDays)
    }

    @Test
    fun currentStreakCanBecomePersonalBest() {
        val start = ms(2026, 1, 1)
        val events = listOf(TrackerResetRecord("one", ms(2026, 1, 3)))
        val stats = TrackerCalculator.stats(start, events, ms(2026, 1, 20), zone)
        assertEquals(17, stats.currentDays)
        assertEquals(17, stats.bestDays)
        assertEquals(2, stats.previousDays)
    }

    @Test
    fun sameTimestampEventsRemainDistinctWithoutNegativeIntervals() {
        val start = ms(2026, 1, 1, 12)
        val sameTime = ms(2026, 1, 5, 12)
        val stats = TrackerCalculator.stats(
            start,
            listOf(
                TrackerResetRecord("a", sameTime),
                TrackerResetRecord("b", sameTime)
            ),
            ms(2026, 1, 7, 12),
            zone
        )
        assertEquals(2, stats.totalResets)
        assertEquals(2, stats.currentDays)
        assertEquals(4, stats.bestDays)
    }

    @Test
    fun editingLatestResetRecalculatesTheCurrentStreak() {
        val start = ms(2026, 1, 1)
        val editedEvents = listOf(
            TrackerResetRecord("first", ms(2026, 1, 4)),
            TrackerResetRecord("latest", ms(2026, 1, 6))
        )
        val stats = TrackerCalculator.stats(start, editedEvents, ms(2026, 1, 10), zone)
        assertEquals(4, stats.currentDays)
        assertEquals(2, stats.previousDays)
        assertEquals(4, stats.bestDays)
    }

    @Test
    fun deletingLatestResetRecalculatesHistoryFromTheRemainingEvents() {
        val start = ms(2026, 1, 1)
        val remainingEvents = listOf(TrackerResetRecord("first", ms(2026, 1, 4)))
        val stats = TrackerCalculator.stats(start, remainingEvents, ms(2026, 1, 10), zone)
        assertEquals(6, stats.currentDays)
        assertEquals(3, stats.previousDays)
        assertEquals(6, stats.bestDays)
        assertEquals(1, stats.totalResets)
    }

    @Test
    fun monthAndYearBoundariesUseLocalCalendarDates() {
        val start = ms(2025, 12, 29, 12)
        val events = listOf(
            TrackerResetRecord("december", ms(2025, 12, 31, 12)),
            TrackerResetRecord("january", ms(2026, 1, 1, 12))
        )
        val stats = TrackerCalculator.stats(start, events, ms(2026, 1, 2, 12), zone)
        assertEquals(1, stats.currentDays)
        assertEquals(1, stats.previousDays)
        assertEquals(1, stats.resetsThisMonth)
        assertEquals(2, stats.totalResets)
    }

    @Test
    fun elapsedStreakIsStableAcrossTimezoneChangesWhileMonthUsesLocalDate() {
        val start = java.time.Instant.parse("2025-12-31T23:00:00Z").toEpochMilli()
        val resetAt = java.time.Instant.parse("2026-01-01T01:00:00Z").toEpochMilli()
        val now = java.time.Instant.parse("2026-01-01T22:00:00Z").toEpochMilli()
        val events = listOf(TrackerResetRecord("reset", resetAt))

        val utcStats = TrackerCalculator.stats(start, events, now, ZoneId.of("UTC"))
        val pacificStats = TrackerCalculator.stats(start, events, now, ZoneId.of("America/Los_Angeles"))

        assertEquals(utcStats.currentDays, pacificStats.currentDays)
        assertEquals(1, utcStats.resetsThisMonth)
        assertEquals(0, pacificStats.resetsThisMonth)
    }

}
