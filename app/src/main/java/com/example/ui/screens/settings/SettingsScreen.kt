package com.example.ui.screens.settings

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch

private enum class SettingsSection(val title: String, val icon: ImageVector) {
    Account("Account", Icons.Default.AccountCircle),
    Appearance("Appearance", Icons.Default.Palette),
    Blocking("Blocking", Icons.Default.HourglassBottom),
    Protection("Protection", Icons.Default.Shield),
    Pro("Strict Blocker Pro", Icons.Default.Star),
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
    onNavigateToInsights: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val delayMinutes by repository.settings.strictChangeDelayMinutesFlow.collectAsState(initial = 0)
    val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "system")
    val reduceMotion by repository.settings.reduceMotionFlow.collectAsState(initial = false)
    val showPackageOnBlocker by repository.settings.showPackageOnBlockerFlow.collectAsState(initial = false)
    val accountSignedIn by repository.settings.isAccountSignedInFlow.collectAsState(initial = false)
    val accountEmail by repository.settings.accountEmailFlow.collectAsState(initial = "")
    val historyRetentionDays by repository.settings.blockHistoryRetentionDaysFlow.collectAsState(initial = 30)
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
                                delayMinutes = delayMinutes,
                                isProtectionEnabled = isProtectionEnabled,
                                reduceMotion = reduceMotion,
                                showPackageOnBlocker = showPackageOnBlocker,
                                accountSignedIn = accountSignedIn,
                                accountEmail = accountEmail,
                                historyRetentionDays = historyRetentionDays,
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
                                onReduceMotionChange = { enabled ->
                                    scope.launch { repository.settings.setReduceMotion(enabled) }
                                },
                                onShowPackageOnBlockerChange = { enabled ->
                                    scope.launch { repository.settings.setShowPackageOnBlocker(enabled) }
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

                SettingsSection.entries.forEach { section ->
                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 0.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                SettingsSectionContent(
                                    section = section,
                                    themeMode = themeMode,
                                    delayMinutes = delayMinutes,
                                    isProtectionEnabled = isProtectionEnabled,
                                    reduceMotion = reduceMotion,
                                    showPackageOnBlocker = showPackageOnBlocker,
                                    accountSignedIn = accountSignedIn,
                                    accountEmail = accountEmail,
                                    historyRetentionDays = historyRetentionDays,
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
                                    onReduceMotionChange = { enabled ->
                                        scope.launch { repository.settings.setReduceMotion(enabled) }
                                    },
                                    onShowPackageOnBlockerChange = { enabled ->
                                        scope.launch { repository.settings.setShowPackageOnBlocker(enabled) }
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
    delayMinutes: Int,
    isProtectionEnabled: Boolean,
    reduceMotion: Boolean,
    showPackageOnBlocker: Boolean,
    accountSignedIn: Boolean,
    accountEmail: String,
    historyRetentionDays: Int,
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
    onReduceMotionChange: (Boolean) -> Unit,
    onShowPackageOnBlockerChange: (Boolean) -> Unit,
    onHistoryRetentionChange: (Int) -> Unit
) {
    when (section) {
        SettingsSection.Account -> {
            SettingsDescription(
                if (accountSignedIn) "Signed in as " + accountEmail
                else if (accountEmail.isNotBlank()) "Signed out • remembered account: " + accountEmail
                else "Connect a Google account for identity and future sync features."
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsLinkRow(
                title = if (accountSignedIn) "Manage account" else "Sign in with Google",
                subtitle = "Account identity, sign-out and remembered account controls",
                onClick = onNavigateToAccount
            )
        }

        SettingsSection.Appearance -> {
            SettingsDescription("Choose how the app follows your device theme.")
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

        SettingsSection.Protection -> {
            SettingsLinkRow(
                title = "18+ content protection",
                subtitle = "Browser, SafeSearch and DNS controls",
                onClick = onNavigateToAdultProtection
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsLinkRow(
                title = "Anti-bypass information",
                subtitle = "Understand Android limits and stronger device-owner options",
                onClick = onNavigateToAntiBypassGuide
            )
        }

        SettingsSection.Pro -> {
            SettingsDescription("Unlock custom blocker photos, animated blocker styles and premium personalization.")
            Spacer(modifier = Modifier.height(12.dp))
            SettingsLinkRow(
                title = "View Strict Blocker Pro",
                subtitle = "See subscription options and restore purchases",
                onClick = onNavigateToPaywall
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsLinkRow(
                title = "Customize block screen",
                subtitle = "Choose animations, focus styles or your own photo",
                onClick = onNavigateToBlockerCustomization
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsLinkRow(
                title = "Focus insights",
                subtitle = "Daily, weekly and monthly block statistics",
                onClick = onNavigateToInsights
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
            SettingsDescription("Version 1.3")
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { },
                enabled = false,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Version 1.3")
            }
            Spacer(modifier = Modifier.height(8.dp))
            SettingsDescription("Schedules and app preferences are stored on-device.")
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
