package com.example.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FocusProgressTest {
    @Test
    fun sameDayCompletionDoesNotIncrementStreakTwice() {
        assertEquals(4, FocusProgress.nextStreak(100L, 100L, 4))
    }

    @Test
    fun consecutiveDayCompletionIncrementsStreak() {
        assertEquals(5, FocusProgress.nextStreak(100L, 101L, 4))
    }

    @Test
    fun missedDayStartsANewStreak() {
        assertEquals(1, FocusProgress.nextStreak(98L, 100L, 7))
    }

    @Test
    fun weeklyTotalsAreOrderedMondayThroughSunday() {
        val monday = LocalDate.of(2026, 11, 23).toEpochDay()
        val totals = mapOf(monday to 25, monday + 2L to 45, monday + 6L to 15)
        assertEquals(
            listOf(25, 0, 45, 0, 0, 0, 15),
            FocusProgress.weeklyTotals(totals, monday + 3L)
        )
    }

    @Test
    fun dailyTotalsRoundTripAndDropOldEntries() {
        val encoded = FocusProgress.encodeDailyFocusMinutes(
            mapOf(9L to 5, 50L to 35, 70L to 45),
            todayEpochDay = 70L
        )
        assertEquals(mapOf(50L to 35, 70L to 45), FocusProgress.parseDailyFocusMinutes(encoded))
    }
}
