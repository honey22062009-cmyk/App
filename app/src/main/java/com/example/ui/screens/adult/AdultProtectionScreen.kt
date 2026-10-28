package com.example.ui.screens.adult

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.service.AdultFilterVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdultProtectionScreen(repository: AppBlockerRepository, onNavigateToPermissions: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val enabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val unlockAt by repository.settings.adultUnlockNotBeforeFlow.collectAsState(initial = 0L)
    val resolver by repository.settings.customDnsPrimaryFlow.collectAsState(initial = "1.1.1.3")
    val secondary by repository.settings.customDnsSecondaryFlow.collectAsState(initial = "1.0.0.3")
    val vpn by AdultFilterVpnService.isVpnActive.collectAsState()
    val alwaysOn by AdultFilterVpnService.alwaysOn.collectAsState()
    val queries by AdultFilterVpnService.processedQueries.collectAsState()
    var accessibility by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var advanced by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var showDisable by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf("") }
    var browsers by remember { mutableStateOf<List<String>>(emptyList()) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
                browsers = runCatching {
                    context.packageManager.queryIntentActivities(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE),
                        PackageManager.MATCH_ALL).map { it.loadLabel(context.packageManager).toString() }.distinct().sorted()
                }.getOrDefault(emptyList())
                // Refresh the live service's always-on read after returning from system Settings.
                if (vpn) AdultFilterVpnService.start(context)
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        browsers = runCatching {
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE),
                PackageManager.MATCH_ALL).map { it.loadLabel(context.packageManager).toString() }.distinct().sorted()
        }.getOrDefault(emptyList())
    }
    LaunchedEffect(unlockAt) {
        while (unlockAt > 0 && now < unlockAt) { delay(1000); now = System.currentTimeMillis() }
    }
    val requestVpn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) AdultFilterVpnService.start(context)
        else result = "DNS filter is off. Android VPN permission was not granted."
    }
    fun connect() {
        scope.launch {
            if (!enabled) repository.settings.setAdultProtectionEnabled(true)
            repository.settings.setDnsFilterEnabled(true)
            val intent = VpnService.prepare(context)
            if (intent == null) AdultFilterVpnService.start(context) else requestVpn.launch(intent)
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 128.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("18+ protection", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Clear boundaries for your browsing", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = if (enabled && vpn && accessibility)
                MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Shield, null, Modifier.size(38.dp))
                    Text(when {
                        !enabled -> "Protection is off"
                        vpn && accessibility -> "Both protection layers are running"
                        vpn || accessibility -> "Finish protection setup"
                        else -> "Protection needs setup"
                    }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("${if (accessibility) "Browser inspection on" else "Browser inspection off"} · ${if (vpn) "DNS filter connected" else "DNS filter disconnected"}")
                    if (!vpn) Button(onClick = ::connect) { Text("Connect DNS filter") }
                }
            }
        }
        item { ProtectionStep("1", "Browser inspection", accessibility,
            "Checks visible URLs and searches, including Opera, Opera GX, Chrome, Firefox and other installed browser handlers.",
            "Enable browser inspection", { PermissionHelper.openAccessibilitySettings(context) }) }
        item { ProtectionStep("2", "DNS filtering + SafeSearch", vpn,
            "Blocks restricted domains through system DNS. Google and Bing searches use their strict DNS endpoints while this filter is connected.",
            "Connect DNS filter", ::connect) }
        item { ProtectionStep("3", "Keep the filter running", vpn && alwaysOn,
            "Android's Always-on VPN setting can restart the filter after interruptions. This app does not change that setting for you.",
            "Open Android VPN settings", { PermissionHelper.openVpnSettings(context) }) }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Check your protection", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${queries} DNS queries processed in this app session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        testing = true
                        scope.launch {
                            val before = AdultFilterVpnService.processedQueries.value
                            val check = ProtectionProbe.check(resolver)
                            result = if (AdultFilterVpnService.processedQueries.value <= before)
                                "The check did not pass through this DNS filter. Reconnect the filter and retry."
                                else check
                            testing = false
                        }
                    }, enabled = vpn && !testing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (testing) "Checking DNS…" else "Run a real DNS check")
                    }
                    OutlinedButton(onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://example.com/?q=porn&strictblocker=safe-test")).addCategory(Intent.CATEGORY_BROWSABLE)) }
                            .onFailure { result = "No browser is available for the test." }
                    }, enabled = accessibility, modifier = Modifier.fillMaxWidth()) { Text("Open safe browser check") }
                    Text("The browser check uses example.com with a test query. A working browser guard should interrupt it. No adult page is opened.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    result?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(18.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Browser coverage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (browsers.isEmpty()) "Browser detection refreshes when this screen opens." else browsers.joinToString(" · "))
                    Text("New browsers are detected by their web-opening capability. Private tabs are blocked when their active private-mode controls are visible; private-only Tor and Firefox Focus browsers are restricted.")
                    Text("Built-in browser VPNs, encrypted DNS, hidden URL bars and cached pages can reduce coverage. Turn off a browser's own VPN or secure DNS for system DNS filtering. Android permissions must stay enabled.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) "Hide details" else "Details and controls") }
                    if (advanced) {
                        Text("DNS resolver: $resolver · $secondary")
                        Text("DNS-only VPN: leave ‘Block connections without VPN’ off. Lockdown requires a full-traffic VPN; enabling it here can stop ordinary internet traffic.")
                        Text("Domain queries go to the DNS provider. Page text is inspected on this device and is not uploaded.")
                        TextButton(onClick = onNavigateToPermissions) { Text("Review Android permissions") }
                        if (enabled) OutlinedButton(onClick = {
                            scope.launch {
                                if (unlockAt == 0L) repository.settings.requestAdultProtectionDisable()
                                showDisable = true
                            }
                        }) { Text("Request protection off") }
                    }
                }
            }
        }
    }
    if (showDisable) AlertDialog(onDismissRequest = { showDisable = false },
        title = { Text("Turn protection off") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val remaining = ((unlockAt - now).coerceAtLeast(0) + 59_999) / 60_000
                Text(if (unlockAt == 0L || remaining > 0) "Protection remains on during the 10-minute waiting period. ${remaining} minute(s) remaining."
                    else "Waiting period complete. Type TURN OFF to confirm.")
                if (unlockAt > 0 && now >= unlockAt) OutlinedTextField(value = confirmation, onValueChange = { confirmation = it }, label = { Text("TURN OFF") })
            }
        },
        confirmButton = { TextButton(onClick = {
            scope.launch {
                if (repository.settings.completeAdultProtectionDisable(confirmation.trim() == "TURN OFF")) {
                    AdultFilterVpnService.stop(context); showDisable = false
                }
            }
        }, enabled = unlockAt > 0 && now >= unlockAt && confirmation.trim() == "TURN OFF") { Text("Turn off") } },
        dismissButton = { TextButton(onClick = { showDisable = false }) { Text("Keep protection on") } })
}

@Composable
private fun ProtectionStep(number: String, title: String, ready: Boolean, description: String, action: String, onAction: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(if (ready) "✓" else number, Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!ready) TextButton(onClick = onAction) { Text(action) }
            }
        }
    }
}
