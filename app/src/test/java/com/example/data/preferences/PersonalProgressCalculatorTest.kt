package com.example.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PersonalProgressCalculatorTest {
    @Test
    fun currentDaysNeverGoesNegative() {
        val today = LocalDate.of(2026, 11, 23).toEpochDay()
        assertEquals(0, HabitStreakCalculator.currentDays(today, today + 1))
    }

    @Test
    fun currentDaysCountsElapsedCalendarDays() {
        val start = LocalDate.of(2026, 11, 20).toEpochDay()
        val today = LocalDate.of(2026, 11, 23).toEpochDay()
        assertEquals(3, HabitStreakCalculator.currentDays(today, start))
    }

    @Test
    fun bestAfterResetKeepsLongestPreviousRun() {
        val day = LocalDate.of(2026, 11, 23).toEpochDay()
        assertEquals(12, HabitStreakCalculator.bestAfterReset(12, day, day - 5))
        assertEquals(5, HabitStreakCalculator.bestAfterReset(3, day, day - 5))
    }

    @Test
    fun sameDayCheckInCountsAsFirstDayWhenResetIsLoggedLater() {
        val day = LocalDate.of(2026, 11, 23).toEpochDay()
        assertEquals(1, HabitStreakCalculator.bestAfterReset(0, day, day, checkedInToday = true))
    }

    @Test
    fun checkInDoesNotDoubleCountAnElapsedDay() {
        val day = LocalDate.of(2026, 11, 23).toEpochDay()
        assertEquals(1, HabitStreakCalculator.bestAfterReset(0, day, day - 1, checkedInToday = true))
    }

    @Test
    fun elapsedDaysAdvancesOnlyAfterEachFullTwentyFourHours() {
        val start = 1_000_000L
        assertEquals(0, HabitStreakCalculator.elapsedDays(start + 23L * 60L * 60L * 1000L, start))
        assertEquals(1, HabitStreakCalculator.elapsedDays(start + 24L * 60L * 60L * 1000L, start))
        assertEquals(2, HabitStreakCalculator.elapsedDays(start + 48L * 60L * 60L * 1000L, start))
    }

    @Test
    fun elapsedDaysNeverGoesNegativeWhenDeviceClockMovesBackwards() {
        assertEquals(0, HabitStreakCalculator.elapsedDays(500L, 1_000L))
    }
}
