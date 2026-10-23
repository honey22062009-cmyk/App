package com.example.ui.screens.reports

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppBlockerRepository

@Composable
fun ReportsScreen(repository:AppBlockerRepository,onNavigateBack:()->Unit) {
 val context=LocalContext.current
 val total by repository.totalBlocksCountFlow.collectAsState(initial=0)
 val recent by repository.recentBlocksFlow.collectAsState(initial=emptyList())
 val adultBlocks by repository.settings.adultBlocksCountFlow.collectAsState(initial=0)
 val since= remember { System.currentTimeMillis()-7L*24*60*60*1000 }
 val lastSevenFlow= remember(repository,since) { repository.blockEventDao.getBlocksSinceFlow(since) }
 val lastSeven by lastSevenFlow.collectAsState(initial=0)
 val topApps= remember(recent) { recent.groupingBy { it.appName }.eachCount().entries.sortedByDescending { it.value }.take(5) }
 val reportText= remember(total,lastSeven,adultBlocks,topApps) {
  buildString {
   appendLine("Strict Blocker activity report")
   appendLine("Last 7 days: $lastSeven recorded block events")
   appendLine("All time: $total recorded block events")
   appendLine("Content-protection counter: $adultBlocks")
   appendLine()
   appendLine("Most frequent apps in latest stored event sample:")
   topApps.forEach { appendLine("- ${it.key}: ${it.value}") }
   appendLine()
   appendLine("Generated locally on the device.")
  }
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   IconButton(onClick=onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Back") }
   Spacer(Modifier.width(8.dp))
   Column { Text("Activity reports",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold); Text("Local summary; no analytics upload",color=MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   MetricCard("Last 7 days",lastSeven.toString(),Modifier.weight(1f))
   MetricCard("All time",total.toString(),Modifier.weight(1f))
  }
  MetricCard("Content filter counter",adultBlocks.toString(),Modifier.fillMaxWidth())
  Card {
   Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text("Most frequent apps in recent history",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    if(topApps.isEmpty()) Text("No blocked attempts recorded yet. They'll appear here as Strict Blocker records events.",color=MaterialTheme.colorScheme.onSurfaceVariant)
    topApps.forEach { entry -> Row(verticalAlignment=Alignment.CenterVertically) { Text(entry.key,modifier=Modifier.weight(1f),maxLines=1); Text(entry.value.toString(),fontWeight=FontWeight.Bold) } }
    if(topApps.isNotEmpty()) Text("The ranking uses the latest stored event sample, not a complete lifetime ranking.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  Card {
   Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Text("Recent blocked attempts",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
    if(recent.isEmpty()) Text("No blocked attempts recorded yet.")
    recent.take(12).forEach { event -> Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
     Text(event.appName,fontWeight=FontWeight.Medium)
     Text(event.formattedTime,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     if(event.reason.isNotBlank()) Text(event.reason,style=MaterialTheme.typography.bodySmall)
    } }
   }
  }
  Button(onClick={
   val intent=Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_SUBJECT,"Strict Blocker activity report"); putExtra(Intent.EXTRA_TEXT,reportText) }
   context.startActivity(Intent.createChooser(intent,"Share activity report"))
  },modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Share,contentDescription=null); Spacer(Modifier.width(8.dp)); Text("Share summary") }
  Text("The report is shared only after you choose an app. It never leaves this device unless you share it.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
@Composable private fun MetricCard(label:String,value:String,modifier:Modifier=Modifier) {
 Card(modifier=modifier,colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.primaryContainer)) {
  Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
   Icon(Icons.Default.BarChart,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
   Text(value,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
   Text(label,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
