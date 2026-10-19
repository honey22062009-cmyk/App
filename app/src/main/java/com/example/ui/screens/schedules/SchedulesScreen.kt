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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Applications Managed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select applications to configure strict allowed hours.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToApps,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Select Apps to Manage")
                    }
                }
            }
        } else if (isTablet) {
            // Dual Pane Layout for Samsung Galaxy Tab A9+
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Apps selector list
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Managed Apps",
                            fontSize = 18.sp,
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
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAppPackage = app.packageName },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
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
                                                fontSize = 14.sp,
                                                color = if (isSelected) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurface
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
                // Horizontal App Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Schedules",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                            modifier = Modifier.clickable { selectedAppPackage = app.packageName },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) Color(0xFF0284C7) else MaterialTheme.colorScheme.surfaceVariant
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(packageName = app.packageName, size = 48.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.appName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Allowed ${periods.count { it.isEnabled }} periods • Outside these times = BLOCKED",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onAddPeriod,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_period")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Period", fontSize = 13.sp)
                    }
                }
            }
        }

        // Overlap Warning Banner
        if (overlapWarnings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03))
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
                                text = "Overlapping Allowed Periods Detected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                            Text(
                                text = overlapWarnings.first().description,
                                fontSize = 11.sp,
                                color = Color(0xFFFBBF24)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onMergeOverlapping,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color(0xFF78350F)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(14.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (periods.isNotEmpty()) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.name.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (periods.isEmpty()) "Strictly Blocked All Day" else "${periods.size} allowed period(s)",
                    fontSize = 12.sp,
                    color = if (periods.isEmpty()) Color(0xFFF87171) else Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
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
        color = if (period.isEnabled) Color(0xFF0F1E36) else MaterialTheme.colorScheme.surfaceVariant,
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
                    tint = Color(0xFFA5B4FC),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = period.timeSpanDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (period.isEnabled) Color.White else Color(0xFF94A3B8)
                )
                Text(
                    text = if (period.isEnabled) "Allowed window" else "Disabled rule",
                    fontSize = 11.sp,
                    color = if (period.isEnabled) Color(0xFF38BDF8) else Color(0xFF64748B)
                )
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }

            IconButton(
                onClick = {
                    // Duplicate to next day
                    onDuplicate(period.dayOfWeek.plus(1))
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFF87171), modifier = Modifier.size(16.dp))
            }

            Switch(
                checked = period.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF38BDF8),
                    checkedTrackColor = Color(0xFF0284C7)
                ),
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
