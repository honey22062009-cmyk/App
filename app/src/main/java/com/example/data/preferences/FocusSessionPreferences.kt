package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.util.Base64

private val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_blocker_focus")

data class FocusSessionSnapshot(
    val selectedMinutes: Int = 25,
    val endAtMillis: Long = 0L,
    val pausedRemainingMillis: Long = 0L,
    val completedSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletionDay: Long = -1L,
    val dailyFocusMinutes: List<Int> = List(7) { 0 },
    val weeklyFocusMinutes: Int = 0,
    val selectedTask: String = "",
    val activityCategory: String = "Study"
) {
    val isRunning: Boolean get() = endAtMillis > 0L
    val isPaused: Boolean get() = !isRunning && pausedRemainingMillis > 0L
}

class FocusSessionPreferences(private val context: Context) {
    private object Keys {
        val SELECTED_MINUTES = intPreferencesKey("selected_minutes")
        val END_AT = longPreferencesKey("end_at_millis")
        val PAUSED_REMAINING = longPreferencesKey("paused_remaining_millis")
        val COMPLETED_SESSIONS = intPreferencesKey("completed_sessions")
        val TOTAL_FOCUS_MINUTES = intPreferencesKey("total_focus_minutes")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val LAST_COMPLETION_DAY = longPreferencesKey("last_completion_day")
        val DAILY_FOCUS_MINUTES = stringPreferencesKey("daily_focus_minutes")
        val SELECTED_TASK = stringPreferencesKey("selected_task")
        val ACTIVITY_CATEGORY = stringPreferencesKey("activity_category")
    }

    val snapshotFlow: Flow<FocusSessionSnapshot> = context.focusDataStore.data.map { p ->
        val dailyTotals = FocusProgress.parseDailyFocusMinutes(p[Keys.DAILY_FOCUS_MINUTES] ?: "")
        val weeklyTotals = FocusProgress.weeklyTotals(dailyTotals, LocalDate.now().toEpochDay())
        FocusSessionSnapshot(
            selectedMinutes = (p[Keys.SELECTED_MINUTES] ?: 25).coerceIn(5, 120),
            endAtMillis = p[Keys.END_AT] ?: 0L,
            pausedRemainingMillis = p[Keys.PAUSED_REMAINING] ?: 0L,
            completedSessions = p[Keys.COMPLETED_SESSIONS] ?: 0,
            totalFocusMinutes = p[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
            currentStreak = p[Keys.CURRENT_STREAK] ?: 0,
            bestStreak = p[Keys.BEST_STREAK] ?: 0,
            lastCompletionDay = p[Keys.LAST_COMPLETION_DAY] ?: -1L,
            dailyFocusMinutes = weeklyTotals,
            weeklyFocusMinutes = weeklyTotals.sum(),
            selectedTask = p[Keys.SELECTED_TASK] ?: "",
            activityCategory = p[Keys.ACTIVITY_CATEGORY] ?: "Study"
        )
    }

    /** Imports the legacy app's encoded focus history into the current local summary. */
    suspend fun importLegacyRecords(records: Set<String>) {
        if (records.isEmpty()) return
        data class LegacySession(
            val startedAt: Long,
            val plannedSeconds: Int,
            val actualSeconds: Int,
            val completed: Boolean,
            val activity: String,
            val task: String
        )

        val decoder = Base64.getUrlDecoder()
        val parsed = records.mapNotNull { raw ->
            runCatching {
                val parts = raw.split("|", limit = 7)
                require(parts.size == 7)
                val startedAt = parts[1].toLong().takeIf { it > 0L } ?: return@runCatching null
                val planned = parts[2].toInt().coerceIn(0, 10_800)
                val actual = parts[3].toInt().coerceIn(0, planned)
                val completed = parts[4] == "1" && actual >= planned
                val activity = runCatching {
                    String(decoder.decode(parts[5]), StandardCharsets.UTF_8)
                }.getOrDefault("Study")
                val task = runCatching {
                    String(decoder.decode(parts[6]), StandardCharsets.UTF_8)
                }.getOrDefault("")
                LegacySession(startedAt, planned, actual, completed, activity, task)
            }.getOrNull()
        }.sortedBy { it.startedAt }

        if (parsed.isEmpty()) return
        val zone = ZoneId.systemDefault()
        val dailyTotals = mutableMapOf<Long, Int>()
        parsed.forEach { session ->
            val day = Instant.ofEpochMilli(session.startedAt).atZone(zone).toLocalDate().toEpochDay()
            val minutes = ((session.actualSeconds + 30) / 60).coerceAtLeast(0)
            dailyTotals[day] = (dailyTotals[day] ?: 0) + minutes
        }
        val completedDays = parsed.filter { it.completed }
            .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate().toEpochDay() }
            .distinct()
            .sorted()
        var streak = 0
        var bestStreak = 0
        var previousDay = -1L
        completedDays.forEach { day ->
            streak = FocusProgress.nextStreak(previousDay, day, streak)
            bestStreak = maxOf(bestStreak, streak)
            previousDay = day
        }
        val today = LocalDate.now().toEpochDay()
        val currentStreak = if (previousDay >= today - 1L) streak else 0
        val last = parsed.last()
        val category = last.activity.takeIf { it in setOf("Study", "Revision", "Practice", "Reading", "Planning") } ?: "Study"
        val selectedMinutes = ((last.plannedSeconds + 59) / 60).coerceIn(5, 120)
        val totalMinutes = dailyTotals.values.sum()

        context.focusDataStore.edit { p ->
            p[Keys.SELECTED_MINUTES] = selectedMinutes
            p[Keys.COMPLETED_SESSIONS] = completedDays.size
            p[Keys.TOTAL_FOCUS_MINUTES] = totalMinutes
            p[Keys.CURRENT_STREAK] = currentStreak
            p[Keys.BEST_STREAK] = bestStreak
            p[Keys.LAST_COMPLETION_DAY] = previousDay
            p[Keys.DAILY_FOCUS_MINUTES] = FocusProgress.encodeDailyFocusMinutes(dailyTotals, today)
            p[Keys.SELECTED_TASK] = last.task.take(100)
            p[Keys.ACTIVITY_CATEGORY] = category
            p[Keys.END_AT] = 0L
            p[Keys.PAUSED_REMAINING] = 0L
        }
    }

    suspend fun setSelectedTask(task: String) {
        context.focusDataStore.edit { p -> p[Keys.SELECTED_TASK] = task.trim().take(100) }
    }

    suspend fun setActivityCategory(category: String) {
        val safe = category.takeIf { it in setOf("Study", "Revision", "Practice", "Reading", "Planning") } ?: "Study"
        context.focusDataStore.edit { p -> p[Keys.ACTIVITY_CATEGORY] = safe }
    }

    suspend fun setSelectedMinutes(minutes: Int) {
        context.focusDataStore.edit { p ->
            if ((p[Keys.END_AT] ?: 0L) == 0L && (p[Keys.PAUSED_REMAINING] ?: 0L) == 0L) {
                p[Keys.SELECTED_MINUTES] = minutes.coerceIn(5, 120)
            }
        }
    }

    suspend fun startSession(minutes: Int) {
        val duration = minutes.coerceIn(5, 120)
        context.focusDataStore.edit { p ->
            p[Keys.SELECTED_MINUTES] = duration
            p[Keys.END_AT] = System.currentTimeMillis() + duration * 60_000L
            p[Keys.PAUSED_REMAINING] = 0L
        }
    }

    suspend fun pauseSession(remainingMillis: Long) {
        context.focusDataStore.edit { p ->
            p[Keys.END_AT] = 0L
            p[Keys.PAUSED_REMAINING] = remainingMillis.coerceAtLeast(0L)
        }
    }

    suspend fun resumeSession() {
        val remaining = snapshotFlow.first().pausedRemainingMillis
        if (remaining <= 0L) return
        context.focusDataStore.edit { p ->
            p[Keys.END_AT] = System.currentTimeMillis() + remaining
            p[Keys.PAUSED_REMAINING] = 0L
        }
    }

    suspend fun stopSession() {
        context.focusDataStore.edit { p ->
            p[Keys.END_AT] = 0L
            p[Keys.PAUSED_REMAINING] = 0L
        }
    }

    /** Records one completed session after its persisted deadline, exactly once. */
    suspend fun completeIfElapsed(): Boolean {
        var completed = false
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toEpochDay()
        context.focusDataStore.edit { p ->
            val endAt = p[Keys.END_AT] ?: 0L
            if (endAt > 0L && now >= endAt) {
                val minutes = (p[Keys.SELECTED_MINUTES] ?: 25).coerceIn(5, 120)
                val sessions = p[Keys.COMPLETED_SESSIONS] ?: 0
                val total = p[Keys.TOTAL_FOCUS_MINUTES] ?: 0
                val previousDay = p[Keys.LAST_COMPLETION_DAY] ?: -1L
                val oldStreak = p[Keys.CURRENT_STREAK] ?: 0
                val nextStreak = FocusProgress.nextStreak(previousDay, today, oldStreak)
                val bestStreak = maxOf(p[Keys.BEST_STREAK] ?: 0, nextStreak)
                val dailyTotals = FocusProgress
                    .parseDailyFocusMinutes(p[Keys.DAILY_FOCUS_MINUTES] ?: "")
                    .toMutableMap()
                dailyTotals[today] = (dailyTotals[today] ?: 0) + minutes

                p[Keys.COMPLETED_SESSIONS] = sessions + 1
                p[Keys.TOTAL_FOCUS_MINUTES] = total + minutes
                p[Keys.CURRENT_STREAK] = nextStreak
                p[Keys.BEST_STREAK] = bestStreak
                p[Keys.LAST_COMPLETION_DAY] = today
                p[Keys.DAILY_FOCUS_MINUTES] = FocusProgress.encodeDailyFocusMinutes(dailyTotals, today)
                p[Keys.END_AT] = 0L
                p[Keys.PAUSED_REMAINING] = 0L
                completed = true
            }
        }
        return completed
    }
}
