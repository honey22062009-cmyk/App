package com.example.ui.screens.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch

@Composable
fun LegalComplianceScreen(
    repository: AppBlockerRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deletionSuccessMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        val horizontalPadding = if (isTablet) 32.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = 14.dp)
                .testTag("legal_compliance_screen_root"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Privacy & Legal Compliance",
                            style = if (isTablet) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Local-first blocking, Firebase payment verification, and data controls",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Deletion feedback banner if clicked
            if (deletionSuccessMessage != null) {
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                        backgroundColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = deletionSuccessMessage!!,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // Section 1: Privacy Policy & Data Minimization
            item {
                LegalSectionCard(
                    title = "Privacy Policy & Data Minimization",
                    icon = Icons.Default.Security,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    content = "Strict Blocker is local-first for blocking data, with a limited Firebase connection for manual Pro payments:\n\n" +
                            "• Local blocking data: Managed apps, schedules, preferences, and block history stay on the device through Room/DataStore.\n" +
                            "• Manual payment records: When a user requests Pro activation, Firebase Authentication and Cloud Firestore store the payment request ID, selected plan, amount, UPI transaction reference, a one-way device hash, request status, and timestamps needed for approval.\n" +
                            "• No analytics or advertising: Firebase Analytics and advertising SDKs are not used by this payment flow.\n" +
                            "• Accessibility boundaries: Browser protection may inspect visible accessibility text in supported browser contexts. Password fields and unrelated editable values are excluded, and page text is not uploaded to Firebase."
                )
            }

            // Section 2: Terms of Service
            item {
                LegalSectionCard(
                    title = "Terms of Service",
                    icon = Icons.Default.Info,
                    iconTint = MaterialTheme.colorScheme.primary,
                    content = "By using Strict Blocker, you agree to the following conditions:\n\n" +
                            "1. Purpose: The software is provided as a personal device management and scheduled app-blocking utility.\n" +
                            "2. Honest Android Architecture: The app does not bypass operating system security boundaries. As the physical device owner, you retain control to disable or uninstall the application at any time unless configured under enterprise Device Owner mode.\n" +
                            "3. Disclaimer of Warranties: The software is provided 'as is', without warranty of any kind, express or implied, including fitness for a particular purpose."
                )
            }

            // Section 3: Free & Open Architecture
            item {
                LegalSectionCard(
                    title = "Pricing & Manual Payment Verification",
                    icon = Icons.Default.CheckCircle,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    content = "• Free core features remain available without Pro.\n" +
                            "• Pro plans shown in the app are ₹39 for 30 days and ₹599 for 365 days.\n" +
                            "• Payments are sent through the user's chosen UPI app. Strict Blocker does not receive card numbers, bank passwords, or UPI PINs.\n" +
                            "• The owner manually verifies the submitted UPI transaction reference before approving Pro. Approval is not automatic bank verification.\n" +
                            "• Refund terms and a monitored support contact should be provided before public commercial distribution."
                )
            }

            // Section 4: Cookie Policy & Web Tracker Audit
            item {
                LegalSectionCard(
                    title = "Cookie Policy & Tracking Technologies",
                    icon = Icons.Default.Info,
                    iconTint = Color(0xFFF59E0B),
                    content = "• No Cookies: This is a 100% native Android application built with Kotlin and Jetpack Compose. It does not use HTTP cookies, session tokens, or local storage cookies.\n" +
                            "• No Cookie Banner Required: Because zero cookies or web beacons are placed, stored, or transmitted, no cookie consent popups or banner trackers are necessary."
                )
            }

            // Section 5: Third-Party SDK & Code Audit
            item {
                LegalSectionCard(
                    title = "Third-Party SDK & Code Audit",
                    icon = Icons.Default.Security,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    content = "Current production dependencies include:\n\n" +
                            "• Advertising SDKs: NONE.\n" +
                            "• Analytics SDKs: NONE.\n" +
                            "• Firebase Authentication: used for anonymous payment-request identity and the owner's authenticated approval account.\n" +
                            "• Cloud Firestore: used only for manual payment requests, approval status, and Pro entitlement timing.\n" +
                            "• Optional DNS filtering: sends DNS queries to the configured resolver when enabled."
                )
            }

            // Section 6: Children's Privacy (COPPA & GDPR-K)
            item {
                LegalSectionCard(
                    title = "Children's Privacy & Age Consent",
                    icon = Icons.Default.Lock,
                    iconTint = Color(0xFFA78BFA),
                    content = "Strict Blocker is designed to minimize personal data. The manual Pro flow can transmit a payment request reference, UPI transaction reference, plan information, timestamps, and a one-way device hash to Firebase. Public distribution to children or school environments requires an appropriate privacy review, consent model, and legally accurate policy for the regions where the app is offered."
                )
            }

            // Section 7: Data Deletion Request (Right to Erasure / GDPR Art. 17)
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                    backgroundColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Data deletion icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Data Deletion Request (Right to Erasure)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Tapping below deletes managed app lists, schedules, local block logs, and local preferences from this device. Manual payment records stored in Firebase are not deleted by this local reset and must be handled separately by the service owner when required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_delete_all_data")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Permanently Delete All Data & Reset",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Confirmation Dialog for Data Deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(28.dp),
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Complete Data Erasure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all data? All managed application rules, schedule time slots, and enforcement history will be immediately and irreversibly wiped from local storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteAllUserData()
                            showDeleteConfirmDialog = false
                            deletionSuccessMessage = "Local Strict Blocker data has been erased from this device. Firebase payment records, if any, are separate."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LegalSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: String
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}
