package com.example.ui.screens.pro

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.preferences.SettingsPreferences
import com.example.ui.components.AppSurface
import kotlinx.coroutines.launch

@Composable
fun BlockerCustomizationScreen(
    settings: SettingsPreferences,
    onUpgrade: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPro by settings.isProEntitledFlow.collectAsState(initial = false)
    val style by settings.blockerStyleFlow.collectAsState(initial = "classic")
    val photoUri by settings.blockerPhotoUriFlow.collectAsState(initial = "")

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some document providers do not expose a persistable grant.
        }
        scope.launch {
            settings.setBlockerPhotoUri(uri.toString())
            settings.setBlockerStyle("photo")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Block screen customization",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Choose how Strict Blocker appears when another app is restricted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isPro) {
            item {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(Modifier.height(10.dp))
                        Text("Premium customization", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Custom photos and animated blocker themes are included with Pro.",
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
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Style",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "classic" to "Classic",
                                "pulse" to "Pulse",
                                "focus" to "Focus"
                            ).forEach { (value, label) ->
                                FilterChip(
                                    selected = style == value,
                                    onClick = {
                                        scope.launch { settings.setBlockerStyle(value) }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                AppSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            "Custom photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Use your own photo as a softened full-screen background behind the blocker.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))

                        if (photoUri.isNotBlank()) {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = "Selected blocker photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { photoPicker.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (photoUri.isBlank()) "Choose photo" else "Change photo")
                        }

                        if (photoUri.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        settings.setBlockerPhotoUri(null)
                                        settings.setBlockerStyle("classic")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Remove photo")
                            }
                        }
                    }
                }
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
