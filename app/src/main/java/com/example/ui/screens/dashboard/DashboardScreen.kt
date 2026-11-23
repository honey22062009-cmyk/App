package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.NextTransitionInfo
import com.example.ui.components.AppIconImage
import com.example.ui.components.AppSurface
import com.example.ui.components.BunnyCompanion
import com.example.ui.components.BunnyMood
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val isAdultProtectionEnabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val adultBlocksCount by repository.settings.adultBlocksCountFlow.collectAsState(initial = 0)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val totalBlocksCount by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val recentBlocks by repository.recentBlocksFlow.collectAsState(initial = emptyList())
    val activeManagedApps = managedApps.filter { it.isManaged }

    var nextTransition by remember { mutableStateOf<NextTransitionInfo?>(null) }
    var permissionsHealthy by remember(context) { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(context, lifecycleOwner) {
        fun refreshPermissions() {
            permissionsHealthy = PermissionHelper.isAccessibilityServiceEnabled(context) &&
                PermissionHelper.isUsageAccessGranted(context) &&
                PermissionHelper.canDrawOverlays(context) &&
                PermissionHelper.canScheduleExactAlarms(context)
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        refreshPermissions()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(activeManagedApps) {
        nextTransition = repository.findNextGlobalTransition()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val wide = maxWidth >= 900.dp
        val horizontalPadding = if (maxWidth >= 720.dp) 32.dp else 20.dp
        val contentWidth = when {
            maxWidth >= 1280.dp -> 1120.dp
            maxWidth >= 900.dp -> maxWidth - 64.dp
            else -> maxWidth
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 24.dp)
                .testTag("dashboard_lazy_column"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                    Text("Your day, protected", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Text("Choose what gets your attention and when", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                if (wide) {
                    Row(
                        modifier = Modifier.width(contentWidth).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProtectionPanel(
                            modifier = Modifier.weight(1.15f),
                            enabled = isProtectionEnabled,
                            permissionsHealthy = permissionsHealthy,
                            managedCount = activeManagedApps.size,
                            onToggle = { enabled -> scope.launch { repository.settings.setProtectionEnabled(enabled) } },
                            onPermissions = onNavigateToPermissions
                        )
                        CompanionPanel(modifier = Modifier.weight(0.85f), onFocus = onNavigateToFocus)
                    }
                } else {
                    Column(
                        modifier = Modifier.width(contentWidth).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ProtectionPanel(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isProtectionEnabled,
                            permissionsHealthy = permissionsHealthy,
                            managedCount = activeManagedApps.size,
                            onToggle = { enabled -> scope.launch { repository.settings.setProtectionEnabled(enabled) } },
                            onPermissions = onNavigateToPermissions
                        )
                        CompanionPanel(modifier = Modifier.fillMaxWidth(), onFocus = onNavigateToFocus)
                    }
                }
            }

            if (!permissionsHealthy) {
                item {
                    AttentionPanel(modifier = Modifier.width(contentWidth).fillMaxWidth(), onClick = onNavigateToPermissions)
                }
            }

            item {
                Column(
                    modifier = Modifier.width(contentWidth).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeading("Manage your day", "Shortcuts to the tools you use most")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickAction("Focus", Icons.Default.Timer, onNavigateToFocus, Modifier.weight(1f))
                        QuickAction("Apps", Icons.Default.Apps, onNavigateToApps, Modifier.weight(1f))
                        QuickAction("Schedules", Icons.Default.CalendarMonth, onNavigateToSchedules, Modifier.weight(1f))
                        QuickAction("18+ shield", Icons.Default.Security, onNavigateToAdultProtection, Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.width(contentWidth).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeading("At a glance", "Current protection and recent activity")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricPanel(Modifier.weight(1f), activeManagedApps.size.toString(), "Managed apps")
                        MetricPanel(Modifier.weight(1f), totalBlocksCount.toString(), "Blocked launches")
                        MetricPanel(Modifier.weight(1f), if (isProtectionEnabled && permissionsHealthy) "On" else "Off", "Enforcement")
                    }
                }
            }

            item {
                AppSurface(
                    modifier = Modifier.width(contentWidth).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Next schedule change", style = MaterialTheme.typography.titleMedium)
                        val transitionText = nextTransition?.let {
                            val time = it.transitionDateTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
                            "$time · ${if (it.isOpening) "opens" else "closes"} · ${it.appName}"
                        } ?: "No upcoming transition"
                        InfoLine(Icons.Default.CalendarMonth, transitionText, "Based on your saved app schedules")
                        InfoLine(
                            Icons.Default.Security,
                            if (isAdultProtectionEnabled) "Adult content protection is enabled" else "Adult content protection is off",
                            if (adultBlocksCount > 0) "$adultBlocksCount restricted attempts recorded" else "Web and search protection settings"
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.width(contentWidth).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionHeading("Recent activity", "The latest restricted app launches")
                    if (recentBlocks.isEmpty()) {
                        AppSurface(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Nothing blocked recently", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "When a managed app is restricted, its event will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentBlocks.take(8).forEach { event ->
                                AppSurface(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AppIconImage(packageName = event.appPackage, size = 40.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(event.appName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                            Text(event.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        Text(event.formattedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProtectionPanel(
    modifier: Modifier,
    enabled: Boolean,
    permissionsHealthy: Boolean,
    managedCount: Int,
    onToggle: (Boolean) -> Unit,
    onPermissions: () -> Unit
) {
    AppSurface(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("App protection", style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            !enabled -> "Paused"
                            !permissionsHealthy -> "Needs setup"
                            else -> "Enforcing your schedules"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("protection_toggle_switch").semantics {
                        contentDescription = "Protection"
                        stateDescription = if (enabled) "On" else "Off"
                    }
                )
            }
            Text("$managedCount managed apps", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                when {
                    !enabled -> "Your saved schedules are kept, but app blocking is paused."
                    !permissionsHealthy -> "The setting is on, but Android permissions need attention before enforcement can work."
                    else -> "Managed apps follow their saved access windows. Other apps remain unchanged."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!permissionsHealthy) {
                Button(onClick = onPermissions, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                    Text("Review setup")
                }
            }
        }
    }
}

@Composable
private fun CompanionPanel(modifier: Modifier, onFocus: () -> Unit) {
    AppSurface(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Make room for focus", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Set a timer, settle into one task and let your apps stay in their lanes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onFocus) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open focus")
                }
            }
            BunnyCompanion(size = 128.dp, mood = BunnyMood.Resting)
        }
    }
}

@Composable
private fun AttentionPanel(modifier: Modifier, onClick: () -> Unit) {
    AppSurface(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.errorContainer,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.26f),
        onClick = onClick
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Protection needs attention", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Review Accessibility, Usage access, overlay and exact-alarm permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Review permissions")
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(modifier = modifier, shape = RoundedCornerShape(14.dp), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun MetricPanel(modifier: Modifier, value: String, label: String) {
    AppSurface(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
