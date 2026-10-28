package com.example.ui.screens.adult

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.core.evaluator.AdultContentEvaluator
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import com.example.service.BrowserGuardStatus
import com.example.ui.components.AccessibilityDisclosure
import kotlinx.coroutines.launch

@Composable
fun AdultProtectionScreen(repository: AppBlockerRepository, onNavigateToPermissions: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val enabled by repository.settings.isAdultProtectionEnabledFlow.collectAsState(initial = true)
    val consent by repository.settings.browserInspectionConsentFlow.collectAsState(initial = false)
    val serviceConnected by BrowserGuardStatus.isConnected.collectAsState()
    val inspections by BrowserGuardStatus.inspections.collectAsState()
    val totalBlocks by repository.settings.adultBlocksCountFlow.collectAsState(initial = 0)
    var accessibility by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var browsers by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDisclosure by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        accessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
        browsers = runCatching {
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE),
                PackageManager.MATCH_ALL).map { it.loadLabel(context.packageManager).toString() }.distinct().sorted()
        }.getOrDefault(emptyList())
    }
    DisposableEffect(lifecycle) {
        refresh()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun openWeb(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) }
            .onFailure { result = "No browser is available. Install a browser and try again." }
    }
    val ready = enabled && consent && accessibility && serviceConnected
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max = 980.dp).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Text("Browser Guard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Browse normally. Interrupt explicit searches.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Shield, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(when {
                            !enabled -> "Browser Guard is off"
                            ready -> "Browser Guard is ready"
                            !consent -> "Review browser access"
                            !accessibility -> "Enable Accessibility to begin"
                            else -> "Waiting for the Android service"
                        }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${totalBlocks} restricted searches or sites interrupted · ${inspections} field checks this session")
                        Text("No VPN connection. No DNS tunnel. Your normal browser connection stays in use.", style = MaterialTheme.typography.bodyMedium)
                        if (enabled && !ready) Button(onClick = {
                            if (!consent) showDisclosure = true else PermissionHelper.openAccessibilitySettings(context)
                        }) { Text(if (!consent) "Review and enable" else "Open Accessibility settings") }
                    }
                }
            }
            item {
                GuardCard("What gets interrupted") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Explicit-search guard", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(checked = enabled, onCheckedChange = { on ->
                            if (on && !consent) showDisclosure = true
                            else scope.launch { repository.settings.setAdultProtectionEnabled(on) }
                        })
                    }
                    Text("Restricted domains and explicit words such as porn, sex and sexual are checked in visible address or search fields. A match returns you to Home and shows a reason.")
                    Text("Opening Chrome, Opera, Firefox or a private tab is allowed. Ordinary page paragraphs, menus and unrelated form fields cannot trigger Browser Guard.")
                    Text("An app you added to a blocking schedule still follows that schedule.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                GuardCard("Start a filtered search") {
                    Text("These shortcuts request the search engine's filter mode. This app cannot lock SafeSearch across every browser without network or managed-device controls.")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { openWeb("https://www.google.com/search?safe=active&q=") }, modifier = Modifier.weight(1f)) { Text("Google · filtered") }
                        FilledTonalButton(onClick = { openWeb("https://www.bing.com/search?adlt=strict&q=") }, modifier = Modifier.weight(1f)) { Text("Bing · strict") }
                    }
                }
            }
            item {
                GuardCard("Try the word check safely") {
                    OutlinedTextField(value = query, onValueChange = { query = it.take(500); result = null },
                        label = { Text("A search phrase or website") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedButton(onClick = {
                        result = when (val evaluation = AdultContentEvaluator.evaluateText(query)) {
                            AdultContentEvaluator.EvaluationResult.Clean -> "Allowed by the word and domain check."
                            is AdultContentEvaluator.EvaluationResult.Blocked -> "Would interrupt: ${evaluation.category.label}."
                        }
                    }, enabled = query.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Check without opening a page") }
                    result?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
            item {
                GuardCard("Browser compatibility") {
                    Text(if (browsers.isEmpty()) "No browser handlers detected." else browsers.joinToString(" · "))
                    Text("Opera variants and installed web-opening apps are detected. The guard needs the browser to expose its address or search field to Android Accessibility. Hidden fields, custom browser interfaces and protected private windows can limit inspection.")
                    Text("This is a search and domain guard, not an image/video classifier. It cannot guarantee every adult page is blocked.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onNavigateToPermissions) { Text("Review permissions") }
                }
            }
        }
    }
    if (showDisclosure) AccessibilityDisclosure(onAgree = {
        scope.launch {
            repository.settings.setBrowserInspectionConsent(true)
            repository.settings.setAdultProtectionEnabled(true)
            showDisclosure = false
            if (!accessibility) PermissionHelper.openAccessibilitySettings(context)
        }
    }, onDismiss = { showDisclosure = false })
}

@Composable
private fun GuardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
