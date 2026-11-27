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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.FocusSessionPreferences
import com.example.data.preferences.FocusSessionSnapshot
import com.example.data.preferences.SettingsPreferences
import com.example.ui.components.AppSurface
import com.example.ui.components.StrictBlockerLogo
import com.example.ui.components.CompanionScene
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
    val settings = remember(context) { SettingsPreferences(context.applicationContext) }
    val dailyGoal by settings.focusDailyGoalFlow.collectAsState(initial = 120)
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
                        onTaskChange = { value -> scope.launch { preferences.setSelectedTask(value) } },
                        onCategoryChange = { value -> scope.launch { preferences.setActivityCategory(value) } },
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
                    onTaskChange = { value -> scope.launch { preferences.setSelectedTask(value) } },
                    onCategoryChange = { value -> scope.launch { preferences.setActivityCategory(value) } },
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
                dailyGoal = dailyGoal,
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
    onTaskChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    AppSurface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        when {
                            justCompleted -> "Session complete"
                            isRunning -> "You're in focus mode"
                            isPaused -> "Focus is paused"
                            else -> "Build your focus session"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        when {
                            justCompleted -> "Your completed time has been saved"
                            isRunning -> "Stay with one task. The timer is saved locally."
                            isPaused -> "Pick up where you left off"
                            else -> "A calm workspace for one thing at a time"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("YOUR TASK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = snapshot.selectedTask,
                    onValueChange = onTaskChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g. Biology · Human Physiology") },
                    enabled = !isRunning && !isPaused,
                    shape = RoundedCornerShape(16.dp)
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Study", "Revision", "Practice", "Reading", "Planning").forEach { category ->
                        FilterChip(
                            selected = snapshot.activityCategory == category,
                            onClick = { onCategoryChange(category) },
                            enabled = !isRunning && !isPaused,
                            label = { Text(category) }
                        )
                    }
                }
            }

            FocusTimerVisual(
                progress = progress,
                timerText = timerText,
                status = when {
                    isRunning -> "TIME REMAINING"
                    isPaused -> "PAUSED"
                    justCompleted -> "COMPLETED"
                    else -> "MINUTES"
                },
                compact = false
            )

            if (!isRunning && !isPaused) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SESSION LENGTH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.weight(1f))
                        Text("${snapshot.selectedMinutes} minutes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(15, 25, 45, 60, 90).forEach { value ->
                            FilterChip(
                                selected = snapshot.selectedMinutes == value,
                                onClick = { onSelectMinutes(value) },
                                label = { Text("${value} min") }
                            )
                        }
                    }
                }
            }

            if (justCompleted) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "Nicely done. Your focus time is in your history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            when {
                isRunning -> {
                    Button(onClick = onPause, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pause session")
                    }
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.Replay, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text("End session")
                    }
                }
                isPaused -> {
                    Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Resume session")
                    }
                    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("End session")
                    }
                }
                else -> {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(9.dp))
                        Text("Start focus session", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusTimerVisual(
    progress: Float,
    timerText: String,
    status: String,
    compact: Boolean
) {
    val ringSize = if (compact) 224.dp else 252.dp
    Box(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(ringSize).clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(ringSize - 16.dp),
                strokeWidth = 11.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    timerText,
                    fontSize = if (compact) 42.sp else 50.sp,
                    lineHeight = 56.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("focus_timer_value")
                )
                Text("one thing at a time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            CompanionScene(
                characterId = "luna",
                modifier = Modifier.size(176.dp),
                interactive = true,
                reduceMotion = false,
                reaction = when (mood) {
                    BunnyMood.Resting -> "idle"
                    BunnyMood.Focused -> "look_around"
                    BunnyMood.Celebrating -> "jump"
                    BunnyMood.Running -> "dance"
                },
                reactionKey = when (mood) {
                    BunnyMood.Resting -> 1
                    BunnyMood.Focused -> 2
                    BunnyMood.Celebrating -> 3
                    BunnyMood.Running -> 4
                }
            )
            Text(
                when (mood) {
                    BunnyMood.Celebrating -> "A good stretch of work"
                    BunnyMood.Focused -> "I'll keep the time"
                    BunnyMood.Running -> "Getting things ready"
                    BunnyMood.Resting -> "Ready when you are"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                when (mood) {
                    BunnyMood.Celebrating -> "Your effort is now part of your progress"
                    BunnyMood.Focused -> "One task. No need to rush."
                    BunnyMood.Running -> "A fresh session is starting"
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
    dailyGoal: Int,
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily target", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("A gentle goal, not a deadline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("${snapshot.dailyFocusMinutes.getOrNull(todayIndex) ?: 0}/$dailyGoal min", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                LinearProgressIndicator(
                    progress = { ((snapshot.dailyFocusMinutes.getOrNull(todayIndex) ?: 0).toFloat() / dailyGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

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
