package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.ProtectionHealth
import com.example.core.system.ProtectionHealthSnapshot
import com.example.data.preferences.DailyCheckIn
import com.example.data.preferences.TrackerCalculator
import com.example.data.preferences.TrackerResetRecord
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassVariant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun buildTabletDashboardRows(cards: List<String>): List<List<String>> {
    val fullWidthCards = setOf("hero", "checkin", "graph", "stats", "protection")
    val rows = mutableListOf<List<String>>()
    var index = 0
    while (index < cards.size) {
        val current = cards[index]
        if (current in fullWidthCards) {
            rows += listOf(current)
            index++
        } else {
            val next = cards.getOrNull(index + 1)
            if (next != null && next !in fullWidthCards) {
                rows += listOf(current, next)
                index += 2
            } else {
                rows += listOf(current)
                index++
            }
        }
    }
    return rows
}

@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    initialTrackerAction: String? = null,
    progressOnly: Boolean = false,
    onDataEntryVisibilityChanged: (Boolean) -> Unit = {},
    onTrackerActionConsumed: () -> Unit = {},
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {},
    onOpenPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val zone = remember { ZoneId.systemDefault() }

    val trackerStartedAt by repository.settings.trackerStartedAtFlow.collectAsState(initial = 0L)
    val resetRecords by repository.settings.trackerResetRecordsFlow.collectAsState(initial = emptyList())
    val checkIns by repository.settings.dailyCheckInsFlow.collectAsState(initial = emptyList())
    val sleepEntries by repository.settings.sleepEntriesFlow.collectAsState(initial = emptyList())
    val goalDays by repository.settings.trackerGoalDaysFlow.collectAsState(initial = 0)
    val cardOrder by repository.settings.dashboardCardOrderFlow.collectAsState(
        initial = com.example.data.preferences.SettingsPreferences.defaultDashboardCardOrder()
    )
    val hiddenCards by repository.settings.dashboardHiddenCardsFlow.collectAsState(initial = emptySet())
    val seenMilestones by repository.settings.seenMilestonesFlow.collectAsState(initial = emptySet())
    val reduceMotion by repository.settings.reduceMotionFlow.collectAsState(initial = false)
    val userName by repository.settings.userNameFlow.collectAsState(initial = "")
    val companionName by repository.settings.characterNameFlow.collectAsState(initial = "")
    val selectedCharacterId by repository.settings.selectedCharacterIdFlow.collectAsState(initial = "nova")
    val isPro by repository.settings.isProEntitledFlow.collectAsState(initial = false)
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val totalBlocks by repository.totalBlocksCountFlow.collectAsState(initial = 0)

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var health by remember(context) { mutableStateOf(ProtectionHealth.snapshot(context)) }
    var showQuickEntry by remember { mutableStateOf(false) }
    var showResetEditor by remember { mutableStateOf(false) }
    var showSleepEditor by remember { mutableStateOf(false) }
    var showDailyPrompt by remember { mutableStateOf(false) }
    var editingReset by remember { mutableStateOf<TrackerResetRecord?>(null) }
    var deleteCandidate by remember { mutableStateOf<TrackerResetRecord?>(null) }
    var showGoal by remember { mutableStateOf(false) }
    var showMilestones by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }
    var showCustomization by remember { mutableStateOf(false) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }
    var celebrationMilestone by remember { mutableStateOf<Int?>(null) }

    val entryVisible = showQuickEntry || showResetEditor || showSleepEditor || showDailyPrompt || showGoal
    LaunchedEffect(entryVisible) { onDataEntryVisibilityChanged(entryVisible) }
    DisposableEffect(Unit) { onDispose { onDataEntryVisibilityChanged(false) } }

    LaunchedEffect(Unit) {
        repository.settings.ensureStreakTrackerStarted()
        repository.settings.refreshExpiredProEntitlement()
    }

    LaunchedEffect(initialTrackerAction) {
        when (initialTrackerAction) {
            "sleep" -> showSleepEditor = true
            "daily" -> showDailyPrompt = true
        }
        if (initialTrackerAction != null) onTrackerActionConsumed()
    }


    // Refresh day-based tracker values at local midnight without running a per-second timer.
    LaunchedEffect(zone) {
        while (true) {
            val current = System.currentTimeMillis()
            val nextMidnight = Instant.ofEpochMilli(current)
                .atZone(zone)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zone)
                .toInstant()
                .toEpochMilli()
            delay((nextMidnight - current + 1_000L).coerceAtLeast(1_000L))
            nowMillis = System.currentTimeMillis()
        }
    }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                nowMillis = System.currentTimeMillis()
                health = ProtectionHealth.snapshot(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val stats = remember(trackerStartedAt, resetRecords, nowMillis, zone) {
        TrackerCalculator.stats(trackerStartedAt, resetRecords, nowMillis, zone)
    }
    val latestReset = resetRecords.maxByOrNull { it.timestamp }
    val lastResetLabel = remember(latestReset, zone) {
        latestReset?.let {
            Instant.ofEpochMilli(it.timestamp).atZone(zone)
                .format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault()))
        } ?: "No reset logged"
    }
    val today = remember(nowMillis, zone) { Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate() }
    val todayCheckIn = remember(checkIns, today) { checkIns.firstOrNull { it.localDate == today } }
    val todaySleep = remember(sleepEntries, today) { sleepEntries.firstOrNull { it.localDate == today } }
    val insight = remember(stats, resetRecords, nowMillis, zone) {
        buildInsight(stats, resetRecords, nowMillis, zone)
    }
    val journeyItems = remember(stats, trackerStartedAt, resetRecords, goalDays, nowMillis, zone) {
        buildJourneyItems(stats, trackerStartedAt, resetRecords, goalDays, nowMillis, zone)
    }

    LaunchedEffect(stats.currentDays, seenMilestones) {
        val milestone = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)
            .lastOrNull { stats.currentDays >= it && it !in seenMilestones }
        if (milestone != null) {
            celebrationMilestone = milestone
            repository.settings.markMilestoneSeen(milestone)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val tablet = maxWidth >= 720.dp
        val progressKeys = setOf("stats", "graph", "calendar", "timeline", "milestones", "insights")
        val visibleCardOrder = if (progressOnly) cardOrder.filter { it in progressKeys && it !in hiddenCards }
            else listOf("protection", "hero")
        val tabletRows = remember(visibleCardOrder) {
            buildTabletDashboardRows(visibleCardOrder)
        }
        val renderDashboardCard: @Composable (String) -> Unit = { key ->
                    when (key) {
                        "hero" -> StreakHeroCard(
                            stats = stats,
                            characterId = selectedCharacterId,
                            companionName = companionName,
                            onRecordToday = { showQuickEntry = true },
                            lastResetLabel = lastResetLabel,
                            goalDays = goalDays,
                            reduceMotion = reduceMotion,
                            tablet = tablet,
                            onRecordReset = {
                                editingReset = null
                                showResetEditor = true
                            },
                            onEditGoal = { showGoal = true }
                        )
    
                        "stats" -> StatsStrip(stats, tablet)
    
                        "graph" -> StreakGraphCard(
                            trackerStartedAt = trackerStartedAt,
                            resets = resetRecords,
                            zone = zone,
                            tablet = tablet
                        )
    
                        "milestones" -> MilestoneCard(
                            stats = stats,
                            onViewAll = { showMilestones = true }
                        )
    
                        "checkin" -> DailyCheckInCard(
                            currentDays = stats.currentDays,
                            todayCheckIn = todayCheckIn,
                            todaySleep = todaySleep,
                            onOpenCheckIn = { showDailyPrompt = true },
                            onRecordReset = {
                                editingReset = null
                                showResetEditor = true
                            },
                            onAddSleep = { showSleepEditor = true }
                        )

                        "calendar" -> CalendarHeatmapCard(
                            trackerStartedAt = trackerStartedAt,
                            resets = resetRecords,
                            checkIns = checkIns,
                            zone = zone,
                            onDateSelected = { selectedCalendarDate = it }
                        )
    
                        "insights" -> InsightCard(insight)
    
                        "timeline" -> JourneyPreviewCard(
                            items = journeyItems,
                            onOpen = { showTimeline = true }
                        )
    
                        "protection" -> ProtectionStatusCard(
                            enabled = isProtectionEnabled,
                            health = health,
                            activeManagedApps = managedApps.count { it.isManaged },
                            totalBlocks = totalBlocks,
                            onToggle = { enabled ->
                                scope.launch { repository.settings.setProtectionEnabled(enabled) }
                            },
                            onOpenSetup = onNavigateToPermissions
                        )
                    }
        }



        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = if (tablet) 22.dp else 12.dp,
                end = if (tablet) 22.dp else 12.dp,
                top = 18.dp,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (progressOnly) "Your progress" else if (userName.isBlank()) "Your day" else "Hi, ${userName.trim()}",
                        style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text(if (progressOnly) "History, patterns and milestones" else "A little focus. A better day.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (progressOnly) TextButton(onClick = {
                        if (isPro) showCustomization = true else onOpenPro()
                    }) { Text("Customize progress") }
                }
            }
            if (!progressOnly) item(key = "shortcuts") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onNavigateToApps, modifier = Modifier.weight(1f)) { Text("Block apps") }
                    TextButton(onClick = onNavigateToSchedules, modifier = Modifier.weight(1f)) { Text("Schedules") }
                    TextButton(onClick = onNavigateToAdultProtection, modifier = Modifier.weight(1f)) { Text("18+ protection") }
                }
            }

            if (tablet) {
                items(
                    items = tabletRows,
                    key = { row -> row.joinToString("+") }
                ) { row ->
                    if (row.size == 1) {
                        renderDashboardCard(row.single())
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            row.forEach { key ->
                                val cardWeight = if ("calendar" in row) {
                                    if (key == "calendar") 1.25f else 0.75f
                                } else 1f
                                Box(modifier = Modifier.weight(cardWeight)) {
                                    renderDashboardCard(key)
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = visibleCardOrder,
                    key = { it }
                ) { key ->
                    renderDashboardCard(key)
                }
            }
        }
    }

    if (showQuickEntry) {
        QuickRecordSheet(
            current = todayCheckIn,
            currentSleep = todaySleep,
            onDismiss = { showQuickEntry = false },
            onReset = { showQuickEntry = false; editingReset = null; showResetEditor = true },
            onSave = { rating, sleep ->
                repository.settings.saveDailyEntry(today.toString(), rating, sleep)
                showQuickEntry = false
            }
        )
    }

    if (showDailyPrompt) {
        DailyCheckInPromptDialog(
            current = todayCheckIn,
            onDismiss = { showDailyPrompt = false },
            onSelect = { rating ->
                scope.launch {
                    repository.settings.saveDailyCheckIn(today.toString(), rating)
                }
                showDailyPrompt = false
            },
            onRecordReset = {
                showDailyPrompt = false
                editingReset = null
                showResetEditor = true
            }
        )
    }

    if (showSleepEditor) {
        SleepEntryDialog(
            current = todaySleep,
            onDismiss = { showSleepEditor = false },
            onSave = { minutes, note ->
                scope.launch {
                    repository.settings.saveSleepEntry(today.toString(), minutes, note)
                }
                showSleepEditor = false
            }
        )
    }

    if (showResetEditor) {
        ResetEditorDialog(
            editing = editingReset,
            currentDays = stats.currentDays,
            onDismiss = {
                showResetEditor = false
                editingReset = null
            },
            onSave = { timestamp, category, note ->
                val existing = editingReset
                scope.launch {
                    if (existing == null) {
                        repository.settings.recordTrackerReset(timestamp, category, note)
                    } else {
                        repository.settings.updateTrackerReset(existing.id, timestamp, category, note)
                    }
                }
                showResetEditor = false
                editingReset = null
                nowMillis = System.currentTimeMillis()
            }
        )
    }

    if (showGoal) {
        GoalDialog(
            currentGoal = goalDays,
            onDismiss = { showGoal = false },
            onSave = { days ->
                scope.launch { repository.settings.setTrackerGoalDays(days) }
                showGoal = false
            }
        )
    }

    if (showMilestones) {
        MilestonesDialog(
            stats = stats,
            trackerStartedAt = trackerStartedAt,
            resets = resetRecords,
            zone = zone,
            onDismiss = { showMilestones = false }
        )
    }

    if (showTimeline) {
        TimelineDialog(
            items = journeyItems,
            resets = resetRecords,
            onDismiss = { showTimeline = false },
            onEdit = { record ->
                showTimeline = false
                editingReset = record
                showResetEditor = true
            },
            onDelete = { record -> deleteCandidate = record }
        )
    }

    deleteCandidate?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
            title = { Text("Delete this reset?", fontWeight = FontWeight.Bold) },
            text = { Text("The tracker will recalculate streak history after this entry is removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { repository.settings.deleteTrackerReset(record.id) }
                        deleteCandidate = null
                        nowMillis = System.currentTimeMillis()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
    }

    selectedCalendarDate?.let { date ->
        CalendarDetailDialog(
            date = date,
            streakDay = TrackerCalculator.streakAt(date, trackerStartedAt, resetRecords, zone),
            resets = resetRecords,
            checkIn = checkIns.firstOrNull { it.localDate == date },
            zone = zone,
            onDismiss = { selectedCalendarDate = null }
        )
    }

    if (showCustomization) {
        DashboardCustomizationDialog(
            order = cardOrder.filter { it in setOf("stats", "graph", "calendar", "timeline", "milestones", "insights") },
            hidden = hiddenCards,
            onDismiss = { showCustomization = false },
            onSave = { order, hidden ->
                scope.launch { repository.settings.setDashboardLayout(order, hidden) }
                showCustomization = false
            }
        )
    }

    celebrationMilestone?.let { days ->
        MilestoneCelebrationDialog(
            days = days,
            onDismiss = { celebrationMilestone = null }
        )
    }
}

@Composable
private fun ProtectionStatusCard(
    enabled: Boolean,
    health: ProtectionHealthSnapshot,
    activeManagedApps: Int,
    totalBlocks: Int,
    onToggle: (Boolean) -> Unit,
    onOpenSetup: () -> Unit
) {
    val needsAttention = !health.coreReady || health.reliabilityScore < 100
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = needsAttention, onClick = onOpenSetup),
        variant = if (needsAttention) GlassVariant.Warning else GlassVariant.Subtle,
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (needsAttention) Icons.Default.Warning else Icons.Default.Shield,
                contentDescription = null,
                tint = if (needsAttention) MaterialTheme.colorScheme.secondary else WarmMint
            )
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        !enabled -> "Protection paused"
                        !health.coreReady -> "Protection needs setup"
                        health.reliabilityScore < 100 -> "Protection active · ${health.reliabilityScore}%"
                        else -> "Protection healthy"
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (needsAttention) {
                        health.degradedReasons.firstOrNull() ?: "Review Android permissions."
                    } else {
                        "$activeManagedApps restricted apps · $totalBlocks blocks"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
