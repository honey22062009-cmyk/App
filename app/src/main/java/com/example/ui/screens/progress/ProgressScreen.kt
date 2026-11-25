package com.example.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.preferences.HabitStreakCalculator
import com.example.data.preferences.PersonalProgressPreferences
import com.example.data.preferences.PersonalProgressSnapshot
import com.example.data.preferences.StreakRun
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class StreakGraphRange(val label: String, val days: Long?) {
    SEVEN_DAYS("7 days", 7L),
    THIRTY_DAYS("30 days", 30L),
    NINETY_DAYS("90 days", 90L),
    ALL_TIME("All time", null)
}

private data class GraphPoint(val timestampMillis: Long, val streakDays: Int)

@Composable
fun ProgressScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { PersonalProgressPreferences(context.applicationContext) }
    val snapshot by store.snapshotFlow.collectAsState(initial = PersonalProgressSnapshot())
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nowMillis = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val today = LocalDate.now().toEpochDay()
    val currentStreak = HabitStreakCalculator.elapsedDays(nowMillis, snapshot.startAtMillis)
    val personalBest = maxOf(snapshot.previousBestStreak, currentStreak)
    val milestones = listOf(1, 3, 7, 14, 30, 60, 90)
    val achieved = milestones.filter { personalBest >= it }

    var title by remember(snapshot.goalTitle) { mutableStateOf(snapshot.goalTitle) }
    var selectedRange by remember { mutableStateOf(StreakGraphRange.THIRTY_DAYS) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var editingTitle by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Progress", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Your streak updates automatically after each complete 24-hour period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "CURRENT STREAK",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    currentStreak.toString(),
                                    style = MaterialTheme.typography.displayLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "days",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(bottom = 9.dp)
                                )
                            }
                            Text(
                                when {
                                    currentStreak > snapshot.previousBestStreak && snapshot.previousBestStreak > 0 ->
                                        "A new personal best. Keep building at your pace."
                                    currentStreak == 0 -> "Your next full 24-hour period starts from your saved start time."
                                    else -> "The counter advances automatically. No daily check-in needed."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            Modifier.size(76.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Flag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("Previous best", "${snapshot.previousBestStreak} days")
                        MetricPill("Resets", snapshot.resetCount.toString())
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "My goal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { editingTitle = !editingTitle }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (editingTitle) "Close" else "Edit")
                        }
                    }
                    if (editingTitle) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it.take(48) },
                            label = { Text("Goal name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                scope.launch { store.setGoalTitle(title) }
                                editingTitle = false
                            },
                            enabled = title.isNotBlank()
                        ) { Text("Save goal") }
                    } else {
                        Text(
                            snapshot.goalTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "The timer uses the exact start time saved on this device. Logging a reset starts a new run.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("Log reset")
                    }
                    if (snapshot.dailyStatus[today] == 0) {
                        Text(
                            "A reset is recorded for today. Your new run is underway.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Streak history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Past runs remain visible. Each reset is a drop to zero; the dashed line marks your previous best.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StreakGraphRange.entries.forEach { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = { selectedRange = range },
                                label = { Text(range.label) }
                            )
                        }
                    }
                    StreakLineChart(
                        runs = snapshot.streakRuns,
                        currentDays = currentStreak,
                        previousBestDays = snapshot.previousBestStreak,
                        range = selectedRange,
                        nowMillis = nowMillis
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        LineLegend(MaterialTheme.colorScheme.primary, "Current run")
                        LineLegend(MaterialTheme.colorScheme.secondary, "Previous runs")
                        LineLegend(MaterialTheme.colorScheme.tertiary, "Previous best")
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Milestones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        achieved.forEach { days ->
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("${days}d") }
                            )
                        }
                        if (achieved.isEmpty()) {
                            Text(
                                "Your first milestone is one day.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        "Next milestone: " + (milestones.firstOrNull { it > personalBest }?.let { "$it days" }
                            ?: "All milestones reached"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                "Your records stay on this device. Export a backup from Settings before changing or reinstalling the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Record a reset?") },
            text = {
                Text("Your previous run will remain in the graph, and the current streak will restart from zero.")
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { store.recordReset() }
                    showResetConfirm = false
                }) { Text("Record reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StreakLineChart(
    runs: List<StreakRun>,
    currentDays: Int,
    previousBestDays: Int,
    range: StreakGraphRange,
    nowMillis: Long
) {
    val currentColor = MaterialTheme.colorScheme.primary
    val historyColor = MaterialTheme.colorScheme.secondary
    val bestColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val rangeStart = range.days?.let { nowMillis - it * HabitStreakCalculator.DAY_MILLIS }
        ?: (runs.minOfOrNull { it.startAtMillis } ?: (nowMillis - 7L * HabitStreakCalculator.DAY_MILLIS))
    val chartEnd = maxOf(nowMillis, rangeStart + 1L)
    val visibleSegments = remember(runs, rangeStart, nowMillis) {
        buildGraphSegments(runs, rangeStart, nowMillis)
    }
    val historicalMaximum = visibleSegments.flatten().maxOfOrNull { it.streakDays } ?: 0
    val chartMax = maxOf(7, currentDays, previousBestDays, historicalMaximum)
        .let { ((it + 6) / 7) * 7 }

    Canvas(modifier = Modifier.fillMaxWidth().height(214.dp)) {
        val left = 12.dp.toPx()
        val right = 12.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 14.dp.toPx()
        val plotWidth = (size.width - left - right).coerceAtLeast(1f)
        val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)

        for (tick in 0..4) {
            val ratio = tick / 4f
            val y = top + plotHeight * (1f - ratio)
            drawLine(
                color = gridColor.copy(alpha = 0.58f),
                start = androidx.compose.ui.geometry.Offset(left, y),
                end = androidx.compose.ui.geometry.Offset(left + plotWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun xFor(timestamp: Long): Float {
            val ratio = ((timestamp - rangeStart).toDouble() / (chartEnd - rangeStart).toDouble())
                .toFloat().coerceIn(0f, 1f)
            return left + plotWidth * ratio
        }

        fun yFor(days: Int): Float {
            val ratio = (days.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)
            return top + plotHeight * (1f - ratio)
        }

        if (previousBestDays > 0) {
            val recordY = yFor(previousBestDays)
            drawLine(
                color = bestColor,
                start = androidx.compose.ui.geometry.Offset(left, recordY),
                end = androidx.compose.ui.geometry.Offset(left + plotWidth, recordY),
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
        }

        visibleSegments.forEach { segment ->
            if (segment.size < 2) return@forEach
            val isCurrent = segment.lastOrNull()?.let { point ->
                runs.lastOrNull()?.endAtMillis == null &&
                    point.timestampMillis == nowMillis
            } == true
            val path = Path().apply {
                moveTo(xFor(segment.first().timestampMillis), yFor(segment.first().streakDays))
                segment.drop(1).forEach { point ->
                    lineTo(xFor(point.timestampMillis), yFor(point.streakDays))
                }
            }
            drawPath(
                path = path,
                color = if (isCurrent) currentColor else historyColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val lastCurrent = visibleSegments.lastOrNull()
            ?.lastOrNull { it.timestampMillis == nowMillis && it.streakDays == currentDays }
        if (lastCurrent != null) {
            drawCircle(
                color = currentColor,
                radius = 4.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(xFor(nowMillis), yFor(currentDays))
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            if (range == StreakGraphRange.ALL_TIME) "First saved run" else range.label + " ago",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text("Now", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun buildGraphSegments(
    runs: List<StreakRun>,
    rangeStart: Long,
    nowMillis: Long
): List<List<GraphPoint>> {
    val result = mutableListOf<List<GraphPoint>>()
    runs.forEach { run ->
        val runEnd = minOf(run.endAtMillis ?: nowMillis, nowMillis)
        if (runEnd < rangeStart || run.startAtMillis > nowMillis) return@forEach

        val points = mutableListOf<GraphPoint>()
        val firstVisible = maxOf(run.startAtMillis, rangeStart)
        points += GraphPoint(
            timestampMillis = firstVisible,
            streakDays = HabitStreakCalculator.elapsedDays(firstVisible, run.startAtMillis)
        )

        var sampleAt = run.startAtMillis + HabitStreakCalculator.DAY_MILLIS
        while (sampleAt < runEnd) {
            if (sampleAt >= rangeStart) {
                points += GraphPoint(
                    timestampMillis = sampleAt,
                    streakDays = HabitStreakCalculator.elapsedDays(sampleAt, run.startAtMillis)
                )
            }
            sampleAt += HabitStreakCalculator.DAY_MILLIS
        }

        val endDays = HabitStreakCalculator.elapsedDays(runEnd, run.startAtMillis)
        if (runEnd >= rangeStart) {
            points += GraphPoint(runEnd, endDays)
            if (run.endAtMillis != null) {
                // Same x coordinate produces the vertical drop at the reset moment.
                points += GraphPoint(runEnd, 0)
            }
        }
        if (points.size >= 2) result += points
    }
    return result
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun LineLegend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(18.dp).height(3.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
