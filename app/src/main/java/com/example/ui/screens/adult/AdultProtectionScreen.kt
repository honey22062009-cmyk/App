package com.example.ui.screens.adult

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdultProtectionScreen(
    repository: AppBlockerRepository,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val enabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val blockIncognito by repository.settings.isBlockIncognitoEnabledFlow.collectAsState(initial = true)
    val safeSearch by repository.settings.isForceSafeSearchEnabledFlow.collectAsState(initial = true)
    val blockedCount by repository.settings.adultBlocksCountFlow.collectAsState(initial = 0)
    val unlockNotBefore by repository.settings.adultUnlockNotBeforeFlow.collectAsState(initial = 0L)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val remainingSeconds = ((unlockNotBefore - now) / 1000L).coerceAtLeast(0L)
    var accessibilityAllowed by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(unlockNotBefore) {
        while (unlockNotBefore > 0L && System.currentTimeMillis() < unlockNotBefore) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
        now = System.currentTimeMillis()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityAllowed = PermissionHelper.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Browser Guard", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Optional, on-device checks for supported browser address bars and search pages. DNS and VPN filtering have been removed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 5.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Browser checks", style = MaterialTheme.typography.titleLarge)
                        Text(if (enabled && accessibilityAllowed) "Enabled and Accessibility is allowed" else if (enabled) "Waiting for Accessibility permission" else "Paused", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enabled, onCheckedChange = { value ->
                        if (value) scope.launch { repository.settings.setAdultProtectionEnabled(true) }
                        else showDisableDialog = true
                    })
                }
                Text("$blockedCount locally recorded interruptions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!accessibilityAllowed) {
                    Button(onClick = onNavigateToPermissions, modifier = Modifier.fillMaxWidth()) { Text("Set up Accessibility") }
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 5.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Private browsing indicator", style = MaterialTheme.typography.titleMedium)
                        Text("Block incognito/private browser windows when detectable.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = blockIncognito, onCheckedChange = { value -> scope.launch { repository.settings.setBlockIncognitoEnabled(value) } })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 5.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SafeSearch preference", style = MaterialTheme.typography.titleMedium)
                        Text("Use supported search-page SafeSearch checks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = safeSearch, onCheckedChange = { value -> scope.launch { repository.settings.setForceSafeSearchEnabled(value) } })
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Important limits", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Browser protection uses visible accessibility information on supported browsers. It cannot guarantee detection in every browser, app, encrypted view or web page. It does not inspect or upload your browsing history. No DNS resolver, VPN tunnel or network traffic filter is included.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (unlockNotBefore > 0L) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (remainingSeconds > 0L) "Pause request: ${remainingSeconds / 60L}m ${remainingSeconds % 60L}s remaining"
                        else "The pause delay is complete.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("After the delay, confirm once more to finish pausing Browser Guard.", style = MaterialTheme.typography.bodySmall)
                    if (remainingSeconds == 0L) {
                        Button(onClick = {
                            scope.launch {
                                repository.settings.markAdultUnlockVerificationPassed()
                                val completed = repository.settings.completeAdultProtectionDisable(false)
                                notice = if (completed) "Browser Guard is paused." else "The pause could not be completed. Check the timer and try again."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Complete pause") }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onNavigateToPermissions, modifier = Modifier.fillMaxWidth()) { Text("Review app permissions") }
        if (notice != null) Text(notice!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }

    if (showDisableDialog) {
        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text("Pause Browser Guard?") },
            text = { Text("A delay is applied before strict protection can be disabled. Continue to request the change?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.settings.requestAdultProtectionDisable(10)
                        notice = "Pause request saved. The 10-minute delay is now running."
                    }
                    showDisableDialog = false
                }) { Text("Request pause") }
            },
            dismissButton = { TextButton(onClick = { showDisableDialog = false }) { Text("Cancel") } }
        )
    }
}
