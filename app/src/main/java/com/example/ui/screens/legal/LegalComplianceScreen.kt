package com.example.ui.screens.legal

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.coroutines.launch

@Composable
fun LegalComplianceScreen(
    repository: AppBlockerRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deletionSuccessMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        text = "Privacy, Terms & Compliance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Full regulatory transparency & data controls",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Deletion feedback banner if clicked
        if (deletionSuccessMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF064E3B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = deletionSuccessMessage!!,
                        color = Color(0xFFA7F3D0),
                        fontSize = 13.sp,
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
                iconTint = Color(0xFF10B981),
                content = "Strict Blocker operates on a strict zero-telemetry, privacy-first principle:\n\n" +
                        "• 100% Local Storage: All application schedules, configuration, and interception events are stored exclusively in the on-device Room SQLite database.\n" +
                        "• Data Minimization: The app only records the minimum necessary attributes (package name, local day/hour/minute schedule ranges, and block event timestamps).\n" +
                        "• Zero Cloud Servers: The application maintains no remote servers, has no network endpoints, and never transmits device state, usage metrics, or metadata off your device.\n" +
                        "• Accessibility Boundaries: The Accessibility Service explicitly configures canRetrieveWindowContent=false. It physically cannot inspect, record, or access keystrokes, passwords, screen text, or messages."
            )
        }

        // Section 2: Terms of Service
        item {
            LegalSectionCard(
                title = "Terms of Service",
                icon = Icons.Default.Info,
                iconTint = Color(0xFF38BDF8),
                content = "By using Strict Blocker, you agree to the following conditions:\n\n" +
                        "1. Purpose: The software is provided as a personal device management and scheduled app-blocking utility.\n" +
                        "2. Honest Android Architecture: The app does not bypass operating system security boundaries. As the physical device owner, you retain control to disable or uninstall the application at any time unless configured under enterprise Device Owner mode.\n" +
                        "3. Disclaimer of Warranties: The software is provided 'as is', without warranty of any kind, express or implied, including fitness for a particular purpose."
            )
        }

        // Section 3: Refund Policy & Fee Transparency
        item {
            LegalSectionCard(
                title = "Refund Policy & Fee Transparency",
                icon = Icons.Default.CheckCircle,
                iconTint = Color(0xFF34D399),
                content = "• Free & Open Source: Strict Blocker is completely free software.\n" +
                        "• No Hidden Fees: There are no subscription fees, in-app purchases, micro-transactions, or locked tier features.\n" +
                        "• No Payment Information: Because the application processes no financial transactions and collects no billing details, refund claims do not apply."
            )
        }

        // Section 4: Cookie Policy & Web Tracker Audit
        item {
            LegalSectionCard(
                title = "Cookie Policy & Tracking Technologies",
                icon = Icons.Default.Info,
                iconTint = Color(0xFFFBBF24),
                content = "• No Cookies: This is a 100% native Android application built with Kotlin and Jetpack Compose. It does not use HTTP cookies, session tokens, or local storage cookies.\n" +
                        "• No Cookie Banner Required: Because zero cookies or web beacons are placed, stored, or transmitted, no cookie consent popups or banner trackers are necessary."
            )
        }

        // Section 5: Third-Party SDK & Advertising Audit
        item {
            LegalSectionCard(
                title = "Third-Party SDK & Code Audit",
                icon = Icons.Default.Security,
                iconTint = Color(0xFF00D2FF),
                content = "A complete audit of our build dependencies confirms:\n\n" +
                        "• Advertising SDKs: NONE (Zero AdMob, Unity, AppLovin, etc.)\n" +
                        "• Analytics SDKs: NONE (Zero Firebase Analytics, Mixpanel, Adjust, etc.)\n" +
                        "• Social Trackers: NONE (Zero Facebook SDK, etc.)\n" +
                        "• Network Calls: NONE (Zero HTTP REST clients active in production)"
            )
        }

        // Section 6: Children's Privacy (COPPA & GDPR-K)
        item {
            LegalSectionCard(
                title = "Children's Privacy & Age Consent",
                icon = Icons.Default.Lock,
                iconTint = Color(0xFFA78BFA),
                content = "Strict Blocker complies with the Children's Online Privacy Protection Act (COPPA) and GDPR Article 8 (GDPR-K):\n\n" +
                        "• Because the application collects zero personally identifiable information (PII) from any user, it does not collect personal data from children under the age of 13 (or under 16 in the EEA).\n" +
                        "• Parents and guardians can safely deploy this tool on children's or school tablets (such as Samsung Galaxy Tab A9+) without privacy risks."
            )
        }

        // Section 7: Anti-Spam & Email Communications Notice
        item {
            LegalSectionCard(
                title = "Communications & Email Policy",
                icon = Icons.Default.Info,
                iconTint = Color(0xFF94A3B8),
                content = "• No Email Harvesting: Strict Blocker does not ask for, store, or process email addresses.\n" +
                        "• No Marketing Emails: The application never sends marketing, promotional, or automated newsletters.\n" +
                        "• Unsubscribe Links: Since no email correspondence is ever initiated, unsubscribe links are not applicable."
            )
        }

        // Section 8: Licensing, Fonts & Business Details
        item {
            LegalSectionCard(
                title = "Publisher Details & Asset Licensing",
                icon = Icons.Default.Info,
                iconTint = Color(0xFF38BDF8),
                content = "• Application: Strict Blocker for Android\n" +
                        "• Package ID: com.aistudio.strictblocker.kzvwpq\n" +
                        "• Typography: Native Android system fonts (Roboto & Sans-Serif)\n" +
                        "• Icons: Material Design 3 Symbols (Licensed under Apache License 2.0)\n" +
                        "• Software License: Open Source (Apache License, Version 2.0)"
            )
        }

        // Section 9: Data Deletion Request (Right to Erasure / GDPR Art. 17)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1018))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Data deletion icon",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Data Deletion Request (Right to Erasure)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFFECDD3)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "In compliance with GDPR Article 17 and CCPA, you have the absolute right to purge all data. Tapping below permanently deletes all managed app lists, custom schedules, block logs, and preferences stored in the on-device database.",
                        fontSize = 12.sp,
                        color = Color(0xFFFDA4AF),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
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
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Confirmation Dialog for Data Deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Confirm Complete Data Erasure",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all data? All managed application rules, schedule time slots, and enforcement history will be immediately and irreversibly wiped from local storage.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteAllUserData()
                            showDeleteConfirmDialog = false
                            deletionSuccessMessage = "All local records have been successfully erased from the database."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}
