package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val activeManagedApps = managedApps.filter { it.isManaged }
    val totalBlocksCount by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val recentBlocks by repository.recentBlocksFlow.collectAsState(initial = emptyList())

    var nextTransition by remember { mutableStateOf<NextTransitionInfo?>(null) }
    var currentlyBlockedCount by remember { mutableStateOf(0) }

    // Permission checks
    val isAccessibilityOk = remember(context) { PermissionHelper.isAccessibilityServiceEnabled(context) }
    val isUsageOk = remember(context) { PermissionHelper.isUsageAccessGranted(context) }
    val isOverlayOk = remember(context) { PermissionHelper.canDrawOverlays(context) }
    val allCriticalPermissionsOk = isAccessibilityOk && isUsageOk && isOverlayOk

    LaunchedEffect(activeManagedApps) {
        nextTransition = repository.findNextGlobalTransition()

        // Calculate how many apps are blocked right now
        var blockedCount = 0
        for (app in activeManagedApps) {
            val eval = repository.evaluateApp(app.packageName)
            if (eval.isBlocked) blockedCount++
        }
        currentlyBlockedCount = blockedCount
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = 16.dp)
                .testTag("dashboard_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Strict Blocker",
                            fontSize = if (isTablet) 30.sp else 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isTablet) "Samsung Galaxy Tab Enforcement Engine" else "Strict Scheduled Enforcement",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isProtectionEnabled) Color(0xFF10B981).copy(alpha = 0.15f)
                                else Color(0xFFEF4444).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isProtectionEnabled) Color(0xFF10B981) else Color(0xFFEF4444))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isProtectionEnabled) "ENFORCING" else "PAUSED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProtectionEnabled) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                }
            }

            // Hero Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProtectionEnabled) Color(0xFF0F233A) else Color(0xFF281118)
                    )
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
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isProtectionEnabled) Color(0xFF0284C7).copy(alpha = 0.25f)
                                        else Color(0xFFEF4444).copy(alpha = 0.25f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isProtectionEnabled) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isProtectionEnabled) Color(0xFF38BDF8) else Color(0xFFF87171),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isProtectionEnabled) "Strict Mode Active" else "Protection Disabled",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isProtectionEnabled) {
                                        "Blocking unallowed apps outside scheduled hours"
                                    } else {
                                        "App restrictions are currently suspended"
                                    },
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    repository.settings.setProtectionEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF38BDF8),
                                checkedTrackColor = Color(0xFF0369A1)
                            ),
                            modifier = Modifier.testTag("protection_toggle_switch")
                        )
                    }
                }
            }

            // Permission Warning if any missing
            if (!allCriticalPermissionsOk) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF451A03)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Missing Required System Permissions",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A)
                                )
                                Text(
                                    text = "Accessibility or Usage Access is disabled. Blocking cannot react without them.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateToPermissions,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    contentColor = Color(0xFF78350F)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Fix", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Stat Cards Grid (Adaptive layout)
            item {
                Text(
                    text = "Enforcement Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val cardWidth = if (isTablet) (maxWidth - 96.dp) / 4 else (maxWidth - 44.dp) / 2

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (isTablet) 4 else 2
                ) {
                    MetricCard(
                        title = "Protected Apps",
                        value = "${activeManagedApps.size}",
                        subtitle = "Launchable apps managed",
                        icon = Icons.Default.Shield,
                        iconTint = Color(0xFF38BDF8),
                        onClick = onNavigateToApps,
                        modifier = Modifier.width(cardWidth)
                    )

                    MetricCard(
                        title = "Blocked Right Now",
                        value = "$currentlyBlockedCount",
                        subtitle = "Outside allowed hours",
                        icon = Icons.Default.Lock,
                        iconTint = Color(0xFFF87171),
                        onClick = onNavigateToApps,
                        modifier = Modifier.width(cardWidth)
                    )

                    MetricCard(
                        title = "Next Transition",
                        value = nextTransition?.let {
                            it.transitionDateTime.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
                        } ?: "--:--",
                        subtitle = nextTransition?.let { "${it.appName} opens" } ?: "No schedule upcoming",
                        icon = Icons.Default.HourglassTop,
                        iconTint = Color(0xFFFBBF24),
                        onClick = onNavigateToSchedules,
                        modifier = Modifier.width(cardWidth)
                    )

                    MetricCard(
                        title = "Blocks Prevented",
                        value = "$totalBlocksCount",
                        subtitle = "Total access attempts",
                        icon = Icons.Default.History,
                        iconTint = Color(0xFF34D399),
                        onClick = {},
                        modifier = Modifier.width(cardWidth)
                    )
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${recentBlocks.size} logged",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentBlocks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No Block Events Yet",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Attempts to open managed apps outside allowed hours will appear here.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(recentBlocks.take(10)) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(packageName = event.appPackage, size = 36.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.appName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = event.reason,
                                    fontSize = 11.sp,
                                    color = Color(0xFFF87171)
                                )
                            }
                            Text(
                                text = event.formattedTime,
                                fontSize = 11.sp,
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
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
