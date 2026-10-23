package com.example.ui.screens.upgrade

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import java.math.BigDecimal
import java.net.URLEncoder

@Composable
fun UpgradeScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val upiId = BuildConfig.PAYMENT_UPI_ID.trim()
    val payee = BuildConfig.PAYMENT_PAYEE_NAME.trim()
    val supportEmail = BuildConfig.PAYMENT_SUPPORT_EMAIL.trim()
    val websiteUrl = BuildConfig.PAYMENT_WEBSITE_URL.trim()
    val price = BuildConfig.PAYMENT_PRICE_INR.toBigDecimalOrNull()
    val validUpi = Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z0-9.-]{2,64}$").matches(upiId)
    val validPrice = price != null && price > BigDecimal.ZERO && price <= BigDecimal("100000")
    val paymentReady = validUpi && payee.isNotBlank() && validPrice
    var reference by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Upgrade to Premium", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Purchase options and manual verification", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Strict Blocker Premium", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (validPrice) "₹${price!!.stripTrailingZeros().toPlainString()}" else "Price not configured", style = MaterialTheme.typography.titleLarge)
                listOf("Local-first schedule management", "Android permission diagnostics", "Encrypted backup and restore", "Updates listed on the official distribution page").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(it)
                    }
                }
                Text("Only advertise Premium benefits that are actually included in the release you deliver.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pay with UPI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (paymentReady) "Confirm the payee and amount inside your UPI app before authorizing payment." else "The payment screen is present, but live checkout is disabled in this build until the genuine receiving UPI ID, verified payee name, and final price are configured.")
                Text("Payee: ${payee.ifBlank { "Not configured" }}")
                Text("UPI ID: ${if (validUpi) upiId else "Not configured"}")
                Button(
                    enabled = paymentReady,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val uri = Uri.Builder().scheme("upi").authority("pay")
                            .appendQueryParameter("pa", upiId).appendQueryParameter("pn", payee)
                            .appendQueryParameter("am", price!!.stripTrailingZeros().toPlainString())
                            .appendQueryParameter("cu", "INR").appendQueryParameter("tn", "Strict Blocker Premium")
                            .build()
                        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "Choose a UPI app")) }
                            .onSuccess { message = "Verify the recipient and amount in your UPI app." }
                            .onFailure { message = "No compatible UPI app was found." }
                    }
                ) { Text("Continue to UPI payment") }
                if (!paymentReady) Text("Configure PAYMENT_UPI_ID, PAYMENT_PAYEE_NAME and PAYMENT_PRICE_INR for a reviewed release build. Never use placeholder payment details.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                if (websiteUrl.isNotBlank()) TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))) }
                        .onFailure { message = "Unable to open payment website." }
                }) { Text("Open payment website") }
            }
        }

        Card {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Already paid? Request manual verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Enter the transaction reference to create an email draft. This app cannot independently confirm a UPI payment.")
                OutlinedTextField(reference, { reference = it.take(120); message = "" }, label = { Text("UPI transaction reference") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    enabled = reference.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!supportEmail.contains("@")) message = "Support email is not configured in this build."
                        else {
                            val subject = URLEncoder.encode("Strict Blocker payment verification", "UTF-8")
                            val body = URLEncoder.encode("Please verify my purchase.\nTransaction reference: $reference\nAmount: ${if (validPrice) "₹${price!!.stripTrailingZeros().toPlainString()}" else "not configured"}", "UTF-8")
                            val uri = Uri.parse("mailto:$supportEmail?subject=$subject&body=$body")
                            runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
                                .onSuccess { message = "Review and send the email draft to request verification." }
                                .onFailure { message = "No email app was found, or the email could not be opened." }
                        }
                    }
                ) { Text("Email transaction reference") }
                if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.primary)
            }
        }

        Card {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Purchase safety", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("A UPI intent or screenshot is not proof of payment. The seller must verify the successful transaction in the receiving account. This build does not automatically unlock Premium; a verified entitlement backend is still required.")
                }
            }
        }
    }
}
