package com.example.core.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Result of evaluating whether a managed application is currently blocked or allowed.
 */
data class BlockEvaluationResult(
    val isBlocked: Boolean,
    val activePeriod: SchedulePeriod? = null,
    val nextAllowedDateTime: LocalDateTime? = null,
    val reason: String = ""
) {
    val formattedNextAllowedTime: String
        get() = nextAllowedDateTime?.let { dt ->
            val now = LocalDateTime.now()
            if (dt.toLocalDate() == now.toLocalDate()) {
                "Today at " + dt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
            } else if (dt.toLocalDate() == now.toLocalDate().plusDays(1)) {
                "Tomorrow at " + dt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
            } else {
                dt.format(DateTimeFormatter.ofPattern("EEEE at hh:mm a", Locale.getDefault()))
            }
        } ?: "No upcoming scheduled access"
}
