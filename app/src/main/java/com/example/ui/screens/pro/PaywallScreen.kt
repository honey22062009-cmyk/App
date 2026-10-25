package com.example.ui.screens.pro

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.SettingsPreferences
import com.example.payments.ManualPaymentManager
import com.example.payments.ManualPlan
import com.example.payments.PaymentRequest
import kotlinx.coroutines.launch

@Composable
fun PaywallDialog(
    settings: SettingsPreferences,
    onDismiss: () -> Unit,
    onOpenCustomization: () -> Unit
) {
    ManualPaywallContent(
        settings = settings,
        onDismiss = onDismiss,
        onOpenCustomization = onOpenCustomization
    )
}

@Composable
fun PaywallScreen(
    settings: SettingsPreferences,
    onBack: () -> Unit,
    onOpenCustomization: () -> Unit
) {
    ManualPaywallContent(
        settings = settings,
        onDismiss = onBack,
        onOpenCustomization = onOpenCustomization
    )
}

@Composable
private fun ManualPaywallContent(
    settings: SettingsPreferences,
    onDismiss: () -> Unit,
    onOpenCustomization: () -> Unit
) {
    val context = LocalContext.current
    val manager = remember(context, settings) {
        ManualPaymentManager(context.applicationContext, settings)
    }
    DisposableEffect(manager) {
        onDispose { manager.close() }
    }

    val isPro by settings.isProEntitledFlow.collectAsState(initial = false)
    val expiresAt by settings.proExpiresAtFlow.collectAsState(initial = 0L)
    val savedRequestId by settings.lastPaymentRequestIdFlow.collectAsState(initial = "")
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedPlan by remember { mutableStateOf<ManualPlan?>(null) }
    var requestId by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var submittedRequest by remember { mutableStateOf<PaymentRequest?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(savedRequestId) {
        if (savedRequestId.isNotBlank()) {
            manager.observeUserRequest(
                requestId = savedRequestId,
                onUpdate = { submittedRequest = it },
                onError = { error ->
                    scope.launch {
                        snackbar.showSnackbar(error.message ?: "Could not check payment status.")
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        settings.refreshExpiredProEntitlement()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 620.dp),
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFF12151C),
                contentColor = Color(0xFFF4F6FC),
                shadowElevation = 20.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isPro) "Strict Blocker Pro" else "Upgrade to Pro",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF4F6FC)
                            )
                            Text(
                                if (isPro) {
                                    "Premium is active on this device."
                                } else {
                                    "Pay by UPI. The owner manually verifies the payment before Pro unlocks."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFB8C0D4)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFCAD2E8))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 560.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF1A1F29),
                                contentColor = Color(0xFFF4F6FC)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PremiumFeature("Custom blocker photos")
                                    PremiumFeature("Animated blocker styles")
                                    PremiumFeature("Focus insights and trends")
                                    PremiumFeature("Up to 8 app groups")
                                    PremiumFeature("More personalization")
                                }
                            }
                        }

                        if (isPro) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF1A1F29)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("Premium active", fontWeight = FontWeight.Bold)
                                        if (expiresAt > 0L) {
                                            Text(
                                                "Expires automatically at the end of the approved plan period.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFB8C0D4)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Button(
                                    onClick = onOpenCustomization,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Customize blocker")
                                }
                            }
                        } else {
                            item {
                                PlanOption(
                                    title = "Monthly",
                                    subtitle = "30 days",
                                    price = "₹39",
                                    selected = selectedPlan == ManualPlan.MONTHLY,
                                    onClick = {
                                        selectedPlan = ManualPlan.MONTHLY
                                        requestId = manager.newRequestId()
                                        transactionId = ""
                                    }
                                )
                            }
                            item {
                                PlanOption(
                                    title = "Yearly",
                                    subtitle = "365 days",
                                    price = "₹599",
                                    selected = selectedPlan == ManualPlan.YEARLY,
                                    onClick = {
                                        selectedPlan = ManualPlan.YEARLY
                                        requestId = manager.newRequestId()
                                        transactionId = ""
                                    }
                                )
                            }

                            selectedPlan?.let { plan ->
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFF1A1F29)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("Pay by UPI", fontWeight = FontWeight.Bold)
                                            Text(
                                                "UPI ID: ${ManualPaymentManager.UPI_ID}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (requestId.isNotBlank()) {
                                                Text(
                                                    "Payment reference: $requestId",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFB8C0D4)
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        val uri = Uri.parse(
                                                            "upi://pay?pa=" +
                                                                Uri.encode(ManualPaymentManager.UPI_ID) +
                                                                "&pn=" + Uri.encode("Strict Blocker") +
                                                                "&am=" + plan.amountRupees.toString() + ".00" +
                                                                "&cu=INR" +
                                                                "&tr=" + Uri.encode(requestId) +
                                                                "&tn=" + Uri.encode("Strict Blocker ${plan.label} $requestId")
                                                        )
                                                        runCatching {
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                        }.onFailure {
                                                            scope.launch {
                                                                snackbar.showSnackbar("No UPI app could be opened.")
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Pay ₹${plan.amountRupees}")
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                        clipboard.setPrimaryClip(
                                                            android.content.ClipData.newPlainText("UPI ID", ManualPaymentManager.UPI_ID)
                                                        )
                                                        scope.launch {
                                                            snackbar.showSnackbar("UPI ID copied.")
                                                        }
                                                    }
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Copy UPI")
                                                }
                                            }

                                            OutlinedTextField(
                                                value = transactionId,
                                                onValueChange = { transactionId = it.take(64) },
                                                modifier = Modifier.fillMaxWidth(),
                                                label = { Text("UPI transaction ID / UTR") },
                                                supportingText = {
                                                    Text("After paying, paste the transaction ID shown by your payment app.")
                                                },
                                                singleLine = true
                                            )

                                            Button(
                                                onClick = {
                                                    isSubmitting = true
                                                    manager.submitPaymentRequest(
                                                        requestId = requestId,
                                                        plan = plan,
                                                        transactionId = transactionId
                                                    ) { result ->
                                                        isSubmitting = false
                                                        result.onSuccess {
                                                            manager.observeUserRequest(
                                                                requestId = requestId,
                                                                onUpdate = { submittedRequest = it },
                                                                onError = { error ->
                                                                    scope.launch {
                                                                        snackbar.showSnackbar(error.message ?: "Could not check payment status.")
                                                                    }
                                                                }
                                                            )
                                                            scope.launch {
                                                                snackbar.showSnackbar("Payment sent for owner verification.")
                                                            }
                                                        }.onFailure { error ->
                                                            scope.launch {
                                                                snackbar.showSnackbar(error.message ?: "Could not submit payment.")
                                                            }
                                                        }
                                                    }
                                                },
                                                enabled = !isSubmitting && transactionId.trim().length >= 6,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(if (isSubmitting) "Submitting…" else "I paid · Send for verification")
                                            }
                                        }
                                    }
                                }
                            }

                            submittedRequest?.let { request ->
                                item {
                                    PaymentStatusCard(request)
                                }
                            }
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF91A7FF)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Premium unlocks only after the owner approves the matching payment request.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF9AA6C0)
                                )
                            }
                        }
                    }

                    SnackbarHost(snackbar)
                }
            }
        }
    }
}

@Composable
private fun PlanOption(
    title: String,
    subtitle: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) Color(0xFF24325E) else Color(0xFF1A1F29),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFFF4F6FC))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C0D4))
            }
            Text(price, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentStatusCard(request: PaymentRequest) {
    val (title, body) = when (request.status) {
        "approved" -> "Payment approved" to "Premium is being activated on this device."
        "rejected" -> "Payment not approved" to "Check the transaction ID and contact the owner before submitting again."
        else -> "Waiting for verification" to "The owner will verify the UPI payment manually. Keep this request ID: ${request.id}"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1F29)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C0D4))
        }
    }
}

@Composable
private fun PremiumFeature(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF91A7FF)
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Color(0xFFF4F6FC))
    }
}
