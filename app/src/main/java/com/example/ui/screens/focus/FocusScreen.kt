package com.example.ui.screens.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.FocusSessionPreferences
import com.example.data.preferences.FocusSessionSnapshot
import com.example.ui.components.AppSurface
import com.example.ui.components.BunnyCompanion
import com.example.ui.components.BunnyMood
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import kotlin.math.max

@Composable
fun FocusScreen() {
    val context = LocalContext.current
    val preferences = remember(context) { FocusSessionPreferences(context.applicationContext) }
    val snapshot by preferences.snapshotFlow.collectAsState(initial = FocusSessionSnapshot())
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var justCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(snapshot.endAtMillis) {
        if (snapshot.endAtMillis > 0L) {
            while (System.currentTimeMillis() < snapshot.endAtMillis) {
                now = System.currentTimeMillis()
                delay(1_000L)
            }
            now = System.currentTimeMillis()
            justCompleted = preferences.completeIfElapsed()
        }
    }

    val totalMillis = snapshot.selectedMinutes * 60_000L
    val remainingMillis = when {
        snapshot.endAtMillis > 0L -> max(0L, snapshot.endAtMillis - now)
        snapshot.pausedRemainingMillis > 0L -> snapshot.pausedRemainingMillis
        else -> totalMillis
    }
    val progress = (1f - remainingMillis.toFloat() / totalMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, animationSpec = tween(700), label = "focus_progress")
    val minutes = remainingMillis / 60_000L
    val seconds = (remainingMillis % 60_000L) / 1_000L
    val timerText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val isRunning = snapshot.endAtMillis > 0L
    val isPaused = snapshot.isPaused
    val todayEpochDay = LocalDate.now().toEpochDay()
    val visibleStreak = if (snapshot.lastCompletionDay >= todayEpochDay - 1L) snapshot.currentStreak else 0
    val todayIndex = LocalDate.now().dayOfWeek.value - 1
    val currentStreak = if (snapshot.lastCompletionDay >= todayEpochDay - 1L) snapshot.currentStreak else 0
    val mascotMood = when {
        justCompleted -> BunnyMood.Celebrating
        isRunning -> BunnyMood.Focused
        else -> BunnyMood.Resting
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("focus_screen_root")
    ) {
        val isWideLayout = maxWidth >= 900.dp
        val horizontalInset = if (maxWidth >= 720.dp) 32.dp else 20.dp
        val contentWidth = when {
            maxWidth >= 1280.dp -> 1200.dp
            maxWidth >= 900.dp -> maxWidth - 64.dp
            else -> maxWidth - horizontalInset * 2
        }

        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalInset, vertical = 24.dp)
                .width(contentWidth),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Focus", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Choose a stretch of time and let the rest wait",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    TimerPanel(
                        modifier = Modifier.weight(1.2f),
                        snapshot = snapshot,
                        remainingMillis = remainingMillis,
                        progress = animatedProgress,
                        timerText = timerText,
                        isRunning = isRunning,
                        isPaused = isPaused,
                        justCompleted = justCompleted,
                        onSelectMinutes = {
                            scope.launch { preferences.setSelectedMinutes(it) }
                            justCompleted = false
                        },
                        onStart = {
                            scope.launch { preferences.startSession(snapshot.selectedMinutes) }
                            justCompleted = false
                        },
                        onPause = { scope.launch { preferences.pauseSession(remainingMillis) } },
                        onResume = { scope.launch { preferences.resumeSession() } },
                        onReset = {
                            scope.launch { preferences.stopSession() }
                            justCompleted = false
                        }
                    )
                    BunnyPanel(
                        modifier = Modifier.weight(0.8f),
                        mood = mascotMood,
                        sessions = snapshot.completedSessions,
                        minutes = snapshot.totalFocusMinutes,
                        streak = visibleStreak
                    )
                }
            } else {
                TimerPanel(
                    modifier = Modifier.fillMaxWidth(),
                    snapshot = snapshot,
                    remainingMillis = remainingMillis,
                    progress = animatedProgress,
                    timerText = timerText,
                    isRunning = isRunning,
                    isPaused = isPaused,
                    justCompleted = justCompleted,
                    onSelectMinutes = {
                        scope.launch { preferences.setSelectedMinutes(it) }
                        justCompleted = false
                    },
                    onStart = {
                        scope.launch { preferences.startSession(snapshot.selectedMinutes) }
                        justCompleted = false
                    },
                    onPause = { scope.launch { preferences.pauseSession(remainingMillis) } },
                    onResume = { scope.launch { preferences.resumeSession() } },
                    onReset = {
                        scope.launch { preferences.stopSession() }
                        justCompleted = false
                    }
                )
                BunnyPanel(
                    modifier = Modifier.fillMaxWidth(),
                    mood = mascotMood,
                    sessions = snapshot.completedSessions,
                    minutes = snapshot.totalFocusMinutes,
                    streak = visibleStreak
                )
            }

            ProgressPanel(
                snapshot = snapshot,
                todayIndex = todayIndex,
                currentStreak = currentStreak,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TimerPanel(
    modifier: Modifier,
    snapshot: FocusSessionSnapshot,
    remainingMillis: Long,
    progress: Float,
    timerText: String,
    isRunning: Boolean,
    isPaused: Boolean,
    justCompleted: Boolean,
    onSelectMinutes: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    AppSurface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        when {
                            justCompleted -> "Session complete"
                            isRunning -> "Stay with this task"
                            isPaused -> "Your session is paused"
                            else -> "Set your focus time"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        when {
                            justCompleted -> "A full session has been added to your progress"
                            isRunning -> "The timer is saved on this device"
                            isPaused -> "Resume whenever you are ready"
                            else -> "A quiet stretch, one task at a time"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(206.dp),
                    strokeWidth = 8.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        timerText,
                        fontSize = 48.sp,
                        lineHeight = 54.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("focus_timer_value")
                    )
                    Text(
                        when {
                            isRunning -> "remaining"
                            isPaused -> "paused"
                            justCompleted -> "complete"
                            else -> "minutes"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isRunning && !isPaused) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Session length", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60, 90).forEach { value ->
                            FilterChip(
                                selected = snapshot.selectedMinutes == value,
                                onClick = { onSelectMinutes(value) },
                                label = { Text("${value}m") }
                            )
                        }
                    }
                }
            }

            if (justCompleted) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Well done. Your time was recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    isRunning -> {
                        Button(onClick = onPause, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Pause")
                        }
                        OutlinedButton(onClick = onReset) {
                            Icon(Icons.Default.Replay, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("End")
                        }
                    }
                    isPaused -> {
                        Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(7.dp))
                            Text("Resume")
                        }
                        OutlinedButton(onClick = onReset) { Text("Reset") }
                    }
                    else -> {
                        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start focus session")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BunnyPanel(
    modifier: Modifier,
    mood: BunnyMood,
    sessions: Int,
    minutes: Int,
    streak: Int
) {
    AppSurface(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BunnyCompanion(size = 176.dp, mood = mood)
            Text(
                when (mood) {
                    BunnyMood.Celebrating -> "A good stretch of work"
                    BunnyMood.Focused -> "I'll keep the time"
                    BunnyMood.Resting -> "Ready when you are"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                when (mood) {
                    BunnyMood.Celebrating -> "Your effort is now part of your progress"
                    BunnyMood.Focused -> "One task. No need to rush."
                    BunnyMood.Resting -> "Pick a duration, then settle into one task"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FocusMetric("Sessions", sessions.toString())
                FocusMetric("Minutes", minutes.toString())
                FocusMetric("Streak", streak.toString())
            }
        }
    }
}

@Composable
private fun ProgressPanel(
    snapshot: FocusSessionSnapshot,
    todayIndex: Int,
    currentStreak: Int,
    modifier: Modifier = Modifier
) {
    val highestDay = (snapshot.dailyFocusMinutes.maxOrNull() ?: 0).coerceAtLeast(1)
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    AppSurface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Your progress", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Daily focus time, saved on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(snapshot.weeklyFocusMinutes.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    Text("min this week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FocusMetric("Current streak", currentStreak.toString())
                FocusMetric("Best streak", snapshot.bestStreak.toString())
                FocusMetric("Sessions", snapshot.completedSessions.toString())
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(116.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                snapshot.dailyFocusMinutes.forEachIndexed { index, dayMinutes ->
                    val fraction = if (dayMinutes == 0) 0.035f else (dayMinutes.toFloat() / highestDay).coerceAtLeast(0.12f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(dayMinutes.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier.width(22.dp).height(68.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().fillMaxHeight(fraction.coerceIn(0.035f, 1f))
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        if (index == todayIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
                                    )
                            )
                        }
                        Text(
                            labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index == todayIndex) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
