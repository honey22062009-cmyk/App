package com.example.data.preferences

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/**
 * A locally stored focus-session record, independent of tracker/reset history.
 * Category and task title are separate so later history views can filter by either.
 */
data class FocusSessionRecord(
    val id: String,
    val startedAtMillis: Long,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    val activity: String,
    val taskName: String,
    val completed: Boolean
) {
    fun encode(): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val safeActivity = encoder.encodeToString(activity.toByteArray(StandardCharsets.UTF_8))
        val safeTask = encoder.encodeToString(taskName.toByteArray(StandardCharsets.UTF_8))
        return listOf(
            id,
            startedAtMillis.toString(),
            plannedSeconds.toString(),
            actualSeconds.toString(),
            if (completed) "1" else "0",
            safeActivity,
            safeTask
        ).joinToString("|")
    }

    companion object {
        private fun clean(value: String, maxLength: Int, fallback: String = ""): String =
            value.trim().replace(Regex("\\s+"), " ").take(maxLength).ifBlank { fallback }

        fun create(
            startedAtMillis: Long,
            plannedSeconds: Int,
            actualSeconds: Int,
            activity: String,
            taskName: String = "",
            completed: Boolean
        ): FocusSessionRecord {
            val planned = plannedSeconds.coerceIn(60, 3 * 60 * 60)
            val actual = actualSeconds.coerceIn(0, planned)
            return FocusSessionRecord(
                id = UUID.randomUUID().toString(),
                startedAtMillis = startedAtMillis.coerceAtLeast(1L),
                plannedSeconds = planned,
                actualSeconds = actual,
                activity = clean(activity, 24, "Study"),
                taskName = clean(taskName, 60),
                completed = completed && actual >= planned
            )
        }

        fun decode(raw: String): FocusSessionRecord? = runCatching {
            val parts = raw.split('|', limit = 7)
            require(parts.size == 7)
            val id = parts[0].takeIf { it.isNotBlank() } ?: return null
            val startedAt = parts[1].toLong().takeIf { it > 0L } ?: return null
            val planned = parts[2].toInt().takeIf { it in 60..10_800 } ?: return null
            val actual = parts[3].toInt().takeIf { it in 0..planned } ?: return null
            val completed = when (parts[4]) {
                "1" -> true
                "0" -> false
                else -> return null
            }
            val decoder = Base64.getUrlDecoder()
            val activity = clean(
                String(decoder.decode(parts[5]), StandardCharsets.UTF_8), 24, "Study"
            )
            val taskName = clean(
                String(decoder.decode(parts[6]), StandardCharsets.UTF_8), 60
            )
            FocusSessionRecord(id, startedAt, planned, actual, activity, taskName, completed && actual >= planned)
        }.getOrNull()
    }
}
