package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.StrictBlockerLogo

@Composable
fun LocalProfileGateScreen(
    existingName: String,
    hasPin: Boolean,
    onCreateProfile: (String, String) -> Unit,
    onUnlock: (String) -> Boolean,
    onAuthenticated: () -> Unit,
    onLockoutRemainingSeconds: () -> Long = { 0L }
) {
    var name by remember(existingName) { mutableStateOf(existingName) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 480.dp).verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StrictBlockerLogo(size = 104.dp)
                Text(
                    if (hasPin) "Welcome back" else if (existingName.isBlank()) "Create your profile" else "Secure your profile",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    if (hasPin) "Sign in to open Strict Blocker." else "Set up the profile this app will remember on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (hasPin) {
                    if (existingName.isNotBlank()) {
                        Text(existingName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6); error = null },
                        label = { Text("6-digit PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null
                    )
                    Button(
                        onClick = {
                            if (onUnlock(pin)) {
                                onAuthenticated()
                            } else {
                                val waitSeconds = onLockoutRemainingSeconds()
                                error = if (waitSeconds > 0L) {
                                    "Too many attempts. Try again in $waitSeconds seconds."
                                } else {
                                    "That PIN is not correct. Try again."
                                }
                            }
                        },
                        enabled = pin.length == 6,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sign in") }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(40); error = null },
                        label = { Text("Your name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter(Char::isDigit).take(6); error = null },
                        label = { Text("Create a 6-digit PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { confirmPin = it.filter(Char::isDigit).take(6); error = null },
                        label = { Text("Confirm PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = error != null && confirmPin != pin
                    )
                    Button(
                        onClick = {
                            when {
                                name.trim().length < 2 -> error = "Enter a name with at least 2 characters."
                                pin.length != 6 -> error = "Your PIN must contain 6 digits."
                                pin != confirmPin -> error = "The PINs do not match."
                                else -> onCreateProfile(name.trim(), pin)
                            }
                        },
                        enabled = name.trim().length >= 2 && pin.length == 6 && confirmPin.length == 6,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Create profile and continue") }
                }

                if (error != null) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    "This is a device-only profile, not a Google or cloud account. Your name and progress stay in this app's local storage. The PIN is an app entry lock, not a replacement for your device lock or data encryption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
