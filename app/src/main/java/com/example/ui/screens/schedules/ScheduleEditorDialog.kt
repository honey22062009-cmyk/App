package com.example.ui.screens.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.model.SchedulePeriod
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ScheduleEditorDialog(
    initialPeriod: SchedulePeriod? = null,
    appPackage: String,
    appName: String,
    onDismiss: () -> Unit,
    onSave: (SchedulePeriod) -> Unit
) {
    var selectedDay by remember {
        mutableStateOf(initialPeriod?.dayOfWeek ?: DayOfWeek.MONDAY)
    }

    var startHour by remember { mutableIntStateOf(initialPeriod?.startHour ?: 18) }
    var startMinute by remember { mutableIntStateOf(initialPeriod?.startMinute ?: 0) }

    var endHour by remember { mutableIntStateOf(initialPeriod?.endHour ?: 19) }
    var endMinute by remember { mutableIntStateOf(initialPeriod?.endMinute ?: 0) }

    val startTime = remember(startHour, startMinute) { LocalTime.of(startHour, startMinute) }
    val endTime = remember(endHour, endMinute) { LocalTime.of(endHour, endMinute) }
    // The model treats equal non-midnight times as a 24-hour window, but 00:00-00:00
    // is a zero-length allowed window. Prevent that accidental always-blocked rule.
    val invalidMidnightWindow = startTime == LocalTime.MIDNIGHT && endTime == LocalTime.MIDNIGHT

    val crossesMidnight = remember(startTime, endTime) {
        startTime > endTime || (startTime == endTime && (startHour != 0 || startMinute != 0))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (initialPeriod == null) "Add Allowed Time Window" else "Edit Allowed Time Window",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "App: $appName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("schedule_editor_dialog")
            ) {
                // Day Selection
                Text(
                    text = "Day of the Week",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf(
                        DayOfWeek.MONDAY to "M",
                        DayOfWeek.TUESDAY to "T",
                        DayOfWeek.WEDNESDAY to "W",
                        DayOfWeek.THURSDAY to "T",
                        DayOfWeek.FRIDAY to "F",
                        DayOfWeek.SATURDAY to "S",
                        DayOfWeek.SUNDAY to "S"
                    )

                    days.forEach { (day, label) ->
                        val isSelected = selectedDay == day
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Start Time Row
                TimeSelectorRow(
                    label = "Allowed From (Start):",
                    hour = startHour,
                    minute = startMinute,
                    onTimeChanged = { h, m ->
                        startHour = h
                        startMinute = m
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // End Time Row
                TimeSelectorRow(
                    label = "Allowed Until (End):",
                    hour = endHour,
                    minute = endMinute,
                    onTimeChanged = { h, m ->
                        endHour = h
                        endMinute = m
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Schedule validation and overnight alert.
                if (invalidMidnightWindow) {
                    Text(
                        text = "Start and end cannot both be 12:00 AM. Choose a different time; this would otherwise allow no time at all.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (crossesMidnight) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This allowed window continues into the next day.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val period = SchedulePeriod(
                        id = initialPeriod?.id ?: 0,
                        appPackage = appPackage,
                        dayOfWeek = selectedDay,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute,
                        isEnabled = initialPeriod?.isEnabled ?: true
                    )
                    onSave(period)
                },
                enabled = !invalidMidnightWindow,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("schedule_save_button")
            ) {
                Text("Save Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimeSelectorRow(
    label: String,
    hour: Int,
    minute: Int,
    onTimeChanged: (Int, Int) -> Unit
) {
    val time = LocalTime.of(hour, minute)
    val formatted = time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = formatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Quick hour / minute adjusters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickTimeButton(label = "-1h") {
                val newHour = (hour - 1 + 24) % 24
                onTimeChanged(newHour, minute)
            }
            QuickTimeButton(label = "+1h") {
                val newHour = (hour + 1) % 24
                onTimeChanged(newHour, minute)
            }
            QuickTimeButton(label = "-15m") {
                var newMinute = minute - 15
                var newHour = hour
                if (newMinute < 0) {
                    newMinute += 60
                    newHour = (newHour - 1 + 24) % 24
                }
                onTimeChanged(newHour, newMinute)
            }
            QuickTimeButton(label = "+15m") {
                var newMinute = minute + 15
                var newHour = hour
                if (newMinute >= 60) {
                    newMinute -= 60
                    newHour = (newHour + 1) % 24
                }
                onTimeChanged(newHour, newMinute)
            }
        }
    }
}

@Composable
private fun QuickTimeButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
