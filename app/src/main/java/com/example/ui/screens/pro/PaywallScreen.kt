package com.example.ui.screens.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard

/**
 * Reserved destination for future Pro-area features.
 * This build intentionally contains no paid plans, checkout, subscriptions or payment actions.
 */
@Composable
fun PaywallScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "  Pro workspace",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onBack) { Text("Back") }
        }

        GlassCard(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "A space for what's next.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "This area is being kept for future features. The current app remains usable without a subscription, purchase or payment.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                FutureFeatureRow(Icons.Default.Pets, "Companion studio", "More pet personalities, expressions and interaction styles")
                FutureFeatureRow(Icons.Default.Tune, "Personalisation", "More ways to shape the focus experience")
                FutureFeatureRow(Icons.Default.Insights, "Deeper insights", "Clearer summaries and helpful progress views")
                Spacer(Modifier.height(2.dp))
                Text(
                    "No checkout or payment option is available in this build.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Home") }
            }
        }
    }
}

@Composable
private fun FutureFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier.padding(start = 14.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
