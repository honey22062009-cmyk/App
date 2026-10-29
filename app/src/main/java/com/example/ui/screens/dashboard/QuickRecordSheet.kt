package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.preferences.DailyCheckIn
import com.example.data.preferences.SleepEntry
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickRecordSheet(
    current: DailyCheckIn?, currentSleep: SleepEntry?, onDismiss: () -> Unit,
    onReset: () -> Unit, onSave: suspend (String, Int?, Boolean) -> Unit
) {
    var rating by rememberSaveable { mutableStateOf(current?.rating ?: "okay") }
    var addSleep by rememberSaveable { mutableStateOf(currentSleep != null) }
    var minutes by rememberSaveable { mutableIntStateOf(currentSleep?.durationMinutes ?: 480) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("How was today?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("easy" to "Good", "okay" to "Okay", "difficult" to "Tough").forEach { (id, label) ->
                    FilterChip(selected = rating == id, onClick = { rating = id },
                        label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Add last night's sleep", style = MaterialTheme.typography.titleMedium)
                Switch(checked = addSleep, onCheckedChange = { addSleep = it })
            }
            AnimatedVisibility(visible = addSleep) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (4..10).forEach { hours ->
                            FilterChip(selected = minutes == hours * 60, onClick = { minutes = hours * 60 }, label = { Text("${hours}h") })
                        }
                    }
                    Text("${minutes / 60}h ${minutes % 60}m", style = MaterialTheme.typography.titleLarge)
                    Slider(value = minutes.toFloat(), onValueChange = { minutes = (it.toInt() / 15) * 15 },
                        valueRange = 0f..1440f, steps = 95)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                if (saving) return@Button
                error = null
                saving = true
                scope.launch {
                    try { onSave(rating, if (addSleep) minutes else null, currentSleep != null && !addSleep) }
                    catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "Could not save. Try again." }
                    finally { saving = false }
                }
            }, enabled = !saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (saving) "Saving…" else "Save today's entry") }
            TextButton(onClick = onReset, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("I need to reset my streak") }
            Text("One entry per day. Saving again updates today’s entry. Your elapsed streak counts time separately.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }
}
