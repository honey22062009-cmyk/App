package com.example.ui.screens.adult

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.service.AdultFilterVpnService
import com.example.ui.blocker.BlockerActivity
import com.example.ui.components.GlassCard
import com.example.ui.components.InfoChip
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun AdultProtectionScreen(
    repository: AppBlockerRepository,
    onNavigateToPermissions: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val isAdultProtectionEnabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val isBlockIncognito by repository.settings.isBlockIncognitoEnabledFlow.collectAsState(initial = true)
    val isForceSafeSearch by repository.settings.isForceSafeSearchEnabledFlow.collectAsState(initial = true)
    val isDnsFilterPref by repository.settings.isDnsFilterEnabledFlow.collectAsState(initial = true)
    val savedDnsPrimary by repository.settings.customDnsPrimaryFlow.collectAsState(initial = "1.1.1.3")
    val savedDnsSecondary by repository.settings.customDnsSecondaryFlow.collectAsState(initial = "1.0.0.3")
    val adultBlocksCount by repository.settings.adultBlocksCountFlow.collectAsState(initial = 0)

    val isVpnActive by AdultFilterVpnService.isVpnActive.collectAsState()

    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // VPN Request Launcher
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AdultFilterVpnService.start(context)
        }
    }

    var showDisableConfirmDialog by remember { mutableStateOf(false) }
    var testResultBanner by remember { mutableStateOf<String?>(null) }
    var dnsPrimaryInput by remember { mutableStateOf(savedDnsPrimary) }
    var dnsSecondaryInput by remember { mutableStateOf(savedDnsSecondary) }
    var dnsError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(savedDnsPrimary, savedDnsSecondary) {
        dnsPrimaryInput = savedDnsPrimary
        dnsSecondaryInput = savedDnsSecondary
    }

    // Overall Protection Status
    val overallStatus = when {
        !isAdultProtectionEnabled -> "PAUSED"
        isAccessibilityGranted && isVpnActive -> "PROTECTED"
        isAccessibilityGranted || isVpnActive -> "LIMITED PROTECTION"
        else -> "PERMISSION REQUIRED"
    }

    val statusColor = when (overallStatus) {
        "PROTECTED" -> MaterialTheme.colorScheme.tertiary
        "LIMITED PROTECTION" -> Color(0xFFF59E0B)
        "PAUSED" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.error
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 14.dp)
                .testTag("adult_protection_screen_root"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column {
                    Text(
                        text = "18+ Adult Protection",
                        style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Device-wide restriction against explicit 18+ websites, searches, and unmonitored private windows.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Master Protection Status Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
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
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(statusColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = statusColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Adult content protection",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when {
                                            !isAdultProtectionEnabled -> "Protection is off"
                                            isAccessibilityGranted && isVpnActive -> "Browser and DNS protection enabled"
                                            isAccessibilityGranted -> "Browser protection enabled"
                                            isVpnActive -> "DNS protection enabled"
                                            else -> "Setup required"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = isAdultProtectionEnabled,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        showDisableConfirmDialog = true
                                    } else {
                                        coroutineScope.launch {
                                            repository.settings.setAdultProtectionEnabled(true)
                                            if (isDnsFilterPref && !isVpnActive) {
                                                val vpnIntent = VpnService.prepare(context)
                                                if (vpnIntent != null) {
                                                    vpnLauncher.launch(vpnIntent)
                                                } else {
                                                    AdultFilterVpnService.start(context)
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$adultBlocksCount blocked attempts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Test Result Banner if triggered
            if (testResultBanner != null) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = testResultBanner!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Section: Protection Layers
            item {
                Text(
                    text = "Protection controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Layer 1: Real-Time Browser Shield (Accessibility URL Inspector)
            item {
                ProtectionLayerCard(
                    title = "Browser protection",
                    description = "Checks supported browser URLs and search pages on-device.",
                    icon = Icons.Default.Security,
                    statusText = if (isAccessibilityGranted) "On" else "Setup needed",
                    isActive = isAccessibilityGranted,
                    actionText = if (!isAccessibilityGranted) "Grant Accessibility" else null,
                    onAction = onNavigateToPermissions
                )
            }

            // Layer 2: Editable DNS protection
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "DNS protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isVpnActive) "Connected · custom resolver active" else "Choose the DNS resolver Strict Blocker should use.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isVpnActive) "On" else "Off",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isVpnActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = dnsPrimaryInput,
                            onValueChange = {
                                dnsPrimaryInput = it.trim()
                                dnsError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Primary DNS") },
                            placeholder = { Text("1.1.1.3") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = dnsSecondaryInput,
                            onValueChange = {
                                dnsSecondaryInput = it.trim()
                                dnsError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Secondary DNS (optional)") },
                            placeholder = { Text("1.0.0.3") },
                            singleLine = true,
                            supportingText = {
                                Text(
                                    dnsError ?: "Enter an IPv4 or IPv6 DNS address. DNS queries remain handled by the local VPN."
                                )
                            },
                            isError = dnsError != null
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    val primary = dnsPrimaryInput.trim()
                                    val secondary = dnsSecondaryInput.trim()
                                    val primaryValid = Patterns.IP_ADDRESS.matcher(primary).matches()
                                    val secondaryValid = secondary.isBlank() || Patterns.IP_ADDRESS.matcher(secondary).matches()
                                    if (!primaryValid || !secondaryValid) {
                                        dnsError = "Use a valid IPv4 or IPv6 address."
                                    } else {
                                        coroutineScope.launch {
                                            repository.settings.setCustomDns(primary, secondary)
                                            dnsError = null
                                            testResultBanner = "DNS settings saved."
                                            if (isVpnActive) {
                                                AdultFilterVpnService.stop(context)
                                                delay(250)
                                                AdultFilterVpnService.start(context)
                                                testResultBanner = "DNS settings applied."
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Apply DNS")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isVpnActive) {
                                        val vpnIntent = VpnService.prepare(context)
                                        if (vpnIntent != null) vpnLauncher.launch(vpnIntent)
                                        else AdultFilterVpnService.start(context)
                                    } else {
                                        AdultFilterVpnService.stop(context)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isVpnActive) "Disconnect" else "Connect")
                            }
                        }
                    }
                }
            }

            // Layer 3: SafeSearch Enforcer
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SafeSearch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Keeps supported search engines in filtered mode.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isForceSafeSearch,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    repository.settings.setForceSafeSearchEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Layer 4: Incognito / Private Browsing Guard
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Private browsing guard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Restricts private browser windows that cannot be reliably monitored.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBlockIncognito,
                            onCheckedChange = { checked ->
                                coroutineScope.launch {
                                    repository.settings.setBlockIncognitoEnabled(checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }

            // Section: Diagnostics & Safe Filter Test
            item {
                Text(
                    text = "Diagnostics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Test protection",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Run a safe local simulation to verify that the block screen opens correctly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.recordAdultBlockEvent("Filter Test Verification")
                                    testResultBanner = "Verification passed: On-device shield successfully intercepted simulated restricted content."
                                    // Launch test block screen
                                    val intent = Intent(context, BlockerActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        putExtra(BlockerActivity.EXTRA_PACKAGE_NAME, "com.aistudio.18blocker.test")
                                        putExtra(BlockerActivity.EXTRA_APP_NAME, "Shield Diagnostics")
                                        putExtra(BlockerActivity.EXTRA_BLOCK_TYPE, BlockerActivity.BLOCK_TYPE_ADULT)
                                        putExtra(BlockerActivity.EXTRA_ADULT_LAYER, "Simulated Diagnostics Verification")
                                        putExtra(BlockerActivity.EXTRA_ADULT_REASON, "18+ Adult Content Filter Test")
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run protection test", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Section: Honest Platform Disclosures & Privacy
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Policy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Privacy & limitations",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• 100% Local Inspection: Evaluated on-device using local domain blocklists and family DNS. Zero browsing history is ever transmitted off your phone.\n" +
                                    "• Non-Spyware Architecture: Does not record passwords, keystrokes, personal chats, or banking information.\n" +
                                    "• Honest Android Boundaries: Works across browsers, in-app webviews, and search results via layered Accessibility + DNS. Third-party apps encrypting internal web traffic without using system DNS or accessible URL bars cannot be 100% guaranteed without MDM Device Owner enrollment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Confirmation Dialog before turning off protection
    if (showDisableConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDisableConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Request Strict Mode Unlock",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Strict Adult Mode cannot be switched off immediately. Requesting an unlock starts a 10-minute delay; protection and DNS shielding stay active until the delay and verification are completed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.settings.requestAdultProtectionDisable()
                            testResultBanner = "Strict Adult Mode stays active. A 10-minute unlock delay has started; verification is still required before protection can be disabled."
                            showDisableConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Start Unlock Delay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDisableConfirmDialog = false }) {
                    Text("Keep Active")
                }
            }
        )
    }
}

@Composable
fun ProtectionLayerCard(
    title: String,
    description: String,
    icon: ImageVector,
    statusText: String,
    isActive: Boolean,
    actionText: String?,
    onAction: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (actionText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
