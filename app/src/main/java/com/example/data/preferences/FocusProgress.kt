package com.example.data.preferences

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Pure progress calculations, separated for unit testing. */
internal object FocusProgress {
    fun parseDailyFocusMinutes(serialized: String): Map<Long, Int> {
        if (serialized.isBlank()) return emptyMap()
        return serialized.split(",").mapNotNull { entry ->
            val parts = entry.split(":", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val day = parts[0].toLongOrNull() ?: return@mapNotNull null
            val minutes = parts[1].toIntOrNull()?.coerceAtLeast(0) ?: return@mapNotNull null
            day to minutes
        }.toMap()
    }

    fun encodeDailyFocusMinutes(dailyTotals: Map<Long, Int>, todayEpochDay: Long): String =
        dailyTotals
            .filterKeys { it >= todayEpochDay - 60L }
            .toSortedMap()
            .entries
            .joinToString(",") { (day, minutes) -> "$day:${minutes.coerceAtLeast(0)}" }

    fun nextStreak(lastCompletionDay: Long, todayEpochDay: Long, currentStreak: Int): Int = when {
        lastCompletionDay == todayEpochDay -> currentStreak.coerceAtLeast(1)
        lastCompletionDay == todayEpochDay - 1L -> currentStreak.coerceAtLeast(0) + 1
        else -> 1
    }

    fun weeklyTotals(dailyTotals: Map<Long, Int>, todayEpochDay: Long): List<Int> {
        val today = LocalDate.ofEpochDay(todayEpochDay)
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        return (0L..6L).map { dayOffset -> (dailyTotals[monday + dayOffset] ?: 0).coerceAtLeast(0) }
    }
}
