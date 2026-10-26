package com.example.ui.screens.dashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.preferences.DailyCheckIn
import com.example.data.preferences.SettingsPreferences
import com.example.data.preferences.SleepEntry
import com.example.data.preferences.StreakPoint
import com.example.data.preferences.TrackerCalculator
import com.example.data.preferences.TrackerResetRecord
import com.example.data.preferences.TrackerStats
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassVariant
import com.example.ui.screens.onboarding.CompanionAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

internal val WarmMagenta = Color(0xFFE45B99)
internal val WarmCoral = Color(0xFFF4866B)
internal val WarmPlum = Color(0xFFA953C7)
internal val WarmMint = Color(0xFF62B49A)

internal enum class GraphRange(val label: String, val days: Long?) {
    Seven("7D", 7),
    Thirty("30D", 30),
    Ninety("90D", 90),
    All("ALL", null)
}

internal data class JourneyItem(
    val timestamp: Long,
    val title: String,
    val detail: String,
    val kind: String
)

@Composable
internal fun StreakHeroCard(
    stats: TrackerStats,
    characterId: String,
    lastResetLabel: String,
    goalDays: Int,
    reduceMotion: Boolean,
    tablet: Boolean,
    onRecordReset: () -> Unit,
    onEditGoal: () -> Unit
) {
    val goalProgress = if (goalDays > 0) (stats.currentDays.toFloat() / goalDays).coerceIn(0f, 1f) else 0f
    val bestProgress = if (stats.bestDays <= 0) 0.06f
    else (stats.currentDays.toFloat() / stats.bestDays.coerceAtLeast(1)).coerceIn(0.06f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = if (goalDays > 0) goalProgress.coerceAtLeast(0.04f) else bestProgress,
        animationSpec = if (reduceMotion) tween(0) else tween(700),
        label = "hero_progress"
    )
    val highlight = if (reduceMotion) {
        0.15f
    } else {
        val infinite = rememberInfiniteTransition(label = "hero_glass")
        val drift by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(10_000, easing = LinearEasing)),
            label = "hero_highlight"
        )
        drift
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        variant = GlassVariant.Accent,
        shape = RoundedCornerShape(30.dp),
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            WarmMagenta.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(120f + 420f * highlight, 20f),
                        radius = 520f
                    )
                )
        ) {
            if (tablet) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroRing(
                        stats = stats,
                        characterId = characterId,
                        progress = animatedProgress,
                        modifier = Modifier.weight(1f)
                    )
                    HeroActions(
                        stats = stats,
                        lastResetLabel = lastResetLabel,
                        goalDays = goalDays,
                        goalProgress = goalProgress,
                        onRecordReset = onRecordReset,
                        onEditGoal = onEditGoal,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HeroRing(
                        stats = stats,
                        characterId = characterId,
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HeroActions(
                        stats = stats,
                        lastResetLabel = lastResetLabel,
                        goalDays = goalDays,
                        goalProgress = goalProgress,
                        onRecordReset = onRecordReset,
                        onEditGoal = onEditGoal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroRing(
    stats: TrackerStats,
    characterId: String,
    progress: Float,
    modifier: Modifier
) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .height(210.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(224.dp)) {
            val stroke = size.minDimension * 0.052f
            val inset = stroke
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(WarmMagenta, WarmCoral, WarmPlum, WarmMagenta)),
                startAngle = 140f,
                sweepAngle = 260f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompanionAvatar(
                characterId = characterId,
                modifier = Modifier.size(118.dp)
            )
            AnimatedContent(
                targetState = stats.currentDays,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "streak_days"
            ) { days ->
                Text(
                    text = days.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                if (stats.currentDays == 1) "DAY" else "DAYS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Current streak",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeroActions(
    stats: TrackerStats,
    lastResetLabel: String,
    goalDays: Int,
    goalProgress: Float,
    onRecordReset: () -> Unit,
    onEditGoal: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Current streak",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Last reset: $lastResetLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (goalDays > 0) {
            Text(
                "Day ${stats.currentDays} of $goalDays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LinearProgressIndicator(
                progress = { goalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = WarmMagenta,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
            )
        } else {
            Text(
                "No personal goal set",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onRecordReset,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Record reset")
            }
            OutlinedButton(
                onClick = onEditGoal,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Flag, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (goalDays > 0) "Goal" else "Set goal")
            }
        }
        Text(
            "Manual tracker only. Nothing is detected automatically.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HumanPairIllustration() {
    Canvas(
        modifier = Modifier
            .size(width = 126.dp, height = 78.dp)
            .semantics { contentDescription = "Two stylized human figures" }
    ) {
        fun figure(centerX: Float, main: Color, light: Color, width: Float) {
            val headY = size.height * 0.19f
            val headR = size.minDimension * 0.11f
            drawCircle(
                Color.Black.copy(alpha = 0.16f),
                radius = headR + 2f,
                center = Offset(centerX + 3f, headY + 3f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(light, main)),
                radius = headR,
                center = Offset(centerX, headY)
            )
            val torsoTop = size.height * 0.36f
            val torsoHeight = size.height * 0.31f
            drawRoundRect(
                brush = Brush.linearGradient(listOf(light, main)),
                topLeft = Offset(centerX - width / 2f, torsoTop),
                size = Size(width, torsoHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )
            val limb = size.width * 0.05f
            drawLine(main, Offset(centerX - width * .32f, torsoTop + 8f), Offset(centerX - width * .78f, size.height * .60f), limb, StrokeCap.Round)
            drawLine(main, Offset(centerX + width * .32f, torsoTop + 8f), Offset(centerX + width * .78f, size.height * .60f), limb, StrokeCap.Round)
            drawLine(main, Offset(centerX - width * .18f, torsoTop + torsoHeight), Offset(centerX - width * .30f, size.height * .92f), limb * 1.1f, StrokeCap.Round)
            drawLine(main, Offset(centerX + width * .18f, torsoTop + torsoHeight), Offset(centerX + width * .30f, size.height * .92f), limb * 1.1f, StrokeCap.Round)
        }
        figure(size.width * .34f, WarmPlum, Color(0xFFE0A4F3), size.width * .23f)
        figure(size.width * .66f, WarmMagenta, Color(0xFFFFB1D0), size.width * .22f)
    }
}

@Composable
internal fun StatsStrip(stats: TrackerStats, tablet: Boolean) {
    val metrics = listOf(
        "Current" to stats.currentDays.toString(),
        "Best" to stats.bestDays.toString(),
        "Previous" to stats.previousDays.toString(),
        "Total resets" to stats.totalResets.toString(),
        "This month" to stats.resetsThisMonth.toString(),
        "Longest this year" to stats.longestThisYear.toString()
    )
    if (tablet) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            metrics.forEachIndexed { index, item ->
                SmallMetricCard(
                    label = item.first,
                    value = item.second,
                    accent = listOf(WarmMagenta, WarmPlum, WarmCoral, WarmMint)[index % 4],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            metrics.forEachIndexed { index, item ->
                SmallMetricCard(
                    label = item.first,
                    value = item.second,
                    accent = listOf(WarmMagenta, WarmPlum, WarmCoral, WarmMint)[index % 4],
                    modifier = Modifier.width(128.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallMetricCard(label: String, value: String, accent: Color, modifier: Modifier) {
    GlassCard(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(13.dp)) {
            Box(Modifier.size(7.dp).background(accent, CircleShape))
            Spacer(Modifier.height(7.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun StreakGraphCard(
    trackerStartedAt: Long,
    resets: List<TrackerResetRecord>,
    zone: ZoneId,
    tablet: Boolean
) {
    var range by remember { mutableStateOf(GraphRange.Seven) }
    val today = LocalDate.now(zone)
    val earliest = remember(trackerStartedAt, resets, today) {
        val trackerDate = trackerStartedAt.takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: today
        val resetDate = resets.minOfOrNull { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
        listOfNotNull(trackerDate, resetDate).minOrNull() ?: today
    }
    val startDate = range.days?.let { today.minusDays(it - 1) } ?: earliest
    val points = remember(trackerStartedAt, resets, startDate, today, zone) {
        TrackerCalculator.graphPoints(trackerStartedAt, resets, startDate, today, zone)
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Streak history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tap or drag across the line for a day", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GraphRange.entries.forEach { item ->
                        FilterChip(
                            selected = range == item,
                            onClick = { range = item },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            InteractiveStreakGraph(points, Modifier.fillMaxWidth().height(if (tablet) 220.dp else 170.dp))
        }
    }
}

@Composable
private fun InteractiveStreakGraph(points: List<StreakPoint>, modifier: Modifier) {
    var selectedIndex by remember(points) { mutableIntStateOf(if (points.isEmpty()) -1 else points.lastIndex) }
    var widthPx by remember { mutableStateOf(1f) }
    val maxValue = max(1, points.maxOfOrNull { it.streakDays } ?: 1)
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }

    fun indexForX(x: Float): Int {
        if (points.isEmpty()) return -1
        val normalized = (x / widthPx).coerceIn(0f, 1f)
        return (normalized * points.lastIndex).roundToInt().coerceIn(0, points.lastIndex)
    }

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val guideColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    Column {
        Canvas(
            modifier = modifier
                .semantics {
                    contentDescription = if (selectedIndex in points.indices) {
                        val point = points[selectedIndex]
                        "Streak history, " + point.date.format(formatter) +
                            ", day " + point.streakDays +
                            if (point.reset) ", reset logged" else ""
                    } else {
                        "Streak history, no tracker history yet"
                    }
                }
                .pointerInput(points, widthPx) {
                    detectTapGestures { selectedIndex = indexForX(it.x) }
                }
                .pointerInput(points, widthPx) {
                    detectDragGestures(
                        onDragStart = { selectedIndex = indexForX(it.x) },
                        onDrag = { change, _ -> selectedIndex = indexForX(change.position.x) }
                    )
                }
        ) {
            widthPx = size.width.coerceAtLeast(1f)
            if (points.isEmpty()) return@Canvas
            val left = 10f
            val right = size.width - 10f
            val top = 10f
            val bottom = size.height - 16f
            val stepX = if (points.size <= 1) 0f else (right - left) / points.lastIndex
            val offsets = points.mapIndexed { index, point ->
                Offset(
                    left + stepX * index,
                    bottom - (point.streakDays.toFloat() / maxValue) * (bottom - top)
                )
            }
            repeat(3) { line ->
                val y = top + (bottom - top) * line / 2f
                drawLine(gridColor, Offset(left, y), Offset(right, y), 1f)
            }
            val fill = Path().apply {
                moveTo(offsets.first().x, bottom)
                lineTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(offsets.last().x, bottom)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(WarmMagenta.copy(alpha = .26f), WarmCoral.copy(alpha = .05f), Color.Transparent)))
            val linePath = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(linePath, Brush.horizontalGradient(listOf(WarmMagenta, WarmCoral, WarmPlum)), style = Stroke(5f, cap = StrokeCap.Round))
            offsets.forEachIndexed { index, point ->
                if (points[index].reset) {
                    drawCircle(WarmCoral, 6f, point)
                    drawCircle(surfaceColor, 2.5f, point)
                }
            }
            if (selectedIndex in offsets.indices) {
                val p = offsets[selectedIndex]
                drawLine(guideColor, Offset(p.x, top), Offset(p.x, bottom), 2f)
                drawCircle(surfaceColor, 9f, p)
                drawCircle(WarmMagenta, 6f, p)
            }
        }
        if (selectedIndex in points.indices) {
            val point = points[selectedIndex]
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .78f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "${point.date.format(formatter)} · Day ${point.streakDays}${if (point.reset) " · reset logged" else ""}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else {
            Text("No tracker history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MilestoneCard(
    stats: TrackerStats,
    onViewAll: () -> Unit
) {
    val milestones = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
    val next = milestones.firstOrNull { it > stats.currentDays }
    val progress = next?.let { stats.currentDays.toFloat() / it }?.coerceIn(0f, 1f) ?: 1f
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmPlum)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Next milestone", fontWeight = FontWeight.Bold)
                    Text(
                        if (next == null) "365-day milestone reached" else "${stats.currentDays} / $next days",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onViewAll) { Text("View all") }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = WarmPlum,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .09f)
            )
        }
    }
}

@Composable
internal fun DailyCheckInCard(
    currentDays: Int,
    todayCheckIn: DailyCheckIn?,
    todaySleep: SleepEntry?,
    onOpenCheckIn: () -> Unit,
    onRecordReset: () -> Unit,
    onAddSleep: () -> Unit
) {
    val sleepLabel = todaySleep?.let {
        (it.durationMinutes / 60).toString() + "h " +
            (it.durationMinutes % 60).toString().padStart(2, '0') + "m"
    } ?: "Not logged"

    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Streak · Day $currentDays", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = CircleShape,
                    color = if (todayCheckIn == null) WarmCoral.copy(alpha = .14f) else WarmMint.copy(alpha = .18f)
                ) {
                    Text(
                        if (todayCheckIn == null) "Check-in pending" else "Checked in",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (todayCheckIn == null) WarmCoral else WarmMint,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onOpenCheckIn, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) {
                    Text(if (todayCheckIn == null) "Check in" else "Edit check-in")
                }
                OutlinedButton(onClick = onAddSleep, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) {
                    Text(if (todaySleep == null) "Add sleep" else "Edit sleep")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sleep", fontWeight = FontWeight.SemiBold)
                    Text(sleepLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onRecordReset) { Text("Record reset") }
            }
            Text("Your streak uses elapsed time, not check-in taps. Entries stay on this device.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun DailyCheckInPromptDialog(
    current: DailyCheckIn?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onRecordReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Daily check-in", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This records today's check-in only. Your streak still comes from elapsed time since the last reset.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    Triple("easy", "Easy", "🙂"),
                    Triple("okay", "Okay", "😐"),
                    Triple("difficult", "Difficult", "😮‍💨")
                ).forEach { (value, label, icon) ->
                    FilterChip(
                        selected = current?.rating == value,
                        onClick = { onSelect(value) },
                        label = { Text(icon + " " + label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedButton(
                    onClick = onRecordReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record reset instead")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Skip for today") }
        }
    )
}

@Composable
internal fun SleepEntryDialog(
    current: SleepEntry?,
    onDismiss: () -> Unit,
    onSave: (Int, String?) -> Unit
) {
    var minutes by remember(current) { mutableIntStateOf(current?.durationMinutes ?: 8 * 60) }
    var note by remember(current) { mutableStateOf(current?.note.orEmpty()) }
    val presets = listOf(5, 6, 7, 8, 9)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Log sleep", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Manual entry only. Strict Blocker does not measure sleep automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { hours ->
                        FilterChip(
                            selected = minutes == hours * 60,
                            onClick = { minutes = hours * 60 },
                            label = { Text(hours.toString() + "h") }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { minutes = (minutes - 15).coerceAtLeast(0) },
                        modifier = Modifier.weight(1f)
                    ) { Text("−15m") }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
                    ) {
                        Text(
                            (minutes / 60).toString() + "h " +
                                (minutes % 60).toString().padStart(2, '0') + "m",
                            modifier = Modifier.padding(vertical = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = { minutes = (minutes + 15).coerceAtMost(24 * 60) },
                        modifier = Modifier.weight(1f)
                    ) { Text("+15m") }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Optional note") },
                    supportingText = { Text("Saved only on this device.") },
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(minutes, note.trim().takeIf { it.isNotBlank() }) }) {
                Text("Save sleep")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun CalendarHeatmapCard(
    trackerStartedAt: Long,
    resets: List<TrackerResetRecord>,
    checkIns: List<DailyCheckIn>,
    zone: ZoneId,
    onDateSelected: (LocalDate) -> Unit
) {
    var month by remember { mutableStateOf(YearMonth.now(zone)) }
    val today = LocalDate.now(zone)
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val totalCells = leading + month.lengthOfMonth()
    val weeks = (totalCells + 6) / 7
    val resetDates = remember(resets, zone) {
        resets.groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() }
    }
    val checkMap = remember(checkIns) { checkIns.associateBy { it.localDate } }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = WarmMagenta)
                Spacer(Modifier.width(8.dp))
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
                TextButton(
                    enabled = month < YearMonth.from(today),
                    onClick = { month = month.plusMonths(1) }
                ) { Text("›") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("M","T","W","T","F","S","S").forEach {
                    Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            repeat(weeks) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val index = row * 7 + col
                        val day = index - leading + 1
                        if (day !in 1..month.lengthOfMonth()) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = month.atDay(day)
                            val future = date.isAfter(today)
                            val streakDay = TrackerCalculator.streakAt(date, trackerStartedAt, resets, zone)
                            val reset = resetDates[date].orEmpty().isNotEmpty()
                            val checkIn = checkMap[date]
                            val intensity = (streakDay / 14f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .border(
                                        width = if (date == today) 1.5.dp else 0.dp,
                                        color = if (date == today) WarmPlum else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .background(
                                        if (future) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .28f)
                                        else WarmMagenta.copy(alpha = .10f + .38f * intensity),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .pointerInput(date, future) {
                                        if (!future) detectTapGestures { onDateSelected(date) }
                                    }
                                    .semantics {
                                        contentDescription = buildString {
                                            append(date.format(DateTimeFormatter.ofPattern("MMMM d")))
                                            append(", streak day $streakDay")
                                            if (reset) append(", reset logged")
                                            if (checkIn != null) append(", check-in ${checkIn.rating}")
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.toString(), style = MaterialTheme.typography.labelMedium)
                                if (reset) {
                                    Box(
                                        Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 4.dp)
                                            .size(5.dp)
                                            .background(WarmCoral, CircleShape)
                                    )
                                }
                                if (checkIn != null) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 4.dp, end = 4.dp)
                                            .size(5.dp)
                                            .background(WarmMint, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
internal fun InsightCard(text: String?) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Insights, contentDescription = null, tint = WarmPlum)
            Spacer(Modifier.width(11.dp))
            Column {
                Text("Pattern insight", fontWeight = FontWeight.Bold)
                Text(
                    text ?: "Log a few reset events before patterns are shown.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun JourneyPreviewCard(items: List<JourneyItem>, onOpen: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timeline, contentDescription = null, tint = WarmCoral)
                Spacer(Modifier.width(8.dp))
                Text("Journey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpen) { Text("Full timeline") }
            }
            items.take(3).forEachIndexed { index, item ->
                JourneyRow(item)
                if (index < minOf(2, items.lastIndex)) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
            }
        }
    }
}

@Composable
private fun JourneyRow(item: JourneyItem) {
    val color = when (item.kind) {
        "milestone" -> WarmPlum
        "goal" -> WarmMint
        "reset" -> WarmCoral
        else -> WarmMagenta
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 5.dp).size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(11.dp))
        Column {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ResetEditorDialog(
    editing: TrackerResetRecord?,
    currentDays: Int,
    onDismiss: () -> Unit,
    onSave: (Long, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var timestamp by remember(editing) { mutableStateOf(editing?.timestamp ?: System.currentTimeMillis()) }
    var category by remember(editing) { mutableStateOf(editing?.category) }
    var note by remember(editing) { mutableStateOf(editing?.note.orEmpty()) }
    val categories = listOf("Boredom", "Stress", "Habit", "Social media", "Trouble sleeping", "Other")
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a") }

    fun chooseDateTime() {
        val current = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val selected = LocalDateTime.of(year, month + 1, day, hour, minute)
                            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        timestamp = selected.coerceAtMost(System.currentTimeMillis())
                    },
                    current.hour,
                    current.minute,
                    false
                ).show()
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(30.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        title = { Text(if (editing == null) "Record reset" else "Edit reset", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (editing == null) {
                    Text("This closes the current $currentDays-day streak and starts a new one at the time you choose.")
                }
                Text("When?", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { timestamp = System.currentTimeMillis() }) { Text("Now") }
                    OutlinedButton(onClick = ::chooseDateTime) { Text("Choose date/time") }
                }
                Text(
                    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Optional category", fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { item ->
                                FilterChip(
                                    selected = category == item,
                                    onClick = { category = if (category == item) null else item },
                                    label = { Text(item, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text("Optional note") },
                    supportingText = { Text("Saved only on this device.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(timestamp, category, note.trim().takeIf { it.isNotBlank() }) }) {
                Text(if (editing == null) "Record" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun GoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int?) -> Unit
) {
    var custom by remember(currentGoal) { mutableStateOf(if (currentGoal in listOf(7,14,30,60,90,0)) "" else currentGoal.toString()) }
    var selected by remember(currentGoal) { mutableIntStateOf(currentGoal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Personal goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Choose a target. It changes progress display only, not your history.")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(7,14,30,60,90).forEach { days ->
                        FilterChip(selected = selected == days, onClick = { selected = days; custom = "" }, label = { Text("$days d") })
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = {
                        custom = it.filter(Char::isDigit).take(4)
                        custom.toIntOrNull()?.let { value -> selected = value.coerceIn(1, 3650) }
                    },
                    label = { Text("Custom days (1–3650)") },
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { selected = 0; custom = "" }) { Text("No goal") }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selected.takeIf { it > 0 }) }) { Text("Save goal") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun MilestonesDialog(
    stats: TrackerStats,
    trackerStartedAt: Long,
    resets: List<TrackerResetRecord>,
    zone: ZoneId,
    onDismiss: () -> Unit
) {
    val milestones = listOf(1,3,7,14,30,60,90,180,365)
    val dates = remember(trackerStartedAt, resets, zone) {
        milestoneUnlockDates(trackerStartedAt, resets, zone)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Milestones", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                milestones.forEach { milestone ->
                    val earned = stats.bestDays >= milestone
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (earned) WarmPlum.copy(alpha = .12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = if (earned) WarmPlum else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("$milestone day${if (milestone == 1) "" else "s"}", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (earned) dates[milestone]?.let { "Unlocked ${it.format(DateTimeFormatter.ofPattern("d MMM"))}" } ?: "Unlocked"
                                    else "Not reached yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun CalendarDetailDialog(
    date: LocalDate,
    streakDay: Int,
    resets: List<TrackerResetRecord>,
    checkIn: DailyCheckIn?,
    zone: ZoneId,
    onDismiss: () -> Unit
) {
    val resetItems = resets.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == date }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = { Text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Streak day $streakDay")
                Text(if (resetItems.isEmpty()) "No reset logged." else "${resetItems.size} reset${if (resetItems.size == 1) "" else "s"} logged.")
                checkIn?.let { Text("Daily check-in: ${it.rating.replaceFirstChar { it.uppercase() }}") }
                resetItems.firstOrNull()?.category?.let { Text("Category: $it") }
                resetItems.firstOrNull()?.note?.let { Text("Note: $it") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun TimelineDialog(
    items: List<JourneyItem>,
    resets: List<TrackerResetRecord>,
    onDismiss: () -> Unit,
    onEdit: (TrackerResetRecord) -> Unit,
    onDelete: (TrackerResetRecord) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Journey timeline", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.height(460.dp).widthIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items.take(18).forEach { JourneyRow(it) }
                if (resets.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Reset entries", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    resets.take(12).forEach { record ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    Instant.ofEpochMilli(record.timestamp).atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a")),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    listOfNotNull(record.category, record.note).joinToString(" · ").ifBlank { "No note" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onEdit(record) }) { Icon(Icons.Default.Edit, contentDescription = "Edit reset") }
                            IconButton(onClick = { onDelete(record) }) { Icon(Icons.Default.Delete, contentDescription = "Delete reset") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
internal fun DashboardCustomizationDialog(
    order: List<String>,
    hidden: Set<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>, Set<String>) -> Unit
) {
    var localOrder by remember(order) { mutableStateOf(order) }
    var localHidden by remember(hidden) { mutableStateOf(hidden) }
    val labels = mapOf(
        "hero" to "Streak hero",
        "stats" to "Stats",
        "graph" to "Graph",
        "milestones" to "Milestones",
        "checkin" to "Daily check-in",
        "calendar" to "Calendar",
        "insights" to "Insights",
        "timeline" to "Journey timeline",
        "protection" to "Protection status"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Customize dashboard", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Visibility and order are saved on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                localOrder.forEachIndexed { index, key ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = key !in localHidden,
                            onCheckedChange = { show ->
                                localHidden = if (show) localHidden - key else localHidden + key
                            }
                        )
                        Text(labels[key] ?: key, Modifier.weight(1f).padding(start = 8.dp))
                        IconButton(
                            enabled = index > 0,
                            onClick = {
                                val list = localOrder.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index - 1, item)
                                localOrder = list
                            }
                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                        IconButton(
                            enabled = index < localOrder.lastIndex,
                            onClick = {
                                val list = localOrder.toMutableList()
                                val item = list.removeAt(index)
                                list.add(index + 1, item)
                                localOrder = list
                            }
                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                    }
                }
                TextButton(
                    onClick = {
                        localOrder = SettingsPreferences.defaultDashboardCardOrder()
                        localHidden = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset dashboard layout")
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(localOrder, localHidden) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun MilestoneCelebrationDialog(days: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmPlum, modifier = Modifier.size(42.dp)) },
        title = { Text("$days-day milestone", fontWeight = FontWeight.Black) },
        text = { Text("Milestone recorded. Your tracker history stays exactly as logged.") },
        confirmButton = { Button(onClick = onDismiss) { Text("Nice") } }
    )
}

internal fun buildInsight(
    stats: TrackerStats,
    resets: List<TrackerResetRecord>,
    nowMillis: Long,
    zone: ZoneId
): String? {
    if (resets.size < 3) return null
    val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val thisMonth = resets.filter {
        val d = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        d.year == nowDate.year && d.month == nowDate.month
    }
    val weekendCount = thisMonth.count {
        val d = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        d.dayOfWeek.value >= 6
    }
    if (thisMonth.size >= 3 && weekendCount * 2 > thisMonth.size) {
        return "Most logged resets this month happened on weekends."
    }
    if (stats.averageRecentDays > 0 && stats.currentDays > stats.averageRecentDays) {
        return "Current streak is ${stats.currentDays - stats.averageRecentDays} days longer than your recent average."
    }
    return "Longest streak this year is ${stats.longestThisYear} days."
}

internal fun buildJourneyItems(
    stats: TrackerStats,
    trackerStartedAt: Long,
    resets: List<TrackerResetRecord>,
    goalDays: Int,
    nowMillis: Long,
    zone: ZoneId
): List<JourneyItem> {
    val formatter = DateTimeFormatter.ofPattern("d MMM")
    val items = mutableListOf(
        JourneyItem(nowMillis, "Today · Day ${stats.currentDays}", "Current streak", "today")
    )
    resets.forEach { record ->
        val detail = buildString {
            append(Instant.ofEpochMilli(record.timestamp).atZone(zone).format(formatter))
            record.category?.let { append(" · $it") }
            record.note?.let { append(" · $it") }
        }
        items += JourneyItem(record.timestamp, "Reset logged", detail, "reset")
    }
    milestoneUnlockDates(trackerStartedAt, resets, zone).forEach { (days, date) ->
        items += JourneyItem(
            date.atStartOfDay(zone).toInstant().toEpochMilli(),
            "$days-day milestone",
            date.format(formatter),
            "milestone"
        )
    }
    if (goalDays > 0 && stats.currentDays >= goalDays) {
        items += JourneyItem(nowMillis - 1, "Goal completed", "$goalDays-day goal", "goal")
    }
    return items.sortedByDescending { it.timestamp }
}

internal fun milestoneUnlockDates(
    trackerStartedAt: Long,
    resets: List<TrackerResetRecord>,
    zone: ZoneId
): Map<Int, LocalDate> {
    val milestones = listOf(1,3,7,14,30,60,90,180,365)
    if (trackerStartedAt <= 0L) return emptyMap()
    val result = linkedMapOf<Int, LocalDate>()
    val sorted = resets.sortedBy { it.timestamp }
    var baseline = trackerStartedAt
    val ends = sorted.map { it.timestamp } + System.currentTimeMillis()
    ends.forEach { end ->
        val length = TrackerCalculator.fullDaysBetween(baseline, end)
        milestones.filter { it <= length && it !in result }.forEach { milestone ->
            result[milestone] = Instant.ofEpochMilli(baseline)
                .atZone(zone)
                .toLocalDate()
                .plusDays(milestone.toLong())
        }
        baseline = max(baseline, end)
    }
    return result
}
