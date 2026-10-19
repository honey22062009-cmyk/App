package com.example.ui.blocker

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.MainActivity
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.theme.StrictBlockerTheme

class BlockerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_NEXT_ALLOWED = "extra_next_allowed"

        const val EXTRA_BLOCK_TYPE = "extra_block_type"
        const val EXTRA_ADULT_LAYER = "extra_adult_layer"
        const val EXTRA_ADULT_REASON = "extra_adult_reason"

        const val BLOCK_TYPE_SCHEDULE = "SCHEDULE"
        const val BLOCK_TYPE_ADULT = "ADULT_CONTENT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App"
        val nextAllowed = intent.getStringExtra(EXTRA_NEXT_ALLOWED) ?: getString(R.string.blocker_no_upcoming)
        val blockType = intent.getStringExtra(EXTRA_BLOCK_TYPE) ?: BLOCK_TYPE_SCHEDULE
        val adultLayer = intent.getStringExtra(EXTRA_ADULT_LAYER) ?: "18+ Adult Protection Shield"
        val adultReason = intent.getStringExtra(EXTRA_ADULT_REASON) ?: "18+ Adult Content Blocked"

        val appIcon: Drawable? = try {
            if (packageName.isNotEmpty()) packageManager.getApplicationIcon(packageName) else null
        } catch (_: Exception) {
            null
        }

        setContent {
            StrictBlockerTheme(darkTheme = true) {
                // Intercept hardware/gesture back to always exit to Home screen
                BackHandler {
                    navigateHome()
                }

                if (blockType == BLOCK_TYPE_ADULT) {
                    AdultBlockerScreen(
                        appName = appName,
                        layer = adultLayer,
                        reason = adultReason,
                        onGoHome = { navigateHome() }
                    )
                } else {
                    BlockerScreen(
                        appName = appName,
                        packageName = packageName,
                        nextAllowed = nextAllowed,
                        appIcon = appIcon,
                        onGoHome = { navigateHome() },
                        onViewSchedule = { navigateToSchedule(packageName) }
                    )
                }
            }
        }
    }

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(homeIntent)
        finish()
    }

    private fun navigateToSchedule(packageName: String) {
        val scheduleIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TARGET_PACKAGE, packageName)
        }
        startActivity(scheduleIntent)
        finish()
    }
}

/**
 * Clean, neutral, non-shaming 18+ Adult Content Block Screen.
 * Never displays explicit text, search queries, or thumbnails.
 */
@Composable
fun AdultBlockerScreen(
    appName: String,
    layer: String,
    reason: String,
    onGoHome: () -> Unit
) {
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090D16),
                Color(0xFF0F172A),
                Color(0xFF070A10)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Calm, clean 18+ Shield Emblem
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "18+",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "BLOCKED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Content Restricted",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("blocker_screen_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "18+ Adult Material Blocked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Access to this page, explicit search query, or unmonitored private window was restricted under 18 Blocker protection.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enforcement Layer Info Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF111827).copy(alpha = 0.9f),
                borderColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enforcement Shield",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF94A3B8)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0284C7).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ON-DEVICE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = layer,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Detected in $appName. Content was intercepted locally before rendering.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Single Non-Bypass Action Button
            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("blocker_btn_home"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Close & Return to Home",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun BlockerScreen(
    appName: String,
    packageName: String,
    nextAllowed: String,
    appIcon: Drawable?,
    onGoHome: () -> Unit,
    onViewSchedule: () -> Unit
) {
    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF090D16),
                Color(0xFF0D1424),
                Color(0xFF070A10)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Calm, refined Lock Icon with subtle glow ring
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Shield Locked",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Calm Headline
            Text(
                text = stringResource(R.string.blocker_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("blocker_screen_title")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This application is currently outside its scheduled hours.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App Identity Glass Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF111827).copy(alpha = 0.9f),
                borderColor = Color(0xFF334155).copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (appIcon != null) {
                        val bitmap = remember(appIcon) {
                            try {
                                appIcon.toBitmap(128, 128).asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = appName,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            FallbackIcon()
                        }
                    } else {
                        FallbackIcon()
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (packageName.isNotEmpty()) {
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Next Availability Window Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF0F172A).copy(alpha = 0.85f),
                borderColor = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.blocker_available_again),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = nextAllowed,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Button(
                onClick = onGoHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("blocker_btn_home"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.blocker_btn_home),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onViewSchedule,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("blocker_btn_view_schedule"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE2E8F0)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.blocker_btn_view_schedule),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FallbackIcon() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF334155)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(24.dp)
        )
    }
}
