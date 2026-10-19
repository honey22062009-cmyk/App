package com.example.ui.screens.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.evaluator.ScheduleEvaluator
import com.example.core.model.SchedulePeriod
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
import com.example.ui.components.InfoChip
import com.example.ui.components.StatusBadge
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.Locale

@Composable
fun SchedulesScreen(
    repository: AppBlockerRepository,
    initialSelectedPackage: String? = null,
    onNavigateToApps: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val activeManagedApps = managedApps.filter { it.isManaged }

    var selectedAppPackage by remember { mutableStateOf(initialSelectedPackage ?: "") }

    LaunchedEffect(activeManagedApps) {
        if (selectedAppPackage.isEmpty() || activeManagedApps.none { it.packageName == selectedAppPackage }) {
            if (activeManagedApps.isNotEmpty()) {
                selectedAppPackage = activeManagedApps.first().packageName
            }
        }
    }

    val selectedApp = activeManagedApps.firstOrNull { it.packageName == selectedAppPackage }
    val appPeriods by repository.getPeriodsForAppFlow(selectedAppPackage).collectAsState(initial = emptyList())

    // Overlap checks
    val overlapWarnings = remember(appPeriods) {
        ScheduleEvaluator.detectOverlaps(appPeriods)
    }

    // Dialog state
    var editingPeriod by remember { mutableStateOf<SchedulePeriod?>(null) }
    var isEditorOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("schedules_screen_root")
    ) {
        val isTablet = maxWidth >= 720.dp

        if (activeManagedApps.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "No Applications Managed",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Protect apps from the Apps screen to configure allowed schedule windows.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToApps,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select Apps to Manage", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (isTablet) {
            // Dual Pane Layout for Samsung Galaxy Tab A9+ / Tablets
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Apps selector list
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Managed Apps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeManagedApps) { app ->
                                val isSelected = app.packageName == selectedAppPackage
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                    onClick = { selectedAppPackage = app.packageName }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AppIconImage(packageName = app.packageName, size = 36.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = app.appName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Pane: Schedule Details & Timeline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    if (selectedApp != null) {
                        ScheduleDetailPane(
                            app = selectedApp,
                            periods = appPeriods,
                            overlapWarnings = overlapWarnings,
                            onAddPeriod = {
                                editingPeriod = null
                                isEditorOpen = true
                            },
                            onEditPeriod = { period ->
                                editingPeriod = period
                                isEditorOpen = true
                            },
                            onDeletePeriod = { id ->
                                coroutineScope.launch { repository.deleteSchedulePeriod(id) }
                            },
                            onToggleEnabled = { id, enabled ->
                                coroutineScope.launch { repository.togglePeriodEnabled(id, enabled) }
                            },
                            onDuplicatePeriod = { period, targetDay ->
                                coroutineScope.launch { repository.duplicateSchedulePeriod(period, targetDay) }
                            },
                            onMergeOverlapping = {
                                coroutineScope.launch { repository.mergeOverlappingForApp(selectedApp.packageName) }
                            }
                        )
                    }
                }
            }
        } else {
            // Phone Single Pane Layout with App Switcher Tabs
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schedules",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // App Horizontal Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeManagedApps.take(4).forEach { app ->
                        val isSelected = app.packageName == selectedAppPackage
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedAppPackage = app.packageName },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconImage(packageName = app.packageName, size = 20.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = app.appName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedApp != null) {
                    ScheduleDetailPane(
                        app = selectedApp,
                        periods = appPeriods,
                        overlapWarnings = overlapWarnings,
                        onAddPeriod = {
                            editingPeriod = null
                            isEditorOpen = true
                        },
                        onEditPeriod = { period ->
                            editingPeriod = period
                            isEditorOpen = true
                        },
                        onDeletePeriod = { id ->
                            coroutineScope.launch { repository.deleteSchedulePeriod(id) }
                        },
                        onToggleEnabled = { id, enabled ->
                            coroutineScope.launch { repository.togglePeriodEnabled(id, enabled) }
                        },
                        onDuplicatePeriod = { period, targetDay ->
                            coroutineScope.launch { repository.duplicateSchedulePeriod(period, targetDay) }
                        },
                        onMergeOverlapping = {
                            coroutineScope.launch { repository.mergeOverlappingForApp(selectedApp.packageName) }
                        }
                    )
                }
            }
        }

        // Schedule Editor Dialog
        if (isEditorOpen && selectedApp != null) {
            ScheduleEditorDialog(
                initialPeriod = editingPeriod,
                appPackage = selectedApp.packageName,
                appName = selectedApp.appName,
                onDismiss = { isEditorOpen = false },
                onSave = { savedPeriod ->
                    coroutineScope.launch {
                        if (savedPeriod.id == 0L) {
                            repository.saveSchedulePeriod(savedPeriod)
                        } else {
                            repository.updateSchedulePeriod(savedPeriod)
                        }
                        isEditorOpen = false
                    }
                }
            )
        }
    }
}

@Composable
fun ScheduleDetailPane(
    app: ManagedAppEntity,
    periods: List<SchedulePeriod>,
    overlapWarnings: List<ScheduleEvaluator.OverlapWarning>,
    onAddPeriod: () -> Unit,
    onEditPeriod: (SchedulePeriod) -> Unit,
    onDeletePeriod: (Long) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onDuplicatePeriod: (SchedulePeriod, DayOfWeek) -> Unit,
    onMergeOverlapping: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Header Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(packageName = app.packageName, size = 46.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${periods.count { it.isEnabled }} active window(s) • Outside = strictly blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onAddPeriod,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_period")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Period", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Overlap Warning Banner
        if (overlapWarnings.isNotEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.5f),
                    backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Overlapping Allowed Periods",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = overlapWarnings.first().description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onMergeOverlapping,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Merge", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Weekly Days Breakdown (Monday to Sunday)
        val daysOfWeek = DayOfWeek.values()
        daysOfWeek.forEach { day ->
            val dayPeriods = periods.filter { it.dayOfWeek == day }
            item {
                DayScheduleCard(
                    day = day,
                    periods = dayPeriods,
                    onEditPeriod = onEditPeriod,
                    onDeletePeriod = onDeletePeriod,
                    onToggleEnabled = onToggleEnabled,
                    onDuplicatePeriod = onDuplicatePeriod
                )
            }
        }
    }
}

@Composable
fun DayScheduleCard(
    day: DayOfWeek,
    periods: List<SchedulePeriod>,
    onEditPeriod: (SchedulePeriod) -> Unit,
    onDeletePeriod: (Long) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onDuplicatePeriod: (SchedulePeriod, DayOfWeek) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (periods.isEmpty()) "Strictly Blocked All Day" else "${periods.size} allowed window(s)",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (periods.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (periods.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    periods.forEach { period ->
                        PeriodItemRow(
                            period = period,
                            onEdit = { onEditPeriod(period) },
                            onDelete = { onDeletePeriod(period.id) },
                            onToggleEnabled = { onToggleEnabled(period.id, it) },
                            onDuplicate = { targetDay -> onDuplicatePeriod(period, targetDay) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodItemRow(
    period: SchedulePeriod,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onDuplicate: (DayOfWeek) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (period.isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (period.crossesMidnight) {
                Icon(
                    imageVector = Icons.Default.Nightlight,
                    contentDescription = "Crosses midnight",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = period.timeSpanDisplay,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (period.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (period.isEnabled) "Allowed window" else "Disabled rule",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (period.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
            }

            IconButton(
                onClick = { onDuplicate(period.dayOfWeek.plus(1)) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(17.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
            }

            Spacer(modifier = Modifier.width(4.dp))

            Switch(
                checked = period.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
