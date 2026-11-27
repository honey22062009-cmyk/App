package com.example.ui.screens.insights

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.AppBlockerRepository
import com.example.ui.components.AppSurface
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun InsightsScreen(
    repository: AppBlockerRepository,
    onUpgrade: () -> Unit,
    onBack: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val zone = remember { ZoneId.systemDefault() }
    val startOfToday = remember {
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
    val sevenDaysAgo = remember(now) { now - 7L * 24L * 60L * 60L * 1000L }
    val thirtyDaysAgo = remember(now) { now - 30L * 24L * 60L * 60L * 1000L }

    val isPro by repository.settings.isProEntitledFlow.collectAsState(initial = false)
    val total by repository.totalBlocksCountFlow.collectAsState(initial = 0)
    val today by repository.blockEventDao.getBlocksSinceFlow(startOfToday).collectAsState(initial = 0)
    val sevenDays by repository.blockEventDao.getBlocksSinceFlow(sevenDaysAgo).collectAsState(initial = 0)
    val thirtyDays by repository.blockEventDao.getBlocksSinceFlow(thirtyDaysAgo).collectAsState(initial = 0)
    val recent by repository.recentBlocksFlow.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Insights",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "See how often Strict Blocker protected your focus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isPro) {
            item {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Pro insights",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Unlock daily and monthly block statistics plus your most-triggered apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = onUpgrade) {
                            Icon(Icons.Default.Star, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("View Pro")
                        }
                    }
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("Today", today.toString(), Modifier.weight(1f))
                    MetricCard("7 days", sevenDays.toString(), Modifier.weight(1f))
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("30 days", thirtyDays.toString(), Modifier.weight(1f))
                    MetricCard("All time", total.toString(), Modifier.weight(1f))
                }
            }

            item {
                val topApps = recent
                    .filter { it.timestamp >= sevenDaysAgo }
                    .groupBy { it.appName }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5)

                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Most blocked this week",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        if (topApps.isEmpty()) {
                            Text(
                                "No blocked launches recorded in the last 7 days.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            topApps.forEachIndexed { index, pair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(26.dp)
                                    )
                                    Text(
                                        text = pair.first,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = pair.second.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Insights are calculated from block events stored locally on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    AppSurface(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
