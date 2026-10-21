package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.core.system.PermissionHelper
import com.example.ui.components.GlassCard
import com.example.ui.components.StatusBadge

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

    // Re-check permissions automatically on resume
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

    val criticalGrantedCount = listOf(isAccessibilityGranted, isUsageGranted, isOverlayGranted).count { it }
    val totalCritical = 3
    val allCriticalGranted = criticalGrantedCount == totalCritical

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 16.dp)
                .testTag("permissions_screen_root"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Title
            item {
                Column {
                    Text(
                        text = "Permissions",
                        style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Allow the Android permissions Strict Blocker needs to detect and block restricted apps reliably.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overview Progress Card
            item {
                val bannerBg = if (allCriticalGranted) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                } else {
                    Color(0xFFF59E0B).copy(alpha = 0.12f)
                }
                val bannerBorder = if (allCriticalGranted) {
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                } else {
                    Color(0xFFF59E0B).copy(alpha = 0.35f)
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = bannerBg,
                    borderColor = bannerBorder
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (allCriticalGranted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                                            else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (allCriticalGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (allCriticalGranted) MaterialTheme.colorScheme.tertiary else Color(0xFFF59E0B),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (allCriticalGranted) "Protection is ready" else "Setup needs attention",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$criticalGrantedCount of $totalCritical required permissions allowed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            StatusBadge(
                                text = if (allCriticalGranted) "Ready" else "Action Needed",
                                isActive = allCriticalGranted,
                                activeColor = MaterialTheme.colorScheme.tertiary,
                                inactiveColor = Color(0xFFF59E0B)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { criticalGrantedCount.toFloat() / totalCritical },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (allCriticalGranted) MaterialTheme.colorScheme.tertiary else Color(0xFFF59E0B),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }

            // Section 1: Core Protection
            item {
                Text(
                    text = "Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 1. Accessibility Service
            item {
                PermissionSetupCard(
                    title = "Accessibility",
                    technicalName = "Accessibility Service",
                    icon = Icons.Default.AccessibilityNew,
                    isGranted = isAccessibilityGranted,
                    isRequired = true,
                    summary = "Lets Strict Blocker detect when a restricted app opens and show the block screen.",
                    why = stringResource(R.string.perm_accessibility_why),
                    whatStops = stringResource(R.string.perm_accessibility_what),
                    howToEnable = stringResource(R.string.perm_accessibility_how),
                    onGrantClick = { PermissionHelper.openAccessibilitySettings(context) }
                )
            }

            // 2. Usage Access
            item {
                PermissionSetupCard(
                    title = "App Usage Access",
                    technicalName = "UsageStatsManager",
                    icon = Icons.Default.DataUsage,
                    isGranted = isUsageGranted,
                    isRequired = true,
                    summary = "Lets Strict Blocker verify which app is currently in use.",
                    why = stringResource(R.string.perm_usage_why),
                    whatStops = stringResource(R.string.perm_usage_what),
                    howToEnable = stringResource(R.string.perm_usage_how),
                    onGrantClick = { PermissionHelper.openUsageAccessSettings(context) }
                )
            }

            // 3. Display Over Other Apps (Overlay)
            item {
                PermissionSetupCard(
                    title = "Display Over Other Apps",
                    technicalName = "Overlay Shield Window",
                    icon = Icons.Default.PictureInPicture,
                    isGranted = isOverlayGranted,
                    isRequired = true,
                    summary = "Allows the blocking screen to appear over a restricted app when needed.",
                    why = stringResource(R.string.perm_overlay_why),
                    whatStops = stringResource(R.string.perm_overlay_what),
                    howToEnable = stringResource(R.string.perm_overlay_how),
                    onGrantClick = { PermissionHelper.openOverlaySettings(context) }
                )
            }

            // Section 2: Enhancements
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Recommended",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 4. Samsung Battery Optimization
            item {
                PermissionSetupCard(
                    title = "Battery reliability",
                    technicalName = "Ignore Battery Optimizations",
                    icon = Icons.Default.BatteryAlert,
                    isGranted = isBatteryOptIgnored,
                    isRequired = false,
                    summary = "Helps protection keep running when Android limits background activity.",
                    why = stringResource(R.string.perm_battery_why),
                    whatStops = stringResource(R.string.perm_battery_what),
                    howToEnable = stringResource(R.string.perm_battery_how),
                    onGrantClick = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )
            }

            // 5. Exact Alarms
            item {
                PermissionSetupCard(
                    title = "Precise schedules",
                    technicalName = "SCHEDULE_EXACT_ALARM",
                    icon = Icons.Default.Alarm,
                    isGranted = isExactAlarmGranted,
                    isRequired = false,
                    summary = "Helps schedule changes happen at the expected time.",
                    why = stringResource(R.string.perm_alarm_why),
                    whatStops = stringResource(R.string.perm_alarm_what),
                    howToEnable = stringResource(R.string.perm_alarm_how),
                    onGrantClick = { PermissionHelper.openExactAlarmSettings(context) }
                )
            }

            // 6. Notifications & Foreground Service
            item {
                PermissionSetupCard(
                    title = "Notifications",
                    technicalName = "POST_NOTIFICATIONS",
                    icon = Icons.Default.Notifications,
                    isGranted = isNotificationGranted,
                    isRequired = false,
                    summary = "Shows important protection and background-service information.",
                    why = stringResource(R.string.perm_notification_why),
                    whatStops = stringResource(R.string.perm_notification_what),
                    howToEnable = stringResource(R.string.perm_notification_how),
                    onGrantClick = { PermissionHelper.openAppNotificationSettings(context) }
                )
            }

            // Finish / Return Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allCriticalGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (allCriticalGranted) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = if (allCriticalGranted) "Return to Dashboard" else "Continue for now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PermissionSetupCard(
    title: String,
    technicalName: String,
    icon: ImageVector,
    isGranted: Boolean,
    isRequired: Boolean,
    summary: String,
    why: String,
    whatStops: String,
    howToEnable: String,
    onGrantClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card Header
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
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGranted) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = technicalName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isRequired) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "REQUIRED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    StatusBadge(
                        text = if (isGranted) "Allowed" else "Not Enabled",
                        isActive = isGranted,
                        activeColor = MaterialTheme.colorScheme.tertiary,
                        inactiveColor = if (isRequired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Short summary
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expandable details toggle
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isExpanded) "Hide details" else "Why is this needed?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Expanded instructions
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WHY IT IS NEEDED:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = why,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "WHAT HAPPENS WITHOUT IT:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = whatStops,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "HOW TO ENABLE:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = howToEnable,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Action Button
            Spacer(modifier = Modifier.height(12.dp))
            if (!isGranted) {
                Button(
                    onClick = onGrantClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRequired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isRequired) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Allow in Settings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Allowed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}
