package com.example.ui.screens.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.auth.GoogleAuthManager
import com.example.data.preferences.SettingsPreferences
import com.example.data.backup.CloudBackupRepository
import com.example.data.db.AppDatabase
import com.example.security.GuardianPolicyManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
fun AccountDialog(
    settings: SettingsPreferences,
    onDismiss: () -> Unit,
    firstRun: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val authManager = remember(context, settings) {
        GoogleAuthManager(context.applicationContext, settings)
    }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val backupRepository = remember(context, settings) {
        CloudBackupRepository(AppDatabase.getInstance(context.applicationContext), settings)
    }
    var recoveryPhrase by remember { mutableStateOf("") }
    var backupBusy by remember { mutableStateOf(false) }

    val signedIn by settings.isAccountSignedInFlow.collectAsState(initial = false)
    val email by settings.accountEmailFlow.collectAsState(initial = "")
    val displayName by settings.accountDisplayNameFlow.collectAsState(initial = "")
    val photoUri by settings.accountPhotoUriFlow.collectAsState(initial = "")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF12151C),
                contentColor = Color(0xFFF4F6FC),
                shadowElevation = 18.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (firstRun) "Welcome to Strict Blocker" else "Google account",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF4F6FC)
                            )
                            Text(
                                if (firstRun) {
                                    "Connect your account now, or skip and finish setup first."
                                } else {
                                    "Use your Google account to securely back up and restore your protection setup."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB8C0D4)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFCAD2E8))
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFF1A1F29),
                        contentColor = Color(0xFFF4F6FC)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (photoUri.isNotBlank()) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.size(68.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(68.dp),
                                    tint = Color(0xFF91A7FF)
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                when {
                                    signedIn && displayName.isNotBlank() -> displayName
                                    signedIn -> "Google account connected"
                                    email.isNotBlank() -> "Welcome back"
                                    else -> "No account connected"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F6FC),
                                textAlign = TextAlign.Center
                            )
                            if (email.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFB8C0D4),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            if (!signedIn) {
                                Button(
                                    onClick = {
                                        val host = activity
                                        if (host == null) {
                                            scope.launch { snackbar.showSnackbar("Unable to open Google Sign-In.") }
                                        } else {
                                            scope.launch {
                                                val result = authManager.signIn(host)
                                                if (result.isSuccess) {
                                                    snackbar.showSnackbar("Google account connected.")
                                                } else {
                                                    val raw = result.exceptionOrNull()?.message.orEmpty()
                                                    val friendly = if (raw.contains("GOOGLE_WEB_CLIENT_ID", ignoreCase = true) ||
                                                        raw.contains("not configured", ignoreCase = true)
                                                    ) {
                                                        "Google Sign-In is not connected in this build yet."
                                                    } else {
                                                        raw.ifBlank { "Google Sign-In failed. Please try again." }
                                                    }
                                                    snackbar.showSnackbar(friendly)
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Login, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (email.isBlank()) "Continue with Google" else "Sign in again")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            authManager.signOut()
                                            snackbar.showSnackbar("Signed out.")
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Logout, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Sign out")
                                }
                            }
                        }
                    }

                    if (signedIn) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1A1F29),
                            contentColor = Color(0xFFF4F6FC)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Encrypted cloud backup", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Backs up app settings, schedules, managed-app selections and recent protection history. A recovery phrase encrypts the backup on this device and is never uploaded.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB8C0D4)
                                )
                                OutlinedTextField(
                                    value = recoveryPhrase,
                                    onValueChange = { recoveryPhrase = it.take(128) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Guardian recovery phrase") },
                                    placeholder = { Text("At least 12 characters") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            backupBusy = true
                                            scope.launch {
                                                val result = backupRepository.createOrUpdateBackup(recoveryPhrase.toCharArray())
                                                backupBusy = false
                                                recoveryPhrase = ""
                                                snackbar.showSnackbar(
                                                    result.fold(
                                                        onSuccess = { "Encrypted backup saved." },
                                                        onFailure = { it.message ?: "Backup failed." }
                                                    )
                                                )
                                            }
                                        },
                                        enabled = !backupBusy && recoveryPhrase.length >= 12,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Back up") }
                                    OutlinedButton(
                                        onClick = {
                                            backupBusy = true
                                            scope.launch {
                                                val result = backupRepository.restoreBackup(recoveryPhrase.toCharArray())
                                                backupBusy = false
                                                recoveryPhrase = ""
                                                if (result.isSuccess) {
                                                    val managed = AppDatabase.getInstance(context).managedAppDao()
                                                        .getAllManagedAppsFlow().first()
                                                        .filter { it.isManaged }
                                                        .map { it.packageName }
                                                        .toSet()
                                                    val guardian = GuardianPolicyManager(context).applyRestrictions(managed)
                                                    snackbar.showSnackbar(
                                                        if (guardian.isDeviceOwner) {
                                                            "Backup restored. Guardian restrictions re-applied."
                                                        } else {
                                                            "Backup restored. Guardian device-owner protection is not active on this device."
                                                        }
                                                    )
                                                } else {
                                                    snackbar.showSnackbar(result.exceptionOrNull()?.message ?: "Restore failed.")
                                                }
                                            }
                                        },
                                        enabled = !backupBusy && recoveryPhrase.length >= 12,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Restore") }
                                }
                                if (backupBusy) {
                                    Text("Working securely…", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    } else {
                        Text(
                            "Connect an account to enable encrypted backup and restore. Your recovery phrase is never stored by the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9AA6C0)
                        )
                    }

                    if (!signedIn && email.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    authManager.forgetRememberedAccount()
                                    snackbar.showSnackbar("Remembered account cleared.")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Forget remembered account")
                        }
                    }

                    FilledTonalButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (firstRun && !signedIn) "Continue setup without account" else "Done")
                    }

                    SnackbarHost(snackbar)
                }
            }
        }
    }
}

@Composable
fun AccountScreen(
    settings: SettingsPreferences,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AccountDialog(settings = settings, onDismiss = onBack)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
