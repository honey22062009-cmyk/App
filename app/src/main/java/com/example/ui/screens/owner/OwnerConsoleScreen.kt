package com.example.ui.screens.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.preferences.SettingsPreferences
import com.example.payments.ManualPaymentManager
import com.example.payments.PaymentRequest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun OwnerConsoleScreen(
    settings: SettingsPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember(context, settings) {
        ManualPaymentManager(context.applicationContext, settings)
    }
    DisposableEffect(manager) {
        onDispose { manager.close() }
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf(ManualPaymentManager.OWNER_EMAIL) }
    var password by remember { mutableStateOf("") }
    var signedIn by remember { mutableStateOf(false) }
    var signingIn by remember { mutableStateOf(false) }
    var requests by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Owner payment console",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Verify the payment in your UPI app before approving.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (!signedIn) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("Owner sign-in", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Owner Firebase email") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            signingIn = true
                            manager.ownerSignIn(email, password) { result ->
                                signingIn = false
                                result.onSuccess {
                                    signedIn = true
                                    manager.observePendingOwnerRequests(
                                        onUpdate = { requests = it },
                                        onError = { error ->
                                            scope.launch {
                                                snackbar.showSnackbar(error.message ?: "Could not load payment requests.")
                                            }
                                        }
                                    )
                                }.onFailure { error ->
                                    scope.launch {
                                        snackbar.showSnackbar(error.message ?: "Owner sign-in failed.")
                                    }
                                }
                            }
                        },
                        enabled = !signingIn && email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (signingIn) "Signing in…" else "Open owner console")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (requests.isEmpty()) "No pending payments" else "${requests.size} pending payment(s)",
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = {
                        manager.ownerSignOut()
                        signedIn = false
                        requests = emptyList()
                        password = ""
                    }
                ) {
                    Text("Sign out")
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(requests, key = { it.id }) { request ->
                    PaymentRequestCard(
                        request = request,
                        onApprove = {
                            manager.approve(request) { result ->
                                result.onSuccess {
                                    scope.launch {
                                        snackbar.showSnackbar("Approved. The user's app can unlock Pro now.")
                                    }
                                }.onFailure { error ->
                                    scope.launch {
                                        snackbar.showSnackbar(error.message ?: "Approval failed.")
                                    }
                                }
                            }
                        },
                        onReject = {
                            manager.reject(request) { result ->
                                result.onFailure { error ->
                                    scope.launch {
                                        snackbar.showSnackbar(error.message ?: "Could not reject request.")
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        SnackbarHost(snackbar)
    }
}

@Composable
private fun PaymentRequestCard(
    request: PaymentRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    request.plan?.label ?: request.planId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("₹${request.amountRupees}", fontWeight = FontWeight.Bold)
            }

            Text("Request: ${request.id}", style = MaterialTheme.typography.bodySmall)
            Text(
                "UPI transaction ID / UTR: ${request.transactionId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (request.createdAtMillis > 0L) {
                Text(
                    "Submitted: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(request.createdAtMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Check this transaction in your UPI app first. Approval cannot verify the bank payment automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onApprove, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 3.dp))
                    Text("Approve")
                }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 3.dp))
                    Text("Reject")
                }
            }
        }
    }
}
