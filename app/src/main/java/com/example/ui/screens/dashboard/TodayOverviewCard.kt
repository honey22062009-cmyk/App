package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.DailyCheckIn
import com.example.data.preferences.SleepEntry
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun TodayOverviewCard(entries: List<DailyCheckIn>, current: DailyCheckIn?, sleep: SleepEntry?, today: LocalDate,
    reduceMotion: Boolean, onRecord: () -> Unit) {
    val logged = entries.map { it.localDate }.toSet()
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .48f)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(if (current == null) Icons.Default.EditNote else Icons.Default.CheckCircle, null,
                    Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    AnimatedContent(current != null, label = "saved_today",
                        transitionSpec = { fadeIn(tween(if (reduceMotion) 0 else 200)) togetherWith fadeOut(tween(100)) }) { saved ->
                        Text(if (saved) "Saved today" else "Your daily check-in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text(if (current == null) "A few taps. A clear picture of your day."
                        else "${when (current.rating) { "easy" -> "Good"; "difficult" -> "Tough"; else -> "Okay" }}${sleep?.let { " · Sleep ${it.durationMinutes / 60}h ${it.durationMinutes % 60}m" }.orEmpty()} · stored on this device",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (6L downTo 0L).forEach { offset ->
                    val day = today.minusDays(offset)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(30.dp).background(if (day in logged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Text(if (day in logged) "✓" else "·", color = if (day in logged) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(day.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text("${logged.size} ${if (logged.size == 1) "day" else "days"} recorded · ${logged.count { it in today.minusDays(6)..today }} of 7 this week",
                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRecord, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(18.dp)) {
                Text(if (current == null) "Record today" else "Edit today's entry")
            }
        }
    }
}

@Composable
internal fun QuickTourCard(reduceMotion: Boolean, onDone: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val steps = listOf(
        "Welcome to your day" to "Home is for a quick check-in and your companion. One save records today; your elapsed streak grows with time.",
        "Set your boundaries" to "Block holds your apps and schedules. Browser Guard interrupts explicit searches after you enable Accessibility and consent to field inspection.",
        "Make it yours" to "Progress keeps your history. Pro has its own dock tab. Tap, turn, feed or talk to your companion; sound is optional. Colours and motion live in Settings."
    )
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .50f)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("QUICK TOUR · ${page + 1}/3", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onDone, contentPadding = PaddingValues(0.dp)) { Text("Skip tour") }
            }
            AnimatedContent(page, transitionSpec = {
                (slideInHorizontally(tween(if (reduceMotion) 0 else 240)) { it / 5 } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(if (reduceMotion) 0 else 180)) { -it / 5 } + fadeOut())
            }, label = "tour_slide") { step ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(steps[step].first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(steps[step].second, style = MaterialTheme.typography.bodyMedium)
                }
            }
            FilledTonalButton(onClick = { if (page == 2) onDone() else page++ }) { Text(if (page == 2) "Let's begin" else "Next tip") }
        }
    }
}
