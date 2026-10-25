package com.example.ui.screens.account

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.GoogleAuthManager
import com.example.data.preferences.SettingsPreferences
import com.example.ui.components.AppSurface
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(
    settings: SettingsPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val authManager = remember(context, settings) {
        GoogleAuthManager(context.applicationContext, settings)
    }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val signedIn by settings.isAccountSignedInFlow.collectAsState(initial = false)
    val email by settings.accountEmailFlow.collectAsState(initial = "")
    val displayName by settings.accountDisplayNameFlow.collectAsState(initial = "")
    val photoUri by settings.accountPhotoUriFlow.collectAsState(initial = "")

    Column(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(snackbar)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Use your Google account for identity and future synced features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (photoUri.isNotBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(76.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = when {
                                signedIn && displayName.isNotBlank() -> displayName
                                displayName.isNotBlank() -> "Welcome back, " + displayName
                                signedIn -> "Google account"
                                email.isNotBlank() -> "Welcome back"
                                else -> "No account connected"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )

                        if (email.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        if (!signedIn) {
                            Button(
                                onClick = {
                                    val host = activity
                                    if (host == null) {
                                        scope.launch {
                                            snackbar.showSnackbar("Unable to open Google Sign-In.")
                                        }
                                    } else {
                                        scope.launch {
                                            val result = authManager.signIn(host)
                                            if (result.isSuccess) {
                                                snackbar.showSnackbar("Signed in with Google.")
                                            } else {
                                                snackbar.showSnackbar(
                                                    result.exceptionOrNull()?.message
                                                        ?: "Google Sign-In failed."
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (email.isBlank()) "Continue with Google" else "Sign in again with Google")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        authManager.signOut()
                                        snackbar.showSnackbar(
                                            "Signed out. Your account name is remembered for easier sign-in."
                                        )
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
            }

            if (!signedIn && email.isNotBlank()) {
                item {
                    AppSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Remembered account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Strict Blocker remembers only the account display identity after logout so the next sign-in feels familiar. It does not keep you authenticated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
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
                    }
                }
            }

            item {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Privacy",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Your remembered account identity is stored on this device. Purchase access remains controlled by Google Play Billing. Cloud sync is not enabled until a verified backend is added.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
