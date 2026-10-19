package com.example.core.model

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Represents a single allowed time period for a managed application.
 * All times are stored as hour/minute (0-23, 0-59) in local device time.
 * Supports periods crossing midnight (e.g. 22:00 to 01:00).
 */
data class SchedulePeriod(
    val id: Long = 0,
    val appPackage: String,
    val dayOfWeek: DayOfWeek,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val isEnabled: Boolean = true
) {
    init {
        require(startHour in 0..23) { "startHour must be 0..23, was $startHour" }
        require(startMinute in 0..59) { "startMinute must be 0..59, was $startMinute" }
        require(endHour in 0..23) { "endHour must be 0..23, was $endHour" }
        require(endMinute in 0..59) { "endMinute must be 0..59, was $endMinute" }
    }

    val startTime: LocalTime get() = LocalTime.of(startHour, startMinute)
    val endTime: LocalTime get() = LocalTime.of(endHour, endMinute)

    /**
     * Returns true if this period crosses midnight (e.g. 22:00 -> 01:00).
     */
    val crossesMidnight: Boolean
        get() = startTime > endTime || (startTime == endTime && (startHour != 0 || startMinute != 0))

    val formattedStartTime: String
        get() = startTime.format(TIME_FORMATTER)

    val formattedEndTime: String
        get() = endTime.format(TIME_FORMATTER)

    val timeSpanDisplay: String
        get() = if (crossesMidnight) {
            "$formattedStartTime – $formattedEndTime (+1 day)"
        } else {
            "$formattedStartTime – $formattedEndTime"
        }

    val dayNameDisplay: String
        get() = dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    }
}
