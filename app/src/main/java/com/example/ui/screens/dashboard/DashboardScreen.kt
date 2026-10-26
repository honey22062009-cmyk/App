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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val zone = remember { ZoneId.systemDefault() }

    val trackerStartedAt by repository.settings.trackerStartedAtFlow.collectAsState(initial = 0L)
    val resetRecords by repository.settings.trackerResetRecordsFlow.collectAsState(initial = emptyList())
    val checkIns by repository.settings.dailyCheckInsFlow.collectAsState(initial = emptyList())
    val goalDays by repository.settings.trackerGoalDaysFlow.collectAsState(initial = 0)
    val cardOrder by repository.settings.dashboardCardOrderFlow.collectAsState(
        initial = com.example.data.preferences.SettingsPreferences.defaultDashboardCardOrder()
    )
    val hiddenCards by repository.settings.dashboardHiddenCardsFlow.collectAsState(initial = emptySet())
    val seenMilestones by repository.settings.seenMilestonesFlow.collectAsState(initial = emptySet())
    val reduceMotion by repository.settings.reduceMotionFlow.collectAsState(initial = false)
    val isPro by repository.settings.isProEntitledFlow.collectAsState(initial = false)
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val totalBlocks by repository.totalBlocksCountFlow.collectAsState(initial = 0)

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var health by remember(context) { mutableStateOf(ProtectionHealth.snapshot(context)) }
    var showResetEditor by remember { mutableStateOf(false) }
    var editingReset by remember { mutableStateOf<TrackerResetRecord?>(null) }
    var deleteCandidate by remember { mutableStateOf<TrackerResetRecord?>(null) }
    var showGoal by remember { mutableStateOf(false) }
    var showMilestones by remember { mutableStateOf(false) }
    var showTimeline by remember { mutableStateOf(false) }
    var showCustomization by remember { mutableStateOf(false) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }
    var celebrationMilestone by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        repository.settings.ensureStreakTrackerStarted()
        repository.settings.refreshExpiredProEntitlement()
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
                DashboardHeader(
                    isPro = isPro,
                    onOpenPro = onOpenPro,
                    onCustomize = {
                        if (isPro) showCustomization = true else onOpenPro()
                    }
                )
            }

            items(
                items = cardOrder.filterNot { it in hiddenCards },
                key = { it }
            ) { key ->
                when (key) {
                    "hero" -> StreakHeroCard(
                        stats = stats,
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
                        todayCheckIn = todayCheckIn,
                        onSelect = { rating ->
                            scope.launch {
                                repository.settings.saveDailyCheckIn(today.toString(), rating)
                            }
                        }
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
        }
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
            order = cardOrder,
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
private fun DashboardHeader(
    isPro: Boolean,
    onOpenPro: () -> Unit,
    onCustomize: () -> Unit
) {
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
                "Private tracking, built around what you explicitly log.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onCustomize) {
            Icon(
                Icons.Default.Tune,
                contentDescription = "Customize dashboard",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            onClick = onOpenPro,
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            modifier = Modifier.widthIn(min = 72.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                WarmMagenta.copy(alpha = .95f),
                                WarmCoral.copy(alpha = .92f),
                                WarmPlum.copy(alpha = .90f)
                            )
                        ),
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (isPro) Icons.Default.Star else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.size(5.dp))
                Text(
                    if (isPro) "PRO" else "PRO",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black
                )
            }
        }
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
