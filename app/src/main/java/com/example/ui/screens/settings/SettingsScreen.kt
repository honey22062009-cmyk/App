package com.example.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupManager
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: AppBlockerRepository,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAntiBypassGuide: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {},
    onReplayTour: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "system")
    val profileName by repository.settings.profileNameFlow.collectAsState(initial = "")
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val dailyGoal by repository.settings.focusDailyGoalFlow.collectAsState(initial = 120)
    var editableName by remember(profileName) { mutableStateOf(profileName) }
    var message by remember { mutableStateOf<String?>(null) }
    var importUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    BackupManager.export(context, repository, uri)
                    message = "Backup saved successfully. Your existing app session remains open."
                } catch (error: Exception) {
                    message = "Backup failed: ${error.message ?: "unknown error"}"
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> importUri = uri }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Make Strict Blocker work the way you do.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SettingsCard(title = "Your profile", icon = Icons.Default.Person) {
                OutlinedTextField(
                    value = editableName,
                    onValueChange = { editableName = it.take(40) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Display name") },
                    placeholder = { Text("What should the app call you?") }
                )
                Button(
                    onClick = { scope.launch { repository.settings.setProfileName(editableName); message = "Your local profile is saved." } },
                    enabled = editableName.trim().isNotEmpty()
                ) { Text("Save profile") }
                Text(
                    "This is a local profile, not a cloud account. Your name stays on this device; no login server is configured in this build.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard(title = "Appearance", icon = Icons.Default.Palette) {
                Text("Choose a calmer plum-and-rose theme. The green palette has been removed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { scope.launch { repository.settings.setThemeMode(mode) } },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Blocking and focus", icon = Icons.Default.Settings) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Scheduled app blocking", style = MaterialTheme.typography.titleMedium)
                        Text("Apply your saved app schedules.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isProtectionEnabled, onCheckedChange = { enabled -> scope.launch { repository.settings.setProtectionEnabled(enabled) } })
                }
                Spacer(Modifier.height(8.dp))
                Text("Daily focus target", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120, 180, 240).forEach { minutes ->
                        FilterChip(
                            selected = dailyGoal == minutes,
                            onClick = { scope.launch { repository.settings.setFocusDailyGoal(minutes) } },
                            label = { Text("${minutes}m") }
                        )
                    }
                }
                Text("The target is used in Focus and Progress summaries; it does not force a timer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            SettingsCard(title = "Backup and restore", icon = Icons.Default.Backup) {
                Text(
                    "Create a ZIP backup of schedules, app rules, settings, focus history, your local profile and personal progress. You can also import the JSON backup format used by Strict Blocker 1.8.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { exportLauncher.launch("strict-blocker-backup.zip") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create backup")
                }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/json", "application/octet-stream", "text/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import previous version data")
                }
                Text(
                    "A 1.8 JSON backup restores preferences, profile name, tracker dates and focus totals; it did not include the Room app-list/schedule database. New ZIP backups can include the database. Export a fresh backup before importing; restore restarts the app process.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard(title = "Protection and permissions", icon = Icons.Default.Security) {
                SettingsLink("Browser protection", "Supported browser checks only; DNS/VPN has been removed.", onNavigateToAdultProtection)
                SettingsLink("System permissions", "Accessibility, Usage Access, overlay and battery settings.", onNavigateToPermissions)
                SettingsLink("Security guide", "Understand Android's app-blocking limits.", onNavigateToAntiBypassGuide)
            }
        }

        item {
            SettingsCard(title = "App tour", icon = Icons.Default.CheckCircle) {
                Text("Replay the walkthrough explaining Dashboard, Focus, Progress, Apps, Schedules and Settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onReplayTour, modifier = Modifier.fillMaxWidth()) { Text("Replay app tour") }
            }
        }

        item {
            SettingsCard(title = "About Strict Blocker", icon = Icons.Default.Info) {
                Text("Strict Blocker 2.1.1", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("A local-first Android focus and app-scheduling tool.", style = MaterialTheme.typography.bodyMedium)
                AboutRow("App scheduling", "Choose apps and recurring allowed time windows. A local Accessibility Service checks app launches.")
                AboutRow("Focus", "Run a distraction-free timer with saved sessions, daily goals and focus history.")
                AboutRow("Progress", "Track a private personal goal with day counts, check-ins, milestones and notes.")
                AboutRow("Storage", "Schedules and app records use a local Room database; preferences and progress use on-device DataStore files.")
                AboutRow("Backups", "Export a ZIP or import supported 1.8 JSON backups. Legacy JSON does not contain the Room app-list/schedule database. Backups may contain private activity and should be stored securely.")
                AboutRow("Network", "No DNS filtering or local VPN service is included. This build does not require a cloud account for its core features.")
                AboutRow("Limitations", "Android can stop background work, revoke permissions or uninstall regular apps. Scheduled enforcement depends on the permissions remaining enabled.")
                AboutRow("Support", "Review Privacy & Legal for data-handling details and platform limitations.")
                OutlinedButton(onClick = onNavigateToLegal, modifier = Modifier.fillMaxWidth()) { Text("Privacy, terms and legal information") }
            }
        }

        item {
            Text(
                "Local profile only. No real cloud login or cross-device sync is claimed. Keep a backup before reinstalling or switching signing keys.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )
        }
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Strict Blocker") },
            text = { Text(message!!) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
        )
    }

    if (importUri != null) {
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces matching local app data with the contents of the selected backup. Create a backup of the current app first.") },
            confirmButton = {
                TextButton(onClick = {
                    val selected = importUri
                    importUri = null
                    if (selected != null) scope.launch {
                        try {
                            val count = BackupManager.restore(context, selected)
                            android.widget.Toast.makeText(context, "Restore completed. Reopen Strict Blocker.", android.widget.Toast.LENGTH_LONG).show()
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }, 1_500L)
                        } catch (error: Exception) {
                            message = "Restore failed: ${error.message ?: "unsupported backup"}"
                        }
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            content()
        }
    }
}

@Composable
private fun AboutRow(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
