package com.example.ui.screens.settings

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.system.PermissionHelper
import com.example.core.system.TrackerReminderScheduler
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch

private enum class SettingsSection(val title: String, val icon: ImageVector) {
    Appearance("Appearance", Icons.Default.Palette),
    Blocking("Blocking", Icons.Default.HourglassBottom),
    Progress("Progress reminders", Icons.Default.Notifications),
    Protection("Protection", Icons.Default.Shield),
    Permissions("Permissions", Icons.Default.Security),
    Privacy("Privacy & legal", Icons.Default.PrivacyTip),
    About("About", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    repository: AppBlockerRepository,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAntiBypassGuide: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateToBlockerCustomization: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onReplayTour: () -> Unit = {},
    onNavigateToOwnerConsole: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val delayMinutes by repository.settings.strictChangeDelayMinutesFlow.collectAsState(initial = 0)
    val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "light")
    val accentStyle by repository.settings.accentStyleFlow.collectAsState(initial = "rose")
    val companionSound by repository.settings.companionSoundFlow.collectAsState(initial = false)
    val reduceMotion by repository.settings.reduceMotionFlow.collectAsState(initial = false)
    val showPackageOnBlocker by repository.settings.showPackageOnBlockerFlow.collectAsState(initial = false)
    val accountSignedIn by repository.settings.isAccountSignedInFlow.collectAsState(initial = false)
    val accountEmail by repository.settings.accountEmailFlow.collectAsState(initial = "")
    val historyRetentionDays by repository.settings.blockHistoryRetentionDaysFlow.collectAsState(initial = 30)
    val dailyReminderEnabled by repository.settings.dailyReminderEnabledFlow.collectAsState(initial = false)
    val dailyReminderHour by repository.settings.dailyReminderHourFlow.collectAsState(initial = 20)
    val dailyReminderMinute by repository.settings.dailyReminderMinuteFlow.collectAsState(initial = 0)
    val sleepReminderEnabled by repository.settings.sleepReminderEnabledFlow.collectAsState(initial = false)
    val sleepReminderHour by repository.settings.sleepReminderHourFlow.collectAsState(initial = 6)
    val sleepReminderMinute by repository.settings.sleepReminderMinuteFlow.collectAsState(initial = 0)
    var selectedSection by remember { mutableStateOf(SettingsSection.Appearance) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen_root")
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(modifier = Modifier.width(280.dp)) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Preferences and protection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    SettingsSection.entries.forEach { section ->
                        SettingsSidebarItem(
                            section = section,
                            selected = selectedSection == section,
                            onClick = { selectedSection = section }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    elevation = 0.dp
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Text(
                                text = selectedSection.title,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            SettingsSectionContent(
                                section = selectedSection,
                                themeMode = themeMode,
                                accentStyle = accentStyle,
                                companionSound = companionSound,
                                onAccentChange = { scope.launch { repository.settings.setAccentStyle(it) } },
                                onCompanionSoundChange = { scope.launch { repository.settings.setCompanionSound(it) } },
                                onReplayTour = onReplayTour,
                                delayMinutes = delayMinutes,
                                isProtectionEnabled = isProtectionEnabled,
                                reduceMotion = reduceMotion,
                                showPackageOnBlocker = showPackageOnBlocker,
                                accountSignedIn = accountSignedIn,
                                accountEmail = accountEmail,
                                historyRetentionDays = historyRetentionDays,
                                dailyReminderEnabled = dailyReminderEnabled,
                                dailyReminderHour = dailyReminderHour,
                                dailyReminderMinute = dailyReminderMinute,
                                sleepReminderEnabled = sleepReminderEnabled,
                                sleepReminderHour = sleepReminderHour,
                                sleepReminderMinute = sleepReminderMinute,
                                onThemeChange = { mode ->
                                    scope.launch { repository.settings.setThemeMode(mode) }
                                },
                                onDelayChange = { minutes ->
                                    scope.launch { repository.settings.setStrictChangeDelayMinutes(minutes) }
                                },
                                onProtectionChange = { enabled ->
                                    scope.launch { repository.settings.setProtectionEnabled(enabled) }
                                },
                                onNavigateToPermissions = onNavigateToPermissions,
                                onNavigateToAntiBypassGuide = onNavigateToAntiBypassGuide,
                                onNavigateToLegal = onNavigateToLegal,
                                onNavigateToAdultProtection = onNavigateToAdultProtection,
                                onNavigateToPaywall = onNavigateToPaywall,
                                onNavigateToBlockerCustomization = onNavigateToBlockerCustomization,
                                onNavigateToAccount = onNavigateToAccount,
                                onNavigateToInsights = onNavigateToInsights,
                                onNavigateToOwnerConsole = onNavigateToOwnerConsole,
                                onReduceMotionChange = { enabled ->
                                    scope.launch { repository.settings.setReduceMotion(enabled) }
                                },
                                onShowPackageOnBlockerChange = { enabled ->
                                    scope.launch { repository.settings.setShowPackageOnBlocker(enabled) }
                                },
                                onDailyReminderChange = { enabled, hour, minute ->
                                    scope.launch {
                                        repository.settings.setDailyReminder(enabled, hour, minute)
                                        TrackerReminderScheduler.refresh(context)
                                    }
                                },
                                onSleepReminderChange = { enabled, hour, minute ->
                                    scope.launch {
                                        repository.settings.setSleepReminder(enabled, hour, minute)
                                        TrackerReminderScheduler.refresh(context)
                                    }
                                },
                                onHistoryRetentionChange = { days ->
                                    scope.launch {
                                        repository.settings.setBlockHistoryRetentionDays(days)
                                        repository.pruneBlockHistoryNow()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "App behavior and system controls",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), elevation = 0.dp) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            SettingsSection.entries.forEach { section ->
                                SettingsSidebarItem(
                                    section = section,
                                    selected = selectedSection == section,
                                    onClick = { selectedSection = section }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 0.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = selectedSection.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SettingsSectionContent(
                                section = selectedSection,
                                themeMode = themeMode,
                                accentStyle = accentStyle,
                                companionSound = companionSound,
                                onAccentChange = { scope.launch { repository.settings.setAccentStyle(it) } },
                                onCompanionSoundChange = { scope.launch { repository.settings.setCompanionSound(it) } },
                                onReplayTour = onReplayTour,
                                delayMinutes = delayMinutes,
                                isProtectionEnabled = isProtectionEnabled,
                                reduceMotion = reduceMotion,
                                showPackageOnBlocker = showPackageOnBlocker,
                                accountSignedIn = accountSignedIn,
                                accountEmail = accountEmail,
                                historyRetentionDays = historyRetentionDays,
                                dailyReminderEnabled = dailyReminderEnabled,
                                dailyReminderHour = dailyReminderHour,
                                dailyReminderMinute = dailyReminderMinute,
                                sleepReminderEnabled = sleepReminderEnabled,
                                sleepReminderHour = sleepReminderHour,
                                sleepReminderMinute = sleepReminderMinute,
                                onThemeChange = { mode ->
                                    scope.launch { repository.settings.setThemeMode(mode) }
                                },
                                onDelayChange = { minutes ->
                                    scope.launch { repository.settings.setStrictChangeDelayMinutes(minutes) }
                                },
                                onProtectionChange = { enabled ->
                                    scope.launch { repository.settings.setProtectionEnabled(enabled) }
                                },
                                onNavigateToPermissions = onNavigateToPermissions,
                                onNavigateToAntiBypassGuide = onNavigateToAntiBypassGuide,
                                onNavigateToLegal = onNavigateToLegal,
                                onNavigateToAdultProtection = onNavigateToAdultProtection,
                                onNavigateToPaywall = onNavigateToPaywall,
                                onNavigateToBlockerCustomization = onNavigateToBlockerCustomization,
                                onNavigateToAccount = onNavigateToAccount,
                                onNavigateToInsights = onNavigateToInsights,
                                onNavigateToOwnerConsole = onNavigateToOwnerConsole,
                                onReduceMotionChange = { enabled ->
                                    scope.launch { repository.settings.setReduceMotion(enabled) }
                                },
                                onShowPackageOnBlockerChange = { enabled ->
                                    scope.launch { repository.settings.setShowPackageOnBlocker(enabled) }
                                },
                                onDailyReminderChange = { enabled, hour, minute ->
                                    scope.launch {
                                        repository.settings.setDailyReminder(enabled, hour, minute)
                                        TrackerReminderScheduler.refresh(context)
                                    }
                                },
                                onSleepReminderChange = { enabled, hour, minute ->
                                    scope.launch {
                                        repository.settings.setSleepReminder(enabled, hour, minute)
                                        TrackerReminderScheduler.refresh(context)
                                    }
                                },
                                onHistoryRetentionChange = { days ->
                                    scope.launch {
                                        repository.settings.setBlockHistoryRetentionDays(days)
                                        repository.pruneBlockHistoryNow()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSidebarItem(
    section: SettingsSection,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    } else {
        MaterialTheme.colorScheme.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    themeMode: String,
    accentStyle: String,
    companionSound: Boolean,
    onAccentChange: (String) -> Unit,
    onCompanionSoundChange: (Boolean) -> Unit,
    onReplayTour: () -> Unit,
    delayMinutes: Int,
    isProtectionEnabled: Boolean,
    reduceMotion: Boolean,
    showPackageOnBlocker: Boolean,
    accountSignedIn: Boolean,
    accountEmail: String,
    historyRetentionDays: Int,
    dailyReminderEnabled: Boolean,
    dailyReminderHour: Int,
    dailyReminderMinute: Int,
    sleepReminderEnabled: Boolean,
    sleepReminderHour: Int,
    sleepReminderMinute: Int,
    onThemeChange: (String) -> Unit,
    onDelayChange: (Int) -> Unit,
    onProtectionChange: (Boolean) -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAntiBypassGuide: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToAdultProtection: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToBlockerCustomization: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToInsights: () -> Unit,
    onNavigateToOwnerConsole: () -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onShowPackageOnBlockerChange: (Boolean) -> Unit,
    onHistoryRetentionChange: (Int) -> Unit,
    onDailyReminderChange: (Boolean, Int, Int) -> Unit,
    onSleepReminderChange: (Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val notificationsAllowed = PermissionHelper.isNotificationPermissionGranted(context)
    val exactAlarmsAllowed = PermissionHelper.canScheduleExactAlarms(context)

    when (section) {
        SettingsSection.Appearance -> {
            SettingsDescription("Soft colour, clear contrast. Choose the finish that feels like you.")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("rose" to "Rose", "lilac" to "Lilac", "peach" to "Peach").forEach { (id, name) ->
                    FilterChip(selected = accentStyle == id, onClick = { onAccentChange(id) }, label = { Text(name) })
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("system", "System", Icons.Default.PhoneAndroid),
                    Triple("light", "Light", Icons.Default.LightMode),
                    Triple("dark", "Dark", Icons.Default.DarkMode)
                ).forEach { (mode, label, icon) ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            SettingsToggleRow(title = "Companion sounds", subtitle = "Soft effects and optional offline speech. Silent until you turn this on.",
                checked = companionSound, onCheckedChange = onCompanionSoundChange)
            Spacer(Modifier.height(18.dp))
            SettingsToggleRow(
                title = "Reduce motion",
                subtitle = "Uses simpler transitions and disables decorative blocker animation.",
                checked = reduceMotion,
                onCheckedChange = onReduceMotionChange
            )
        }

        SettingsSection.Blocking -> {
            SettingsToggleRow(
                title = "App blocking",
                subtitle = "Apply saved schedules to protected apps.",
                checked = isProtectionEnabled,
                onCheckedChange = onProtectionChange
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Change delay",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            SettingsDescription("Adds a waiting period before schedule changes take effect.")
            Spacer(modifier = Modifier.height(18.dp))
            SettingsToggleRow(
                title = "Show technical app ID",
                subtitle = "Display the package name on the block screen for diagnostics.",
                checked = showPackageOnBlocker,
                onCheckedChange = onShowPackageOnBlockerChange
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "Instant", 5 to "5 min", 15 to "15 min", 30 to "30 min").forEach { (mins, label) ->
                    FilterChip(
                        selected = delayMinutes == mins,
                        onClick = { onDelayChange(mins) },
                        label = { Text(label) }
                    )
                }
            }
        }

        SettingsSection.Progress -> {
            SettingsDescription(
                "Reminders are optional. They are scheduled once per day and can be turned off at any time."
            )
            Spacer(modifier = Modifier.height(12.dp))
            ReminderSettingRow(
                title = "Daily progress reminder",
                subtitle = "Opens the daily check-in directly.",
                enabled = dailyReminderEnabled,
                hour = dailyReminderHour,
                minute = dailyReminderMinute,
                onChange = onDailyReminderChange
            )
            Spacer(modifier = Modifier.height(18.dp))
            ReminderSettingRow(
                title = "Sleep reminder",
                subtitle = "Reminds you to manually log last night's sleep.",
                enabled = sleepReminderEnabled,
                hour = sleepReminderHour,
                minute = sleepReminderMinute,
                onChange = onSleepReminderChange
            )
            Spacer(modifier = Modifier.height(16.dp))
            ReminderDiagnostics()

        }

        SettingsSection.Protection -> {
            SettingsLinkRow(
                title = "18+ content protection",
                subtitle = "Explicit-search guard and filtered search shortcuts",
                onClick = onNavigateToAdultProtection
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsLinkRow(
                title = "Anti-bypass information",
                subtitle = "Understand Android limits and stronger device-owner options",
                onClick = onNavigateToAntiBypassGuide
            )
        }

        SettingsSection.Permissions -> {
            SettingsDescription("Review the Android permissions used for blocking and background reliability.")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsLinkRow(
                title = "System permissions",
                subtitle = "Accessibility, usage access, overlay and battery settings",
                onClick = onNavigateToPermissions
            )
        }

        SettingsSection.Privacy -> {
            SettingsLinkRow(
                title = "Privacy, terms & data controls",
                subtitle = "Read policies and manage local data",
                onClick = onNavigateToLegal
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Block history retention",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            SettingsDescription(
                "Choose how long local block events are kept on this device."
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7 to "7d", 30 to "30d", 90 to "90d", 365 to "1y").forEach { (days, label) ->
                    FilterChip(
                        selected = historyRetentionDays == days,
                        onClick = { onHistoryRetentionChange(days) },
                        label = { Text(label) }
                    )
                }
            }
        }

        SettingsSection.About -> {
            Text(
                text = "Strict Blocker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            SettingsDescription("Version " + com.example.BuildConfig.VERSION_NAME)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Version " + com.example.BuildConfig.VERSION_NAME)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsDescription("Schedules and app preferences are stored on-device.")
            Spacer(Modifier.height(12.dp))
            SettingsLinkRow("Replay the quick tour", "Three short tips for recording, browsing and your companion", onReplayTour)
            Spacer(modifier = Modifier.height(14.dp))
            SettingsLinkRow(
                title = "Owner payment console",
                subtitle = "Review pending UPI payments and approve Pro manually",
                onClick = onNavigateToOwnerConsole
            )
        }
    }
}

@Composable
private fun SettingsDescription(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun ReminderSettingRow(
    title: String,
    subtitle: String,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onChange: (Boolean, Int, Int) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onChange(true, hour, minute)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsToggleRow(
            title = title,
            subtitle = subtitle,
            checked = enabled,
            onCheckedChange = { value ->
                if (value && Build.VERSION.SDK_INT >= 33 && !PermissionHelper.isNotificationPermissionGranted(context))
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else onChange(value, hour, minute)
            }
        )
        if (enabled) Text(
            "Next: " + TrackerReminderScheduler.nextOccurrence(hour, minute).format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM · h:mm a")),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        onChange(enabled, selectedHour, selectedMinute)
                    },
                    hour,
                    minute,
                    false
                ).show()
            },
            enabled = enabled
        ) {
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val suffix = if (hour < 12) "AM" else "PM"
            Text(
                displayHour.toString() + ":" +
                    minute.toString().padStart(2, '0') + " " + suffix
            )
        }
    }
}
