package com.example.ui.screens.backup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.example.core.backup.BackupCrypto
import com.example.core.model.SchedulePeriod
import com.example.core.system.AlarmScheduler
import com.example.data.db.entity.ManagedAppEntity
import com.example.data.db.entity.SchedulePeriodEntity
import com.example.data.repository.AppBlockerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

@Composable
fun BackupRestoreScreen(repository:AppBlockerRepository,onNavigateBack:()->Unit) {
 val context=LocalContext.current
 val scope= rememberCoroutineScope()
 var passphrase by remember { mutableStateOf("") }
 var status by remember { mutableStateOf("") }
 var working by remember { mutableStateOf(false) }
 var lastError by remember { mutableStateOf(false) }

 val saveLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri:Uri? ->
  if(uri!=null) {
   val passwordAtLaunch=passphrase
   scope.launch {
    working=true; status=""; lastError=false
    runCatching {
     val plain=buildBackupJson(repository)
     val encrypted=BackupCrypto.encrypt(plain,passwordAtLaunch)
     withContext(Dispatchers.IO) {
      context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(encrypted) }
       ?: error("Could not write to the selected location.")
     }
    }.onSuccess { status="Encrypted backup saved. Keep the passphrase safe; it cannot be recovered."; lastError=false }
     .onFailure { status=it.localizedMessage ?: "Backup failed."; lastError=true }
    working=false; passphrase=""
   }
  }
 }
 val restoreLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri:Uri? ->
  if(uri!=null) {
   val passwordAtLaunch=passphrase
   scope.launch {
    working=true; status=""; lastError=false
    runCatching {
     val encrypted=withContext(Dispatchers.IO) {
      context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
       ?: error("Could not read the selected file.")
     }
     require(encrypted.length<5_000_000) { "Backup file is unexpectedly large." }
     val plain=BackupCrypto.decrypt(encrypted,passwordAtLaunch)
     restoreBackupJson(repository,context,plain)
    }.onSuccess { status="Restore complete. Review Diagnostics and test the restored schedules."; lastError=false }
     .onFailure { status=it.localizedMessage ?: "Restore failed. Check the file and passphrase."; lastError=true }
    working=false; passphrase=""
   }
  }
 }

 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically) {
   IconButton(onClick=onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack,contentDescription="Back") }
   Spacer(Modifier.width(8.dp))
   Column { Text("Backup & restore",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold); Text("Transfer your setup securely",color=MaterialTheme.colorScheme.onSurfaceVariant) }
  }
  Card {
   Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Icon(Icons.Default.Lock,contentDescription=null,tint=MaterialTheme.colorScheme.primary)
    Text("Encrypted local backup",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
    Text("Exports managed app names/status, schedule windows and app groups to an AES-GCM encrypted file. Activity history, passwords, payment information and Android permission grants are not included.")
   }
  }
  OutlinedTextField(passphrase,{passphrase=it;status="";lastError=false},label={Text("Backup passphrase (8+ characters)")},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),singleLine=true,enabled=!working,modifier=Modifier.fillMaxWidth())
  Button(onClick={saveLauncher.launch("strict-blocker-backup.sblocker")},enabled=!working && passphrase.length>=8,modifier=Modifier.fillMaxWidth()) { Text("Create encrypted backup") }
  OutlinedButton(onClick={restoreLauncher.launch(arrayOf("*/*"))},enabled=!working && passphrase.length>=8,modifier=Modifier.fillMaxWidth()) { Text("Restore encrypted backup") }
  if(working) Text("Working with local data…",color=MaterialTheme.colorScheme.primary)
  if(status.isNotBlank()) Text(status,color=if(lastError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
  Card {
   Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.Top) {
    Icon(Icons.Default.Security,contentDescription=null)
    Spacer(Modifier.width(10.dp))
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)) {
     Text("Restore safety",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
     Text("Restoring replaces the current managed-app list and schedules. Validate carefully, use only backup files you trust, and check permissions again afterwards. Forgotten passphrases cannot be recovered.")
    }
   }
  }
 }
}

private suspend fun buildBackupJson(repository:AppBlockerRepository):String=withContext(Dispatchers.IO) {
 val apps=repository.managedAppsFlow.first()
 val periods=repository.allPeriodsFlow.first()
 val groups=repository.settings.appGroupsFlow.first()
 val appArray=JSONArray()
 apps.forEach { app -> appArray.put(JSONObject().put("packageName",app.packageName).put("appName",app.appName).put("isManaged",app.isManaged).put("createdAt",app.createdAt)) }
 val scheduleArray=JSONArray()
 periods.forEach { p -> scheduleArray.put(JSONObject().put("appPackage",p.appPackage).put("dayOfWeek",p.dayOfWeek.value).put("startHour",p.startHour).put("startMinute",p.startMinute).put("endHour",p.endHour).put("endMinute",p.endMinute).put("isEnabled",p.isEnabled)) }
 val groupArray=JSONArray().also { array -> groups.forEach { array.put(it) } }
 JSONObject().put("format","strict-blocker-backup").put("schemaVersion",1).put("createdAt",System.currentTimeMillis()).put("apps",appArray).put("periods",scheduleArray).put("appGroups",groupArray).toString(2)
}

private suspend fun restoreBackupJson(repository:AppBlockerRepository,context:Context,json:String)=withContext(Dispatchers.IO) {
 val root=JSONObject(json)
 require(root.optString("format")=="strict-blocker-backup" && root.optInt("schemaVersion")==1) { "Unsupported backup format." }
 val appArray=root.getJSONArray("apps")
 val periodArray=root.getJSONArray("periods")
 require(appArray.length()<=5000 && periodArray.length()<=20000) { "Backup contains too many records." }
 val apps=ArrayList<ManagedAppEntity>(appArray.length())
 val packages=mutableSetOf<String>()
 for(i in 0 until appArray.length()) {
  val item=appArray.getJSONObject(i)
  val pkg=item.getString("packageName").trim()
  val name=item.getString("appName").trim()
  require(pkg.isNotBlank() && name.isNotBlank() && pkg.length<=255 && name.length<=255) { "Invalid app record." }
  require(packages.add(pkg)) { "Backup contains duplicate app entries." }
  apps.add(ManagedAppEntity(pkg,name,item.optBoolean("isManaged",true),item.optLong("createdAt",System.currentTimeMillis())))
 }
 val periods=ArrayList<SchedulePeriodEntity>(periodArray.length())
 for(i in 0 until periodArray.length()) {
  val item=periodArray.getJSONObject(i)
  val pkg=item.getString("appPackage")
  require(pkg in packages) { "A schedule references an app missing from the backup." }
  val day=item.getInt("dayOfWeek")
  require(day in 1..7) { "Invalid day of week." }
  val period=SchedulePeriod(appPackage=pkg,dayOfWeek=DayOfWeek.of(day),startHour=item.getInt("startHour"),startMinute=item.getInt("startMinute"),endHour=item.getInt("endHour"),endMinute=item.getInt("endMinute"),isEnabled=item.optBoolean("isEnabled",true))
  require(!period.isZeroLengthMidnightWindow) { "Backup contains an empty midnight schedule window." }
  periods.add(SchedulePeriodEntity.fromDomain(period))
 }
 val groupArray=root.optJSONArray("appGroups") ?: JSONArray()
 require(groupArray.length()<=500) { "Backup contains too many app groups." }
 val groups=(0 until groupArray.length()).map { groupArray.getString(it) }.toSet()

 // Parse and validate the entire backup before touching existing records.
 repository.database.withTransaction {
  repository.scheduleDao.deleteAll()
  repository.managedAppDao.deleteAll()
  if(apps.isNotEmpty()) repository.managedAppDao.insertAll(apps)
  if(periods.isNotEmpty()) repository.scheduleDao.insertAll(periods)
 }
 repository.settings.setAppGroups(groups)
 AlarmScheduler.scheduleNextTransition(context)
}
