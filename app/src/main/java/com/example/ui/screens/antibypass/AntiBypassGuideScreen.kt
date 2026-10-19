package com.example.ui.screens.antibypass

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard

@Composable
fun AntiBypassGuideScreen(
    onNavigateBack: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 14.dp)
                .testTag("anti_bypass_guide_root"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Security Architecture",
                            style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Honest, legitimate non-root Android enforcement principles",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 1: Non-Root Platform Security
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Standard Android Platform Guarantees",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Strict Blocker employs the strongest legitimate non-root blocking mechanism supported by Android:\n\n" +
                                    "1. Instant Interception: The Accessibility Service captures window state transitions in real time (<50ms) and interrupts unallowed applications.\n" +
                                    "2. Dual-Engine Verification: A background monitoring service uses UsageStatsManager to verify active applications even if accessibility is temporarily busy.\n" +
                                    "3. Exact Schedule Cutoffs: AlarmManager RTC_WAKEUP alarms ensure apps running during an allowed period are immediately terminated when the allowed window expires.\n" +
                                    "4. Self-Protection: Essential system packages, launcher, and Strict Blocker itself are automatically shielded from being blocked.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Section 2: Honest Boundaries
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = Color(0xFFF59E0B).copy(alpha = 0.35f),
                    backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.08f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Honest Platform Limitations",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Android does not allow non-root, non-enterprise applications to prevent uninstallation or revoke permissions. As the physical device owner, you can always:\n\n" +
                                    "• Revoke Accessibility or Usage Access in Android Settings\n" +
                                    "• Force-Stop the application\n" +
                                    "• Uninstall Strict Blocker\n\n" +
                                    "Strict Blocker does NOT use deceptive lock-in tricks or malicious hidden Device Admin privileges. Instead, it creates intentional friction (schedule change delays) to defeat impulsive bypass without trapping the user.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Section 3: Advanced Enterprise Device Owner
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Optional: Enterprise Device Owner Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "For managed enterprise environments, school tablets, or dedicated Samsung Galaxy Tab A9+ kiosk setups requiring complete uninstall prevention:\n\n" +
                                    "Device Owner mode can be provisioned via Android Debug Bridge (ADB) upon a fresh factory reset using Samsung Knox or Android Enterprise:\n\n" +
                                    "adb shell dpm set-device-owner com.aistudio.strictblocker.kzvwpq/.receiver.DeviceAdmin\n\n" +
                                    "This is strictly optional for enterprise deployments. Normal daily operation requires neither root nor ADB.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
