package com.example.ui.screens.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.billing.ProBillingManager
import com.example.billing.ProPlan
import com.example.data.preferences.SettingsPreferences
import kotlinx.coroutines.launch

@Composable
fun PaywallDialog(
    settings: SettingsPreferences,
    onDismiss: () -> Unit,
    onOpenCustomization: () -> Unit
) {
    val context = LocalContext.current
    val billingManager = remember(context, settings) {
        ProBillingManager(context.applicationContext, settings)
    }
    DisposableEffect(billingManager) {
        onDispose { billingManager.close() }
    }
    PaywallDialogContent(
        billingManager = billingManager,
        settings = settings,
        onDismiss = onDismiss,
        onOpenCustomization = onOpenCustomization
    )
}

@Composable
private fun PaywallDialogContent(
    billingManager: ProBillingManager,
    settings: SettingsPreferences,
    onDismiss: () -> Unit,
    onOpenCustomization: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val plans by billingManager.plans.collectAsState()
    val ready by billingManager.isReady.collectAsState()
    val message by billingManager.message.collectAsState()
    val isPro by settings.isProEntitledFlow.collectAsState(initial = false)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        message?.let {
            // Billing availability is reflected inline below. Do not throw a large
            // snackbar over the purchase dialog for normal unavailable states.
            if (!it.contains("unavailable", ignoreCase = true) &&
                !it.contains("not available", ignoreCase = true)
            ) {
                snackbar.showSnackbar(it)
            }
            billingManager.clearMessage()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                                "Personalization, insights and expanded groups. Core blocking stays available without Pro.",
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
                        modifier = Modifier.heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
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
                                Button(
                                    onClick = onOpenCustomization,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Customize blocker")
                                }
                            }
                        } else if (!ready && plans.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF1A1F29),
                                    contentColor = Color(0xFFF4F6FC)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF91A7FF))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Google Play checkout isn't ready", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Install this app through a configured Google Play test/release track to load live prices and checkout.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB8C0D4)
                                        )
                                    }
                                }
                            }
                        } else if (plans.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF1A1F29),
                                    contentColor = Color(0xFFF4F6FC)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF91A7FF))
                                        Spacer(Modifier.height(6.dp))
                                        Text("Plans are not available yet", fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Google Play products still need to be configured for this app.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFB8C0D4)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(plans, key = { it.productId }) { plan ->
                                PlanCard(
                                    plan = plan,
                                    recommended = plan.productId == ProBillingManager.YEARLY_PRODUCT_ID,
                                    onBuy = {
                                        if (activity != null) {
                                            billingManager.launchPurchase(activity, plan)
                                        } else {
                                            scope.launch { snackbar.showSnackbar("Unable to open Google Play checkout.") }
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { billingManager.restorePurchases() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Restore purchases")
                            }
                        }

                        if (isPro) {
                            item {
                                OutlinedButton(
                                    onClick = {
                                        val uri = Uri.parse(
                                            "https://play.google.com/store/account/subscriptions?package=" + context.packageName
                                        )
                                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Manage subscription")
                                }
                            }
                        }

                        item {
                            Text(
                                "Checkout and subscription management are handled by Google Play.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9AA6C0)
                            )
                        }
                    }

                    SnackbarHost(snackbar)
                }
            }
        }
    }
}

@Composable
fun PaywallScreen(
    billingManager: ProBillingManager,
    settings: SettingsPreferences,
    onBack: () -> Unit,
    onOpenCustomization: () -> Unit
) {
    PaywallDialogContent(
        billingManager = billingManager,
        settings = settings,
        onDismiss = onBack,
        onOpenCustomization = onOpenCustomization
    )
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

@Composable
private fun PlanCard(
    plan: ProPlan,
    recommended: Boolean,
    onBuy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (recommended) Color(0xFF24325E) else Color(0xFF1A1F29),
        contentColor = Color(0xFFF4F6FC)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when {
                        plan.productId == ProBillingManager.LIFETIME_PRODUCT_ID -> "Lifetime · One-time"
                        recommended -> plan.title + " · Best value"
                        else -> plan.title
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF4F6FC)
                )
                Text(
                    plan.price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF4F6FC)
                )
            }
            Button(onClick = onBuy) {
                Text(if (plan.productId == ProBillingManager.LIFETIME_PRODUCT_ID) "Buy" else "Subscribe")
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
