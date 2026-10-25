package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.ProtectionHealth
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.AppSurface
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    repository: AppBlockerRepository,
    onNavigateToApps: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToAdultProtection: () -> Unit = {},
    onOpenPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val isProtectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial = true)
    val isAdultProtectionEnabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val managedApps by repository.managedAppsFlow.collectAsState(initial = emptyList())
    val totalBlocks by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val activeManagedApps = managedApps.count { it.isManaged }
    var health by remember(context) { mutableStateOf(ProtectionHealth.snapshot(context)) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) health = ProtectionHealth.snapshot(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        val tablet = maxWidth >= 720.dp
        val contentWidth = if (tablet) 900.dp else maxWidth

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(contentWidth)
                .padding(horizontal = if (tablet) 28.dp else 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Strict Blocker", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("Focus, schedules and protection in one place", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(
                    modifier = Modifier.size(48.dp).shadow(14.dp, CircleShape).background(Color(0xFF7657FF), CircleShape).clickable(onClick = onOpenPro),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = "Strict Blocker Pro", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            AppSurface(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = if (isProtectionEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            when {
                                !isProtectionEnabled -> "Protection paused"
                                !health.coreReady -> "Protection needs setup"
                                health.reliabilityScore < 100 -> "Protection active · ${health.reliabilityScore}% reliability"
                                else -> "Protection active"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text("$activeManagedApps managed apps · $totalBlocks blocks recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isProtectionEnabled, onCheckedChange = { enabled -> scope.launch { repository.settings.setProtectionEnabled(enabled) } })
                }
            }

            if (!health.coreReady || health.reliabilityScore < 100) {
                AppSurface(modifier = Modifier.fillMaxWidth(), backgroundColor = MaterialTheme.colorScheme.surfaceVariant, onClick = onNavigateToPermissions) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (!health.coreReady) "Finish protection setup" else "Improve background reliability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(health.degradedReasons.firstOrNull() ?: "Review Android permissions and reliability settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Text("Quick access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CompactAction("Apps", Icons.Default.Apps, onNavigateToApps, Modifier.weight(1f))
                CompactAction("Schedules", Icons.Default.CalendarMonth, onNavigateToSchedules, Modifier.weight(1f))
                CompactAction("18+ Shield", Icons.Default.Security, onNavigateToAdultProtection, Modifier.weight(1f))
            }

            AppSurface(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToAdultProtection) {
                Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Adult content protection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(if (isAdultProtectionEnabled) "Enabled" else "Off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurface(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.padding(vertical = 15.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
