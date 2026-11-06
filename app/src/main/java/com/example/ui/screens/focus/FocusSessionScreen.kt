package com.example.ui.screens.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.preferences.FocusSessionRecord
import com.example.data.preferences.SettingsPreferences
import com.example.ui.components.CompanionBehavior
import com.example.ui.components.CompanionScene
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val focusActivities = listOf("Study", "Revision", "Practice", "Reading")

@Composable
fun FocusSessionScreen(
    settings: SettingsPreferences,
    characterId: String,
    companionName: String,
    reduceMotion: Boolean,
    onBack: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var selectedMinutes by rememberSaveable { mutableIntStateOf(25) }
    var activity by rememberSaveable { mutableStateOf("Study") }
    var taskName by rememberSaveable { mutableStateOf("") }
    var elapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var sessionStartedAt by rememberSaveable { mutableLongStateOf(0L) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var hasStarted by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var showEndConfirmation by remember { mutableStateOf(false) }
    var showCompletion by rememberSaveable { mutableStateOf(false) }
    var reaction by remember { mutableStateOf("focus") }
    var reactionKey by remember { mutableIntStateOf(1) }
    var companionCaption by remember { mutableStateOf("Pick a duration. I'll keep the study corner cosy.") }

    val plannedSeconds = selectedMinutes * 60
    val remainingSeconds = (plannedSeconds - elapsedSeconds).coerceAtLeast(0)
    val progress = if (plannedSeconds > 0) (elapsedSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f) else 0f

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && isRunning) {
                isRunning = false
                companionCaption = "Paused while you were away. Resume when you're ready."
                reaction = "listen"
                reactionKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun persistSession(completed: Boolean, leaveScreen: Boolean) {
        isRunning = false
        if (hasStarted && !saved && elapsedSeconds > 0) {
            saved = true
            val record = FocusSessionRecord.create(
                startedAtMillis = sessionStartedAt,
                plannedSeconds = plannedSeconds,
                actualSeconds = elapsedSeconds,
                activity = taskName.trim().ifBlank { activity },
                completed = completed
            )
            scope.launch {
                settings.saveFocusSession(record)
                if (leaveScreen) onBack()
            }
        } else if (leaveScreen) {
            onBack()
        }
    }

    LaunchedEffect(isRunning, plannedSeconds) {
        while (isRunning && elapsedSeconds < plannedSeconds) {
            delay(1_000L)
            if (isRunning) elapsedSeconds = (elapsedSeconds + 1).coerceAtMost(plannedSeconds)
        }
        if (isRunning && elapsedSeconds >= plannedSeconds) {
            isRunning = false
            showCompletion = true
            companionCaption = "Session complete. You stayed with it. That's worth celebrating."
            reaction = "celebrate"
            reactionKey++
            if (!saved && sessionStartedAt > 0L) {
                saved = true
                settings.saveFocusSession(
                    FocusSessionRecord.create(
                        startedAtMillis = sessionStartedAt,
                        plannedSeconds = plannedSeconds,
                        actualSeconds = elapsedSeconds,
                        activity = taskName.trim().ifBlank { activity },
                        completed = true
                    )
                )
            }
        }
    }

    LaunchedEffect(isRunning, reduceMotion) {
        if (!isRunning || reduceMotion) return@LaunchedEffect
        while (isRunning) {
            delay(35_000L)
            if (isRunning) {
                val cue = CompanionBehavior.nextIdleCue()
                reaction = cue.action
                reactionKey++
                companionCaption = cue.line
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (hasStarted && elapsedSeconds > 0 && !saved) showEndConfirmation = true else onBack()
            }) { Text("‹  Back") }
            Spacer(Modifier.weight(1f))
            Text("FOCUS ROOM", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                when {
                    showCompletion -> "You did it."
                    isRunning -> "One thing at a time."
                    hasStarted -> "Take your next step."
                    else -> "Make space to focus."
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                when {
                    showCompletion -> "Your session has been added to Progress."
                    isRunning -> "Stay with the task. Your companion will keep you company."
                    hasStarted -> "Your session is paused. Continue whenever you're ready."
                    else -> "Choose a session length and let your companion settle in."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .42f)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                    CompanionScene(
                        characterId = characterId,
                        modifier = Modifier.fillMaxSize(),
                        interactive = true,
                        reduceMotion = reduceMotion,
                        reaction = reaction,
                        reactionKey = reactionKey,
                        backgroundColor = androidx.compose.ui.graphics.Color.Transparent,
                        onPet = {
                            reaction = "pet"
                            reactionKey++
                            companionCaption = CompanionBehavior.dialogue("pet")
                        }
                    )
                }
                AnimatedContent(
                    targetState = companionCaption,
                    transitionSpec = {
                        (fadeIn(tween(220)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = .98f)) togetherWith fadeOut(tween(140))
                    },
                    label = "focus_companion_caption"
                ) { caption ->
                    Text(caption, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                }
                Text(
                    companionName.ifBlank { characterId.replaceFirstChar(Char::titlecase) },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!hasStarted) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("HOW LONG?", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 25, 45, 60).forEach { minutes ->
                            FilterChip(
                                selected = selectedMinutes == minutes,
                                onClick = { selectedMinutes = minutes },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                    Text("Custom length · $selectedMinutes minutes", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = (it / 5f).roundToInt() * 5 },
                        valueRange = 10f..120f,
                        steps = 21
                    )
                    Text("WHAT ARE YOU WORKING ON?", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        focusActivities.forEach { option ->
                            FilterChip(
                                selected = activity == option,
                                onClick = { activity = option },
                                label = { Text(option) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = taskName,
                        onValueChange = { taskName = it.take(60) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Task (optional)") },
                        placeholder = { Text("e.g. Biology: Human Physiology") }
                    )
                    Text("A short, realistic session is a perfectly valid start.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            hasStarted = true
                            saved = false
                            elapsedSeconds = 0
                            sessionStartedAt = System.currentTimeMillis()
                            isRunning = true
                            showCompletion = false
                            reaction = "focus"
                            reactionKey++
                            companionCaption = "I'm settling in. You take the first step."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start $selectedMinutes-minute session") }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(activity.uppercase(), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(taskName.ifBlank { "Your focus session" },
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Box(contentAlignment = Alignment.Center) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = tween(if (reduceMotion) 0 else 700, easing = FastOutSlowInEasing),
                                label = "focus_progress"
                            )
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(86.dp),
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        if (isRunning) "FOCUSING · ${elapsedSeconds / 60} MIN ${elapsedSeconds % 60} SEC ELAPSED"
                        else if (showCompletion) "SESSION COMPLETE"
                        else "PAUSED · ${elapsedSeconds / 60} MIN ${elapsedSeconds % 60} SEC COMPLETED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (showCompletion) {
                                    onBack()
                                } else if (!isRunning) {
                                    isRunning = true
                                    companionCaption = "Welcome back. Pick up right where you paused."
                                    reaction = "wave"
                                    reactionKey++
                                } else {
                                    isRunning = false
                                    companionCaption = "A pause is allowed. Your session is safe."
                                    reaction = "rest"
                                    reactionKey++
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(if (showCompletion) "Done" else if (isRunning) "Pause" else "Resume") }
                        if (!showCompletion) {
                            OutlinedButton(onClick = { showEndConfirmation = true }) { Text("End session") }
                        }
                    }
                    Text("Your time is saved on this device and appears in Progress, separately from streak history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (!hasStarted) {
            Text("Your session history will appear in Progress after you finish or end a session.",
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showEndConfirmation) {
        AlertDialog(
            onDismissRequest = { showEndConfirmation = false },
            title = { Text("End this session?") },
            text = { Text("The time you've completed will be saved as an ended-early session in Progress.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirmation = false
                    persistSession(completed = false, leaveScreen = true)
                }) { Text("Save and exit") }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirmation = false }) { Text("Keep focusing") }
            }
        )
    }
}

@Composable
fun FocusSessionsProgressCard(
    sessions: List<FocusSessionRecord>,
    onOpenHistory: () -> Unit
) {
    val totalMinutes = sessions.sumOf { it.actualSeconds } / 60
    val completed = sessions.count { it.completed }
    val zone = ZoneId.systemDefault()
    val weekStart = LocalDate.now(zone).with(java.time.DayOfWeek.MONDAY)
    val weekSessions = sessions.count {
        Instant.ofEpochMilli(it.startedAtMillis).atZone(zone).toLocalDate() >= weekStart
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpenHistory),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .42f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("FOCUS SESSION HISTORY", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Your dedicated study-time record", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusMetric("Sessions", sessions.size.toString(), Modifier.weight(1f))
                FocusMetric("Completed", completed.toString(), Modifier.weight(1f))
                FocusMetric("Minutes", totalMinutes.toString(), Modifier.weight(1f))
            }
            Text("$weekSessions sessions this week · Tap to open the full history",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FocusMetric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .75f)) {
        Column(Modifier.padding(11.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FocusSessionHistoryDialog(
    sessions: List<FocusSessionRecord>,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    val visible = remember(sessions, filter) {
        when (filter) {
            "Completed" -> sessions.filter { it.completed }
            "Ended early" -> sessions.filterNot { it.completed }
            else -> sessions
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Focus history", fontWeight = FontWeight.Bold)
                Text("${sessions.size} saved sessions · separate from streak history",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("All", "Completed", "Ended early").forEach { option ->
                        FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                    }
                }
                if (visible.isEmpty()) {
                    Text(if (sessions.isEmpty()) "No focus sessions yet. Finish your first session and it will show up here."
                    else "No sessions match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(
                        modifier = Modifier.height(340.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        visible.take(100).forEachIndexed { index, session ->
                            val date = Instant.ofEpochMilli(session.startedAtMillis).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy · hh:mm a", Locale.getDefault()))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(session.activity, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Text(if (session.completed) "Completed" else "Ended early",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (session.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${session.actualSeconds / 60} min ${session.actualSeconds % 60} sec of ${session.plannedSeconds / 60} min · $date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (index != visible.take(100).lastIndex) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
