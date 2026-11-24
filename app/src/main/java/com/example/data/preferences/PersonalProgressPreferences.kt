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
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val Context.habitDataStore: DataStore<Preferences> by preferencesDataStore(name = "strict_blocker_habits")

data class PersonalProgressSnapshot(
    val profileName: String = "",
    val goalTitle: String = "My personal goal",
    val startDay: Long = LocalDate.now().toEpochDay(),
    val lastResetDay: Long = LocalDate.now().toEpochDay(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val previousBestStreak: Int = 0,
    val resetCount: Int = 0,
    val checkInCount: Int = 0,
    val dailyStatus: Map<Long, Int> = emptyMap(),
    val todayNote: String = ""
)

object HabitStreakCalculator {
    fun currentDays(today: Long, lastResetDay: Long): Int =
        (today - lastResetDay).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    fun bestAfterReset(
        previousBest: Int,
        today: Long,
        lastResetDay: Long,
        checkedInToday: Boolean = false
    ): Int {
        val completedCurrentRun = currentDays(today, lastResetDay) +
            if (checkedInToday && today == lastResetDay) 1 else 0
        return maxOf(previousBest, completedCurrentRun)
    }
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
        val previousBest = (p[Keys.BEST_STREAK] ?: 0).coerceAtLeast(0)
        val elapsedDays = HabitStreakCalculator.currentDays(today, lastReset)
        val current = if (statuses[today] == 1 && today == lastReset) 1 else elapsedDays
        PersonalProgressSnapshot(
            profileName = p[Keys.PROFILE_NAME] ?: "",
            goalTitle = p[Keys.GOAL_TITLE] ?: "My personal goal",
            startDay = start,
            lastResetDay = lastReset,
            currentStreak = current,
            bestStreak = maxOf(previousBest, current),
            previousBestStreak = previousBest,
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
            if (statuses[day] == 0) return@edit
            val alreadyCheckedIn = statuses[day] == 1
            statuses[day] = 1
            p[Keys.DAILY_STATUS] = encodeStatus(statuses, day)
            if (!alreadyCheckedIn) {
                p[Keys.CHECK_IN_COUNT] = (p[Keys.CHECK_IN_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun recordReset(day: Long = LocalDate.now().toEpochDay()) {
        context.habitDataStore.edit { p ->
            val previousReset = p[Keys.LAST_RESET_DAY] ?: (p[Keys.START_DAY] ?: day)
            val statuses = parseStatus(p[Keys.DAILY_STATUS] ?: "").toMutableMap()
            val alreadyCheckedIn = statuses[day] == 1
            val best = HabitStreakCalculator.bestAfterReset(
                p[Keys.BEST_STREAK] ?: 0, day, previousReset, alreadyCheckedIn
            )
            val alreadyResetToday = statuses[day] == 0
            statuses[day] = 0
            p[Keys.START_DAY] = day
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

    /**
     * Migrates date-only tracker history from the legacy JSON backup into the
     * new neutral personal-goal timeline. Free-text legacy notes are not copied.
     */
    suspend fun importLegacyBackup(entries: JSONObject) {
        fun longValue(name: String): Long =
            entries.optJSONObject(name)?.takeIf { it.optString("type") == "long" }?.optLong("value") ?: 0L

        fun intValue(name: String): Int =
            entries.optJSONObject(name)?.takeIf { it.optString("type") == "int" }?.optInt("value") ?: 0

        fun stringSet(name: String): Set<String> {
            val item = entries.optJSONObject(name) ?: return emptySet()
            if (item.optString("type") != "string_set") return emptySet()
            val values = item.optJSONArray("value") ?: return emptySet()
            return buildSet {
                for (index in 0 until values.length()) {
                    val value = values.optString(index).takeIf { it.isNotBlank() } ?: continue
                    add(value)
                }
            }
        }

        fun toEpochDay(timestamp: Long): Long? = if (timestamp > 0L) {
            runCatching {
                Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
            }.getOrNull()
        } else null

        val resetTimestamps = buildSet {
            stringSet("fap_event_timestamps").mapNotNullTo(this) { it.toLongOrNull() }
            stringSet("tracker_reset_records_v2").forEach { raw ->
                raw.split("|", limit = 4).getOrNull(1)?.toLongOrNull()?.let(::add)
            }
            longValue("last_fap_at").takeIf { it > 0L }?.let(::add)
        }.filter { it > 0L }.sorted()

        val today = LocalDate.now().toEpochDay()
        val startDay = toEpochDay(longValue("tracker_started_at"))
            ?: resetTimestamps.firstOrNull()?.let(::toEpochDay)
            ?: today
        val lastResetDay = resetTimestamps.lastOrNull()?.let(::toEpochDay) ?: startDay

        val dailyStatus = mutableMapOf<Long, Int>()
        resetTimestamps.mapNotNull(::toEpochDay).forEach { dailyStatus[it] = 0 }
        stringSet("daily_check_ins").forEach { raw ->
            val date = raw.substringBefore("|").let { runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull() }
            if (date != null) dailyStatus[date] = 1
        }

        val best = maxOf(
            intValue("best_fap_streak_days"),
            intValue("previous_fap_streak_days")
        ).coerceAtLeast(0)
        val resetCount = maxOf(intValue("fap_log_count"), resetTimestamps.size).coerceAtLeast(0)
        val checkIns = dailyStatus.values.count { it == 1 }

        context.habitDataStore.edit { p ->
            p[Keys.START_DAY] = startDay
            p[Keys.LAST_RESET_DAY] = lastResetDay
            p[Keys.BEST_STREAK] = best
            p[Keys.RESET_COUNT] = resetCount
            p[Keys.CHECK_IN_COUNT] = checkIns
            p[Keys.DAILY_STATUS] = encodeStatus(dailyStatus, today)
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
