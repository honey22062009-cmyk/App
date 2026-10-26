package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.ProtectionHealth
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassVariant
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {},
    onOpenPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val zone = remember { ZoneId.systemDefault() }

    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val isAdultProtectionEnabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val totalBlocks by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val trackerStartedAt by repository.settings.trackerStartedAtFlow.collectAsState(initial = 0L)
    val lastFapAt by repository.settings.lastFapAtFlow.collectAsState(initial = 0L)
    val previousStreak by repository.settings.previousFapStreakDaysFlow.collectAsState(initial = 0)
    val bestStreak by repository.settings.bestFapStreakDaysFlow.collectAsState(initial = 0)
    val resetCount by repository.settings.fapLogCountFlow.collectAsState(initial = 0)
    val eventTimestamps by repository.settings.fapEventTimestampsFlow.collectAsState(initial = emptySet())

    val activeManagedApps = managedApps.count { it.isManaged }
    var health by remember(context) { mutableStateOf(ProtectionHealth.snapshot(context)) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.settings.ensureStreakTrackerStarted()
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) health = ProtectionHealth.snapshot(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val baseline = when {
        lastFapAt > 0L -> lastFapAt
        trackerStartedAt > 0L -> trackerStartedAt
        else -> System.currentTimeMillis()
    }
    val now = System.currentTimeMillis()
    val currentDays = ((now - baseline).coerceAtLeast(0L) / 86_400_000L).toInt()
    val displayedBest = max(bestStreak, currentDays)
    val ringProgressTarget = if (displayedBest <= 0) 0.08f
        else (currentDays.toFloat() / displayedBest.toFloat()).coerceIn(0.08f, 1f)
    val ringProgress by animateFloatAsState(
        targetValue = ringProgressTarget,
        animationSpec = tween(650),
        label = "streak_ring"
    )

    val lastLabel = if (lastFapAt > 0L) {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault())
        Instant.ofEpochMilli(lastFapAt).atZone(zone).format(formatter)
    } else {
        "No reset logged"
    }

    val today = LocalDate.now(zone)
    val weekly = remember(eventTimestamps, today) {
        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val count = eventTimestamps.count {
                Instant.ofEpochMilli(it).atZone(zone).toLocalDate() == date
            }
            date to count
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val tablet = maxWidth >= 720.dp
        val contentWidth = maxWidth

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(contentWidth)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = if (tablet) 22.dp else 12.dp,
                end = if (tablet) 22.dp else 12.dp,
                top = 18.dp,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Your progress",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "A private, user-logged streak with protection tools underneath.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(12.dp, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE45B99), Color(0xFFE36DAE))
                                ),
                                CircleShape
                            )
                            .clickable(onClick = onOpenPro),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Strict Blocker Pro",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "PRO",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    variant = GlassVariant.Accent,
                    shape = RoundedCornerShape(28.dp),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "CURRENT STREAK",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (tablet) 2.5f else 1.45f),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(if (tablet) 260.dp else 220.dp)) {
                                val stroke = size.minDimension * 0.052f
                                val inset = stroke
                                drawArc(
                                    color = Color.White.copy(alpha = 0.10f),
                                    startAngle = 140f,
                                    sweepAngle = 260f,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - inset * 2, size.height - inset * 2),
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFE45B99),
                                            Color(0xFFF4866B),
                                            Color(0xFFE76AAE),
                                            Color(0xFFE45B99)
                                        )
                                    ),
                                    startAngle = 140f,
                                    sweepAngle = 260f * ringProgress,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = Size(size.width - inset * 2, size.height - inset * 2),
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HumanPairIllustration()
                                Spacer(Modifier.height(4.dp))
                                AnimatedContent(
                                    targetState = currentDays,
                                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                                    label = "streak_days"
                                ) { days ->
                                    Text(
                                        text = "$days",
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    if (currentDays == 1) "day" else "days",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            "Last reset: $lastLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { showResetConfirm = true },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("Record reset", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "This tracker only changes when you log a reset. It does not monitor your body or private activity.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("Previous", "$previousStreak d", Color(0xFFA953C7), Modifier.weight(1f))
                    MetricCard("Best", "$displayedBest d", Color(0xFFE46FAE), Modifier.weight(1f))
                    MetricCard("Resets", "$resetCount", Color(0xFF62B49A), Modifier.weight(1f))
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Last 7 days",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Logged reset trend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "${weekly.sumOf { it.second }} logged",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        StreakLineGraph(
                            values = weekly.map { it.second },
                            labels = weekly.map { it.first.dayOfWeek.name.take(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (tablet) 176.dp else 145.dp)
                        )
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isProtectionEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                when {
                                    !isProtectionEnabled -> "Protection paused"
                                    !health.coreReady -> "Protection needs setup"
                                    health.reliabilityScore < 100 -> "Protection active · ${health.reliabilityScore}%"
                                    else -> "Protection active"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$activeManagedApps restricted apps · $totalBlocks blocks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LiquidToggle(
                            checked = isProtectionEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { repository.settings.setProtectionEnabled(enabled) }
                            }
                        )
                    }
                }
            }

            if (!health.coreReady || health.reliabilityScore < 100) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToPermissions),
                        variant = GlassVariant.Warning,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (!health.coreReady) "Finish protection setup"
                                    else "Improve background reliability",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    health.degradedReasons.firstOrNull()
                                        ?: "Review Android permissions.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    "Record a reset?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This ends the current streak at $currentDays day${if (currentDays == 1) "" else "s"} and starts a new one now. Only log it if that is what you want recorded."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.settings.recordFapReset() }
                        showResetConfirm = false
                    }
                ) {
                    Text("Record")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HumanPairIllustration() {
    Canvas(modifier = Modifier.size(width = 124.dp, height = 96.dp)) {
        fun figure(
            centerX: Float,
            main: Color,
            light: Color,
            bodyWidth: Float
        ) {
            val headY = size.height * 0.18f
            val headR = size.minDimension * 0.11f
            drawCircle(
                color = Color.Black.copy(alpha = 0.20f),
                radius = headR + 2f,
                center = Offset(centerX + 3f, headY + 3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(light, main),
                    center = Offset(centerX - 4f, headY - 4f),
                    radius = headR * 2.2f
                ),
                radius = headR,
                center = Offset(centerX, headY)
            )

            val torsoTop = size.height * 0.34f
            val torsoHeight = size.height * 0.34f
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(centerX - bodyWidth / 2f + 3f, torsoTop + 4f),
                size = Size(bodyWidth, torsoHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
            drawRoundRect(
                brush = Brush.linearGradient(listOf(light, main, main.copy(alpha = 0.9f))),
                topLeft = Offset(centerX - bodyWidth / 2f, torsoTop),
                size = Size(bodyWidth, torsoHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )

            val limb = size.width * 0.055f
            drawLine(
                brush = Brush.linearGradient(listOf(light, main)),
                start = Offset(centerX - bodyWidth * 0.32f, torsoTop + 8f),
                end = Offset(centerX - bodyWidth * 0.80f, size.height * 0.58f),
                strokeWidth = limb,
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.linearGradient(listOf(light, main)),
                start = Offset(centerX + bodyWidth * 0.32f, torsoTop + 8f),
                end = Offset(centerX + bodyWidth * 0.80f, size.height * 0.58f),
                strokeWidth = limb,
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.linearGradient(listOf(light, main)),
                start = Offset(centerX - bodyWidth * 0.20f, torsoTop + torsoHeight - 2f),
                end = Offset(centerX - bodyWidth * 0.34f, size.height * 0.91f),
                strokeWidth = limb * 1.15f,
                cap = StrokeCap.Round
            )
            drawLine(
                brush = Brush.linearGradient(listOf(light, main)),
                start = Offset(centerX + bodyWidth * 0.20f, torsoTop + torsoHeight - 2f),
                end = Offset(centerX + bodyWidth * 0.34f, size.height * 0.91f),
                strokeWidth = limb * 1.15f,
                cap = StrokeCap.Round
            )
        }

        figure(
            centerX = size.width * 0.34f,
            main = Color(0xFF9A4ABA),
            light = Color(0xFFE0A4F3),
            bodyWidth = size.width * 0.24f
        )
        figure(
            centerX = size.width * 0.66f,
            main = Color(0xFFE85D98),
            light = Color(0xFFFFB1D0),
            bodyWidth = size.width * 0.23f
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(accent, CircleShape)
            )
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StreakLineGraph(
    values: List<Int>,
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    val maxValue = max(1, values.maxOrNull() ?: 1)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (values.isEmpty()) return@Canvas

            val left = 10f
            val right = size.width - 10f
            val top = 12f
            val bottom = size.height - 12f
            val stepX = if (values.size <= 1) 0f else (right - left) / (values.size - 1)

            val points = values.mapIndexed { index, value ->
                Offset(
                    x = left + stepX * index,
                    y = bottom - (value.toFloat() / maxValue.toFloat()) * (bottom - top)
                )
            }

            val fill = Path().apply {
                moveTo(points.first().x, bottom)
                lineTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, bottom)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFE45B99).copy(alpha = 0.30f),
                        Color(0xFFF4866B).copy(alpha = 0.08f),
                        Color.Transparent
                    )
                )
            )

            val line = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = line,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFE45B99),
                        Color(0xFFF4866B),
                        Color(0xFFA953C7)
                    )
                ),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            points.forEach { point ->
                drawCircle(Color.White.copy(alpha = 0.92f), 7f, point)
                drawCircle(Color(0xFFE45B99), 4f, point)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 27.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "liquid_toggle"
    )

    Box(
        modifier = Modifier
            .width(57.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (checked) {
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFE45B99),
                            Color(0xFFF4866B),
                            Color(0xFFA953C7)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            )
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(28.dp)
                .shadow(7.dp, CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color.White, Color(0xFFFFEEF5))
                    ),
                    CircleShape
                )
        )
    }
}
