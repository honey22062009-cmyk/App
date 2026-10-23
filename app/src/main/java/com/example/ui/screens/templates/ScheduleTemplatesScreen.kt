package com.example.ui.screens.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.launch

@Composable
fun ScheduleTemplatesScreen(repository:AppBlockerRepository,onNavigateBack:()->Unit,onNavigateToApps:()->Unit) {
 val apps by repository.activeManagedAppsFlow.collectAsState(initial=emptyList())
 val periods by repository.allPeriodsFlow.collectAsState(initial=emptyList())
 val scope= rememberCoroutineScope()
 var selectedPackage by remember { mutableStateOf("") }
 var templateId by remember { mutableStateOf(ScheduleTemplates.all.first().id) }
 var appMenuOpen by remember { mutableStateOf(false) }
 var confirmReplace by remember { mutableStateOf(false) }
 var status by remember { mutableStateOf("") }
 val selectedApp=apps.firstOrNull { it.packageName==selectedPackage } ?: apps.firstOrNull()
 val selectedTemplate=ScheduleTemplates.all.first { it.id==templateId }
 val oldCount=selectedApp?.let { app -> periods.count { it.appPackage==app.packageName } } ?: 0
 LaunchedEffect(apps) { if(apps.isNotEmpty() && apps.none { it.packageName==selectedPackage }) selectedPackage=apps.first().packageName }

 if(confirmReplace) AlertDialog(
  onDismissRequest={confirmReplace=false},
  title={Text("Replace this app's schedule?")},
  text={Text("This will replace all $oldCount existing schedule window(s) for ${selectedApp?.appName ?: "the selected app"} with ${selectedTemplate.title}. Other apps won't change.")},
  confirmButton={ Button(onClick={
   val app=selectedApp; confirmReplace=false
   if(app!=null) scope.launch {
    runCatching { repository.replaceSchedulePeriodsForApp(app.packageName,selectedTemplate.createPeriods(app.packageName)) }
     .onSuccess { status="Applied ${selectedTemplate.title} to ${app.appName}. Review the Schedule screen to confirm the allowed hours." }
     .onFailure { status="Could not apply template: ${it.localizedMessage ?: "unknown error"}" }
   }
  }) { Text("Replace schedule") } },
  dismissButton={ OutlinedButton(onClick={confirmReplace=false}) { Text("Cancel") } }
 )

 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   IconButton(onClick=onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Back") }
   Spacer(Modifier.width(8.dp))
   Column { Text("Schedule templates",style=MaterialTheme.typography.headlineLarge); Text("Create a weekly routine faster",color=MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) {
   Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Text("Important: schedules are allowed windows",style=MaterialTheme.typography.titleMedium)
    Text("The selected app is normally blocked outside its allowed window. Applying a template replaces that app's current windows, so read the time and days before confirming.")
   }
  }
  if(apps.isEmpty()) {
   Card { Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { Text("No apps are managed yet",style=MaterialTheme.typography.titleMedium); Text("Choose an app and enable management first."); Button(onClick=onNavigateToApps) { Text("Choose apps") } } }
  } else {
   Text("1. Choose an app",style=MaterialTheme.typography.titleLarge)
   Box {
    OutlinedButton(onClick={appMenuOpen=true},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.CalendarMonth,contentDescription=null); Spacer(Modifier.width(8.dp)); Text(selectedApp?.appName ?: "Select app") }
    DropdownMenu(expanded=appMenuOpen,onDismissRequest={appMenuOpen=false}) {
     apps.forEach { app -> DropdownMenuItem(text={Text(app.appName)},onClick={selectedPackage=app.packageName;appMenuOpen=false;status=""}) }
    }
   }
   Text("2. Select a template",style=MaterialTheme.typography.titleLarge)
   ScheduleTemplates.all.forEach { template ->
    val chosen=template.id==templateId
    Card(colors=CardDefaults.cardColors(containerColor=if(chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
     Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      Row(verticalAlignment=Alignment.CenterVertically) {
       Text(template.title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
       if(chosen) Icon(Icons.Default.Check,contentDescription="Selected")
       FilterChip(selected=chosen,onClick={templateId=template.id;status=""},label={Text(if(chosen) "Selected" else "Choose")})
      }
      Text(template.timeLabel,style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
      Text("${template.days.size} day(s) per week",style=MaterialTheme.typography.labelMedium)
      Text(template.description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
    }
   }
   Button(onClick={confirmReplace=true},modifier=Modifier.fillMaxWidth()) { Text("Apply selected template") }
  }
  if(status.isNotBlank()) Text(status,color=MaterialTheme.colorScheme.primary)
  Text("Templates are starting points. Review the result in the schedule editor and test blocking on your device.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
