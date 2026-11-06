package com.example.data.preferences

import java.util.Base64
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * A locally stored focus-session record. This is deliberately separate from
 * tracker/reset history so the two timelines cannot accidentally mix.
 */
data class FocusSessionRecord(
    val id: String,
    val startedAtMillis: Long,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val activity: String,
    val completed: Boolean
) {
    fun encode(): String {
        val safeActivity = Base64.getUrlEncoder().withoutPadding().encodeToString(activity.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            id,
            startedAtMillis.toString(),
            plannedSeconds.toString(),
            actualSeconds.toString(),
            if (completed) "1" else "0",
            safeActivity
        ).joinToString("|")
    }

    companion object {
        fun create(
            startedAtMillis: Long,
            plannedSeconds: Int,
            actualSeconds: Int,
            activity: String,
            completed: Boolean
        ): FocusSessionRecord {
            val planned = plannedSeconds.coerceIn(60, 3 * 60 * 60)
            val actual = actualSeconds.coerceIn(0, planned)
            val cleanActivity = activity.trim().replace(Regex("\\s+"), " ").take(32)
                .ifBlank { "Study" }
            return FocusSessionRecord(
                id = UUID.randomUUID().toString(),
                startedAtMillis = startedAtMillis.coerceAtLeast(1L),
                plannedSeconds = planned,
                actualSeconds = actual,
                activity = cleanActivity,
                completed = completed && actual >= planned
            )
        }

        fun decode(raw: String): FocusSessionRecord? = runCatching {
            val parts = raw.split('|', limit = 6)
            require(parts.size == 6)
            val id = parts[0].takeIf { it.isNotBlank() } ?: return null
            val startedAt = parts[1].toLong().takeIf { it > 0L } ?: return null
            val planned = parts[2].toInt().takeIf { it in 60..10_800 } ?: return null
            val actual = parts[3].toInt().takeIf { it in 0..planned } ?: return null
            val completed = when (parts[4]) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            val activity = String(
                Base64.getUrlDecoder().decode(parts[5]),
                StandardCharsets.UTF_8
            ).trim().take(32).ifBlank { "Study" }
            FocusSessionRecord(id, startedAt, planned, actual, activity, completed && actual >= planned)
        }.getOrNull()
    }
}
