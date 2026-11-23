package com.example.ui.screens.progress

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.preferences.PersonalProgressPreferences
import com.example.data.preferences.PersonalProgressSnapshot
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    val last30 = (29L downTo 0L).map { offset ->
        val day = today - offset
        val clean = snapshot.dailyStatus[day] ?: if (day > snapshot.lastResetDay) 1 else 0
        day to clean
    }
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
                    "Your private progress, one day at a time. No judgement and no lost progress hidden from you.",
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
                                if (snapshot.currentStreak == 0) "Your next day starts now." else "Keep going at your own pace.",
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
                        MetricPill("Best", "${snapshot.bestStreak} days")
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
                        Button(
                            onClick = { scope.launch { store.markGoalKept(today) } },
                            modifier = Modifier.weight(1f)
                        ) {
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
                    if (snapshot.dailyStatus[today] == 1) {
                        Text("Today's check-in is saved.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    } else if (snapshot.dailyStatus[today] == 0) {
                        Text("A reset is recorded for today. Your next day is a fresh start.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Last 30 days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Each bar represents one calendar day in your current record.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().height(112.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last30.forEach { (day, status) ->
                            val isFuture = day > today
                            val color = when {
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant
                                status == 1 -> MaterialTheme.colorScheme.tertiary
                                day == snapshot.lastResetDay -> MaterialTheme.colorScheme.secondary
                                day < snapshot.startDay -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Box(
                                modifier = Modifier.weight(1f).height(if (status == 1) 86.dp else if (day == snapshot.lastResetDay) 42.dp else 16.dp)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(color)
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("30 days ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(MaterialTheme.colorScheme.tertiary, "Goal kept / active streak")
                        LegendDot(MaterialTheme.colorScheme.secondary, "Reset day")
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
            text = { Text("This updates your personal day count. Your older history and best streak remain saved.") },
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
private fun MetricPill(label: String, value: String) {
    Column(
        modifier = Modifier.weightlessForMetric().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)).padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

private fun Modifier.weightlessForMetric() = this

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
