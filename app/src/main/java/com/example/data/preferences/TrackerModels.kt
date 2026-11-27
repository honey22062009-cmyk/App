package com.example.data.preferences

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID
import kotlin.math.max

data class TrackerResetRecord(
    val id: String,
    val timestamp: Long,
    val category: String? = null,
    val note: String? = null
) {
    fun encode(): String = listOf(
        id,
        timestamp.toString(),
        encodeText(category.orEmpty()),
        encodeText(note.orEmpty())
    ).joinToString("|")

    companion object {
        fun create(timestamp: Long, category: String?, note: String?): TrackerResetRecord =
            TrackerResetRecord(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                category = category?.trim()?.takeIf { it.isNotEmpty() },
                note = note?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            )

        fun legacy(timestamp: Long): TrackerResetRecord =
            TrackerResetRecord(id = "legacy-$timestamp", timestamp = timestamp)

        fun decode(raw: String): TrackerResetRecord? {
            val parts = raw.split("|", limit = 4)
            if (parts.size < 2) return null
            val timestamp = parts[1].toLongOrNull() ?: return null
            return TrackerResetRecord(
                id = parts[0].ifBlank { "legacy-$timestamp" },
                timestamp = timestamp,
                category = parts.getOrNull(2)?.let(::decodeText)?.takeIf { it.isNotBlank() },
                note = parts.getOrNull(3)?.let(::decodeText)?.takeIf { it.isNotBlank() }
            )
        }

        private fun encodeText(value: String): String =
            Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

        private fun decodeText(value: String): String = runCatching {
            String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        }.getOrDefault("")
    }
}

data class DailyCheckIn(
    val localDate: LocalDate,
    val rating: String
) {
    fun encode(): String = "${localDate}|$rating"

    companion object {
        val allowedRatings = setOf("easy", "okay", "difficult")

        fun decode(raw: String): DailyCheckIn? {
            val parts = raw.split("|", limit = 2)
            val date = parts.getOrNull(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            val rating = parts.getOrNull(1)?.lowercase() ?: return null
            if (rating !in allowedRatings) return null
            return DailyCheckIn(date, rating)
        }
    }
}

data class SleepEntry(
    val localDate: LocalDate,
    val durationMinutes: Int,
    val note: String? = null
) {
    fun encode(): String = listOf(
        localDate.toString(),
        durationMinutes.coerceIn(0, 24 * 60).toString(),
        java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(note.orEmpty().toByteArray(StandardCharsets.UTF_8))
    ).joinToString("|")

    companion object {
        fun decode(raw: String): SleepEntry? {
            val parts = raw.split("|", limit = 3)
            val date = parts.getOrNull(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            val minutes = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 24 * 60) ?: return null
            val note = parts.getOrNull(2)?.let {
                runCatching {
                    String(java.util.Base64.getUrlDecoder().decode(it), StandardCharsets.UTF_8)
                }.getOrDefault("")
            }?.trim()?.take(500)?.takeIf { it.isNotBlank() }
            return SleepEntry(date, minutes, note)
        }
    }
}

data class TrackerStats(
    val currentDays: Int,
    val previousDays: Int,
    val bestDays: Int,
    val totalResets: Int,
    val resetsThisMonth: Int,
    val longestThisYear: Int,
    val averageRecentDays: Int
)

data class StreakPoint(
    val date: LocalDate,
    val streakDays: Int,
    val reset: Boolean
)

object TrackerCalculator {
    private const val DAY_MS = 86_400_000L

    fun fullDaysBetween(startMillis: Long, endMillis: Long): Int =
        ((endMillis - startMillis).coerceAtLeast(0L) / DAY_MS)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    fun stats(
        trackerStartedAt: Long,
        resets: List<TrackerResetRecord>,
        nowMillis: Long,
        zone: ZoneId
    ): TrackerStats {
        val sorted = resets
            .filter { it.timestamp in 1..nowMillis }
            .sortedBy { it.timestamp }
        val start = trackerStartedAt.takeIf { it > 0L } ?: nowMillis
        val intervals = mutableListOf<Int>()
        var baseline = start

        sorted.forEach { event ->
            intervals += fullDaysBetween(baseline, event.timestamp)
            baseline = max(baseline, event.timestamp)
        }
        val current = fullDaysBetween(baseline, nowMillis)
        val previous = intervals.lastOrNull() ?: 0
        val best = (intervals + current).maxOrNull() ?: current

        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val resetsThisMonth = sorted.count {
            val d = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            d.year == nowDate.year && d.month == nowDate.month
        }

        val yearStart = nowDate.withDayOfYear(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var yearBaseline = max(start, yearStart)
        var longestThisYear = 0
        sorted.filter { it.timestamp >= yearStart }.forEach { event ->
            longestThisYear = max(longestThisYear, fullDaysBetween(yearBaseline, event.timestamp))
            yearBaseline = max(yearBaseline, event.timestamp)
        }
        longestThisYear = max(longestThisYear, fullDaysBetween(yearBaseline, nowMillis))

        val recentIntervals = intervals.takeLast(5)
        val averageRecent = if (recentIntervals.isEmpty()) 0 else recentIntervals.average().toInt()

        return TrackerStats(
            currentDays = current,
            previousDays = previous,
            bestDays = best,
            totalResets = sorted.size,
            resetsThisMonth = resetsThisMonth,
            longestThisYear = longestThisYear,
            averageRecentDays = averageRecent
        )
    }

    fun streakAt(
        date: LocalDate,
        trackerStartedAt: Long,
        resets: List<TrackerResetRecord>,
        zone: ZoneId
    ): Int {
        val endOfDay = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
        val start = trackerStartedAt.takeIf { it > 0L } ?: endOfDay
        if (endOfDay < start) return 0
        val latestReset = resets.asSequence()
            .map { it.timestamp }
            .filter { it <= endOfDay }
            .maxOrNull()
        return fullDaysBetween(latestReset ?: start, endOfDay)
    }

    fun graphPoints(
        trackerStartedAt: Long,
        resets: List<TrackerResetRecord>,
        startDate: LocalDate,
        endDate: LocalDate,
        zone: ZoneId
    ): List<StreakPoint> {
        if (endDate.isBefore(startDate)) return emptyList()
        val resetDates = resets.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        val days = ChronoUnit.DAYS.between(startDate, endDate).coerceAtMost(3700)
        return (0..days).map { offset ->
            val date = startDate.plusDays(offset)
            StreakPoint(
                date = date,
                streakDays = streakAt(date, trackerStartedAt, resets, zone),
                reset = resetDates[date].orEmpty().isNotEmpty()
            )
        }
    }
}
