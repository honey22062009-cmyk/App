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

data class StreakRun(
    val startAtMillis: Long,
    val endAtMillis: Long? = null
)

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
    val todayNote: String = "",
    val startAtMillis: Long = 0L,
    val lastResetAtMillis: Long = 0L,
    val streakRuns: List<StreakRun> = emptyList()
)

object HabitStreakCalculator {
    const val DAY_MILLIS: Long = 86_400_000L

    /** Completed full 24-hour periods since the saved start timestamp. */
    fun elapsedDays(nowMillis: Long, startAtMillis: Long): Int {
        if (startAtMillis <= 0L || nowMillis <= startAtMillis) return 0
        return ((nowMillis - startAtMillis) / DAY_MILLIS)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    // Kept for compatibility with existing data/tests that only contain calendar days.
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
        val START_AT_MILLIS = longPreferencesKey("start_at_millis")
        val LAST_RESET_AT_MILLIS = longPreferencesKey("last_reset_at_millis")
        val STREAK_RUNS = stringPreferencesKey("streak_runs")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val RESET_COUNT = intPreferencesKey("reset_count")
        val CHECK_IN_COUNT = intPreferencesKey("check_in_count")
        val DAILY_STATUS = stringPreferencesKey("daily_status")
        val DAILY_NOTES = stringPreferencesKey("daily_notes")
    }

    val snapshotFlow: Flow<PersonalProgressSnapshot> = context.habitDataStore.data.map { p ->
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toEpochDay()
        val startDay = p[Keys.START_DAY] ?: today
        val lastResetDay = p[Keys.LAST_RESET_DAY] ?: startDay

        // Older installs stored only epoch days. Migrate those values to the
        // start of that local day once, then keep exact millisecond timestamps.
        val startAt = p[Keys.START_AT_MILLIS] ?: dayStartMillis(startDay, zone)
        val lastResetAt = p[Keys.LAST_RESET_AT_MILLIS] ?: dayStartMillis(lastResetDay, zone)
        val statuses = parseStatus(p[Keys.DAILY_STATUS] ?: "")
        val notes = parseNotes(p[Keys.DAILY_NOTES] ?: "")
        val previousBest = (p[Keys.BEST_STREAK] ?: 0).coerceAtLeast(0)
        val current = HabitStreakCalculator.elapsedDays(now, startAt)
        val closedRuns = parseRuns(p[Keys.STREAK_RUNS] ?: "")
        val runs = closedRuns + StreakRun(startAtMillis = startAt)

        PersonalProgressSnapshot(
            profileName = p[Keys.PROFILE_NAME] ?: "",
            goalTitle = p[Keys.GOAL_TITLE] ?: "My personal goal",
            startDay = startDay,
            lastResetDay = lastResetDay,
            currentStreak = current,
            bestStreak = maxOf(previousBest, current),
            previousBestStreak = previousBest,
            resetCount = p[Keys.RESET_COUNT] ?: 0,
            checkInCount = p[Keys.CHECK_IN_COUNT] ?: statuses.count { it.value == 1 },
            dailyStatus = statuses,
            todayNote = notes[today] ?: "",
            startAtMillis = startAt,
            lastResetAtMillis = lastResetAt,
            streakRuns = runs
        )
    }


    /**
     * Persists the first exact start instant. Existing date-only records migrate
     * to local midnight; brand-new profiles begin at the actual setup time.
     */
    suspend fun ensureInitialized(nowMillis: Long = System.currentTimeMillis()) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toEpochDay()
        context.habitDataStore.edit { p ->
            val savedDay = p[Keys.START_DAY]
            val startAt = p[Keys.START_AT_MILLIS]
                ?: savedDay?.let { dayStartMillis(it, zone) }
                ?: nowMillis
            val startDay = savedDay
                ?: Instant.ofEpochMilli(startAt).atZone(zone).toLocalDate().toEpochDay()
            val lastResetDay = p[Keys.LAST_RESET_DAY] ?: startDay
            val lastResetAt = p[Keys.LAST_RESET_AT_MILLIS]
                ?: if (lastResetDay == startDay) startAt else dayStartMillis(lastResetDay, zone)

            p[Keys.START_DAY] = startDay
            p[Keys.LAST_RESET_DAY] = lastResetDay
            p[Keys.START_AT_MILLIS] = startAt
            p[Keys.LAST_RESET_AT_MILLIS] = lastResetAt
        }
    }

    suspend fun setProfileName(name: String) {
        context.habitDataStore.edit { it[Keys.PROFILE_NAME] = name.trim().take(40) }
    }

    suspend fun setGoalTitle(title: String) {
        val safe = title.trim().take(48).ifBlank { "My personal goal" }
        context.habitDataStore.edit { it[Keys.GOAL_TITLE] = safe }
    }

    /** Optional legacy check-in API, retained so older backups remain readable. */
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

    /**
     * Closes the current timestamp-based run, saves its endpoint, and starts
     * a new run at the reset time. Calling without an argument uses the exact
     * current instant; a supplied epoch day remains supported for old callers.
     */
    suspend fun recordReset(day: Long? = null) {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toEpochDay()
        val requestedAt = if (day == null || day == today) now else dayStartMillis(day, zone)
        context.habitDataStore.edit { p ->
            val startDay = p[Keys.START_DAY] ?: today
            val oldStartAt = p[Keys.START_AT_MILLIS] ?: dayStartMillis(startDay, zone)
            val resetAt = maxOf(requestedAt, oldStartAt)
            val resetDay = Instant.ofEpochMilli(resetAt).atZone(zone).toLocalDate().toEpochDay()
            val statuses = parseStatus(p[Keys.DAILY_STATUS] ?: "").toMutableMap()
            val isNewResetEvent = resetAt > oldStartAt
            val runs = parseRuns(p[Keys.STREAK_RUNS] ?: "").toMutableList()
            if (isNewResetEvent) {
                runs += StreakRun(startAtMillis = oldStartAt, endAtMillis = resetAt)
            }

            val completedRun = HabitStreakCalculator.elapsedDays(resetAt, oldStartAt)
            val best = maxOf(p[Keys.BEST_STREAK] ?: 0, completedRun).coerceAtLeast(0)
            statuses[resetDay] = 0

            p[Keys.START_DAY] = resetDay
            p[Keys.LAST_RESET_DAY] = resetDay
            p[Keys.START_AT_MILLIS] = resetAt
            p[Keys.LAST_RESET_AT_MILLIS] = resetAt
            p[Keys.STREAK_RUNS] = encodeRuns(runs)
            p[Keys.BEST_STREAK] = best
            // Count each distinct reset event, including multiple events on one local day.
            if (isNewResetEvent) p[Keys.RESET_COUNT] = (p[Keys.RESET_COUNT] ?: 0) + 1
            p[Keys.DAILY_STATUS] = encodeStatus(statuses, resetDay)
        }
    }

    /** Retained for backward compatibility; the Progress screen no longer shows notes. */
    suspend fun saveTodayNote(note: String, day: Long = LocalDate.now().toEpochDay()) {
        context.habitDataStore.edit { p ->
            val notes = parseNotes(p[Keys.DAILY_NOTES] ?: "").toMutableMap()
            val clean = note.trim().take(500)
            if (clean.isBlank()) notes.remove(day) else notes[day] = clean
            p[Keys.DAILY_NOTES] = encodeNotes(notes, day)
        }
    }

    /**
     * Imports the legacy 1.8 JSON backup. Exact event timestamps are converted
     * into closed runs; unsupported legacy free-text notes are not restored.
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

        fun timestampToDay(timestamp: Long): Long? = if (timestamp > 0L) {
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

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toEpochDay()
        val originalStartAt = longValue("tracker_started_at").takeIf { it > 0L }
            ?: dayStartMillis(today, zone)

        val importedRuns = mutableListOf<StreakRun>()
        var currentStartAt = originalStartAt
        resetTimestamps.forEach { resetAt ->
            if (resetAt >= currentStartAt) {
                if (resetAt > currentStartAt) {
                    importedRuns += StreakRun(currentStartAt, resetAt)
                }
                currentStartAt = resetAt
            }
        }

        val currentStartDay = timestampToDay(currentStartAt) ?: today
        val dailyStatus = mutableMapOf<Long, Int>()
        resetTimestamps.mapNotNull(::timestampToDay).forEach { dailyStatus[it] = 0 }
        stringSet("daily_check_ins").forEach { raw ->
            val date = raw.substringBefore("|").let {
                runCatching { LocalDate.parse(it).toEpochDay() }.getOrNull()
            }
            if (date != null && dailyStatus[date] != 0) dailyStatus[date] = 1
        }

        val best = maxOf(
            intValue("best_fap_streak_days"),
            intValue("previous_fap_streak_days")
        ).coerceAtLeast(0)
        val resetCount = maxOf(intValue("fap_log_count"), resetTimestamps.size).coerceAtLeast(0)
        val checkIns = dailyStatus.values.count { it == 1 }

        context.habitDataStore.edit { p ->
            p[Keys.START_DAY] = currentStartDay
            p[Keys.LAST_RESET_DAY] = currentStartDay
            p[Keys.START_AT_MILLIS] = currentStartAt
            p[Keys.LAST_RESET_AT_MILLIS] = currentStartAt
            p[Keys.STREAK_RUNS] = encodeRuns(importedRuns)
            p[Keys.BEST_STREAK] = best
            p[Keys.RESET_COUNT] = resetCount
            p[Keys.CHECK_IN_COUNT] = checkIns
            p[Keys.DAILY_STATUS] = encodeStatus(dailyStatus, today)
        }
    }

    private fun dayStartMillis(epochDay: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun parseRuns(value: String): List<StreakRun> {
        if (value.isBlank()) return emptyList()
        return value.split(";").mapNotNull { entry ->
            val pair = entry.split(",", limit = 2)
            val start = pair.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val end = pair.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
            if (start <= 0L || end < start) null else StreakRun(start, end)
        }
    }

    private fun encodeRuns(values: List<StreakRun>): String =
        values.filter { it.endAtMillis != null && it.endAtMillis >= it.startAtMillis }
            .takeLast(500)
            .joinToString(";") { it.startAtMillis.toString() + "," + it.endAtMillis }

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
            } catch (_: Exception) {
                return@mapNotNull null
            }
            day to note
        }.toMap()
    }

    private fun encodeNotes(values: Map<Long, String>, today: Long): String =
        values.filterKeys { it >= today - 365L }.toSortedMap().entries.joinToString(";") {
            val encoded = Base64.encodeToString(it.value.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
            "${it.key}:$encoded"
        }
}
