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
}
