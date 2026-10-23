package com.example.ui.screens.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.MainActivity
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository

private data class DiagnosticItem(val title: String, val detail: String, val healthy: Boolean, val required: Boolean, val fixLabel: String, val fix: () -> Unit)

@Composable
fun DiagnosticsScreen(repository: AppBlockerRepository, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val protectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refreshKey++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val checks = remember(context, refreshKey, protectionEnabled) {
        listOf(
            DiagnosticItem("App blocking switch", if (protectionEnabled) "Protection is switched on." else "Protection is paused.", protectionEnabled, true, "Open app") {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            },
            DiagnosticItem("Accessibility service", "Enables immediate foreground-app detection.", PermissionHelper.isAccessibilityServiceEnabled(context), true, "Fix") { PermissionHelper.openAccessibilitySettings(context) },
            DiagnosticItem("Usage Access", "Provides fallback foreground-app monitoring.", PermissionHelper.isUsageAccessGranted(context), true, "Fix") { PermissionHelper.openUsageAccessSettings(context) },
            DiagnosticItem("Display over other apps", "Allows the blocking shield to appear over a restricted app.", PermissionHelper.canDrawOverlays(context), true, "Fix") { PermissionHelper.openOverlaySettings(context) },
            DiagnosticItem("Exact alarms", "Schedules time-window boundaries.", PermissionHelper.canScheduleExactAlarms(context), true, "Fix") { PermissionHelper.openExactAlarmSettings(context) },
            DiagnosticItem("Notifications", "Recommended for monitoring-service status.", PermissionHelper.isNotificationPermissionGranted(context), false, "Review") { PermissionHelper.openAppNotificationSettings(context) },
            DiagnosticItem("Battery unrestricted", "Recommended on devices that suspend background apps.", PermissionHelper.isIgnoringBatteryOptimizations(context), false, "Review") { PermissionHelper.openBatteryOptimizationSettings(context) }
        )
    }
    val requiredHealthy = protectionEnabled && checks.filter { it.required && it.title != "App blocking switch" }.all { it.healthy }
    val warningCount = checks.count { !it.healthy }
    val diagnosticText = remember(checks, protectionEnabled) {
        buildString {
            appendLine("Strict Blocker diagnostics")
            appendLine("Version: ${BuildConfig.VERSION_NAME}")
            appendLine("Protection switch: ${if (protectionEnabled) "on" else "paused"}")
            checks.forEach { check ->
                appendLine("${check.title}: ${if (check.healthy) "available" else "needs review"}")
            }
            appendLine()
            appendLine("This report excludes schedules, installed-app names, browser content, account data and payment details.")
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Protection diagnostics", style = MaterialTheme.typography.headlineLarge)
                Text("Check live Android settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = if (requiredHealthy) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (requiredHealthy) "Required checks passed" else "Protection needs attention", style = MaterialTheme.typography.titleLarge)
                Text(if (requiredHealthy) "Required permissions appear available. A real-device test is still needed." else "$warningCount check(s) need attention. Android settings can affect blocking reliability.")
            }
        }
        checks.forEach { item ->
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (item.title) {
                                "Battery unrestricted" -> Icons.Default.BatteryChargingFull
                                "Notifications" -> Icons.Default.Notifications
                                else -> Icons.Default.Security
                            },
                            contentDescription = null,
                            tint = if (item.healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            Text(if (item.healthy) "Available" else if (item.required) "Action required" else "Recommended", color = if (item.healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!item.healthy) Button(onClick = item.fix) { Text(item.fixLabel) }
                }
            }
        }
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Strict Blocker diagnostic report")
                    putExtra(Intent.EXTRA_TEXT, diagnosticText)
                }
                context.startActivity(Intent.createChooser(intent, "Share diagnostic report"))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Share diagnostic report") }
        Text("The report includes only app version, protection switch and permission status. These checks are not proof that every app or browser can be blocked in every device state.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
