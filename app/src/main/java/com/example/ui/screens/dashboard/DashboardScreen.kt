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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.NextTransitionInfo
import com.example.ui.components.AppIconImage
import com.example.ui.components.AppSurface
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
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

    val permissionsHealthy = remember(context) {
        PermissionHelper.isAccessibilityServiceEnabled(context) &&
            PermissionHelper.isUsageAccessGranted(context) &&
            PermissionHelper.canDrawOverlays(context)
    }

    LaunchedEffect(activeManagedApps) {
        nextTransition = repository.findNextGlobalTransition()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val horizontalPadding = if (maxWidth >= 720.dp) 28.dp else 16.dp
        // Use the extra landscape space instead of forcing tablet content into a narrow
        // phone-like column. Keep a cap so very large displays remain easy to scan.
        val contentWidth = when {
            maxWidth >= 1280.dp -> 1180.dp
            maxWidth >= 1000.dp -> maxWidth - 72.dp
            maxWidth >= 720.dp -> maxWidth - 40.dp
            else -> maxWidth
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 18.dp)
                .testTag("dashboard_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                    Text(
                        text = "Strict Blocker",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Focus, schedules and protection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                AppSurface(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isProtectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isProtectionEnabled) "Protection is running" else "Protection is paused",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${activeManagedApps.size} apps protected · ${totalBlocksCount} blocked attempts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch { repository.settings.setProtectionEnabled(enabled) }
                            },
                            modifier = Modifier.testTag("protection_toggle_switch")
                        )
                    }
                }
            }

            if (!permissionsHealthy) {
                item {
                    AppSurface(
                        modifier = Modifier.width(contentWidth).fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.42f),
                        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                        onClick = onNavigateToPermissions
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Protection needs attention",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "One or more required Android permissions are off.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                    Text("Quick access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickAction("Apps", Icons.Default.Apps, onNavigateToApps, Modifier.weight(1f))
                        QuickAction("Schedules", Icons.Default.CalendarMonth, onNavigateToSchedules, Modifier.weight(1f))
                        QuickAction("Adult protection", Icons.Default.Security, onNavigateToAdultProtection, Modifier.weight(1.35f))
                    }
                }
            }

            item {
                AppSurface(
                    modifier = Modifier.width(contentWidth).fillMaxWidth(),
                    onClick = onNavigateToAdultProtection
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Adult Content Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (isAdultProtectionEnabled) {
                                    if (adultBlocksCount > 0) "${adultBlocksCount} restricted attempts blocked"
                                    else "Web, search and device protection enabled"
                                } else {
                                    "Protection is off"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Column(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                    Text("Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    AppSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            StatusRow(
                                icon = Icons.Default.CalendarMonth,
                                title = "Next schedule change",
                                value = nextTransition?.let {
                                    val time = it.transitionDateTime.format(
                                        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
                                    )
                                    "$time · ${it.appName}"
                                } ?: "No upcoming transition"
                            )
                            Spacer(Modifier.height(14.dp))
                            StatusRow(
                                icon = Icons.Default.History,
                                title = "Blocked attempts",
                                value = totalBlocksCount.toString()
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                    Text("Recent activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                }
            }

            if (recentBlocks.isEmpty()) {
                item {
                    AppSurface(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "No blocked attempts yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Recent blocked app launches will appear here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentBlocks.take(8)) { event ->
                    AppSurface(modifier = Modifier.width(contentWidth).fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            AppIconImage(packageName = event.appPackage, size = 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    event.appName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    event.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Text(
                                event.formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(48.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun StatusRow(icon: ImageVector, title: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
