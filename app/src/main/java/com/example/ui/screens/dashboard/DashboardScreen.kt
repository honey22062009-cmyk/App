package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.NextTransitionInfo
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val isAdultProtectionEnabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val adultBlocksCount by repository.settings.adultBlocksCountFlow.collectAsState(initial = 0)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val activeManagedApps = managedApps.filter { it.isManaged }
    val totalBlocksCount by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val recentBlocks by repository.recentBlocksFlow.collectAsState(initial = emptyList())

    var nextTransition by remember { mutableStateOf<NextTransitionInfo?>(null) }
    var currentlyBlockedCount by remember { mutableIntStateOf(0) }

    // Permission checks
    val isAccessibilityOk = remember(context) { PermissionHelper.isAccessibilityServiceEnabled(context) }
    val isUsageOk = remember(context) { PermissionHelper.isUsageAccessGranted(context) }
    val isOverlayOk = remember(context) { PermissionHelper.canDrawOverlays(context) }
    val allCriticalPermissionsOk = isAccessibilityOk && isUsageOk && isOverlayOk

    LaunchedEffect(activeManagedApps) {
        nextTransition = repository.findNextGlobalTransition()

        var blockedCount = 0
        for (app in activeManagedApps) {
            val eval = repository.evaluateApp(app.packageName)
            if (eval.isBlocked) blockedCount++
        }
        currentlyBlockedCount = blockedCount
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 16.dp)
                .testTag("dashboard_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Dashboard",
                            style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isTablet) "Tablet Enforcement & Schedule Monitor" else "Strict Scheduled Focus & Discipline",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    StatusBadge(
                        text = if (isProtectionEnabled) "Enforcing" else "Paused",
                        isActive = isProtectionEnabled,
                        activeColor = MaterialTheme.colorScheme.tertiary,
                        inactiveColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Hero Master Protection Card
            item {
                val bannerBorder = if (isProtectionEnabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                }
                val bannerBg = if (isProtectionEnabled) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.22f)
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = bannerBorder,
                    backgroundColor = bannerBg
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isProtectionEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isProtectionEnabled) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isProtectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isProtectionEnabled) "Strict Protection Active" else "Protection Suspended",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isProtectionEnabled) {
                                        "Restricted apps are blocked outside allowed schedule periods"
                                    } else {
                                        "All scheduled blocking is temporarily disabled"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    repository.settings.setProtectionEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("protection_toggle_switch")
                        )
                    }
                }
            }

            // 18+ Adult Content Protection Dedicated Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToAdultProtection
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "18+ Adult Protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(
                                    text = if (isAdultProtectionEnabled) "ACTIVE" else "OFF",
                                    isActive = isAdultProtectionEnabled
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAdultProtectionEnabled) {
                                    if (adultBlocksCount > 0) "$adultBlocksCount adult attempts prevented • Device-wide DNS & Browser Filter"
                                    else "Device-wide adult filter & SafeSearch active across all apps"
                                } else {
                                    "Tap to enable device-wide 18+ sexual content blocking"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Configure Adult Shield",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Permission Warning Card if critical permissions missing
            if (!allCriticalPermissionsOk) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = Color(0xFFF59E0B).copy(alpha = 0.45f),
                        backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Permissions Incomplete",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Accessibility or Usage Access is needed for zero-latency app detection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateToPermissions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color(0xFF1E293B)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Review", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Section: Metric Cards
            item {
                Text(
                    text = "Enforcement Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val cardWidth = if (isTablet) (maxWidth - 64.dp - 36.dp) / 4 else (maxWidth - 32.dp - 12.dp) / 2

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (isTablet) 4 else 2
                ) {
                    DashboardMetricCard(
                        title = "Protected Apps",
                        value = "${activeManagedApps.size}",
                        subtitle = "Managed apps",
                        icon = Icons.Default.Shield,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToApps,
                        modifier = Modifier.width(cardWidth)
                    )

                    DashboardMetricCard(
                        title = "Blocked Now",
                        value = "$currentlyBlockedCount",
                        subtitle = "Outside allowed hours",
                        icon = Icons.Default.Lock,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = onNavigateToApps,
                        modifier = Modifier.width(cardWidth)
                    )

                    DashboardMetricCard(
                        title = "Next Event",
                        value = nextTransition?.let {
                            it.transitionDateTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                        } ?: "--:--",
                        subtitle = nextTransition?.let { "${it.appName} opens" } ?: "No schedule upcoming",
                        icon = Icons.Default.HourglassTop,
                        iconTint = Color(0xFFF59E0B),
                        onClick = onNavigateToSchedules,
                        modifier = Modifier.width(cardWidth)
                    )

                    DashboardMetricCard(
                        title = "Interceptions",
                        value = "$totalBlocksCount",
                        subtitle = "Total prevented attempts",
                        icon = Icons.Default.History,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        onClick = {},
                        modifier = Modifier.width(cardWidth)
                    )
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToApps,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apps", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToSchedules,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Schedules", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToAdultProtection,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("18+ Shield", fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = onNavigateToPermissions,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Security", fontSize = 12.sp)
                    }
                }
            }

            // Recent Enforcement Events
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Interceptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${recentBlocks.size} logged",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentBlocks.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Block Events Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Attempts to open managed apps outside allowed schedules will appear here in real time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentBlocks.take(10)) { event ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(packageName = event.appPackage, size = 38.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = event.reason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = event.formattedTime,
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
fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
