package com.example.ui.screens.pro

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.preferences.SettingsPreferences
import com.example.payments.OwnerAccess
import com.example.payments.ManualPaymentManager
import com.example.payments.ManualPlan
import com.example.payments.PaymentRequest
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassVariant
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
    onOpenCustomization: () -> Unit,
    onOpenInsights: () -> Unit = {},
    onOpenProgress: () -> Unit = {}
) {
    ManualPaywallContent(settings, onBack, onOpenCustomization, false, onOpenInsights, onOpenProgress)
}

@Composable
private fun ManualPaywallContent(
    settings: SettingsPreferences,
    onDismiss: () -> Unit,
    onOpenCustomization: () -> Unit,
    inDialog: Boolean = true,
    onOpenInsights: () -> Unit = {},
    onOpenProgress: () -> Unit = {}
) {
    val context = LocalContext.current
    val manager = remember(context, settings) {
        ManualPaymentManager(context.applicationContext, settings)
    }
    DisposableEffect(manager) {
        onDispose { manager.close() }
    }

    val ownerAccess by OwnerAccess.hasAccess.collectAsState()
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

    ProContainer(inDialog, onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .widthIn(max = 820.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                variant = GlassVariant.Elevated,
                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f),
                elevation = 18.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (ownerAccess) "Pro · Owner access" else if (isPro) "Your Pro workspace" else "Meet Strict Blocker Pro",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isPro) {
                                    if (ownerAccess) "Every Pro feature is available to your verified owner account. No payment needed." else "Pro is active. Open your premium tools below."
                                } else {
                                    "All in this app. No separate Pro download. Compare the features, then choose a plan below."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (inDialog) IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (!isPro) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            variant = GlassVariant.Accent,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFFE45B99),
                                                    Color(0xFFF4866B),
                                                    Color(0xFFA953C7)
                                                )
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "Make blocking feel personal",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Custom visuals, richer insights and more control.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.heightIn(max = if (inDialog) 560.dp else 1600.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PremiumFeature("Custom blocker photos")
                                    PremiumFeature("Animated blocker styles")
                                    PremiumFeature("Focus insights and trends")
                                    PremiumFeature("Up to 8 app groups")
                                    PremiumFeature("Customize your Progress cards")
                                    Text("Free includes daily recording, all six companions, Browser Guard and basic schedules.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (isPro) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
                                ) {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("Premium active", fontWeight = FontWeight.Bold)
                                        if (expiresAt > 0L) {
                                            Text(
                                                "Expires automatically at the end of the approved plan period.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                FilledTonalButton(onClick = onOpenInsights, modifier = Modifier.fillMaxWidth()) { Text("Open Pro insights") }
                            }
                            item {
                                FilledTonalButton(onClick = onOpenProgress, modifier = Modifier.fillMaxWidth()) { Text("Personalize Progress cards") }
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
                                    subtitle = "30 days · flexible",
                                    price = "₹39",
                                    icon = Icons.Default.CalendarMonth,
                                    badge = null,
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
                                    subtitle = "365 days · one approval",
                                    price = "₹599",
                                    icon = Icons.Default.Savings,
                                    badge = "FULL YEAR",
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
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        variant = GlassVariant.Accent,
                                        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(Color(0xFFE45B99), Color(0xFFF4866B))
                                                            ),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Column {
                                                    Text("Pay securely by UPI", fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "Amount and payment reference are filled automatically.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                "UPI ID: ${ManualPaymentManager.UPI_ID}",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (requestId.isNotBlank()) {
                                                Text(
                                                    "Payment reference: $requestId",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    modifier = Modifier.weight(1f),
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
                                                    modifier = Modifier.weight(1f),
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

                                            Text(
                                                "If your UPI app or bank rejects the payment or shows an account/payment limit, " +
                                                    "that message comes from the payment provider. Strict Blocker does not count or cap UPI attempts. " +
                                                    "You can return here and retry without creating a fake success state.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

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
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (ownerAccess) "Owner access is verified against your signed-in admin account." else "UPI requests are manually reviewed. Pro unlocks only after approval; payment submission is not instant activation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun ProContainer(inDialog: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    if (inDialog) Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.padding(vertical = 24.dp).verticalScroll(rememberScrollState())) { content() }
    } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 900.dp).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = 136.dp)) { content() }
    }
}

@Composable
private fun PlanOption(
    title: String,
    subtitle: String,
    price: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        variant = if (selected) GlassVariant.Accent else GlassVariant.Standard,
        backgroundColor = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (selected) {
                            Brush.linearGradient(
                                listOf(Color(0xFFE45B99), Color(0xFFF4866B), Color(0xFFA953C7))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                badge,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumFeature(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
