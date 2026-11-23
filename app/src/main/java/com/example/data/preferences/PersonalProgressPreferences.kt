package com.example.data.preferences

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.habitDataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_blocker_habits")

data class PersonalProgressSnapshot(
    val profileName: String = "",
    val goalTitle: String = "My personal goal",
    val startDay: Long = LocalDate.now().toEpochDay(),
    val lastResetDay: Long = LocalDate.now().toEpochDay(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val resetCount: Int = 0,
    val checkInCount: Int = 0,
    val dailyStatus: Map<Long, Int> = emptyMap(),
    val todayNote: String = ""
)

object HabitStreakCalculator {
    fun currentDays(today: Long, lastResetDay: Long): Int =
        (today - lastResetDay).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun bestAfterReset(previousBest: Int, today: Long, lastResetDay: Long): Int =
        maxOf(previousBest, currentDays(today, lastResetDay))
}

class PersonalProgressPreferences(private val context: Context) {
    private object Keys {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val GOAL_TITLE = stringPreferencesKey("goal_title")
        val START_DAY = longPreferencesKey("start_day")
        val LAST_RESET_DAY = longPreferencesKey("last_reset_day")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val RESET_COUNT = intPreferencesKey("reset_count")
        val CHECK_IN_COUNT = intPreferencesKey("check_in_count")
        val DAILY_STATUS = stringPreferencesKey("daily_status")
        val DAILY_NOTES = stringPreferencesKey("daily_notes")
    }

    val snapshotFlow: Flow<PersonalProgressSnapshot> = context.habitDataStore.data.map { p ->
        val today = LocalDate.now().toEpochDay()
        val start = p[Keys.START_DAY] ?: today
        val lastReset = p[Keys.LAST_RESET_DAY] ?: start
        val statuses = parseStatus(p[Keys.DAILY_STATUS] ?: "")
        val notes = parseNotes(p[Keys.DAILY_NOTES] ?: "")
        PersonalProgressSnapshot(
            profileName = p[Keys.PROFILE_NAME] ?: "",
            goalTitle = p[Keys.GOAL_TITLE] ?: "My personal goal",
            startDay = start,
            lastResetDay = lastReset,
            currentStreak = HabitStreakCalculator.currentDays(today, lastReset),
            bestStreak = maxOf(p[Keys.BEST_STREAK] ?: 0, HabitStreakCalculator.currentDays(today, lastReset)),
            resetCount = p[Keys.RESET_COUNT] ?: 0,
            checkInCount = p[Keys.CHECK_IN_COUNT] ?: statuses.count { it.value == 1 },
            dailyStatus = statuses,
            todayNote = notes[today] ?: ""
        )
    }

    suspend fun setProfileName(name: String) {
        context.habitDataStore.edit { it[Keys.PROFILE_NAME] = name.trim().take(40) }
    }

    suspend fun setGoalTitle(title: String) {
        val safe = title.trim().take(48).ifBlank { "My personal goal" }
        context.habitDataStore.edit { it[Keys.GOAL_TITLE] = safe }
    }

    suspend fun markGoalKept(day: Long = LocalDate.now().toEpochDay()) {
        context.habitDataStore.edit { p ->
            val statuses = parseStatus(p[Keys.DAILY_STATUS] ?: "").toMutableMap()
            statuses[day] = 1
            p[Keys.DAILY_STATUS] = encodeStatus(statuses, day)
            p[Keys.CHECK_IN_COUNT] = (p[Keys.CHECK_IN_COUNT] ?: 0) + 1
        }
    }

    suspend fun recordReset(day: Long = LocalDate.now().toEpochDay()) {
        context.habitDataStore.edit { p ->
            val start = p[Keys.START_DAY] ?: day
            val previousReset = p[Keys.LAST_RESET_DAY] ?: start
            val best = HabitStreakCalculator.bestAfterReset(p[Keys.BEST_STREAK] ?: 0, day, previousReset)
            val statuses = parseStatus(p[Keys.DAILY_STATUS] ?: "").toMutableMap()
            val alreadyResetToday = statuses[day] == 0
            statuses[day] = 0
            p[Keys.START_DAY] = start
            p[Keys.LAST_RESET_DAY] = day
            p[Keys.BEST_STREAK] = best
            if (!alreadyResetToday) p[Keys.RESET_COUNT] = (p[Keys.RESET_COUNT] ?: 0) + 1
            p[Keys.DAILY_STATUS] = encodeStatus(statuses, day)
        }
    }

    suspend fun saveTodayNote(note: String, day: Long = LocalDate.now().toEpochDay()) {
        context.habitDataStore.edit { p ->
            val notes = parseNotes(p[Keys.DAILY_NOTES] ?: "").toMutableMap()
            val clean = note.trim().take(500)
            if (clean.isBlank()) notes.remove(day) else notes[day] = clean
            p[Keys.DAILY_NOTES] = encodeNotes(notes, day)
        }
    }

    private fun parseStatus(value: String): Map<Long, Int> {
        if (value.isBlank()) return emptyMap()
        return value.split(";").mapNotNull { entry ->
            val pair = entry.split(":", limit = 2)
            val day = pair.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val status = pair.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 1) ?: return@mapNotNull null
            day to status
        }.toMap()
    }

    private fun encodeStatus(values: Map<Long, Int>, today: Long): String =
        values.filterKeys { it >= today - 365L }.toSortedMap()
            .entries.joinToString(";") { "${it.key}:${it.value}" }

    private fun parseNotes(value: String): Map<Long, String> {
        if (value.isBlank()) return emptyMap()
        return value.split(";").mapNotNull { entry ->
            val pair = entry.split(":", limit = 2)
            val day = pair.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val note = try {
                String(Base64.decode(pair.getOrNull(1).orEmpty(), Base64.URL_SAFE or Base64.NO_WRAP))
            } catch (_: Exception) { return@mapNotNull null }
            day to note
        }.toMap()
    }

    private fun encodeNotes(values: Map<Long, String>, today: Long): String =
        values.filterKeys { it >= today - 365L }.toSortedMap().entries.joinToString(";") {
            val encoded = Base64.encodeToString(it.value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
            "${it.key}:$encoded"
        }
}
