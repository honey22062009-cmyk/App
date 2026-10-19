package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.core.system.PermissionHelper

@Composable
fun PermissionsScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Live permission states
    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isUsageGranted by remember { mutableStateOf(PermissionHelper.isUsageAccessGranted(context)) }
    var isOverlayGranted by remember { mutableStateOf(PermissionHelper.canDrawOverlays(context)) }
    var isBatteryOptIgnored by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }
    var isNotificationGranted by remember { mutableStateOf(PermissionHelper.isNotificationPermissionGranted(context)) }
    var isExactAlarmGranted by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(context)) }

    // Re-check permissions on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
                isUsageGranted = PermissionHelper.isUsageAccessGranted(context)
                isOverlayGranted = PermissionHelper.canDrawOverlays(context)
                isBatteryOptIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
                isNotificationGranted = PermissionHelper.isNotificationPermissionGranted(context)
                isExactAlarmGranted = PermissionHelper.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val criticalGranted = isAccessibilityGranted && isUsageGranted && isOverlayGranted

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("permissions_screen_root"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Required Permissions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Strict enforcement relies on transparent, non-root Android system capabilities. Review each permission below.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (criticalGranted) Color(0xFF064E3B) else Color(0xFF451A03)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (criticalGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (criticalGranted) Color(0xFF34D399) else Color(0xFFF59E0B),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (criticalGranted) "Core Protection Configured" else "Action Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (criticalGranted) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                        )
                        Text(
                            text = if (criticalGranted) {
                                "All essential interceptor permissions are active."
                            } else {
                                "Grant Accessibility and Usage Access to begin blocking."
                            },
                            fontSize = 12.sp,
                            color = if (criticalGranted) Color(0xFFD1FAE5) else Color(0xFFFBBF24)
                        )
                    }
                }
            }
        }

        // 1. Accessibility Service
        item {
            PermissionCard(
                title = stringResource(R.string.perm_accessibility_title),
                icon = Icons.Default.AccessibilityNew,
                isGranted = isAccessibilityGranted,
                isEssential = true,
                why = stringResource(R.string.perm_accessibility_why),
                whatStops = stringResource(R.string.perm_accessibility_what),
                howToEnable = stringResource(R.string.perm_accessibility_how),
                onGrantClick = { PermissionHelper.openAccessibilitySettings(context) }
            )
        }

        // 2. Usage Access
        item {
            PermissionCard(
                title = stringResource(R.string.perm_usage_title),
                icon = Icons.Default.DataUsage,
                isGranted = isUsageGranted,
                isEssential = true,
                why = stringResource(R.string.perm_usage_why),
                whatStops = stringResource(R.string.perm_usage_what),
                howToEnable = stringResource(R.string.perm_usage_how),
                onGrantClick = { PermissionHelper.openUsageAccessSettings(context) }
            )
        }

        // 3. Display Over Other Apps (Overlay)
        item {
            PermissionCard(
                title = stringResource(R.string.perm_overlay_title),
                icon = Icons.Default.PictureInPicture,
                isGranted = isOverlayGranted,
                isEssential = true,
                why = stringResource(R.string.perm_overlay_why),
                whatStops = stringResource(R.string.perm_overlay_what),
                howToEnable = stringResource(R.string.perm_overlay_how),
                onGrantClick = { PermissionHelper.openOverlaySettings(context) }
            )
        }

        // 4. Samsung Battery Optimization
        item {
            PermissionCard(
                title = stringResource(R.string.perm_battery_title),
                icon = Icons.Default.BatteryAlert,
                isGranted = isBatteryOptIgnored,
                isEssential = false,
                why = stringResource(R.string.perm_battery_why),
                whatStops = stringResource(R.string.perm_battery_what),
                howToEnable = stringResource(R.string.perm_battery_how),
                onGrantClick = { PermissionHelper.openBatteryOptimizationSettings(context) }
            )
        }

        // 5. Exact Alarms
        item {
            PermissionCard(
                title = stringResource(R.string.perm_alarm_title),
                icon = Icons.Default.Alarm,
                isGranted = isExactAlarmGranted,
                isEssential = false,
                why = stringResource(R.string.perm_alarm_why),
                whatStops = stringResource(R.string.perm_alarm_what),
                howToEnable = stringResource(R.string.perm_alarm_how),
                onGrantClick = { PermissionHelper.openExactAlarmSettings(context) }
            )
        }

        // 6. Notifications & Foreground Service
        item {
            PermissionCard(
                title = stringResource(R.string.perm_notification_title),
                icon = Icons.Default.Notifications,
                isGranted = isNotificationGranted,
                isEssential = false,
                why = stringResource(R.string.perm_notification_why),
                whatStops = stringResource(R.string.perm_notification_what),
                howToEnable = stringResource(R.string.perm_notification_how),
                onGrantClick = { PermissionHelper.openAppNotificationSettings(context) }
            )
        }

        // Continue Button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (criticalGranted) Color(0xFF0284C7) else Color(0xFF475569)
                )
            ) {
                Text(
                    text = if (criticalGranted) "Return to Dashboard" else "Proceed Anyway",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    icon: ImageVector,
    isGranted: Boolean,
    isEssential: Boolean,
    why: String,
    whatStops: String,
    howToEnable: String,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF0284C7).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) Color(0xFF10B981) else Color(0xFF38BDF8),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isEssential) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ESSENTIAL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF87171),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isGranted) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isGranted) "ENABLED" else "MISSING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) Color(0xFF10B981) else Color(0xFFF87171),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation sections
            Text(
                text = "WHY IT IS REQUIRED:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = why,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "WHAT FAILS WITHOUT IT:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF87171)
            )
            Text(
                text = whatStops,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "HOW TO ENABLE:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8)
            )
            Text(
                text = howToEnable,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text("Grant in System Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
