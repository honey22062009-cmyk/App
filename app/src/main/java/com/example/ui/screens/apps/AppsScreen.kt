package com.example.ui.screens.apps

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppBlockerRepository
import com.example.data.repository.InstalledAppInfo
import com.example.security.AppCapabilityScanner
import com.example.security.AppCapabilityReport
import com.example.security.GuardianPolicyManager
import com.example.ui.components.AppIconImage
import com.example.ui.components.GlassCard
import com.example.ui.components.InfoChip
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AppsScreen(
    repository: AppBlockerRepository,
    onConfigureScheduleForApp: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Managed", "Unmanaged"
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var showGroupDialog by remember { mutableStateOf(false) }
    var showCapabilityAudit by remember { mutableStateOf(false) }
    var capabilityReports by remember { mutableStateOf<List<AppCapabilityReport>>(emptyList()) }
    var isCapabilityAuditLoading by remember { mutableStateOf(false) }
    var showGuardianConfirm by remember { mutableStateOf(false) }
    var guardianApplyBusy by remember { mutableStateOf(false) }
    val guardianPolicyManager = remember(context) { GuardianPolicyManager(context) }
    val guardianManaged = remember(context) { guardianPolicyManager.isGuardianManaged }
    val groups by repository.appGroupsFlow.collectAsState(initial = emptyList())
    val groupLimit = 8

    val selectedPackages = remember { mutableStateListOf<String>() }

    fun refreshApps() {
        isLoading = true
        loadError = false
        coroutineScope.launch {
            try {
                installedApps = repository.getInstalledLaunchableApps()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadError = true
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshApps()
    }

    val filteredApps = remember(installedApps, searchQuery, selectedFilter) {
        installedApps.filter { app ->
            val matchesSearch = searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Managed" -> app.isManaged
                "Unmanaged" -> !app.isManaged && !app.isSystemCritical
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedBy { it.appName.lowercase(Locale.getDefault()) }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 14.dp)
                .testTag("apps_screen_root")
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Apps",
                        style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${installedApps.count { it.isManaged }} protected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { showGroupDialog = true },
                    enabled = groups.size < groupLimit
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        when {
                            groups.size < groupLimit -> "New group"
                            else -> "Group limit reached"
                        }
                    )
                }
            }

            if (groups.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${groups.size}/$groupLimit groups",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groups.take(3).forEach { group ->
                        GlassCard(elevation = 0.dp) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(group.name, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    isCapabilityAuditLoading = true
                    showCapabilityAudit = true
                    coroutineScope.launch {
                        capabilityReports = withContext(Dispatchers.IO) {
                            AppCapabilityScanner(context).scanInstalledApps()
                        }
                        isCapabilityAuditLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCapabilityAuditLoading
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCapabilityAuditLoading) "Reviewing installed apps…" else "Review app permissions & capabilities")
            }

            if (guardianManaged) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Guardian device protection active", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Android confirms this phone is managed. Apply suspension and uninstall protection to apps you have marked as protected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { showGuardianConfirm = true },
                            enabled = !guardianApplyBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (guardianApplyBusy) "Applying guardian controls…" else "Apply to protected apps") }
                    }
                }
            } else {
                Text(
                    "Guardian device-owner protection is not active. Strong uninstall protection requires guardian-led Android device provisioning.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Modern Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("apps_search_input"),
                label = { Text("Search apps") },
                placeholder = {
                    Text(
                        "Name or package",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("All", "Managed", "Unmanaged").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (selectedPackages.isNotEmpty()) {
                    Text(
                        text = "${selectedPackages.size} selected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Animated Batch Action Toolbar
            AnimatedVisibility(
                visible = selectedPackages.isNotEmpty(),
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(180)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(140))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedPackages.size} selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            for (pkg in selectedPackages) {
                                                val app = installedApps.firstOrNull { it.packageName == pkg }
                                                if (app != null && !app.isSystemCritical) {
                                                    repository.setAppManaged(pkg, app.appName, true)
                                                }
                                            }
                                            selectedPackages.clear()
                                            refreshApps()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Protect", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            for (pkg in selectedPackages) {
                                                val app = installedApps.firstOrNull { it.packageName == pkg }
                                                if (app != null) {
                                                    repository.setAppManaged(pkg, app.appName, false)
                                                }
                                            }
                                            selectedPackages.clear()
                                            refreshApps()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Unprotect", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Apps List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            } else if (loadError) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            "Couldn't load installed apps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Android didn't return the app list. Try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { refreshApps() }) { Text("Retry") }
                    }
                }
            } else if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        val emptyMessage = when {
                            installedApps.isEmpty() -> "No launchable apps found."
                            searchQuery.isNotBlank() -> "No apps match “$searchQuery”."
                            selectedFilter == "Managed" -> "No apps are managed yet."
                            selectedFilter == "Unmanaged" -> "No unmanaged apps in this list."
                            else -> "No apps available."
                        }
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (installedApps.isEmpty()) {
                            OutlinedButton(onClick = { refreshApps() }) {
                                Text("Refresh app list")
                            }
                        } else if (searchQuery.isNotBlank()) {
                            OutlinedButton(onClick = { searchQuery = "" }) {
                                Text("Clear search")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "installed_app" }
                    ) { app ->
                        AppItemCard(
                            app = app,
                            isSelected = selectedPackages.contains(app.packageName),
                            onToggleSelected = {
                                if (selectedPackages.contains(app.packageName)) {
                                    selectedPackages.remove(app.packageName)
                                } else {
                                    selectedPackages.add(app.packageName)
                                }
                            },
                            onToggleManaged = { isManaged ->
                                // Optimistic row update avoids re-querying every installed app
                                // and rebuilding the whole list after each switch.
                                installedApps = installedApps.map { item ->
                                    if (item.packageName == app.packageName) item.copy(isManaged = isManaged) else item
                                }
                                coroutineScope.launch {
                                    repository.setAppManaged(app.packageName, app.appName, isManaged)
                                    if (!isManaged && guardianManaged) {
                                        withContext(Dispatchers.IO) {
                                            guardianPolicyManager.removeRestrictions(app.packageName)
                                        }
                                    }
                                }
                            },
                            onOpenSchedule = {
                                onConfigureScheduleForApp(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showGuardianConfirm) {
        val targets = installedApps.filter { it.isManaged && !it.isSystemCritical }
        AlertDialog(
            onDismissRequest = { if (!guardianApplyBusy) showGuardianConfirm = false },
            title = { Text("Apply guardian restrictions?") },
            text = {
                Text(
                    "Android will suspend ${targets.size} apps and block their uninstall where the system permits it. Essential system apps are excluded. A guardian can remove these restrictions from this app."
                )
            },
            confirmButton = {
                Button(
                    enabled = !guardianApplyBusy && targets.isNotEmpty(),
                    onClick = {
                        guardianApplyBusy = true
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                guardianPolicyManager.applyRestrictions(targets.map { it.packageName }.toSet())
                            }
                            guardianApplyBusy = false
                            showGuardianConfirm = false
                            android.widget.Toast.makeText(
                                context,
                                "Suspended: ${result.suspended.size}. Uninstall protected: ${result.uninstallBlocked.size}. Needs review: ${result.failures.size}.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) { Text("Apply restrictions") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showGuardianConfirm = false }, enabled = !guardianApplyBusy) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCapabilityAudit) {
        AppCapabilityAuditDialog(
            reports = capabilityReports,
            loading = isCapabilityAuditLoading,
            onDismiss = { showCapabilityAudit = false }
        )
    }

    if (showGroupDialog) {
        AppGroupDialog(
            apps = installedApps.filter { !it.isSystemCritical },
            onDismiss = { showGroupDialog = false },
            onSave = { name, packages ->
                coroutineScope.launch {
                    val saved = repository.saveAppGroup(name, packages)
                    if (saved) {
                        packages.forEach { pkg ->
                            installedApps.firstOrNull { it.packageName == pkg }?.let { app ->
                                repository.setAppManaged(app.packageName, app.appName, true)
                            }
                        }
                        showGroupDialog = false
                        refreshApps()
                    } else {
                        showGroupDialog = false
                        android.widget.Toast.makeText(context, "Could not save this group. Check its name and try again.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun AppCapabilityAuditDialog(
    reports: List<AppCapabilityReport>,
    loading: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Installed app capability review") },
        text = {
            when {
                loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
                reports.isEmpty() -> Text("Android did not return an app inventory.")
                else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${reports.size} visible installed apps. Permission requests are signals, not proof of harmful behavior.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(reports, key = { it.packageName }) { report ->
                            var expanded by remember(report.packageName) { mutableStateOf(false) }
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(report.label, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${report.grantedPermissions.size} granted of ${report.requestedPermissionCount} declared permissions",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Components: ${report.declaredActivityCount} activities · ${report.declaredServiceCount} services · ${report.declaredReceiverCount} receivers · ${report.declaredProviderCount} providers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (report.signals.isNotEmpty()) {
                                        Text(report.signals.joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (expanded) {
                                        Spacer(Modifier.height(6.dp))
                                        Text("Package: ${report.packageName}", style = MaterialTheme.typography.labelSmall)
                                        Text("All declared permissions:", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            report.requestedPermissions.joinToString("\n").ifBlank { "None declared" },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            "Granted at install/runtime: " + report.grantedPermissions.joinToString(", ").ifBlank { "None" },
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    } else {
                                        Text("Tap to inspect permission details", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun AppGroupDialog(
    apps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onSave: (String, Set<String>) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    val chosen = remember { mutableStateListOf<String>() }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(28.dp),
        onDismissRequest = onDismiss,
        title = { Text("Create app group") },
        text = {
            Column {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Choose apps",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(apps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (chosen.contains(app.packageName)) chosen.remove(app.packageName)
                                    else chosen.add(app.packageName)
                                }
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(packageName = app.packageName, size = 32.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(app.appName, modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = chosen.contains(app.packageName),
                                onCheckedChange = {
                                    if (chosen.contains(app.packageName)) chosen.remove(app.packageName)
                                    else chosen.add(app.packageName)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(groupName.trim(), chosen.toSet()) },
                enabled = groupName.isNotBlank() && chosen.isNotEmpty()
            ) { Text("Create") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AppItemCard(
    app: InstalledAppInfo,
    isSelected: Boolean,
    onToggleSelected: () -> Unit,
    onToggleManaged: (Boolean) -> Unit,
    onOpenSchedule: () -> Unit
) {
    val cardBg = if (app.isManaged) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardBorder = if (app.isManaged) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = app.isManaged && !app.isSystemCritical) { onOpenSchedule() }
            .testTag("app_item_${app.packageName}"),
        backgroundColor = cardBg,
        borderColor = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(packageName = app.packageName, size = 42.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

            }

            if (!app.isSystemCritical) {
                Switch(
                    checked = app.isManaged,
                    onCheckedChange = { onToggleManaged(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}
