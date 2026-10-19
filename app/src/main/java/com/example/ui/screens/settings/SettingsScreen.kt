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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: AppBlockerRepository,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAntiBypassGuide: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val delayMinutes by repository.settings.strictChangeDelayMinutesFlow.collectAsState(initial = 0)
    val isOverlayEnabled by repository.settings.isOverlayShieldEnabledFlow.collectAsState(initial = true)
    val themeMode by repository.settings.themeModeFlow.collectAsState(initial = "system")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 14.dp)
                .testTag("settings_screen_root"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "Settings",
                        style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Manage application appearance, delay friction, and system policies.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 1: Appearance & Theme
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Color Theme",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose between automatic system matching, bright light mode, or calm dark mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themes = listOf(
                                "system" to "System",
                                "light" to "Light",
                                "dark" to "Dark"
                            )
                            themes.forEach { (mode, label) ->
                                val isSelected = themeMode == mode
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.settings.setThemeMode(mode)
                                        }
                                    },
                                    leadingIcon = {
                                        val icon = when (mode) {
                                            "light" -> Icons.Default.LightMode
                                            "dark" -> Icons.Default.DarkMode
                                            else -> Icons.Default.PhoneAndroid
                                        }
                                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Enforcement & Blocking Behavior
            item {
                Text(
                    text = "Enforcement & Friction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Global Protection Toggle
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Strict Protection Master Switch",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "When active, unallowed applications are strictly blocked outside schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isProtectionEnabled,
                            onCheckedChange = { checked ->
                                coroutineScope.launch { repository.settings.setProtectionEnabled(checked) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Schedule Change Delay
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Anti-Impulsive Change Delay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Enforces an intentional waiting period before schedule modifications take effect, preventing in-the-moment bypass.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val options = listOf(0 to "Instant", 5 to "5 min", 15 to "15 min", 30 to "30 min")
                            options.forEach { (mins, label) ->
                                val isSelected = delayMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.settings.setStrictChangeDelayMinutes(mins)
                                        }
                                    },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Navigation Links
            item {
                Text(
                    text = "Security & Compliance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 18+ Adult Content Protection
            item {
                SettingsNavigationRow(
                    title = "18+ Adult Content Protection",
                    subtitle = "DNS filtering, SafeSearch enforcement & browser incognito lock",
                    icon = Icons.Default.Shield,
                    iconTint = Color(0xFF38BDF8),
                    onClick = onNavigateToAdultProtection
                )
            }

            // System Permissions Menu
            item {
                SettingsNavigationRow(
                    title = "System Permissions & Services",
                    subtitle = "Review Accessibility, Usage Access, and Battery settings",
                    icon = Icons.Default.Security,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToPermissions
                )
            }

            // Anti-Bypass & Architecture Guide
            item {
                SettingsNavigationRow(
                    title = "Security & Anti-Bypass Architecture",
                    subtitle = "Learn about Android platform guarantees and enterprise kiosk mode",
                    icon = Icons.Default.Shield,
                    iconTint = Color(0xFFF59E0B),
                    onClick = onNavigateToAntiBypassGuide
                )
            }

            // Privacy Policy, Terms & Legal Compliance
            item {
                SettingsNavigationRow(
                    title = "Privacy, Terms & Legal Compliance",
                    subtitle = "Zero telemetry audit, terms, children's consent & Data Deletion Request",
                    icon = Icons.Default.PrivacyTip,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToLegal
                )
            }

            // About Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Strict Blocker for Android",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0 (Production Release) • 100% On-Device Room Storage • Zero Analytics or Cloud Tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
