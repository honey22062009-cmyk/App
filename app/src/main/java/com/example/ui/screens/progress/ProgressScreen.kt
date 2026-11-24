package com.example.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.preferences.PersonalProgressPreferences
import com.example.data.preferences.PersonalProgressSnapshot
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ProgressScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember(context) { PersonalProgressPreferences(context.applicationContext) }
    val snapshot by store.snapshotFlow.collectAsState(initial = PersonalProgressSnapshot())
    val scope = rememberCoroutineScope()
    val today = LocalDate.now().toEpochDay()
    var title by remember(snapshot.goalTitle) { mutableStateOf(snapshot.goalTitle) }
    var note by remember(snapshot.todayNote) { mutableStateOf(snapshot.todayNote) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var editingTitle by remember { mutableStateOf(false) }

    val milestones = listOf(1, 3, 7, 14, 30, 60, 90)
    val achieved = milestones.filter { snapshot.bestStreak >= it }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Progress", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "A clear view of your current run and the record you are working towards.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("CURRENT STREAK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(snapshot.currentStreak.toString(), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(8.dp))
                                Text("days", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(bottom = 9.dp))
                            }
                            Text(
                                when {
                                    snapshot.currentStreak > snapshot.previousBestStreak && snapshot.previousBestStreak > 0 -> "New personal record. Keep building at your pace."
                                    snapshot.currentStreak == 0 -> "Your next day starts now."
                                    else -> "Every day adds to your progress."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Box(
                            Modifier.size(78.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricPill("Personal best", "${snapshot.bestStreak} days")
                        MetricPill("Resets", snapshot.resetCount.toString())
                        MetricPill("Check-ins", snapshot.checkInCount.toString())
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("My goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { editingTitle = !editingTitle }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (editingTitle) "Close" else "Edit")
                        }
                    }
                    if (editingTitle) {
                        OutlinedTextField(value = title, onValueChange = { title = it.take(48) }, label = { Text("Goal name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            scope.launch { store.setGoalTitle(title) }
                            editingTitle = false
                        }, enabled = title.isNotBlank()) { Text("Save goal") }
                    } else {
                        Text(snapshot.goalTitle, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("This label stays on this device. Choose wording that feels useful and private to you.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { scope.launch { store.markGoalKept(today) } }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Goal kept")
                        }
                        OutlinedButton(onClick = { showResetConfirm = true }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("Log reset")
                        }
                    }
                    when (snapshot.dailyStatus[today]) {
                        1 -> Text("Today's check-in is saved.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        0 -> Text("A reset is recorded for today. Your next day is a fresh start.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        else -> Unit
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Streak trend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "The solid line is your current run. The dashed line keeps your previous best visible, even after a reset.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StreakLineChart(snapshot.currentStreak, snapshot.previousBestStreak)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        LineLegend(MaterialTheme.colorScheme.primary, "Current · ${snapshot.currentStreak}d")
                        LineLegend(MaterialTheme.colorScheme.secondary, "Previous best · ${snapshot.previousBestStreak}d")
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Text("Milestones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        achieved.forEach { days ->
                            FilterChip(selected = true, onClick = {}, label = { Text("${days}d") }, leadingIcon = {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            })
                        }
                        if (achieved.isEmpty()) Text("Your first milestone is one day.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Next milestone: ${milestones.firstOrNull { it > snapshot.bestStreak }?.let { "$it days" } ?: "All milestones reached"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Daily note", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Optional private note about what helped today or what you want to try tomorrow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = note, onValueChange = { note = it.take(500) }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5, placeholder = { Text("Write a quick reflection…") })
                    Button(onClick = { scope.launch { store.saveTodayNote(note) } }, modifier = Modifier.fillMaxWidth()) { Text("Save note") }
                }
            }
        }

        item {
            Text(
                "Your records are stored locally. Export a backup from Settings before changing or reinstalling the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Record a reset?") },
            text = { Text("Your previous best stays saved. The current line will restart from day zero.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { store.recordReset(today) }
                    showResetConfirm = false
                }) { Text("Record reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StreakLineChart(currentDays: Int, previousBestDays: Int) {
    val currentColor = MaterialTheme.colorScheme.primary
    val previousColor = MaterialTheme.colorScheme.secondary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val chartMax = maxOf(7, currentDays, previousBestDays).let { ((it + 6) / 7) * 7 }

    Canvas(modifier = Modifier.fillMaxWidth().height(204.dp)) {
        val left = 18.dp.toPx()
        val right = 12.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = 12.dp.toPx()
        val plotWidth = (size.width - left - right).coerceAtLeast(1f)
        val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)

        for (tick in 0..4) {
            val ratio = tick / 4f
            val y = top + plotHeight * (1f - ratio)
            drawLine(
                color = gridColor.copy(alpha = 0.65f),
                start = androidx.compose.ui.geometry.Offset(left, y),
                end = androidx.compose.ui.geometry.Offset(left + plotWidth, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        fun xFor(day: Int) = left + plotWidth * (day.toFloat() / chartMax.toFloat())
        fun yFor(day: Int) = top + plotHeight - plotHeight * (day.toFloat() / chartMax.toFloat())

        if (previousBestDays > 0) {
            val recordY = yFor(previousBestDays.coerceAtMost(chartMax))
            drawLine(
                color = previousColor,
                start = androidx.compose.ui.geometry.Offset(left, recordY),
                end = androidx.compose.ui.geometry.Offset(left + plotWidth, recordY),
                strokeWidth = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
            )
        }

        val currentPath = Path().apply {
            moveTo(xFor(0), yFor(0))
            lineTo(xFor(currentDays.coerceAtMost(chartMax)), yFor(currentDays.coerceAtMost(chartMax)))
        }
        drawPath(currentPath, currentColor, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(
            color = currentColor,
            radius = 5.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                xFor(currentDays.coerceAtMost(chartMax)),
                yFor(currentDays.coerceAtMost(chartMax))
            )
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("0 days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$chartMax days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun LineLegend(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.width(18.dp).height(3.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
