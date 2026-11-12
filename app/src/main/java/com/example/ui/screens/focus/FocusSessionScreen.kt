package com.example.ui.screens.focus

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
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
    var reaction by remember { mutableStateOf("happy_hop") }
    var reactionKey by remember { mutableIntStateOf(1) }
    var companionCaption by remember { mutableStateOf("Pick a duration. I'll keep the study corner cosy.") }

    BackHandler(enabled = hasStarted && !saved && !showEndConfirmation) { showEndConfirmation = true }

    val plannedSeconds = selectedMinutes * 60
    val remainingSeconds = (plannedSeconds - elapsedSeconds).coerceAtLeast(0)
    val progress = if (plannedSeconds > 0) (elapsedSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f) else 0f

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isRunning) {
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
        if (hasStarted && !saved) {
            saved = true
            val record = FocusSessionRecord.create(
                startedAtMillis = sessionStartedAt,
                plannedSeconds = plannedSeconds,
                actualSeconds = elapsedSeconds,
                activity = activity,
                taskName = taskName,
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
                        activity = activity,
                        taskName = taskName,
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
                if (hasStarted && !saved) showEndConfirmation = true else onBack()
            }) { Text("‹  Back") }
            Spacer(Modifier.weight(1f))
            Text("FOCUS ROOM", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }

        if (!hasStarted) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Make space to focus.", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text("Choose a session length and let your companion settle in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text(
                if (showCompletion) "Session complete" else if (isRunning) "One thing at a time." else "Paused. Reset your pace.",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (hasStarted) .30f else .42f)
        ) {
            if (hasStarted) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(116.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
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
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(companionName.ifBlank { characterId.replaceFirstChar(Char::titlecase) },
                            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        AnimatedContent(
                            targetState = companionCaption,
                            transitionSpec = {
                                (fadeIn(tween(220)) + scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = .98f)) togetherWith fadeOut(tween(140))
                            },
                            label = "focus_companion_caption"
                        ) { caption ->
                            Text(caption, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        }
                    }
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface.copy(alpha = .75f)) {
                        Text(if (isRunning) "● LIVE" else if (showCompletion) "✓ DONE" else "Ⅱ PAUSED",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                        CompanionScene(
                            characterId = characterId, modifier = Modifier.fillMaxSize(),
                            interactive = true, reduceMotion = reduceMotion,
                            reaction = reaction, reactionKey = reactionKey,
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
                    Text(companionName.ifBlank { characterId.replaceFirstChar(Char::titlecase) },
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                }
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
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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
                            reaction = "happy_hop"
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
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = .97f)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(activity.uppercase(), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(
                                taskName.ifBlank { "Your focus session" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isRunning) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                if (isRunning) "● LIVE" else if (showCompletion) "✓ DONE" else "Ⅱ PAUSED",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(if (reduceMotion) 0 else 650, easing = FastOutSlowInEasing),
                        label = "focus_progress"
                    )
                    Box(
                        Modifier.fillMaxWidth().height(224.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(212.dp),
                            strokeWidth = 13.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .8f),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                                fontSize = 58.sp,
                                lineHeight = 64.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                if (showCompletion) "COMPLETE" else if (isRunning) "TIME LEFT" else "PAUSED",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(7.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("FOCUSED", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SESSION LENGTH", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "$selectedMinutes min",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showCompletion) {
                            Button(
                                onClick = onBack,
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) { Text("Done") }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    if (!isRunning) {
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
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) { Text(if (isRunning) "Pause" else "Resume") }
                            OutlinedButton(
                                onClick = { showEndConfirmation = true },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) { Text("End session") }
                        }
                    }
                    Text(
                        "Your session is saved on this device and kept separate from streak history.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
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
            "Study", "Revision", "Practice", "Reading" -> sessions.filter { it.activity == filter }
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
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("All", "Completed", "Ended early", "Study", "Revision", "Practice", "Reading").forEach { option ->
                        FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
                    }
                }
                if (visible.isEmpty()) {
                    Text(if (sessions.isEmpty()) "No focus sessions yet. Finish your first session and it will show up here."
                    else "No sessions match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(items = visible, key = { it.id }) { session ->
                            val date = Instant.ofEpochMilli(session.startedAtMillis).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy · hh:mm a", Locale.getDefault()))
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(session.taskName.ifBlank { session.activity }, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Text(if (session.completed) "Completed" else "Ended early",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (session.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${session.activity} · ${session.actualSeconds / 60} min ${session.actualSeconds % 60} sec of ${session.plannedSeconds / 60} min · $date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
