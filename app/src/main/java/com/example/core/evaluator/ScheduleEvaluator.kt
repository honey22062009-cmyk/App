package com.example.core.evaluator

import com.example.core.model.BlockEvaluationResult
import com.example.core.model.SchedulePeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Pure Kotlin evaluation engine for scheduled app blocking.
 * Decoupled from Android UI code for clean architecture and unit testing.
 */
object ScheduleEvaluator {

    data class OverlapWarning(
        val periodA: SchedulePeriod,
        val periodB: SchedulePeriod,
        val description: String
    )

    /**
     * Evaluates whether an app is allowed to run at the given [dateTime]
     * based on its configured [periods].
     *
     * Rule: If an app is managed, it is BLOCKED unless current time falls
     * within at least one enabled allowed period.
     */
    fun evaluate(
        periods: List<SchedulePeriod>,
        dateTime: LocalDateTime = LocalDateTime.now()
    ): BlockEvaluationResult {
        val enabledPeriods = periods.filter { it.isEnabled }

        if (enabledPeriods.isEmpty()) {
            return BlockEvaluationResult(
                isBlocked = true,
                activePeriod = null,
                nextAllowedDateTime = null,
                reason = "No allowed schedule periods are configured or enabled."
            )
        }

        val currentDay = dateTime.dayOfWeek
        val currentTime = dateTime.toLocalTime()
        val previousDay = currentDay.minus(1)

        // 1. Check if an active period began earlier today
        val todayMatch = enabledPeriods.firstOrNull { period ->
            if (period.dayOfWeek != currentDay) return@firstOrNull false

            if (!period.crossesMidnight) {
                // Regular period within same day: start <= current < end
                !currentTime.isBefore(period.startTime) && currentTime.isBefore(period.endTime)
            } else {
                // Crosses midnight: active from start until 23:59:59.999 today
                !currentTime.isBefore(period.startTime)
            }
        }

        if (todayMatch != null) {
            return BlockEvaluationResult(
                isBlocked = false,
                activePeriod = todayMatch,
                nextAllowedDateTime = null,
                reason = "Currently within allowed schedule: ${todayMatch.timeSpanDisplay}"
            )
        }

        // 2. Check if a period that started yesterday crossed midnight into today
        val yesterdayOvernightMatch = enabledPeriods.firstOrNull { period ->
            period.dayOfWeek == previousDay &&
                    period.crossesMidnight &&
                    currentTime.isBefore(period.endTime)
        }

        if (yesterdayOvernightMatch != null) {
            return BlockEvaluationResult(
                isBlocked = false,
                activePeriod = yesterdayOvernightMatch,
                nextAllowedDateTime = null,
                reason = "Currently within overnight allowed schedule: ${yesterdayOvernightMatch.timeSpanDisplay}"
            )
        }

        // App is currently BLOCKED. Find the next upcoming allowed window.
        val nextAllowedTime = findNextAllowedTime(enabledPeriods, dateTime)

        return BlockEvaluationResult(
            isBlocked = true,
            activePeriod = null,
            nextAllowedDateTime = nextAllowedTime,
            reason = "Outside allowed scheduled hours."
        )
    }

    /**
     * Determines the exact next upcoming LocalDateTime when an allowed period begins.
     * Looks forward up to 7 days into the future.
     */
    fun findNextAllowedTime(
        periods: List<SchedulePeriod>,
        fromDateTime: LocalDateTime
    ): LocalDateTime? {
        val enabledPeriods = periods.filter { it.isEnabled }
        if (enabledPeriods.isEmpty()) return null

        var candidate: LocalDateTime? = null

        // Check forward across 8 days (day 0 to day 7)
        for (dayOffset in 0L..7L) {
            val targetDate = fromDateTime.toLocalDate().plusDays(dayOffset)
            val targetDayOfWeek = targetDate.dayOfWeek

            val periodsForDay = enabledPeriods.filter { it.dayOfWeek == targetDayOfWeek }
            for (period in periodsForDay) {
                val periodStartDateTime = targetDate.atTime(period.startTime)

                // Must be in the future relative to fromDateTime
                if (periodStartDateTime.isAfter(fromDateTime)) {
                    if (candidate == null || periodStartDateTime.isBefore(candidate)) {
                        candidate = periodStartDateTime
                    }
                }
            }

            // If we found a candidate in an earlier dayOffset, we can stop searching further days
            if (candidate != null && dayOffset > 0) {
                break
            }
        }

        return candidate
    }

    /**
     * Detects overlapping schedule periods for the same day.
     */
    fun detectOverlaps(periods: List<SchedulePeriod>): List<OverlapWarning> {
        val warnings = mutableListOf<OverlapWarning>()
        val groupedByDay = periods.groupBy { it.dayOfWeek }

        for ((day, dayPeriods) in groupedByDay) {
            if (dayPeriods.size < 2) continue

            for (i in 0 until dayPeriods.size) {
                for (j in i + 1 until dayPeriods.size) {
                    val a = dayPeriods[i]
                    val b = dayPeriods[j]

                    if (periodsOverlapOnSameDay(a, b)) {
                        warnings.add(
                            OverlapWarning(
                                periodA = a,
                                periodB = b,
                                description = "Overlapping periods on ${a.dayNameDisplay}: " +
                                        "${a.formattedStartTime}-${a.formattedEndTime} overlaps with " +
                                        "${b.formattedStartTime}-${b.formattedEndTime}"
                            )
                        )
                    }
                }
            }
        }
        return warnings
    }

    /**
     * Helper to test if two periods on the same day overlap in time.
     */
    private fun periodsOverlapOnSameDay(a: SchedulePeriod, b: SchedulePeriod): Boolean {
        // Normalize time intervals to minutes from midnight
        val aStart = a.startHour * 60 + a.startMinute
        val aEnd = if (a.crossesMidnight) 24 * 60 else a.endHour * 60 + a.endMinute

        val bStart = b.startHour * 60 + b.startMinute
        val bEnd = if (b.crossesMidnight) 24 * 60 else b.endHour * 60 + b.endMinute

        return maxOf(aStart, bStart) < minOf(aEnd, bEnd)
    }

    /**
     * Merges overlapping or contiguous periods on the same day into consolidated periods.
     */
    fun mergeOverlapping(periods: List<SchedulePeriod>): List<SchedulePeriod> {
        val result = mutableListOf<SchedulePeriod>()
        val groupedByDay = periods.groupBy { it.dayOfWeek }

        for ((_, dayPeriods) in groupedByDay) {
            if (dayPeriods.isEmpty()) continue

            // Sort by start time
            val sorted = dayPeriods.sortedWith(
                compareBy({ it.startHour * 60 + it.startMinute }, { it.endHour * 60 + it.endMinute })
            )

            var current = sorted.first()
            for (i in 1 until sorted.size) {
                val next = sorted[i]
                if (periodsOverlapOnSameDay(current, next)) {
                    // Merge current and next
                    val earliestStart = if (current.startTime <= next.startTime) current.startTime else next.startTime
                    val latestEnd = when {
                        current.crossesMidnight || next.crossesMidnight -> {
                            if (current.crossesMidnight && next.crossesMidnight) {
                                if (current.endTime >= next.endTime) current.endTime else next.endTime
                            } else if (current.crossesMidnight) current.endTime else next.endTime
                        }
                        current.endTime >= next.endTime -> current.endTime
                        else -> next.endTime
                    }

                    current = current.copy(
                        startHour = earliestStart.hour,
                        startMinute = earliestStart.minute,
                        endHour = latestEnd.hour,
                        endMinute = latestEnd.minute
                    )
                } else {
                    result.add(current)
                    current = next
                }
            }
            result.add(current)
        }

        return result
    }
}
