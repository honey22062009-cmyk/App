package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.focusDataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_blocker_focus")

data class FocusSessionSnapshot(
    val selectedMinutes: Int = 25,
    val endAtMillis: Long = 0L,
    val pausedRemainingMillis: Long = 0L,
    val completedSessions: Int = 0,
    val totalFocusMinutes: Int = 0,
    val currentStreak: Int = 0,
    val lastCompletionDay: Long = -1L
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
        val LAST_COMPLETION_DAY = longPreferencesKey("last_completion_day")
    }

    val snapshotFlow: Flow<FocusSessionSnapshot> = context.focusDataStore.data.map { p ->
        FocusSessionSnapshot(
            selectedMinutes = (p[Keys.SELECTED_MINUTES] ?: 25).coerceIn(5, 120),
            endAtMillis = p[Keys.END_AT] ?: 0L,
            pausedRemainingMillis = p[Keys.PAUSED_REMAINING] ?: 0L,
            completedSessions = p[Keys.COMPLETED_SESSIONS] ?: 0,
            totalFocusMinutes = p[Keys.TOTAL_FOCUS_MINUTES] ?: 0,
            currentStreak = p[Keys.CURRENT_STREAK] ?: 0,
            lastCompletionDay = p[Keys.LAST_COMPLETION_DAY] ?: -1L
        )
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
        val snapshot = snapshotFlow.first()
        val remaining = snapshot.pausedRemainingMillis
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

    /**
     * Records a completed session once, and only once, after its persisted deadline.
     * A process death or app restart does not reset the deadline.
     */
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
                val nextStreak = when {
                    previousDay == today -> oldStreak
                    previousDay == today - 1L -> oldStreak + 1
                    else -> 1
                }
                p[Keys.COMPLETED_SESSIONS] = sessions + 1
                p[Keys.TOTAL_FOCUS_MINUTES] = total + minutes
                p[Keys.CURRENT_STREAK] = nextStreak
                p[Keys.LAST_COMPLETION_DAY] = today
                p[Keys.END_AT] = 0L
                p[Keys.PAUSED_REMAINING] = 0L
                completed = true
            }
        }
        return completed
    }
}
