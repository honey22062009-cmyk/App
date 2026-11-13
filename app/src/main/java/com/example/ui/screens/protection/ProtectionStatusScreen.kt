package com.example.ui.screens.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.evaluator.ScheduleEvaluator
import com.example.core.system.InitialRestrictionConfig
import com.example.core.system.PermissionHelper
import com.example.core.system.ProtectionHealthStore
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TargetStatus(
    val name: String,
    val packageName: String,
    val installed: Boolean,
    val restrictedNow: Boolean,
    val nextBoundary: LocalDateTime?,
    val nextBoundaryLabel: String
)

private data class PermissionStatus(
    val accessibility: Boolean,
    val usageAccess: Boolean,
    val overlay: Boolean,
    val exactAlarms: Boolean,
    val notifications: Boolean
) {
    val requiredReady: Boolean get() = accessibility && usageAccess && overlay
}

@Composable
fun ProtectionStatusScreen(
    repository: AppBlockerRepository,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val protectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)

    var targets by remember { mutableStateOf<List<TargetStatus>>(emptyList()) }
    var permissions by remember { mutableStateOf<PermissionStatus?>(null) }
    var lastHeartbeat by remember { mutableStateOf<Long?>(null) }
    var refreshing by remember { mutableStateOf(true) }

    LaunchedEffect(protectionEnabled) {
        refreshing = true
        val snapshot = withContext(Dispatchers.IO) {
            val permissionSnapshot = PermissionStatus(
                accessibility = PermissionHelper.isAccessibilityServiceEnabled(context),
                usageAccess = PermissionHelper.isUsageAccessGranted(context),
                overlay = PermissionHelper.canDrawOverlays(context),
                exactAlarms = PermissionHelper.canScheduleExactAlarms(context),
                notifications = PermissionHelper.isNotificationPermissionGranted(context)
            )

            val now = LocalDateTime.now()
            val targetSnapshot = InitialRestrictionConfig.targetApps.map { target ->
                val installed = try {
                    @Suppress("DEPRECATION")
                    context.packageManager.getApplicationInfo(target.packageName, 0)
                    true
                } catch (_: Exception) {
                    false
                }

                val evaluation = repository.evaluateApp(target.packageName, now)
                val periods = repository.scheduleDao.getPeriodsForApp(target.packageName)
                    .map { it.toDomain() }
                val nextTransition = ScheduleEvaluator.findNextTransition(periods, now)

                val nextBoundary = if (evaluation.isBlocked) {
                    evaluation.nextAllowedDateTime
                } else {
                    nextTransition?.dateTime
                }
                val nextLabel = if (evaluation.isBlocked) "Expected unlock" else "Next restriction start"

                TargetStatus(
                    name = target.name,
                    packageName = target.packageName,
                    installed = installed,
                    restrictedNow = evaluation.isBlocked,
                    nextBoundary = nextBoundary,
                    nextBoundaryLabel = nextLabel
                )
            }

            Triple(
                permissionSnapshot,
                targetSnapshot,
                ProtectionHealthStore.getLastMonitorHeartbeat(context)
            )
        }

        permissions = snapshot.first
        targets = snapshot.second
        lastHeartbeat = snapshot.third
        refreshing = false
    }

    val nowMillis = System.currentTimeMillis()
    val monitorRecent = lastHeartbeat?.let { nowMillis - it <= 3 * 60 * 1000L } == true
    val coreReady = permissions?.requiredReady == true
    val domainNow = LocalDateTime.now()
    val websiteDomain = InitialRestrictionConfig.targetDomains.first()
    val websitePeriods = InitialRestrictionConfig.defaultAllowedPeriods("domain:$websiteDomain")
    val websiteEvaluation = ScheduleEvaluator.evaluate(websitePeriods, domainNow)
    val websiteNextBoundary = if (websiteEvaluation.isBlocked) {
        websiteEvaluation.nextAllowedDateTime
    } else {
        ScheduleEvaluator.findNextTransition(websitePeriods, domainNow)?.dateTime
    }
    val anyRestricted = targets.any { it.installed && it.restrictedNow } || websiteEvaluation.isBlocked

    val overallStatus = when {
        !protectionEnabled -> "Protection paused"
        !coreReady -> "Permission required"
        anyRestricted -> "Protection active · restrictions in effect"
        else -> "Currently unlocked"
    }
    val zoneName = ZoneId.systemDefault().id
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Protection Status",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Scheduled restrictions are evaluated from the current device date and time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            StatusSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (protectionEnabled && coreReady) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (protectionEnabled && coreReady) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(overallStatus, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (refreshing) "Refreshing status…" else "Time zone: $zoneName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (permissions != null) {
            item {
                StatusSurface {
                    Text("Permission availability", fontWeight = FontWeight.SemiBold)
                    PermissionLine("Accessibility service", permissions!!.accessibility)
                    PermissionLine("Usage access", permissions!!.usageAccess)
                    PermissionLine("Display over other apps", permissions!!.overlay)
                    PermissionLine("Exact alarms", permissions!!.exactAlarms, optional = true)
                    PermissionLine("Notifications", permissions!!.notifications, optional = true)
                    if (!permissions!!.requiredReady) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onNavigateToPermissions) {
                            Text("Review permissions")
                        }
                    }
                }
            }
        }

        item {
            StatusSurface {
                Text("Background monitoring", fontWeight = FontWeight.SemiBold)
                Text(
                    if (monitorRecent) "Recent monitor heartbeat recorded" else "Background monitor not recently confirmed",
                    color = if (monitorRecent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    lastHeartbeat?.let { "Last health check: " + java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(formatter) }
                        ?: "Last health check: not recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "A recent heartbeat confirms the fallback monitor process is alive; it does not prove that Android will allow every background action.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text("Configured applications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }

        items(targets, key = { it.packageName }) { target ->
            StatusSurface {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(target.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            target.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        when {
                            !target.installed -> "Not installed"
                            !protectionEnabled -> "Paused"
                            target.restrictedNow -> "Restricted now"
                            else -> "Available now"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            !target.installed -> MaterialTheme.colorScheme.onSurfaceVariant
                            target.restrictedNow && protectionEnabled -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Text(
                    target.nextBoundary?.let { "${target.nextBoundaryLabel}: ${it.format(formatter)}" }
                        ?: "No upcoming schedule boundary found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            StatusSurface {
                Text("AniFam website", fontWeight = FontWeight.SemiBold)
                Text(
                    websiteDomain,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    when {
                        !protectionEnabled -> "Paused"
                        websiteEvaluation.isBlocked -> "Restricted now"
                        else -> "Available now"
                    },
                    color = if (websiteEvaluation.isBlocked && protectionEnabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    websiteNextBoundary?.let {
                        "${if (websiteEvaluation.isBlocked) "Expected unlock" else "Next restriction start"}: ${it.format(formatter)}"
                    } ?: "No upcoming website boundary found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "The website rule depends on supported browsers exposing the hostname in an accessibility address-bar node. Other domains and browsers that do not expose the address bar are not covered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                "Not installed apps remain in this configuration. App and website rules are separate. This screen does not claim to cover other Android users or profiles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusSurface(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun PermissionLine(label: String, granted: Boolean, optional: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            when {
                granted -> "Available"
                optional -> "Optional · unavailable"
                else -> "Required · missing"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
