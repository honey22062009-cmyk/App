package com.example.ui.screens.pro

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.billing.ProBillingManager
import com.example.billing.ProPlan
import com.example.data.preferences.SettingsPreferences
import com.example.ui.components.AppSurface
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(
    billingManager: ProBillingManager,
    settings: SettingsPreferences,
    onBack: () -> Unit,
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
            snackbar.showSnackbar(it)
            billingManager.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SnackbarHost(hostState = snackbar)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = if (isPro) "Strict Blocker Pro" else "Upgrade to Strict Blocker Pro",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isPro) {
                        "Your premium blocker customization is unlocked."
                    } else {
                        "Make the block screen yours while keeping the core blocker available for everyone."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumFeature("Custom blocker photos")
                        PremiumFeature("Animated blocker styles")
                        PremiumFeature("Premium focus themes")
                        PremiumFeature("More personalization options")
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
                    AppSurface(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.width(24.dp))
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Connecting to Google Play",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Subscription options will appear when Billing is available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (plans.isEmpty()) {
                item {
                    AppSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(Modifier.height(8.dp))
                            Text("Plans are not available yet", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Create matching subscription products in Google Play Console to enable checkout.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                scope.launch {
                                    snackbar.showSnackbar("Unable to open Google Play checkout.")
                                }
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

            item {
                FilledTonalButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }

            item {
                Text(
                    text = "Subscriptions are managed through Google Play. Prices and billing periods shown here come from your Play Console configuration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(
    plan: ProPlan,
    recommended: Boolean,
    onBuy: () -> Unit
) {
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (recommended) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        borderColor = if (recommended) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (recommended) plan.title + " • Best value" else plan.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = plan.price,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = onBuy) {
                Text("Subscribe")
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
