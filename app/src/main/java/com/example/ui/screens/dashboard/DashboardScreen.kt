package com.example.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.system.PermissionHelper
import com.example.data.repository.AppBlockerRepository
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
 repository:AppBlockerRepository,
 onNavigateToApps:()->Unit,
 onNavigateToPermissions:()->Unit,
 onNavigateToSchedules:()->Unit,
 onNavigateToAdultProtection:()->Unit={},
 onNavigateToAccount:()->Unit={},
 onNavigateToUpgrade:()->Unit={},
 onNavigateToDiagnostics:()->Unit={},
 onNavigateToReports:()->Unit={},
 onNavigateToBackup:()->Unit={},
 onNavigateToTemplates:()->Unit={}
) {
 val context=androidx.compose.ui.platform.LocalContext.current
 val owner=LocalLifecycleOwner.current
 val scope= rememberCoroutineScope()
 val protectionEnabled by repository.settings.isProtectionEnabledFlow.collectAsState(initial=true)
 val managedApps by repository.managedAppsFlow.collectAsState(initial=emptyList())
 val periods by repository.allPeriodsFlow.collectAsState(initial=emptyList())
 val totalBlocks by repository.totalBlocksCountFlow.collectAsState(initial=0)
 val recentBlocks by repository.recentBlocksFlow.collectAsState(initial=emptyList())
 val activeApps=managedApps.filter { it.isManaged }
 var permissionsHealthy by remember { mutableStateOf(false) }
 var nextTransition by remember { mutableStateOf<com.example.data.repository.NextTransitionInfo?>(null) }

 DisposableEffect(context,owner) {
  fun refresh() {
   permissionsHealthy=PermissionHelper.isAccessibilityServiceEnabled(context) &&
    PermissionHelper.isUsageAccessGranted(context) && PermissionHelper.canDrawOverlays(context) &&
    PermissionHelper.canScheduleExactAlarms(context)
  }
  val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) refresh() }
  owner.lifecycle.addObserver(observer); refresh()
  onDispose { owner.lifecycle.removeObserver(observer) }
 }
 LaunchedEffect(activeApps,periods) { nextTransition=repository.findNextGlobalTransition() }

 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   Column(Modifier.weight(1f)) {
    Text("STRICT BLOCKER",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
    Text("Focus dashboard",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
    Text(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM",Locale.getDefault())),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
   IconButton(onClick=onNavigateToAccount) { Icon(Icons.Default.Person,contentDescription="Account",modifier=Modifier.size(28.dp)) }
  }

  Card(shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=if(protectionEnabled && permissionsHealthy) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
   Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically) {
     Icon(Icons.Default.Shield,contentDescription=null,modifier=Modifier.size(34.dp))
     Spacer(Modifier.width(12.dp))
     Column(Modifier.weight(1f)) {
      Text(when { !protectionEnabled->"Protection is paused"; !permissionsHealthy->"Setup needs attention"; else->"Protection is ready" },style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text(when { !protectionEnabled->"Turn it back on to enforce your saved rules."; !permissionsHealthy->"One or more Android permissions are missing."; else->"Your schedules are applied on this device." },style=MaterialTheme.typography.bodyMedium)
     }
    }
    Row(verticalAlignment=Alignment.CenterVertically) {
     Text("App blocking",modifier=Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
     Switch(checked=protectionEnabled,onCheckedChange={enabled -> scope.launch { repository.settings.setProtectionEnabled(enabled) }})
    }
    if(!permissionsHealthy) Button(onClick=onNavigateToDiagnostics,modifier=Modifier.fillMaxWidth()) {
     Icon(Icons.Default.Build,contentDescription=null); Spacer(Modifier.width(8.dp)); Text("Run protection check")
    }
   }
  }

  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   SummaryCard("Managed apps",activeApps.size.toString(),Icons.Default.Apps,Modifier.weight(1f))
   SummaryCard("Schedule windows",periods.count { it.isEnabled }.toString(),Icons.Default.Schedule,Modifier.weight(1f))
   SummaryCard("Block events",totalBlocks.toString(),Icons.Default.Assessment,Modifier.weight(1f))
  }

  Card {
   Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("Next schedule boundary",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text(nextTransition?.let { "${it.appName} · ${it.transitionDateTime.format(DateTimeFormatter.ofPattern("EEE, h:mm a",Locale.getDefault()))}" } ?: "No upcoming schedule change found",style=MaterialTheme.typography.titleLarge)
    Text("This is a local schedule calculation. Real-device testing is needed to confirm actual enforcement.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedButton(onClick=onNavigateToSchedules) { Text("Open schedules") }
   }
  }

  Text("Quick actions",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   ActionTile("Manage apps","Choose what to manage",Icons.Default.Apps,Modifier.weight(1f),onNavigateToApps)
   ActionTile("Schedule","Edit allowed hours",Icons.Default.CalendarMonth,Modifier.weight(1f),onNavigateToSchedules)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   ActionTile("Templates","Apply a weekly pattern",Icons.Default.Schedule,Modifier.weight(1f),onNavigateToTemplates)
   ActionTile("Diagnostics","Fix setup problems",Icons.Default.Build,Modifier.weight(1f),onNavigateToDiagnostics)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   ActionTile("Activity reports","Review blocked attempts",Icons.Default.Assessment,Modifier.weight(1f),onNavigateToReports)
   ActionTile("Backup & restore","Protect your setup",Icons.Default.Folder,Modifier.weight(1f),onNavigateToBackup)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   ActionTile("Account","Sign in or create account",Icons.Default.Person,Modifier.weight(1f),onNavigateToAccount)
   ActionTile("Upgrade","Purchase options",Icons.Default.Star,Modifier.weight(1f),onNavigateToUpgrade)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   ActionTile("Content protection","Browser and DNS controls",Icons.Default.Security,Modifier.weight(1f),onNavigateToAdultProtection)
   ActionTile("Permissions","Open setup guide",Icons.Default.Lock,Modifier.weight(1f),onNavigateToPermissions)
  }

  Text("Recent activity",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  if(recentBlocks.isEmpty()) {
   Card { Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text("Nothing recorded yet",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    Text("When Strict Blocker records a blocked attempt, it will appear here.",color=MaterialTheme.colorScheme.onSurfaceVariant)
   } }
  } else {
   recentBlocks.take(4).forEach { event ->
    Card { Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
     Icon(Icons.Default.Shield,contentDescription=null,tint=MaterialTheme.colorScheme.primary); Spacer(Modifier.width(12.dp))
     Column(Modifier.weight(1f)) {
      Text(event.appName,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
      Text(event.formattedTime,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
      if(event.reason.isNotBlank()) Text(event.reason,style=MaterialTheme.typography.bodySmall)
     }
    } }
   }
   TextButton(onClick=onNavigateToReports) { Text("View activity report") }
  }
  Text("Strict Blocker works within Android's permission and background-service limits; it cannot guarantee every device and browser state is unbypassable.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

@Composable private fun SummaryCard(title:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier) {
 Card(modifier=modifier) { Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Icon(icon,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
  Text(value,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
  Text(title,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 } }
}

@Composable private fun ActionTile(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier=Modifier,onClick:()->Unit) {
 Card(modifier=modifier.clickable(onClick=onClick),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)) {
  Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   Icon(icon,contentDescription=null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(24.dp))
   Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
   Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
