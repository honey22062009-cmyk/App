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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.repository.NextTransitionInfo
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassVariant
import kotlinx.coroutines.CancellationException
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
    val listState = rememberLazyListState()
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
    val tourSeen by repository.settings.quickTourSeenFlow.collectAsState(initial = true)
    val companionSound by repository.settings.companionSoundFlow.collectAsState(initial = false)
    val companionName by repository.settings.characterNameFlow.collectAsState(initial = "")
    val selectedCharacterId by repository.settings.selectedCharacterIdFlow.collectAsState(initial = "nova")
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val totalBlocks by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val allSchedules by repository.allPeriodsFlow.collectAsState(initial = emptyList())
    val activeManagedPackages = remember(managedApps) {
        managedApps.filter { it.isManaged }.map { it.packageName }.toSet()
    }
    var nextTransition by remember { mutableStateOf<NextTransitionInfo?>(null) }
    var scheduleStatusLoaded by remember { mutableStateOf(false) }
    var scheduleStatusError by remember { mutableStateOf(false) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var health by remember(context) { mutableStateOf(ProtectionHealth.snapshot(context)) }
    var showQuickEntry by rememberSaveable { mutableStateOf(false) }
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
    }

    // Keep Home's next boundary in sync with schedule edits and actual clock transitions.
    LaunchedEffect(activeManagedPackages, allSchedules) {
        scheduleStatusLoaded = false
        scheduleStatusError = false
        while (true) {
            val upcoming = try {
                repository.findNextGlobalTransition()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                scheduleStatusError = true
                scheduleStatusLoaded = true
                nextTransition = null
                break
            }
            nextTransition = upcoming
            scheduleStatusLoaded = true
            if (upcoming == null) break
            val boundaryMillis = upcoming.transitionDateTime
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            val waitMillis = (boundaryMillis - System.currentTimeMillis() + 1_000L)
                .coerceAtLeast(1_000L)
            delay(waitMillis)
        }
    }

    LaunchedEffect(initialTrackerAction) {
        when (initialTrackerAction) {
            "sleep" -> showSleepEditor = true
            "daily" -> showQuickEntry = true
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
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        val tablet = maxWidth >= 720.dp
        val wideHome = maxWidth >= 1000.dp && !progressOnly
        val progressKeys = setOf("stats", "graph", "calendar", "timeline", "milestones", "insights")
        val visibleCardOrder = if (progressOnly) cardOrder.filter { it in progressKeys && it !in hiddenCards }
            else if (wideHome) listOf("protection") else listOf("protection", "hero")
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
                            soundEnabled = companionSound,
                            onSoundChange = { scope.launch { repository.settings.setCompanionSound(it) } },
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
                            nextTransition = nextTransition,
                            scheduleStatusLoaded = scheduleStatusLoaded,
                            scheduleStatusError = scheduleStatusError,
                            onToggle = { enabled ->
                                scope.launch { repository.settings.setProtectionEnabled(enabled) }
                            },
                            onOpenSetup = onNavigateToPermissions,
                            onOpenApps = onNavigateToApps,
                            onOpenSchedules = onNavigateToSchedules
                        )
                    }
        }



        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 1320.dp).fillMaxSize().align(Alignment.TopCenter).testTag("dashboard_list"),
            contentPadding = PaddingValues(
                start = if (tablet) 22.dp else 12.dp,
                end = if (tablet) 22.dp else 12.dp,
                top = 18.dp,
                bottom = 128.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(17.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "STRICT BLOCKER",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    if (progressOnly) "YOUR JOURNEY" else "PERSONAL CONTROL CENTER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = onOpenPro) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                            Text(" Explore Pro")
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (progressOnly) "Your progress"
                            else if (userName.isBlank()) "Your day"
                            else "Hi, ${userName.trim()}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (progressOnly) "History, patterns and milestones"
                            else today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())) +
                                "  ·  A little more control, every day.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (progressOnly) TextButton(onClick = {
                    showCustomization = true
                }) { Text("Customize progress") }
            }
            if (!progressOnly && !tourSeen) item(key = "quick_tour") {
                QuickTourCard(reduceMotion = reduceMotion,
                    onDone = { scope.launch { repository.settings.setQuickTourSeen(true) } })
            }
            if (!progressOnly && !wideHome) item(key = "today") {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    DashboardSectionHeading(
                        title = "TODAY AT A GLANCE",
                        subtitle = "Your daily snapshot"
                    )
                    TodayOverviewCard(
                        checkIns,
                        todayCheckIn,
                        todaySleep,
                        today,
                        reduceMotion,
                        onRecord = { showQuickEntry = true }
                    )
                }
            }

            if (!progressOnly) item(key = "shortcuts") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DashboardSectionHeading(
                        title = "QUICK CONTROLS",
                        subtitle = "The things you use most"
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardActionTile(
                            title = "Block apps",
                            subtitle = "Choose what to pause",
                            icon = Icons.Default.Apps,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToApps
                        )
                        DashboardActionTile(
                            title = "Schedules",
                            subtitle = "Set allowed hours",
                            icon = Icons.Default.CalendarMonth,
                            accent = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSchedules
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DashboardActionTile(
                            title = "Content shield",
                            subtitle = "Extra web protection",
                            icon = Icons.Default.Security,
                            accent = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAdultProtection
                        )
                        DashboardActionTile(
                            title = "System setup",
                            subtitle = "Check permissions",
                            icon = Icons.Default.Tune,
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToPermissions
                        )
                    }
                }
            }

            if (wideHome) item(key = "home_focus_row") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(Modifier.weight(.85f)) {
                        TodayOverviewCard(
                            checkIns,
                            todayCheckIn,
                            todaySleep,
                            today,
                            reduceMotion,
                            onRecord = { showQuickEntry = true }
                        )
                    }
                    Box(Modifier.weight(1.65f)) { renderDashboardCard("hero") }
                }
            }

            if (!progressOnly) item(key = "protection_heading") {
                DashboardSectionHeading(
                    title = "PROTECTION CENTER",
                    subtitle = "A clear view of what's running"
                )
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
            onSave = { rating, sleep, removeSleep ->
                repository.settings.saveDailyEntry(LocalDate.now(zone).toString(), rating, sleep, removeSleep)
                showQuickEntry = false
                // The receipt belongs at the top of Home. Run scrolling in the
                // screen's scope so dismissing the sheet cannot cancel it.
                scope.launch {
                    if (reduceMotion) listState.scrollToItem(0) else listState.animateScrollToItem(0)
                }
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
private fun DashboardSectionHeading(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DashboardActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        variant = GlassVariant.Standard,
        shape = RoundedCornerShape(23.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = accent.copy(alpha = .13f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ProtectionStatusCard(
    enabled: Boolean,
    health: ProtectionHealthSnapshot,
    activeManagedApps: Int,
    totalBlocks: Int,
    nextTransition: NextTransitionInfo?,
    scheduleStatusLoaded: Boolean,
    scheduleStatusError: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenSetup: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSchedules: () -> Unit
) {
    val hasNoAllowedWindows = activeManagedApps > 0 && scheduleStatusLoaded &&
        !scheduleStatusError && nextTransition == null
    val needsAttention = !enabled || !health.coreReady || activeManagedApps == 0 ||
        hasNoAllowedWindows || scheduleStatusError
    val actionLabel = when {
        !health.coreReady -> "Fix setup"
        activeManagedApps == 0 -> "Choose apps"
        scheduleStatusError -> "Check schedules"
        hasNoAllowedWindows -> "Add allowed window"
        nextTransition != null -> "View schedule"
        health.recommendedIssues.isNotEmpty() -> "Improve reliability"
        else -> null
    }
    val action = when {
        !health.coreReady -> onOpenSetup
        activeManagedApps == 0 -> onOpenApps
        scheduleStatusError || hasNoAllowedWindows || nextTransition != null -> onOpenSchedules
        health.recommendedIssues.isNotEmpty() -> onOpenSetup
        else -> null
    }
    val title = when {
        !enabled -> "App blocking is paused"
        !health.coreReady -> "Finish protection setup"
        activeManagedApps == 0 -> "Choose apps to protect"
        scheduleStatusError -> "Schedule status unavailable"
        hasNoAllowedWindows -> "Apps are blocked all the time"
        else -> "App blocking is active"
    }
    val detail = when {
        !enabled -> "Turn protection on to enforce your schedules."
        !health.coreReady -> health.coreIssues.firstOrNull() ?: "Open setup to restore required access."
        activeManagedApps == 0 -> "No apps are selected yet. Add the apps you want Strict Blocker to protect."
        scheduleStatusError -> "Could not read the next schedule change. Open Schedules to review your allowed windows."
        hasNoAllowedWindows -> "No enabled allowed windows are configured. Protected apps stay blocked until you add one."
        !scheduleStatusLoaded -> "Checking the next app access change…"
        health.recommendedIssues.isNotEmpty() ->
            "$activeManagedApps apps protected · $totalBlocks blocks recorded · ${health.recommendedIssues.size} optional reliability settings can be improved."
        else -> "$activeManagedApps apps protected · $totalBlocks blocks recorded"
    }
    val scheduleDetail = when {
        !enabled -> "Schedule enforcement is paused."
        scheduleStatusError || !scheduleStatusLoaded -> null
        hasNoAllowedWindows -> null
        nextTransition == null -> null
        else -> {
            val time = nextTransition.transitionDateTime
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE · h:mm a", Locale.getDefault()))
            val change = if (nextTransition.isOpening) "becomes allowed" else "will be blocked"
            "Next: ${nextTransition.appName} $change · $time"
        }
    }
    val systemReady = enabled && health.coreReady && activeManagedApps > 0 &&
        !hasNoAllowedWindows && !scheduleStatusError
    val statusTint = if (systemReady) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.secondary
    val statusText = if (systemReady) "PROTECTION ON" else "ACTION NEEDED"

    GlassCard(
        modifier = Modifier.fillMaxWidth().testTag("protection_center_card"),
        variant = if (needsAttention) GlassVariant.Warning else GlassVariant.Accent,
        shape = RoundedCornerShape(30.dp),
        borderWidth = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            statusTint.copy(alpha = .13f),
                            MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                            MaterialTheme.colorScheme.surface.copy(alpha = .15f)
                        )
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(
                        "YOUR DEVICE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = statusTint.copy(alpha = .14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(7.dp)
                                    .background(statusTint, CircleShape)
                            )
                            Text(
                                statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusTint,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(23.dp),
                    color = statusTint.copy(alpha = .13f),
                    modifier = Modifier.size(66.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (needsAttention) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = null,
                            tint = statusTint,
                            modifier = Modifier.size(35.dp)
                        )
                    }
                }
                Spacer(Modifier.size(12.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("protection_toggle")
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    if (systemReady) "You're in control." else "Let's finish the setup.",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProtectionMetric(
                    value = activeManagedApps.toString(),
                    label = "APPS PROTECTED",
                    modifier = Modifier.weight(1f)
                )
                ProtectionMetric(
                    value = totalBlocks.toString(),
                    label = "BLOCKS RECORDED",
                    modifier = Modifier.weight(1f)
                )
            }

            if (scheduleDetail != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .62f)
                ) {
                    Text(
                        scheduleDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)
                    )
                }
            }

            if (actionLabel != null && action != null) {
                Button(
                    onClick = action,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(17.dp),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    "Your protection settings are up to date.",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusTint,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ProtectionMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .68f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
