package com.example.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class TourStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AppTourDialog(onFinish: () -> Unit) {
    val steps = remember {
        listOf(
            TourStep("Your dashboard", "See protection status, quick actions and the next schedule change.", Icons.Default.Tune),
            TourStep("Focus", "Pick a session length, start the timer and review your daily focus target.", Icons.Default.Timer),
            TourStep("Progress", "Track a private personal goal, review your streak, check-ins, milestones and notes.", Icons.Default.Insights),
            TourStep("Apps and schedules", "Choose which apps to manage, then create recurring time windows for each one.", Icons.Default.Apps),
            TourStep("Settings and backup", "Save a local profile, choose your theme, export a backup or import older Strict Blocker data.", Icons.Default.CalendarMonth)
        )
    }
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index]
    AlertDialog(
        onDismissRequest = onFinish,
        title = { Text("Welcome to Strict Blocker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(step.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Text(step.title, style = MaterialTheme.typography.titleLarge)
                }
                Text(step.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    steps.indices.forEach { item ->
                        Text(
                            if (item == index) "●" else "○",
                            color = if (item == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${index + 1} of ${steps.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (index == steps.lastIndex) onFinish() else index++
            }) { Text(if (index == steps.lastIndex) "Finish" else "Next") }
        },
        dismissButton = {
            TextButton(onClick = onFinish) { Text("Skip tour") }
        }
    )
}
